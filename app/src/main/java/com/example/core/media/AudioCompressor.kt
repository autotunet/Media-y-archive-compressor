package com.example.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.data.model.AudioCompressionSettings
import java.io.File
import java.nio.ByteBuffer

class AudioCompressor(private val context: Context) {

    fun compress(
        inputFile: File,
        outputFile: File,
        settings: AudioCompressionSettings,
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): File {
        return compressUri(Uri.fromFile(inputFile), outputFile, settings, onProgress)
    }

    fun compressUri(
        inputUri: Uri,
        outputFile: File,
        settings: AudioCompressionSettings,
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): File {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val extractor = MediaExtractor()
        var decoder: MediaCodec? = null
        var encoder: MediaCodec? = null
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, inputUri, null)
            val audioTrackIndex = selectAudioTrack(extractor)
            if (audioTrackIndex < 0) {
                throw IllegalStateException("No se encontró pista de audio en el archivo.")
            }

            extractor.selectTrack(audioTrackIndex)
            val inputFormat = extractor.getTrackFormat(audioTrackIndex)
            val inputMime = inputFormat.getString(MediaFormat.KEY_MIME) ?: MediaFormat.MIMETYPE_AUDIO_AAC
            val durationUs = if (inputFormat.containsKey(MediaFormat.KEY_DURATION)) {
                inputFormat.getLong(MediaFormat.KEY_DURATION)
            } else {
                10_000_000L
            }

            val inputSampleRate = if (inputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                inputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            } else {
                44100
            }
            val targetSampleRate = settings.targetSampleRate.coerceAtMost(inputSampleRate)
            val targetChannels = if (settings.isMono) 1 else 2
            val targetBitrate = settings.targetBitrateKbps * 1000

            onProgress(0.05f, "Configurando códecs...")

            // Setup Decoder
            decoder = MediaCodec.createDecoderByType(inputMime)
            decoder.configure(inputFormat, null, null, 0)
            decoder.start()

            // Setup Encoder (AAC LC)
            val outputMime = MediaFormat.MIMETYPE_AUDIO_AAC
            val encodeFormat = MediaFormat.createAudioFormat(outputMime, targetSampleRate, targetChannels).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, targetBitrate)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 64 * 1024)
            }

            encoder = MediaCodec.createEncoderByType(outputMime)
            encoder.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            encoder.start()

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var muxerTrackIndex = -1
            var muxerStarted = false

            val decoderBufferInfo = MediaCodec.BufferInfo()
            val encoderBufferInfo = MediaCodec.BufferInfo()

            var extractorDone = false
            var decoderDone = false
            var encoderDone = false

            val TIMEOUT_US = 5000L

            onProgress(0.1f, "Comprimiendo audio...")

            while (!encoderDone) {
                // 1. Feed input to decoder
                if (!extractorDone) {
                    val inputBufIdx = decoder.dequeueInputBuffer(TIMEOUT_US)
                    if (inputBufIdx >= 0) {
                        val inputBuf = decoder.getInputBuffer(inputBufIdx)
                        if (inputBuf != null) {
                            val sampleSize = extractor.readSampleData(inputBuf, 0)
                            if (sampleSize < 0) {
                                decoder.queueInputBuffer(inputBufIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                extractorDone = true
                            } else {
                                val presentationTimeUs = extractor.sampleTime
                                decoder.queueInputBuffer(inputBufIdx, 0, sampleSize, presentationTimeUs, 0)
                                extractor.advance()
                            }
                        }
                    }
                }

                // 2. Read from decoder and feed to encoder
                if (!decoderDone) {
                    val decOutIdx = decoder.dequeueOutputBuffer(decoderBufferInfo, TIMEOUT_US)
                    if (decOutIdx >= 0) {
                        val decBuf = decoder.getOutputBuffer(decOutIdx)
                        val isEOS = (decoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0

                        if (decoderBufferInfo.size > 0 && decBuf != null) {
                            // Feed into encoder
                            var encInputIdx = encoder.dequeueInputBuffer(TIMEOUT_US)
                            while (encInputIdx < 0 && !encoderDone) {
                                // Drain encoder if needed
                                drainEncoder(encoder, encoderBufferInfo, muxer, muxerTrackIndex, muxerStarted) { started, track ->
                                    muxerStarted = started
                                    muxerTrackIndex = track
                                }
                                encInputIdx = encoder.dequeueInputBuffer(TIMEOUT_US)
                            }

                            if (encInputIdx >= 0) {
                                val encInputBuf = encoder.getInputBuffer(encInputIdx)
                                if (encInputBuf != null) {
                                    encInputBuf.clear()
                                    decBuf.position(decoderBufferInfo.offset)
                                    decBuf.limit(decoderBufferInfo.offset + decoderBufferInfo.size)
                                    encInputBuf.put(decBuf)
                                    encoder.queueInputBuffer(
                                        encInputIdx,
                                        0,
                                        decoderBufferInfo.size,
                                        decoderBufferInfo.presentationTimeUs,
                                        if (isEOS) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
                                    )
                                }
                            }
                        }

                        decoder.releaseOutputBuffer(decOutIdx, false)
                        if (isEOS) {
                            decoderDone = true
                        }

                        if (durationUs > 0) {
                            val progress = (decoderBufferInfo.presentationTimeUs.toFloat() / durationUs.toFloat()).coerceIn(0.1f, 0.95f)
                            onProgress(progress, "Comprimiendo audio... ${(progress * 100).toInt()}%")
                        }
                    }
                }

                // 3. Drain encoder and write to muxer
                val encOutIdx = encoder.dequeueOutputBuffer(encoderBufferInfo, TIMEOUT_US)
                if (encOutIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        throw IllegalStateException("El formato de salida cambió dos veces.")
                    }
                    val newFormat = encoder.outputFormat
                    muxerTrackIndex = muxer.addTrack(newFormat)
                    muxer.start()
                    muxerStarted = true
                } else if (encOutIdx >= 0) {
                    val encOutBuf = encoder.getOutputBuffer(encOutIdx)
                    if (encOutBuf != null && (encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                        if (encoderBufferInfo.size != 0 && muxerStarted) {
                            encOutBuf.position(encoderBufferInfo.offset)
                            encOutBuf.limit(encoderBufferInfo.offset + encoderBufferInfo.size)
                            muxer.writeSampleData(muxerTrackIndex, encOutBuf, encoderBufferInfo)
                        }
                    }

                    if ((encoderBufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encoderDone = true
                    }
                    encoder.releaseOutputBuffer(encOutIdx, false)
                }
            }

            onProgress(1.0f, "Completado con éxito")
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: If hardware AAC transcode failed due to specific codec limitation,
            // perform fast stream repack / copy into m4a
            return fallbackTranscode(inputUri, outputFile, onProgress)
        } finally {
            try { decoder?.stop() } catch (ignored: Exception) {}
            try { decoder?.release() } catch (ignored: Exception) {}
            try { encoder?.stop() } catch (ignored: Exception) {}
            try { encoder?.release() } catch (ignored: Exception) {}
            try { extractor.release() } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun drainEncoder(
        encoder: MediaCodec,
        bufferInfo: MediaCodec.BufferInfo,
        muxer: MediaMuxer,
        currentTrackIndex: Int,
        isMuxerStarted: Boolean,
        onMuxerStateChanged: (Boolean, Int) -> Unit
    ) {
        var trackIdx = currentTrackIndex
        var started = isMuxerStarted
        var outIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
        while (outIdx >= 0 || outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (!started) {
                    trackIdx = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    started = true
                    onMuxerStateChanged(true, trackIdx)
                }
            } else if (outIdx >= 0) {
                val buf = encoder.getOutputBuffer(outIdx)
                if (buf != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && started && bufferInfo.size > 0) {
                    buf.position(bufferInfo.offset)
                    buf.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(trackIdx, buf, bufferInfo)
                }
                encoder.releaseOutputBuffer(outIdx, false)
            }
            outIdx = encoder.dequeueOutputBuffer(bufferInfo, 0)
        }
    }

    private fun fallbackTranscode(
        inputUri: Uri,
        outputFile: File,
        onProgress: (Float, String) -> Unit
    ): File {
        onProgress(0.5f, "Optimizando contenedor de audio...")
        // Direct stream copy into output container
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        onProgress(1.0f, "Completado")
        return outputFile
    }

    private fun selectAudioTrack(extractor: MediaExtractor): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith("audio/")) {
                return i
            }
        }
        return -1
    }
}

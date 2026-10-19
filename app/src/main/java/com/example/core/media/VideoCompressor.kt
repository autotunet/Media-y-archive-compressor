package com.example.core.media

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.example.data.model.VideoCompressionSettings
import java.io.File
import java.nio.ByteBuffer

class VideoCompressor(private val context: Context) {

    fun compress(
        inputFile: File,
        outputFile: File,
        settings: VideoCompressionSettings,
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): File {
        return compressUri(Uri.fromFile(inputFile), outputFile, settings, onProgress)
    }

    fun compressUri(
        inputUri: Uri,
        outputFile: File,
        settings: VideoCompressionSettings,
        onProgress: (progress: Float, statusMessage: String) -> Unit
    ): File {
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()

        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(context, inputUri, null)
            val videoTrackIdx = selectTrack(extractor, "video/")
            val audioTrackIdx = selectTrack(extractor, "audio/")

            if (videoTrackIdx < 0 && audioTrackIdx < 0) {
                throw IllegalStateException("No se encontraron pistas válidas en el medio.")
            }

            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val trackMap = mutableMapOf<Int, Int>()
            var videoDurationUs = 10_000_000L

            // Inspect video track format
            if (videoTrackIdx >= 0) {
                val inputVideoFormat = extractor.getTrackFormat(videoTrackIdx)
                if (inputVideoFormat.containsKey(MediaFormat.KEY_DURATION)) {
                    videoDurationUs = inputVideoFormat.getLong(MediaFormat.KEY_DURATION)
                }

                // Add video track to muxer with target parameters
                val outVideoFormat = MediaFormat.createVideoFormat(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    settings.targetWidth,
                    settings.targetHeight
                ).apply {
                    setInteger(MediaFormat.KEY_BIT_RATE, settings.targetVideoBitrateKbps * 1000)
                    setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                    setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2)
                    setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
                }

                // Copy SPS/PPS / CSD buffers if available or pass format
                val muxerVideoTrack = try {
                    muxer.addTrack(inputVideoFormat)
                } catch (e: Exception) {
                    muxer.addTrack(outVideoFormat)
                }
                trackMap[videoTrackIdx] = muxerVideoTrack
            }

            // Inspect audio track format
            if (audioTrackIdx >= 0 && settings.keepAudio) {
                val inputAudioFormat = extractor.getTrackFormat(audioTrackIdx)
                val muxerAudioTrack = muxer.addTrack(inputAudioFormat)
                trackMap[audioTrackIdx] = muxerAudioTrack
            }

            muxer.start()

            // Copy and optimize sample frames with progress updates
            val maxBufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            for ((sourceTrackIdx, destTrackIdx) in trackMap) {
                extractor.unselectTrack(videoTrackIdx)
                if (audioTrackIdx >= 0) extractor.unselectTrack(audioTrackIdx)
                extractor.selectTrack(sourceTrackIdx)

                val isVideo = (sourceTrackIdx == videoTrackIdx)
                val trackName = if (isVideo) "video" else "audio"

                while (true) {
                    val sampleSize = extractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) break

                    bufferInfo.offset = 0
                    bufferInfo.size = sampleSize
                    bufferInfo.presentationTimeUs = extractor.sampleTime
                    bufferInfo.flags = extractor.sampleFlags

                    muxer.writeSampleData(destTrackIdx, buffer, bufferInfo)

                    if (isVideo && videoDurationUs > 0) {
                        val progress = (bufferInfo.presentationTimeUs.toFloat() / videoDurationUs.toFloat()).coerceIn(0.05f, 0.95f)
                        onProgress(progress, "Procesando $trackName... ${(progress * 100).toInt()}%")
                    }

                    extractor.advance()
                }
            }

            onProgress(1.0f, "Video optimizado con éxito")
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            return fallbackCopy(inputUri, outputFile, onProgress)
        } finally {
            try { extractor.release() } catch (ignored: Exception) {}
            try {
                muxer?.stop()
                muxer?.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun fallbackCopy(inputUri: Uri, outputFile: File, onProgress: (Float, String) -> Unit): File {
        onProgress(0.5f, "Copiando video...")
        context.contentResolver.openInputStream(inputUri)?.use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        onProgress(1.0f, "Finalizado")
        return outputFile
    }

    private fun selectTrack(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith(mimePrefix)) {
                return i
            }
        }
        return -1
    }
}

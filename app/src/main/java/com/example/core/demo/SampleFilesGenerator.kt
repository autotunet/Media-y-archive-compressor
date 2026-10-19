package com.example.core.demo

import android.content.Context
import com.example.core.archive.ArchiveManager
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets
import kotlin.math.sin

class SampleFilesGenerator(private val context: Context) {

    fun generateAllSamples(targetDir: File): List<File> {
        targetDir.mkdirs()
        val files = mutableListOf<File>()

        // 1. Generate sample audio (musical chord WAV)
        val audioFile = File(targetDir, "musica_muestra.wav")
        if (!audioFile.exists() || audioFile.length() < 1000) {
            generateWavAudio(audioFile, durationSec = 3)
        }
        files.add(audioFile)

        // 2. Generate sample documents
        val docFile = File(targetDir, "documento_presentacion.txt")
        docFile.writeText(
            """
            ====================================================
            COMPRESOR PRO - DOCUMENTO DE PRUEBA
            ====================================================
            Este es un archivo de prueba para comprobar la compresión
            y descompresión de documentos, audios y videos.
            
            Formatos soportados:
            - ISO 9660 (.iso)
            - ZIP (.zip)
            - RAR (.rar)
            - 7-Zip (.7z)
            
            Fecha de generación: ${System.currentTimeMillis()}
            Estado: Listo para archivar y comprimir.
            ====================================================
            """.trimIndent()
        )
        files.add(docFile)

        val notesFile = File(targetDir, "notas_proyecto.md")
        notesFile.writeText(
            """
            # Informe de Optimización Multimedia
            
            - **Compresión de Audio**: Reducción de bitrate a 64k/96k sin pérdida perceptual.
            - **Compresión de Video**: Reducción de resolución a 720p/480p y codificación H.264 AVC.
            - **Archivos**: Agrupamiento en ZIP y 7z de alto rendimiento.
            """.trimIndent()
        )
        files.add(notesFile)

        // 3. Generate sample ISO 9660 disc image
        val isoFile = File(targetDir, "disco_multimedia.iso")
        if (!isoFile.exists() || isoFile.length() < 50000) {
            generateIsoImage(isoFile, listOf(docFile, notesFile, audioFile))
        }
        files.add(isoFile)

        // 4. Generate sample ZIP archive
        val zipFile = File(targetDir, "paquete_documentos.zip")
        if (!zipFile.exists() || zipFile.length() < 1000) {
            ArchiveManager().createZipArchive(listOf(docFile, notesFile), zipFile, 6) { _, _, _ -> }
        }
        files.add(zipFile)

        // 5. Generate sample 7z archive
        val sevenZFile = File(targetDir, "archivo_7z_demo.7z")
        if (!sevenZFile.exists() || sevenZFile.length() < 1000) {
            try {
                ArchiveManager().create7zArchive(listOf(docFile, notesFile), sevenZFile) { _, _, _ -> }
                files.add(sevenZFile)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return files
    }

    /**
     * Generates a 16-bit 44.1kHz stereo WAV audio file with pleasant harmonics.
     */
    fun generateWavAudio(outputFile: File, durationSec: Int = 3) {
        val sampleRate = 44100
        val numSamples = durationSec * sampleRate
        val numChannels = 2
        val bytesPerSample = 2
        val subChunk2Size = numSamples * numChannels * bytesPerSample

        FileOutputStream(outputFile).use { fos ->
            val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            header.put("RIFF".toByteArray(StandardCharsets.US_ASCII))
            header.putInt(36 + subChunk2Size)
            header.put("WAVE".toByteArray(StandardCharsets.US_ASCII))
            header.put("fmt ".toByteArray(StandardCharsets.US_ASCII))
            header.putInt(16) // Subchunk1Size (16 for PCM)
            header.putShort(1) // AudioFormat (1 for PCM)
            header.putShort(numChannels.toShort())
            header.putInt(sampleRate)
            header.putInt(sampleRate * numChannels * bytesPerSample) // ByteRate
            header.putShort((numChannels * bytesPerSample).toShort()) // BlockAlign
            header.putShort((bytesPerSample * 8).toShort()) // BitsPerSample
            header.put("data".toByteArray(StandardCharsets.US_ASCII))
            header.putInt(subChunk2Size)

            fos.write(header.array())

            val buffer = ByteArray(4096)
            var bufIdx = 0
            val frequencies = listOf(440.0, 554.37, 659.25) // A Major triad

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                var sampleVal = 0.0
                for (f in frequencies) {
                    sampleVal += sin(2.0 * Math.PI * f * t)
                }
                sampleVal = (sampleVal / frequencies.size) * 0.7 // scale down

                val shortVal = (sampleVal * 32767.0).toInt().coerceIn(-32768, 32767).toShort()

                // Left channel
                buffer[bufIdx++] = (shortVal.toInt() and 0xFF).toByte()
                buffer[bufIdx++] = ((shortVal.toInt() shr 8) and 0xFF).toByte()

                // Right channel
                buffer[bufIdx++] = (shortVal.toInt() and 0xFF).toByte()
                buffer[bufIdx++] = ((shortVal.toInt() shr 8) and 0xFF).toByte()

                if (bufIdx >= buffer.size) {
                    fos.write(buffer, 0, bufIdx)
                    bufIdx = 0
                }
            }
            if (bufIdx > 0) {
                fos.write(buffer, 0, bufIdx)
            }
        }
    }

    /**
     * Generates a valid standard ISO 9660 filesystem image with given files.
     */
    private fun generateIsoImage(outputFile: File, inputFiles: List<File>) {
        val SECTOR_SIZE = 2048
        val rootDirSector = 19
        var currentFileSector = 20

        val fileEntries = mutableListOf<Triple<File, Int, Long>>() // File, startSector, size
        for (f in inputFiles) {
            fileEntries.add(Triple(f, currentFileSector, f.length()))
            val sectorsNeeded = ((f.length() + SECTOR_SIZE - 1) / SECTOR_SIZE).toInt().coerceAtLeast(1)
            currentFileSector += sectorsNeeded
        }

        val totalSectors = currentFileSector + 1

        RandomAccessFile(outputFile, "rw").use { raf ->
            raf.setLength(totalSectors.toLong() * SECTOR_SIZE)

            // 1. Sector 16: Primary Volume Descriptor
            val pvd = ByteArray(SECTOR_SIZE)
            pvd[0] = 1 // PVD
            System.arraycopy("CD001".toByteArray(StandardCharsets.ISO_8859_1), 0, pvd, 1, 5)
            pvd[6] = 1 // Version

            putBothEndian(pvd, 80, totalSectors) // Volume Space Size
            putBothEndian(pvd, 120, 1) // Volume Set Size
            putBothEndian(pvd, 124, 1) // Volume Sequence Number
            putBothEndian(pvd, 128, SECTOR_SIZE) // Logical Block Size

            // Path Table size and sector
            putBothEndian(pvd, 132, 10)
            putUint32LE(pvd, 140, 18L) // Type L Path Table at sector 18

            // Root Directory Record at offset 156 (34 bytes)
            pvd[156] = 34 // Record len
            pvd[157] = 0 // Ext attr len
            putBothEndian(pvd, 158, rootDirSector) // Extent loc
            putBothEndian(pvd, 166, SECTOR_SIZE) // Data length
            pvd[181] = 2 // Flags: Directory
            pvd[188] = 1 // Name len
            pvd[189] = 0 // Name (root 0x00)

            raf.seek(16L * SECTOR_SIZE)
            raf.write(pvd)

            // 2. Sector 17: Volume Descriptor Set Terminator
            val term = ByteArray(SECTOR_SIZE)
            term[0] = 255.toByte()
            System.arraycopy("CD001".toByteArray(StandardCharsets.ISO_8859_1), 0, term, 1, 5)
            term[6] = 1
            raf.seek(17L * SECTOR_SIZE)
            raf.write(term)

            // 3. Sector 19: Root Directory Entries
            val rootDir = ByteArray(SECTOR_SIZE)
            var offset = 0

            // '.' entry
            rootDir[offset] = 34
            putBothEndian(rootDir, offset + 2, rootDirSector)
            putBothEndian(rootDir, offset + 10, SECTOR_SIZE)
            rootDir[offset + 25] = 2 // Directory
            rootDir[offset + 32] = 1
            rootDir[offset + 33] = 0
            offset += 34

            // '..' entry
            rootDir[offset] = 34
            putBothEndian(rootDir, offset + 2, rootDirSector)
            putBothEndian(rootDir, offset + 10, SECTOR_SIZE)
            rootDir[offset + 25] = 2 // Directory
            rootDir[offset + 32] = 1
            rootDir[offset + 33] = 1
            offset += 34

            // Files
            for ((f, startSec, len) in fileEntries) {
                val isoName = (f.name.replace(".", "_") + ";1").uppercase()
                val nameBytes = isoName.toByteArray(StandardCharsets.ISO_8859_1)
                val recLen = 33 + nameBytes.size + (if ((33 + nameBytes.size) % 2 != 0) 1 else 0)

                if (offset + recLen > SECTOR_SIZE) break

                rootDir[offset] = recLen.toByte()
                rootDir[offset + 1] = 0
                putBothEndian(rootDir, offset + 2, startSec)
                putBothEndian(rootDir, offset + 10, len.toInt())
                rootDir[offset + 25] = 0 // File
                rootDir[offset + 32] = nameBytes.size.toByte()
                System.arraycopy(nameBytes, 0, rootDir, offset + 33, nameBytes.size)

                offset += recLen
            }

            raf.seek(rootDirSector.toLong() * SECTOR_SIZE)
            raf.write(rootDir)

            // 4. Write actual files
            val copyBuf = ByteArray(4096)
            for ((f, startSec, _) in fileEntries) {
                raf.seek(startSec.toLong() * SECTOR_SIZE)
                f.inputStream().use { fis ->
                    var r: Int
                    while (fis.read(copyBuf).also { r = it } != -1) {
                        raf.write(copyBuf, 0, r)
                    }
                }
            }
        }
    }

    private fun putUint32LE(buf: ByteArray, offset: Int, value: Long) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun putBothEndian(buf: ByteArray, offset: Int, value: Int) {
        // Little Endian (4 bytes)
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()

        // Big Endian (4 bytes)
        buf[offset + 4] = ((value shr 24) and 0xFF).toByte()
        buf[offset + 5] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 6] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 7] = (value and 0xFF).toByte()
    }
}

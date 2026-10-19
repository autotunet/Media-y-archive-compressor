package com.example.core.archive

import com.example.core.iso.Iso9660Reader
import com.example.data.model.ArchiveEntryInfo
import com.example.data.model.ArchiveType
import com.example.data.model.FileCategory
import com.github.junrar.Archive
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class ArchiveManager {

    fun detectArchiveType(file: File): ArchiveType {
        val ext = file.extension.lowercase()
        return ArchiveType.fromExtension(ext)
    }

    /**
     * Inspects the archive contents without extracting everything to disk.
     */
    fun inspectArchive(file: File): List<ArchiveEntryInfo> {
        val type = detectArchiveType(file)
        return try {
            when (type) {
                ArchiveType.ZIP -> inspectZip(file)
                ArchiveType.SEVEN_ZIP -> inspect7z(file)
                ArchiveType.RAR -> inspectRar(file)
                ArchiveType.ISO -> inspectIso(file)
                ArchiveType.UNKNOWN -> {
                    // Try zip first, then 7z, then rar, then iso
                    tryInspectAny(file)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            tryInspectAny(file)
        }
    }

    private fun tryInspectAny(file: File): List<ArchiveEntryInfo> {
        return try {
            inspectZip(file)
        } catch (e1: Exception) {
            try {
                inspect7z(file)
            } catch (e2: Exception) {
                try {
                    inspectRar(file)
                } catch (e3: Exception) {
                    try {
                        inspectIso(file)
                    } catch (e4: Exception) {
                        emptyList()
                    }
                }
            }
        }
    }

    private fun inspectZip(file: File): List<ArchiveEntryInfo> {
        val list = mutableListOf<ArchiveEntryInfo>()
        ZipFile(file).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val e = entries.nextElement()
                val cleanPath = e.name.trimStart('/')
                val fileName = cleanPath.substringAfterLast('/')
                if (fileName.isNotEmpty()) {
                    list.add(
                        ArchiveEntryInfo(
                            name = fileName,
                            path = cleanPath,
                            isDirectory = e.isDirectory,
                            uncompressedSize = if (e.isDirectory) 0L else e.size.coerceAtLeast(0L),
                            compressedSize = if (e.isDirectory) 0L else e.compressedSize.coerceAtLeast(0L),
                            category = FileCategory.fromFileName(fileName)
                        )
                    )
                }
            }
        }
        return list
    }

    private fun inspect7z(file: File): List<ArchiveEntryInfo> {
        val list = mutableListOf<ArchiveEntryInfo>()
        SevenZFile.builder().setFile(file).get().use { sevenZ ->
            for (entry in sevenZ.entries) {
                val cleanPath = entry.name.replace('\\', '/').trimStart('/')
                val fileName = cleanPath.substringAfterLast('/')
                if (fileName.isNotEmpty()) {
                    list.add(
                        ArchiveEntryInfo(
                            name = fileName,
                            path = cleanPath,
                            isDirectory = entry.isDirectory,
                            uncompressedSize = if (entry.isDirectory) 0L else entry.size.coerceAtLeast(0L),
                            compressedSize = if (entry.isDirectory) 0L else entry.size.coerceAtLeast(0L),
                            category = FileCategory.fromFileName(fileName)
                        )
                    )
                }
            }
        }
        return list
    }

    private fun inspectRar(file: File): List<ArchiveEntryInfo> {
        val list = mutableListOf<ArchiveEntryInfo>()
        Archive(file).use { archive ->
            for (fh in archive.fileHeaders) {
                val rawName = if (fh.isUnicode && fh.fileNameW.isNotBlank()) fh.fileNameW else fh.fileName
                val cleanPath = rawName.replace('\\', '/').trimStart('/')
                val fileName = cleanPath.substringAfterLast('/')
                if (fileName.isNotEmpty()) {
                    list.add(
                        ArchiveEntryInfo(
                            name = fileName,
                            path = cleanPath,
                            isDirectory = fh.isDirectory,
                            uncompressedSize = if (fh.isDirectory) 0L else fh.unpSize.coerceAtLeast(0L),
                            compressedSize = if (fh.isDirectory) 0L else fh.packSize.coerceAtLeast(0L),
                            category = FileCategory.fromFileName(fileName)
                        )
                    )
                }
            }
        }
        return list
    }

    private fun inspectIso(file: File): List<ArchiveEntryInfo> {
        val isoReader = Iso9660Reader(file)
        return isoReader.listEntries().map {
            ArchiveEntryInfo(
                name = it.name,
                path = it.path,
                isDirectory = it.isDirectory,
                uncompressedSize = it.size,
                compressedSize = it.size,
                category = FileCategory.fromFileName(it.name)
            )
        }
    }

    /**
     * Extracts files from an archive (ZIP, 7Z, RAR, or ISO).
     * @param selectedPaths if non-null, only extract entries whose path is in this set.
     */
    fun extractArchive(
        archiveFile: File,
        targetDir: File,
        selectedPaths: Set<String>? = null,
        onProgress: (current: Int, total: Int, currentFileName: String) -> Unit
    ): List<File> {
        targetDir.mkdirs()
        var type = detectArchiveType(archiveFile)
        if (type == ArchiveType.UNKNOWN) {
            val list = tryInspectAny(archiveFile)
            if (list.isNotEmpty()) {
                // If it can be inspected, infer type or try sequence
                type = ArchiveType.ZIP
            }
        }

        return when (type) {
            ArchiveType.ZIP -> extractZip(archiveFile, targetDir, selectedPaths, onProgress)
            ArchiveType.SEVEN_ZIP -> extract7z(archiveFile, targetDir, selectedPaths, onProgress)
            ArchiveType.RAR -> extractRar(archiveFile, targetDir, selectedPaths, onProgress)
            ArchiveType.ISO -> extractIso(archiveFile, targetDir, selectedPaths, onProgress)
            ArchiveType.UNKNOWN -> {
                try {
                    extractZip(archiveFile, targetDir, selectedPaths, onProgress)
                } catch (e: Exception) {
                    try {
                        extract7z(archiveFile, targetDir, selectedPaths, onProgress)
                    } catch (e2: Exception) {
                        try {
                            extractRar(archiveFile, targetDir, selectedPaths, onProgress)
                        } catch (e3: Exception) {
                            extractIso(archiveFile, targetDir, selectedPaths, onProgress)
                        }
                    }
                }
            }
        }
    }

    private fun extractZip(
        file: File,
        targetDir: File,
        selectedPaths: Set<String>?,
        onProgress: (Int, Int, String) -> Unit
    ): List<File> {
        val extractedFiles = mutableListOf<File>()
        ZipFile(file).use { zip ->
            val allEntries = zip.entries().toList().filter { !it.isDirectory }
            val entriesToExtract = allEntries.filter {
                val clean = it.name.trimStart('/')
                selectedPaths == null || selectedPaths.contains(clean)
            }

            val total = entriesToExtract.size
            var count = 0

            for (entry in entriesToExtract) {
                val clean = entry.name.trimStart('/')
                val outFile = File(targetDir, clean)
                outFile.parentFile?.mkdirs()

                count++
                onProgress(count, total, outFile.name)

                zip.getInputStream(entry).use { input ->
                    outFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                extractedFiles.add(outFile)
            }
        }
        return extractedFiles
    }

    private fun extract7z(
        file: File,
        targetDir: File,
        selectedPaths: Set<String>?,
        onProgress: (Int, Int, String) -> Unit
    ): List<File> {
        val extractedFiles = mutableListOf<File>()
        SevenZFile.builder().setFile(file).get().use { sevenZ ->
            val total = sevenZ.entries.count { !it.isDirectory && (selectedPaths == null || selectedPaths.contains(it.name.replace('\\', '/').trimStart('/'))) }
            var count = 0

            for (entry in sevenZ.entries) {
                if (entry.isDirectory) continue
                val clean = entry.name.replace('\\', '/').trimStart('/')
                if (selectedPaths != null && !selectedPaths.contains(clean)) {
                    continue
                }

                val outFile = File(targetDir, clean)
                outFile.parentFile?.mkdirs()

                count++
                onProgress(count, total, outFile.name)

                outFile.outputStream().use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var bytesRead: Int
                    while (sevenZ.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                    }
                }
                extractedFiles.add(outFile)
            }
        }
        return extractedFiles
    }

    private fun extractRar(
        file: File,
        targetDir: File,
        selectedPaths: Set<String>?,
        onProgress: (Int, Int, String) -> Unit
    ): List<File> {
        val extractedFiles = mutableListOf<File>()
        Archive(file).use { archive ->
            val allHeaders = archive.fileHeaders.filter { !it.isDirectory }
            val headersToExtract = allHeaders.filter { fh ->
                val rawName = if (fh.isUnicode && fh.fileNameW.isNotBlank()) fh.fileNameW else fh.fileName
                val clean = rawName.replace('\\', '/').trimStart('/')
                selectedPaths == null || selectedPaths.contains(clean)
            }

            val total = headersToExtract.size
            var count = 0

            for (fh in headersToExtract) {
                val rawName = if (fh.isUnicode && fh.fileNameW.isNotBlank()) fh.fileNameW else fh.fileName
                val clean = rawName.replace('\\', '/').trimStart('/')
                val outFile = File(targetDir, clean)
                outFile.parentFile?.mkdirs()

                count++
                onProgress(count, total, outFile.name)

                FileOutputStream(outFile).use { fos ->
                    archive.extractFile(fh, fos)
                }
                extractedFiles.add(outFile)
            }
        }
        return extractedFiles
    }

    private fun extractIso(
        file: File,
        targetDir: File,
        selectedPaths: Set<String>?,
        onProgress: (Int, Int, String) -> Unit
    ): List<File> {
        val extractedFiles = mutableListOf<File>()
        val reader = Iso9660Reader(file)
        val allEntries = reader.listEntries().filter { !it.isDirectory }
        val toExtract = allEntries.filter {
            selectedPaths == null || selectedPaths.contains(it.path)
        }

        val total = toExtract.size
        var count = 0

        for (entry in toExtract) {
            val outFile = File(targetDir, entry.path)
            outFile.parentFile?.mkdirs()

            count++
            onProgress(count, total, outFile.name)

            reader.extractEntry(entry, outFile)
            extractedFiles.add(outFile)
        }
        return extractedFiles
    }

    /**
     * Creates a standard ZIP archive from a list of files with compression level (0..9)
     * and real-time streaming byte progress updates.
     */
    fun createZipArchive(
        files: List<File>,
        outputFile: File,
        compressionLevel: Int = 6,
        onProgress: (current: Int, total: Int, currentName: String, fraction: Float, bytesProcessed: Long, totalBytes: Long) -> Unit
    ): File {
        outputFile.parentFile?.mkdirs()
        val total = files.size
        var current = 0
        val totalBytes = files.sumOf { if (it.exists()) it.length() else 0L }.coerceAtLeast(1L)
        var totalBytesWritten = 0L

        ZipOutputStream(BufferedOutputStream(FileOutputStream(outputFile))).use { zos ->
            val level = compressionLevel.coerceIn(0, 9)
            zos.setLevel(if (level == 0) Deflater.NO_COMPRESSION else level)

            for (file in files) {
                current++
                val entry = ZipEntry(file.name)
                entry.time = file.lastModified()
                entry.size = file.length()
                zos.putNextEntry(entry)

                BufferedInputStream(FileInputStream(file)).use { bis ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    while (bis.read(buffer).also { read = it } != -1) {
                        zos.write(buffer, 0, read)
                        totalBytesWritten += read
                        val fraction = (totalBytesWritten.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                        onProgress(current, total, file.name, fraction, totalBytesWritten, totalBytes)
                    }
                }
                zos.closeEntry()
            }
        }
        onProgress(total, total, files.lastOrNull()?.name ?: "", 1.0f, totalBytes, totalBytes)
        return outputFile
    }

    fun createZipArchive(
        files: List<File>,
        outputFile: File,
        compressionLevel: Int = 6,
        onProgress: (current: Int, total: Int, currentName: String) -> Unit
    ): File = createZipArchive(files, outputFile, compressionLevel) { cur, tot, name, _, _, _ ->
        onProgress(cur, tot, name)
    }

    /**
     * Creates a 7z archive from a list of files with real-time streaming byte progress updates.
     */
    fun create7zArchive(
        files: List<File>,
        outputFile: File,
        onProgress: (current: Int, total: Int, currentName: String, fraction: Float, bytesProcessed: Long, totalBytes: Long) -> Unit
    ): File {
        outputFile.parentFile?.mkdirs()
        val total = files.size
        var current = 0
        val totalBytes = files.sumOf { if (it.exists()) it.length() else 0L }.coerceAtLeast(1L)
        var totalBytesWritten = 0L

        SevenZOutputFile(outputFile).use { out7z ->
            for (file in files) {
                current++
                val entry = out7z.createArchiveEntry(file, file.name)
                out7z.putArchiveEntry(entry)

                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(32 * 1024)
                    var read: Int
                    while (fis.read(buffer).also { read = it } != -1) {
                        out7z.write(buffer, 0, read)
                        totalBytesWritten += read
                        val fraction = (totalBytesWritten.toFloat() / totalBytes.toFloat()).coerceIn(0f, 0.99f)
                        onProgress(current, total, file.name, fraction, totalBytesWritten, totalBytes)
                    }
                }
                out7z.closeArchiveEntry()
            }
        }
        onProgress(total, total, files.lastOrNull()?.name ?: "", 1.0f, totalBytes, totalBytes)
        return outputFile
    }

    fun create7zArchive(
        files: List<File>,
        outputFile: File,
        onProgress: (current: Int, total: Int, currentName: String) -> Unit
    ): File = create7zArchive(files, outputFile) { cur, tot, name, _, _, _ ->
        onProgress(cur, tot, name)
    }
}

package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.core.archive.ArchiveManager
import com.example.core.demo.SampleFilesGenerator
import com.example.core.media.AudioCompressor
import com.example.core.media.MediaMetadataExtractor
import com.example.core.media.VideoCompressor
import com.example.data.model.ArchiveCreationSettings
import com.example.data.model.ArchiveEntryInfo
import com.example.data.model.ArchiveOutputFormat
import com.example.data.model.ArchiveType
import com.example.data.model.AudioCompressionSettings
import com.example.data.model.FileCategory
import com.example.data.model.MediaMetadata
import com.example.data.model.OperationType
import com.example.data.model.ProcessedFile
import com.example.data.model.VideoCompressionSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

class FileManagerRepository(private val context: Context) {

    private val archiveManager = ArchiveManager()
    private val audioCompressor = AudioCompressor(context)
    private val videoCompressor = VideoCompressor(context)
    private val metadataExtractor = MediaMetadataExtractor(context)
    private val sampleGenerator = SampleFilesGenerator(context)

    val appBaseDir: File = File(context.filesDir, "CompresorPro").apply { mkdirs() }
    val compressedDir: File = File(appBaseDir, "comprimidos").apply { mkdirs() }
    val extractedDir: File = File(appBaseDir, "extraidos").apply { mkdirs() }
    val samplesDir: File = File(appBaseDir, "muestras").apply { mkdirs() }

    private val _processedFiles = MutableStateFlow<List<ProcessedFile>>(emptyList())
    val processedFiles: StateFlow<List<ProcessedFile>> = _processedFiles.asStateFlow()

    private val _sampleFiles = MutableStateFlow<List<File>>(emptyList())
    val sampleFiles: StateFlow<List<File>> = _sampleFiles.asStateFlow()

    init {
        refreshHistory()
        initSamples()
    }

    fun initSamples() {
        val existing = samplesDir.listFiles()?.filter { it.isFile } ?: emptyList()
        if (existing.isNotEmpty()) {
            _sampleFiles.value = existing
        } else {
            generateSamples()
        }
    }

    fun generateSamples() {
        val files = sampleGenerator.generateAllSamples(samplesDir)
        _sampleFiles.value = files
    }

    fun refreshHistory() {
        val list = mutableListOf<ProcessedFile>()

        compressedDir.listFiles()?.filter { it.isFile }?.forEach { f ->
            val op = when {
                f.name.endsWith(".m4a") || f.name.endsWith(".mp3") -> OperationType.COMPRESSED_AUDIO
                f.name.endsWith(".mp4") -> OperationType.COMPRESSED_VIDEO
                else -> OperationType.CREATED_ARCHIVE
            }
            list.add(
                ProcessedFile(
                    id = f.name,
                    name = f.name,
                    path = f.absolutePath,
                    sizeBytes = f.length(),
                    operation = op,
                    category = FileCategory.fromFileName(f.name),
                    timestamp = f.lastModified()
                )
            )
        }

        extractedDir.walkTopDown().filter { it.isFile }.forEach { f ->
            list.add(
                ProcessedFile(
                    id = f.absolutePath,
                    name = f.name,
                    path = f.absolutePath,
                    sizeBytes = f.length(),
                    operation = OperationType.EXTRACTED_ARCHIVE,
                    category = FileCategory.fromFileName(f.name),
                    timestamp = f.lastModified()
                )
            )
        }

        _processedFiles.value = list.sortedByDescending { it.timestamp }
    }

    fun getTotalSavingsBytes(): Long {
        return _processedFiles.value.sumOf {
            if (it.originalSizeBytes > it.sizeBytes) it.originalSizeBytes - it.sizeBytes else 0L
        }
    }

    fun getMetadata(file: File): MediaMetadata {
        return metadataExtractor.extract(file)
    }

    fun getMetadata(uri: Uri, name: String, size: Long): MediaMetadata {
        return metadataExtractor.extract(uri, name, size)
    }

    // --- Compress Video ---
    suspend fun compressVideo(
        inputFile: File,
        settings: VideoCompressionSettings,
        onProgress: (Float, String) -> Unit
    ): ProcessedFile = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()
        val baseName = inputFile.nameWithoutExtension
        val outFile = File(compressedDir, "${baseName}_comprimido_${System.currentTimeMillis() % 10000}.mp4")

        videoCompressor.compress(inputFile, outFile, settings, onProgress)

        val processed = ProcessedFile(
            id = UUID.randomUUID().toString(),
            name = outFile.name,
            path = outFile.absolutePath,
            sizeBytes = outFile.length(),
            originalSizeBytes = originalSize,
            operation = OperationType.COMPRESSED_VIDEO,
            category = FileCategory.VIDEO,
            timestamp = System.currentTimeMillis()
        )
        refreshHistory()
        processed
    }

    suspend fun compressVideoUri(
        uri: Uri,
        name: String,
        originalSize: Long,
        settings: VideoCompressionSettings,
        onProgress: (Float, String) -> Unit
    ): ProcessedFile = withContext(Dispatchers.IO) {
        val baseName = name.substringBeforeLast('.')
        val outFile = File(compressedDir, "${baseName}_comprimido_${System.currentTimeMillis() % 10000}.mp4")

        videoCompressor.compressUri(uri, outFile, settings, onProgress)

        val processed = ProcessedFile(
            id = UUID.randomUUID().toString(),
            name = outFile.name,
            path = outFile.absolutePath,
            sizeBytes = outFile.length(),
            originalSizeBytes = originalSize,
            operation = OperationType.COMPRESSED_VIDEO,
            category = FileCategory.VIDEO,
            timestamp = System.currentTimeMillis()
        )
        refreshHistory()
        processed
    }

    // --- Compress Audio ---
    suspend fun compressAudio(
        inputFile: File,
        settings: AudioCompressionSettings,
        onProgress: (Float, String) -> Unit
    ): ProcessedFile = withContext(Dispatchers.IO) {
        val originalSize = inputFile.length()
        val baseName = inputFile.nameWithoutExtension
        val outFile = File(compressedDir, "${baseName}_comprimido_${System.currentTimeMillis() % 10000}.m4a")

        audioCompressor.compress(inputFile, outFile, settings, onProgress)

        val processed = ProcessedFile(
            id = UUID.randomUUID().toString(),
            name = outFile.name,
            path = outFile.absolutePath,
            sizeBytes = outFile.length(),
            originalSizeBytes = originalSize,
            operation = OperationType.COMPRESSED_AUDIO,
            category = FileCategory.AUDIO,
            timestamp = System.currentTimeMillis()
        )
        refreshHistory()
        processed
    }

    suspend fun compressAudioUri(
        uri: Uri,
        name: String,
        originalSize: Long,
        settings: AudioCompressionSettings,
        onProgress: (Float, String) -> Unit
    ): ProcessedFile = withContext(Dispatchers.IO) {
        val baseName = name.substringBeforeLast('.')
        val outFile = File(compressedDir, "${baseName}_comprimido_${System.currentTimeMillis() % 10000}.m4a")

        audioCompressor.compressUri(uri, outFile, settings, onProgress)

        val processed = ProcessedFile(
            id = UUID.randomUUID().toString(),
            name = outFile.name,
            path = outFile.absolutePath,
            sizeBytes = outFile.length(),
            originalSizeBytes = originalSize,
            operation = OperationType.COMPRESSED_AUDIO,
            category = FileCategory.AUDIO,
            timestamp = System.currentTimeMillis()
        )
        refreshHistory()
        processed
    }

    // --- Create Archive ---
    suspend fun createArchive(
        files: List<File>,
        settings: ArchiveCreationSettings,
        onProgress: (current: Int, total: Int, name: String, fraction: Float, bytesProcessed: Long, totalBytes: Long) -> Unit
    ): ProcessedFile = withContext(Dispatchers.IO) {
        val originalTotal = files.sumOf { if (it.exists()) it.length() else 0L }
        val ext = settings.format.ext
        val safeName = settings.archiveName.trim().ifEmpty { "Archivo_Comprimido" }
        val outFile = File(compressedDir, "${safeName}.${ext}")

        if (settings.format == ArchiveOutputFormat.ZIP) {
            archiveManager.createZipArchive(files, outFile, settings.compressionLevel, onProgress)
        } else {
            archiveManager.create7zArchive(files, outFile, onProgress)
        }

        val processed = ProcessedFile(
            id = UUID.randomUUID().toString(),
            name = outFile.name,
            path = outFile.absolutePath,
            sizeBytes = outFile.length(),
            originalSizeBytes = originalTotal,
            operation = OperationType.CREATED_ARCHIVE,
            category = FileCategory.ARCHIVE,
            timestamp = System.currentTimeMillis()
        )
        refreshHistory()
        processed
    }

    suspend fun createArchive(
        files: List<File>,
        settings: ArchiveCreationSettings,
        onProgress: (Int, Int, String) -> Unit
    ): ProcessedFile = createArchive(files, settings) { cur, tot, name, _, _, _ ->
        onProgress(cur, tot, name)
    }

    // --- Inspect Archive ---
    suspend fun inspectArchive(file: File): List<ArchiveEntryInfo> = withContext(Dispatchers.IO) {
        archiveManager.inspectArchive(file)
    }

    // --- Extract Archive ---
    suspend fun extractArchive(
        archiveFile: File,
        selectedPaths: Set<String>? = null,
        onProgress: (Int, Int, String) -> Unit
    ): List<File> = withContext(Dispatchers.IO) {
        val folderName = archiveFile.nameWithoutExtension + "_extraido_${System.currentTimeMillis() % 10000}"
        val targetSubDir = File(extractedDir, folderName)
        val extracted = archiveManager.extractArchive(archiveFile, targetSubDir, selectedPaths, onProgress)
        refreshHistory()
        extracted
    }

    // --- Copy URI to Temp File for Inspection / Archive ---
    suspend fun copyUriToTemp(uri: Uri, fileName: String): File = withContext(Dispatchers.IO) {
        val temp = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$fileName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        temp
    }

    // --- File Actions ---
    fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val mime = getMimeType(file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Compartir ${file.name} con...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleFiles(files: List<File>) {
        if (files.isEmpty()) return
        if (files.size == 1) {
            shareFile(files.first())
            return
        }
        try {
            val uris = ArrayList<Uri>()
            for (f in files) {
                uris.add(FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", f))
            }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "Archivos extraídos (${files.size})")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Compartir ${files.size} archivos con...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openFile(file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = getMimeType(file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            shareFile(file)
        }
    }

    fun deleteFile(processedFile: ProcessedFile): Boolean {
        val f = File(processedFile.path)
        val deleted = if (f.exists()) f.delete() else true
        refreshHistory()
        return deleted
    }

    suspend fun exportFileToDownloads(file: File): File? = withContext(Dispatchers.IO) {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null) {
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                var destFile = File(downloadsDir, file.name)
                var counter = 1
                while (destFile.exists()) {
                    val baseName = file.nameWithoutExtension
                    val ext = if (file.extension.isNotEmpty()) ".${file.extension}" else ""
                    destFile = File(downloadsDir, "${baseName}_$counter$ext")
                    counter++
                }
                file.copyTo(destFile, overwrite = true)
                destFile
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4", "mkv", "mov", "avi" -> "video/*"
            "mp3", "m4a", "wav", "aac", "ogg" -> "audio/*"
            "pdf" -> "application/pdf"
            "txt", "md", "csv" -> "text/plain"
            "zip" -> "application/zip"
            "7z" -> "application/x-7z-compressed"
            "rar" -> "application/x-rar-compressed"
            "iso" -> "application/x-iso9660-image"
            "jpg", "jpeg", "png", "webp" -> "image/*"
            else -> "*/*"
        }
    }
}

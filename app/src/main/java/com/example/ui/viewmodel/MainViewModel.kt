package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import com.example.core.permission.StoragePermissionManager
import com.example.core.permission.StoragePermissionState
import androidx.lifecycle.viewModelScope
import com.example.data.model.ArchiveCreationSettings
import com.example.data.model.ArchiveEntryInfo
import com.example.data.model.ArchiveOutputFormat
import com.example.data.model.AudioCompressionSettings
import com.example.data.model.AudioPreset
import com.example.data.model.FileCategory
import com.example.data.model.MediaMetadata
import com.example.data.model.ProcessedFile
import com.example.data.model.VideoCompressionSettings
import com.example.data.model.VideoPreset
import com.example.data.preferences.AppThemeMode
import com.example.data.preferences.ThemePreferences
import com.example.data.repository.FileManagerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class MainTab {
    COMPRESS,
    DECOMPRESS,
    HISTORY
}

enum class CompressSubTab {
    MEDIA,
    CREATE_ARCHIVE
}

sealed interface OperationProgress {
    object Idle : OperationProgress
    data class Running(
        val progress: Float,
        val message: String,
        val currentFileName: String = "",
        val currentItem: Int = 0,
        val totalItems: Int = 0,
        val bytesProcessed: Long = 0L,
        val totalBytes: Long = 0L,
        val operationName: String = "Procesando"
    ) : OperationProgress
    data class Success(val message: String, val resultFile: File? = null) : OperationProgress
    data class Error(val errorMessage: String) : OperationProgress
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val repository = FileManagerRepository(application)
    private val themePreferences = ThemePreferences(application)

    val themeMode: StateFlow<AppThemeMode> = themePreferences.themeModeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppThemeMode.SYSTEM)

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            themePreferences.setThemeMode(mode)
        }
    }

    fun toggleTheme(isCurrentlyDark: Boolean) {
        val next = if (isCurrentlyDark) AppThemeMode.LIGHT else AppThemeMode.DARK
        setThemeMode(next)
    }

    private val _currentTab = MutableStateFlow(MainTab.COMPRESS)
    val currentTab: StateFlow<MainTab> = _currentTab.asStateFlow()

    private val _compressSubTab = MutableStateFlow(CompressSubTab.MEDIA)
    val compressSubTab: StateFlow<CompressSubTab> = _compressSubTab.asStateFlow()

    // History & Samples
    val processedFiles: StateFlow<List<ProcessedFile>> = repository.processedFiles
    val sampleFiles: StateFlow<List<File>> = repository.sampleFiles

    // Media Compression State
    private val _selectedMedia = MutableStateFlow<MediaMetadata?>(null)
    val selectedMedia: StateFlow<MediaMetadata?> = _selectedMedia.asStateFlow()

    private val _selectedMediaFile = MutableStateFlow<File?>(null)
    val selectedMediaFile: StateFlow<File?> = _selectedMediaFile.asStateFlow()

    private val _videoSettings = MutableStateFlow(VideoCompressionSettings())
    val videoSettings: StateFlow<VideoCompressionSettings> = _videoSettings.asStateFlow()

    private val _audioSettings = MutableStateFlow(AudioCompressionSettings())
    val audioSettings: StateFlow<AudioCompressionSettings> = _audioSettings.asStateFlow()

    private val _mediaCompressionProgress = MutableStateFlow<OperationProgress>(OperationProgress.Idle)
    val mediaCompressionProgress: StateFlow<OperationProgress> = _mediaCompressionProgress.asStateFlow()

    // Archive Creation State
    private val _selectedFilesForArchive = MutableStateFlow<List<File>>(emptyList())
    val selectedFilesForArchive: StateFlow<List<File>> = _selectedFilesForArchive.asStateFlow()

    private val _archiveSettings = MutableStateFlow(ArchiveCreationSettings())
    val archiveSettings: StateFlow<ArchiveCreationSettings> = _archiveSettings.asStateFlow()

    private val _archiveCreationProgress = MutableStateFlow<OperationProgress>(OperationProgress.Idle)
    val archiveCreationProgress: StateFlow<OperationProgress> = _archiveCreationProgress.asStateFlow()

    // Decompression State
    private val _selectedArchiveFile = MutableStateFlow<File?>(null)
    val selectedArchiveFile: StateFlow<File?> = _selectedArchiveFile.asStateFlow()

    private val _archiveEntries = MutableStateFlow<List<ArchiveEntryInfo>>(emptyList())
    val archiveEntries: StateFlow<List<ArchiveEntryInfo>> = _archiveEntries.asStateFlow()

    private val _selectedEntryPaths = MutableStateFlow<Set<String>>(emptySet())
    val selectedEntryPaths: StateFlow<Set<String>> = _selectedEntryPaths.asStateFlow()

    private val _extractionProgress = MutableStateFlow<OperationProgress>(OperationProgress.Idle)
    val extractionProgress: StateFlow<OperationProgress> = _extractionProgress.asStateFlow()

    private val _lastExtractedFiles = MutableStateFlow<List<File>>(emptyList())
    val lastExtractedFiles: StateFlow<List<File>> = _lastExtractedFiles.asStateFlow()

    // Preview File Modal
    private val _previewFile = MutableStateFlow<File?>(null)
    val previewFile: StateFlow<File?> = _previewFile.asStateFlow()

    // Drag and Drop Notification Banner
    private val _dropBannerMessage = MutableStateFlow<String?>(null)
    val dropBannerMessage: StateFlow<String?> = _dropBannerMessage.asStateFlow()

    fun dismissDropBanner() {
        _dropBannerMessage.value = null
    }

    // Storage Permissions State
    private val _storagePermissionState = MutableStateFlow(
        StoragePermissionManager.checkPermissionState(application)
    )
    val storagePermissionState: StateFlow<StoragePermissionState> = _storagePermissionState.asStateFlow()

    fun refreshPermissionState() {
        _storagePermissionState.value = StoragePermissionManager.checkPermissionState(getApplication())
    }

    fun saveFileToPublicDownloads(file: File) {
        viewModelScope.launch {
            val exported = repository.exportFileToDownloads(file)
            if (exported != null) {
                _dropBannerMessage.value = "✓ Guardado en Descargas: ${exported.name}"
            } else {
                _dropBannerMessage.value = "Error al guardar en Descargas. Verifica los permisos de almacenamiento."
            }
        }
    }

    fun handleDroppedFile(file: File) {
        val ext = file.extension.lowercase()
        val cat = FileCategory.fromFileName(file.name)
        when {
            ext in listOf("zip", "rar", "7z", "iso") || cat == FileCategory.ARCHIVE -> {
                _currentTab.value = MainTab.DECOMPRESS
                selectArchiveFile(file)
                _dropBannerMessage.value = "✓ Archivo ${file.name} cargado en Descomprimir"
            }
            cat == FileCategory.VIDEO || cat == FileCategory.AUDIO -> {
                _currentTab.value = MainTab.COMPRESS
                _compressSubTab.value = CompressSubTab.MEDIA
                selectMediaFile(file)
                _dropBannerMessage.value = "✓ Archivo multimedia ${file.name} listo para comprimir"
            }
            else -> {
                _currentTab.value = MainTab.COMPRESS
                _compressSubTab.value = CompressSubTab.CREATE_ARCHIVE
                addFilesForArchive(listOf(file))
                _dropBannerMessage.value = "✓ ${file.name} agregado para comprimir en archivo"
            }
        }
    }

    fun handleDroppedUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            if (uris.size == 1) {
                val uri = uris.first()
                var fileName = "archivo_${System.currentTimeMillis()}"
                try {
                    val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
                    cursor?.use {
                        if (it.moveToFirst()) {
                            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) fileName = it.getString(nameIndex) ?: fileName
                        }
                    }
                } catch (_: Exception) {}

                val localFile = repository.copyUriToTemp(uri, fileName)
                handleDroppedFile(localFile)
            } else {
                _currentTab.value = MainTab.COMPRESS
                _compressSubTab.value = CompressSubTab.CREATE_ARCHIVE
                val copiedFiles = mutableListOf<File>()
                for (uri in uris) {
                    var fileName = "file_${System.currentTimeMillis()}_${copiedFiles.size}"
                    try {
                        val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
                        cursor?.use {
                            if (it.moveToFirst()) {
                                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (nameIndex != -1) fileName = it.getString(nameIndex) ?: fileName
                            }
                        }
                    } catch (_: Exception) {}
                    copiedFiles.add(repository.copyUriToTemp(uri, fileName))
                }
                addFilesForArchive(copiedFiles)
                _dropBannerMessage.value = "✓ ${copiedFiles.size} archivos agregados para empaquetar"
            }
        }
    }

    fun setTab(tab: MainTab) {
        _currentTab.value = tab
    }

    fun setCompressSubTab(subTab: CompressSubTab) {
        _compressSubTab.value = subTab
    }

    // --- Media Selection & Settings ---
    fun selectMediaFile(file: File) {
        viewModelScope.launch {
            _selectedMediaFile.value = file
            _selectedMedia.value = repository.getMetadata(file)
            _mediaCompressionProgress.value = OperationProgress.Idle
        }
    }

    fun selectMediaUri(uri: Uri) {
        viewModelScope.launch {
            try {
                var fileName = "media_${System.currentTimeMillis()}"
                var fileSize = 0L
                val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                        if (nameIndex != -1) fileName = it.getString(nameIndex) ?: fileName
                        if (sizeIndex != -1) fileSize = it.getLong(sizeIndex)
                    }
                }
                val localFile = repository.copyUriToTemp(uri, fileName)
                _selectedMediaFile.value = localFile
                _selectedMedia.value = repository.getMetadata(localFile)
                _mediaCompressionProgress.value = OperationProgress.Idle
            } catch (e: Exception) {
                _mediaCompressionProgress.value = OperationProgress.Error("Error al cargar archivo: ${e.message}")
            }
        }
    }

    fun clearSelectedMedia() {
        _selectedMedia.value = null
        _selectedMediaFile.value = null
        _mediaCompressionProgress.value = OperationProgress.Idle
    }

    fun setVideoPreset(preset: VideoPreset) {
        val currentMeta = _selectedMedia.value
        val originalW = currentMeta?.width?.takeIf { it > 0 } ?: 1280
        val originalH = currentMeta?.height?.takeIf { it > 0 } ?: 720
        val targetW = ((originalW * preset.scale).toInt() / 2) * 2
        val targetH = ((originalH * preset.scale).toInt() / 2) * 2

        _videoSettings.value = _videoSettings.value.copy(
            preset = preset,
            targetWidth = targetW.coerceAtLeast(320),
            targetHeight = targetH.coerceAtLeast(240),
            targetVideoBitrateKbps = preset.bitrateKbps
        )
    }

    fun updateVideoSettings(targetW: Int, targetH: Int, bitrateKbps: Int) {
        _videoSettings.value = _videoSettings.value.copy(
            preset = VideoPreset.CUSTOM,
            targetWidth = targetW,
            targetHeight = targetH,
            targetVideoBitrateKbps = bitrateKbps
        )
    }

    fun setAudioPreset(preset: AudioPreset) {
        _audioSettings.value = _audioSettings.value.copy(
            preset = preset,
            targetBitrateKbps = preset.bitrateKbps,
            isMono = preset.isMono
        )
    }

    fun updateAudioSettings(bitrateKbps: Int, isMono: Boolean, sampleRate: Int = 44100) {
        _audioSettings.value = _audioSettings.value.copy(
            targetBitrateKbps = bitrateKbps,
            isMono = isMono,
            targetSampleRate = sampleRate
        )
    }

    fun startMediaCompression() {
        val file = _selectedMediaFile.value ?: return
        val meta = _selectedMedia.value ?: return

        viewModelScope.launch {
            _mediaCompressionProgress.value = OperationProgress.Running(0f, "Iniciando compresión...")
            try {
                if (meta.isVideo) {
                    val result = repository.compressVideo(file, _videoSettings.value) { p, msg ->
                        _mediaCompressionProgress.value = OperationProgress.Running(p, msg)
                    }
                    val savings = result.savingsPercent?.let { " (Ahorro: $it%)" } ?: ""
                    _mediaCompressionProgress.value = OperationProgress.Success("¡Video comprimido con éxito!$savings", File(result.path))
                } else {
                    val result = repository.compressAudio(file, _audioSettings.value) { p, msg ->
                        _mediaCompressionProgress.value = OperationProgress.Running(p, msg)
                    }
                    val savings = result.savingsPercent?.let { " (Ahorro: $it%)" } ?: ""
                    _mediaCompressionProgress.value = OperationProgress.Success("¡Audio comprimido con éxito!$savings", File(result.path))
                }
            } catch (e: Exception) {
                _mediaCompressionProgress.value = OperationProgress.Error("Fallo en la compresión: ${e.message}")
            }
        }
    }

    // --- Archive Creation ---
    fun addFilesForArchive(files: List<File>) {
        val current = _selectedFilesForArchive.value.toMutableList()
        for (f in files) {
            if (!current.any { it.absolutePath == f.absolutePath }) {
                current.add(f)
            }
        }
        _selectedFilesForArchive.value = current
    }

    fun addUriForArchive(uri: Uri) {
        viewModelScope.launch {
            try {
                var fileName = "archivo_${System.currentTimeMillis()}"
                val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = it.getString(nameIndex) ?: fileName
                    }
                }
                val localFile = repository.copyUriToTemp(uri, fileName)
                addFilesForArchive(listOf(localFile))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun removeFileFromArchive(file: File) {
        _selectedFilesForArchive.value = _selectedFilesForArchive.value.filter { it.absolutePath != file.absolutePath }
    }

    fun clearArchiveFiles() {
        _selectedFilesForArchive.value = emptyList()
        _archiveCreationProgress.value = OperationProgress.Idle
    }

    fun updateArchiveSettings(settings: ArchiveCreationSettings) {
        _archiveSettings.value = settings
    }

    fun startArchiveCreation() {
        val files = _selectedFilesForArchive.value
        if (files.isEmpty()) return

        viewModelScope.launch {
            _archiveCreationProgress.value = OperationProgress.Running(0f, "Creando archivo...")
            try {
                val result = repository.createArchive(files, _archiveSettings.value) { current, total, name ->
                    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0.5f
                    _archiveCreationProgress.value = OperationProgress.Running(progress, "Empaquetando: $name ($current/$total)")
                }
                _archiveCreationProgress.value = OperationProgress.Success("¡Archivo ${_archiveSettings.value.format.ext.uppercase()} creado con éxito!", File(result.path))
            } catch (e: Exception) {
                _archiveCreationProgress.value = OperationProgress.Error("Error al crear archivo: ${e.message}")
            }
        }
    }

    // --- Archive Decompression ---
    fun selectArchiveFile(file: File) {
        viewModelScope.launch {
            _selectedArchiveFile.value = file
            _extractionProgress.value = OperationProgress.Idle
            inspectSelectedArchive(file)
        }
    }

    fun selectArchiveUri(uri: Uri) {
        viewModelScope.launch {
            try {
                var fileName = "archivo_${System.currentTimeMillis()}.zip"
                val cursor = getApplication<Application>().contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) fileName = it.getString(nameIndex) ?: fileName
                    }
                }
                val localFile = repository.copyUriToTemp(uri, fileName)
                _selectedArchiveFile.value = localFile
                _extractionProgress.value = OperationProgress.Idle
                inspectSelectedArchive(localFile)
            } catch (e: Exception) {
                _extractionProgress.value = OperationProgress.Error("Error al cargar archivo: ${e.message}")
            }
        }
    }

    private suspend fun inspectSelectedArchive(file: File) {
        _extractionProgress.value = OperationProgress.Running(0f, "Leyendo contenido del archivo...")
        try {
            val entries = repository.inspectArchive(file)
            _archiveEntries.value = entries
            _selectedEntryPaths.value = entries.filter { !it.isDirectory }.map { it.path }.toSet()
            _extractionProgress.value = OperationProgress.Idle
        } catch (e: Exception) {
            _archiveEntries.value = emptyList()
            _extractionProgress.value = OperationProgress.Error("No se pudo inspeccionar el archivo: ${e.message}")
        }
    }

    fun toggleEntrySelection(path: String) {
        val current = _selectedEntryPaths.value.toMutableSet()
        if (current.contains(path)) {
            current.remove(path)
        } else {
            current.add(path)
        }
        _selectedEntryPaths.value = current
    }

    fun selectAllEntries(select: Boolean) {
        if (select) {
            _selectedEntryPaths.value = _archiveEntries.value.filter { !it.isDirectory }.map { it.path }.toSet()
        } else {
            _selectedEntryPaths.value = emptySet()
        }
    }

    fun startExtraction(onlySelected: Boolean = false) {
        val archive = _selectedArchiveFile.value ?: return

        viewModelScope.launch {
            _extractionProgress.value = OperationProgress.Running(0f, "Iniciando descompresión...")
            try {
                val filterPaths = if (onlySelected) _selectedEntryPaths.value else null
                val extracted = repository.extractArchive(archive, filterPaths) { current, total, name ->
                    val progress = if (total > 0) current.toFloat() / total.toFloat() else 0.5f
                    _extractionProgress.value = OperationProgress.Running(progress, "Extrayendo: $name ($current/$total)")
                }
                _lastExtractedFiles.value = extracted
                _extractionProgress.value = OperationProgress.Success("¡Se extrajeron ${extracted.size} archivos correctamente!")
            } catch (e: Exception) {
                _extractionProgress.value = OperationProgress.Error("Fallo al descomprimir: ${e.message}")
            }
        }
    }

    fun clearSelectedArchive() {
        _selectedArchiveFile.value = null
        _archiveEntries.value = emptyList()
        _selectedEntryPaths.value = emptySet()
        _extractionProgress.value = OperationProgress.Idle
        _lastExtractedFiles.value = emptyList()
    }

    // --- Samples ---
    fun reloadSamples() {
        viewModelScope.launch {
            repository.generateSamples()
        }
    }

    // --- File Actions & Preview ---
    fun openFile(file: File) {
        repository.openFile(file)
    }

    fun shareFile(file: File) {
        repository.shareFile(file)
    }

    fun shareMultipleFiles(files: List<File>) {
        repository.shareMultipleFiles(files)
    }

    fun deleteProcessedFile(processed: ProcessedFile) {
        repository.deleteFile(processed)
    }

    fun showPreview(file: File?) {
        _previewFile.value = file
    }
}

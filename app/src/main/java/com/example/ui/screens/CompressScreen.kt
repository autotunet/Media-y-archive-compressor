package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArchiveOutputFormat
import com.example.data.model.AudioPreset
import com.example.data.model.FileCategory
import com.example.data.model.ProcessedFile
import com.example.data.model.VideoPreset
import com.example.ui.components.ArchiveCompressionSettingsDialog
import com.example.ui.components.CompressionSizeCalculator
import com.example.ui.components.CompressionSizePreviewCard
import com.example.ui.components.CustomFilePickerDialog
import com.example.ui.components.FileCategoryIcon
import com.example.ui.components.FormatBadge
import com.example.ui.components.InteractiveFileDropZone
import com.example.ui.components.Material3TaskProgressCard
import com.example.ui.components.MediaCompressionSettingsDialog
import com.example.ui.components.OperationCircularProgressCard
import com.example.ui.components.OperationProgressDialog
import com.example.ui.components.PickerFilterMode
import com.example.ui.components.SampleFilesBar
import com.example.ui.components.SectionHeader
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber
import com.example.ui.viewmodel.CompressSubTab
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun CompressScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val subTab by viewModel.compressSubTab.collectAsState()
    val sampleFiles by viewModel.sampleFiles.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Sub-tabs: Media vs Archive
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (subTab == CompressSubTab.MEDIA) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.setCompressSubTab(CompressSubTab.MEDIA) }
                        .padding(vertical = 10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (subTab == CompressSubTab.MEDIA) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Audio y Video",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (subTab == CompressSubTab.MEDIA) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (subTab == CompressSubTab.CREATE_ARCHIVE) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { viewModel.setCompressSubTab(CompressSubTab.CREATE_ARCHIVE) }
                        .padding(vertical = 10.dp),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = if (subTab == CompressSubTab.CREATE_ARCHIVE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Crear ZIP / 7Z",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (subTab == CompressSubTab.CREATE_ARCHIVE) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        when (subTab) {
            CompressSubTab.MEDIA -> MediaCompressSection(viewModel, sampleFiles)
            CompressSubTab.CREATE_ARCHIVE -> ArchiveCreatorSection(viewModel, sampleFiles)
        }
    }
}

@Composable
fun MediaCompressSection(viewModel: MainViewModel, sampleFiles: List<File>) {
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val videoSettings by viewModel.videoSettings.collectAsState()
    val audioSettings by viewModel.audioSettings.collectAsState()
    val progress by viewModel.mediaCompressionProgress.collectAsState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.selectMediaUri(uri)
        }
    }

    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        CustomFilePickerDialog(
            initialDirectory = viewModel.repository.samplesDir,
            defaultFilter = PickerFilterMode.ALL_FILES,
            onDismiss = { showCustomPicker = false },
            onFileSelected = { file ->
                viewModel.selectMediaFile(file)
            },
            onLaunchSystemPicker = {
                showCustomPicker = false
                mediaPickerLauncher.launch("video/*,audio/*")
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SampleFilesBar(
                samples = sampleFiles.filter {
                    val cat = FileCategory.fromFileName(it.name)
                    cat == FileCategory.AUDIO || cat == FileCategory.VIDEO
                },
                onSelectSample = { viewModel.selectMediaFile(it) },
                onReloadSamples = { viewModel.reloadSamples() }
            )
        }

        // File Selection Box
        item {
            if (selectedMedia == null) {
                InteractiveFileDropZone(
                    onFilesDropped = { uris ->
                        viewModel.handleDroppedUris(uris)
                    },
                    onFileSelected = { file ->
                        viewModel.handleDroppedFile(file)
                    },
                    onOpenCustomPicker = { showCustomPicker = true },
                    onOpenSystemPicker = { mediaPickerLauncher.launch("video/*,audio/*") },
                    title = "Zona de Arrastre Multimedia",
                    subtitle = "Arrastra audios o videos aquí o pulsa para explorar",
                    acceptedFormats = listOf("MP4", "MKV", "MOV", "MP3", "WAV", "M4A", "AAC", "OGG"),
                    sampleFiles = sampleFiles.filter {
                        val cat = FileCategory.fromFileName(it.name)
                        cat == FileCategory.AUDIO || cat == FileCategory.VIDEO
                    }
                )
            } else {
                val media = selectedMedia!!
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FileCategoryIcon(
                            category = if (media.isVideo) FileCategory.VIDEO else FileCategory.AUDIO,
                            size = 36.dp,
                            tint = if (media.isVideo) DeepCyan else DeepEmerald
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = media.fileName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FormatBadge(if (media.isVideo) "VIDEO" else "AUDIO")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ProcessedFile.formatBytes(media.fileSizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (media.durationFormatted.isNotEmpty()) {
                                    Text(
                                        text = " • ${media.durationFormatted}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (media.resolutionFormatted.isNotEmpty()) {
                                    Text(
                                        text = " • ${media.resolutionFormatted}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        IconButton(onClick = { viewModel.clearSelectedMedia() }) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar selección")
                        }
                    }
                }
            }
        }

        // Compression Settings (when media is selected)
        if (selectedMedia != null) {
            val media = selectedMedia!!
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Nivel de Compresión",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (media.isVideo) {
                            // Video Presets
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                VideoPreset.values().forEach { preset ->
                                    val isSelected = videoSettings.preset == preset
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setVideoPreset(preset) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = preset.desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Audio Presets
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                AudioPreset.values().forEach { preset ->
                                    val isSelected = audioSettings.preset == preset
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) DeepEmerald.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSelected) DeepEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { viewModel.setAudioPreset(preset) }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = preset.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DeepEmerald else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = preset.desc,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = DeepEmerald
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "Modo Mono (Voz)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(text = "Reduce a la mitad el tamaño en podcasts o grabaciones", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Switch(
                                        checked = audioSettings.isMono,
                                        onCheckedChange = { viewModel.updateAudioSettings(audioSettings.targetBitrateKbps, it) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pre-compression size preview UI element
                        val calculatedMediaSize = remember(media, videoSettings.preset, audioSettings.preset, audioSettings.isMono) {
                            if (media.isVideo) {
                                CompressionSizeCalculator.calculateEstimatedVideoSize(media, videoSettings.preset)
                            } else {
                                CompressionSizeCalculator.calculateEstimatedAudioSize(media, audioSettings.preset, audioSettings.isMono)
                            }
                        }
                        val mediaPresetLabel = if (media.isVideo) {
                            "${videoSettings.preset.title} (${videoSettings.preset.desc})"
                        } else {
                            "${audioSettings.preset.title}${if (audioSettings.isMono) " + Modo Mono" else ""}"
                        }

                        CompressionSizePreviewCard(
                            originalSizeBytes = media.fileSizeBytes,
                            calculatedSizeBytes = calculatedMediaSize,
                            algorithmOrPresetLabel = mediaPresetLabel,
                            accentColor = if (media.isVideo) DeepCyan else DeepEmerald
                        )

                        Spacer(modifier = Modifier.height(16.dp))

    var showMediaSettingsDialog by remember { mutableStateOf(false) }

    if (showMediaSettingsDialog && selectedMedia != null) {
        MediaCompressionSettingsDialog(
            media = selectedMedia!!,
            initialVideoSettings = videoSettings,
            initialAudioSettings = audioSettings,
            onDismiss = { showMediaSettingsDialog = false },
            onConfirmVideo = { settings ->
                showMediaSettingsDialog = false
                viewModel.setVideoPreset(settings.preset)
                viewModel.startMediaCompression()
            },
            onConfirmAudio = { settings ->
                showMediaSettingsDialog = false
                viewModel.setAudioPreset(settings.preset)
                viewModel.updateAudioSettings(settings.targetBitrateKbps, settings.isMono)
                viewModel.startMediaCompression()
            }
        )
    }

                        Button(
                            onClick = { showMediaSettingsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_media_compress")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (media.isVideo) "Ajustar y Comprimir Video" else "Ajustar y Comprimir Audio",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Progress / Result Card
        item {
            Material3TaskProgressCard(
                progress = progress,
                onDismiss = { viewModel.clearSelectedMedia() },
                onOpenResult = { viewModel.showPreview(it) },
                onSaveToDownloads = { viewModel.saveFileToPublicDownloads(it) },
                onShareResult = { viewModel.shareFile(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun ArchiveCreatorSection(viewModel: MainViewModel, sampleFiles: List<File>) {
    val selectedFiles by viewModel.selectedFilesForArchive.collectAsState()
    val archiveSettings by viewModel.archiveSettings.collectAsState()
    val progress by viewModel.archiveCreationProgress.collectAsState()

    var archiveNameInput by remember { mutableStateOf(archiveSettings.archiveName) }

    val multiFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            viewModel.addUriForArchive(uri)
        }
    }

    var showCustomArchivePicker by remember { mutableStateOf(false) }

    if (showCustomArchivePicker) {
        CustomFilePickerDialog(
            initialDirectory = viewModel.repository.samplesDir,
            isMultiSelect = true,
            defaultFilter = PickerFilterMode.ALL_FILES,
            onDismiss = { showCustomArchivePicker = false },
            onFileSelected = { file ->
                viewModel.addFilesForArchive(listOf(file))
            },
            onMultipleFilesSelected = { files ->
                viewModel.addFilesForArchive(files)
            },
            onLaunchSystemPicker = {
                showCustomArchivePicker = false
                multiFilePickerLauncher.launch("*/*")
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SampleFilesBar(
                samples = sampleFiles,
                onSelectSample = { viewModel.addFilesForArchive(listOf(it)) },
                onReloadSamples = { viewModel.reloadSamples() }
            )
        }

        // Add Files Card / Drop Zone
        item {
            if (selectedFiles.isEmpty()) {
                InteractiveFileDropZone(
                    onFilesDropped = { uris ->
                        uris.forEach { viewModel.addUriForArchive(it) }
                    },
                    onFileSelected = { file ->
                        viewModel.addFilesForArchive(listOf(file))
                    },
                    onOpenCustomPicker = { showCustomArchivePicker = true },
                    onOpenSystemPicker = { multiFilePickerLauncher.launch("*/*") },
                    title = "Zona de Arrastre para Empaquetar",
                    subtitle = "Arrastra documentos, fotos, audios o videos para crear tu archivo",
                    acceptedFormats = listOf("ZIP", "7Z", "DOCUMENTOS", "IMÁGENES", "AUDIO", "VIDEO"),
                    sampleFiles = sampleFiles
                )
            } else {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Archivos para empaquetar (${selectedFiles.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Agrega más audios, videos, documentos, fotos o cualquier archivo",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showCustomArchivePicker = true },
                                modifier = Modifier.testTag("btn_add_files_to_archive")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Añadir Archivos")
                            }
                            OutlinedButton(
                                onClick = { multiFilePickerLauncher.launch("*/*") }
                            ) {
                                Text("Sistema")
                            }
                        }
                    }
                }
            }
        }

        // List of selected files
        if (selectedFiles.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Archivos seleccionados (${ProcessedFile.formatBytes(selectedFiles.sumOf { it.length() })})",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Limpiar todo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.clearArchiveFiles() }
                            .padding(4.dp)
                    )
                }
            }

            items(selectedFiles) { file ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FileCategoryIcon(category = FileCategory.fromFileName(file.name), size = 22.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ProcessedFile.formatBytes(file.length()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.removeFileFromArchive(file) }) {
                            Icon(Icons.Default.Close, contentDescription = "Eliminar de la lista", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Options: Format, Name, Level
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Opciones del Archivo",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Format selection: ZIP vs 7Z
                        Text(text = "Formato de salida:", style = MaterialTheme.typography.labelMedium)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ArchiveOutputFormat.values().forEach { fmt ->
                                val isSelected = archiveSettings.format == fmt
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateArchiveSettings(archiveSettings.copy(format = fmt)) },
                                    label = { Text(fmt.label) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Name
                        OutlinedTextField(
                            value = archiveNameInput,
                            onValueChange = {
                                archiveNameInput = it
                                viewModel.updateArchiveSettings(archiveSettings.copy(archiveName = it))
                            },
                            label = { Text("Nombre del archivo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Compression Level
                        if (archiveSettings.format == ArchiveOutputFormat.ZIP) {
                            Text(
                                text = "Nivel de compresión ZIP: ${archiveSettings.compressionLevel} (${when (archiveSettings.compressionLevel) {
                                    0 -> "Sin compresión (Rápido)"
                                    in 1..3 -> "Baja (Muy rápido)"
                                    in 4..6 -> "Normal equilibrada"
                                    else -> "Máxima compresión"
                                }})",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Slider(
                                value = archiveSettings.compressionLevel.toFloat(),
                                onValueChange = {
                                    viewModel.updateArchiveSettings(archiveSettings.copy(compressionLevel = it.toInt()))
                                },
                                valueRange = 0f..9f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Pre-compression size preview UI element for archive creation
                        val totalOriginalArchiveBytes = remember(selectedFiles) {
                            selectedFiles.sumOf { if (it.exists()) it.length() else 0L }
                        }
                        val calculatedArchiveSize = remember(selectedFiles, archiveSettings.format, archiveSettings.compressionLevel) {
                            CompressionSizeCalculator.calculateEstimatedArchiveSize(
                                files = selectedFiles,
                                format = archiveSettings.format,
                                compressionLevel = archiveSettings.compressionLevel
                            )
                        }
                        val archivePresetLabel = "${archiveSettings.format.label} • Nivel ${archiveSettings.compressionLevel}/9"

                        var showArchiveSettingsDialog by remember { mutableStateOf(false) }

                        if (showArchiveSettingsDialog) {
                            ArchiveCompressionSettingsDialog(
                                initialSettings = archiveSettings,
                                totalFilesCount = selectedFiles.size,
                                totalFilesSizeBytes = totalOriginalArchiveBytes,
                                onDismiss = { showArchiveSettingsDialog = false },
                                onConfirm = { newSettings ->
                                    showArchiveSettingsDialog = false
                                    viewModel.updateArchiveSettings(newSettings)
                                    viewModel.startArchiveCreation()
                                }
                            )
                        }

                        CompressionSizePreviewCard(
                            originalSizeBytes = totalOriginalArchiveBytes,
                            calculatedSizeBytes = calculatedArchiveSize,
                            algorithmOrPresetLabel = archivePresetLabel,
                            accentColor = SuccessGreen
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { showArchiveSettingsDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_start_archive_create")
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ajustar y Crear Archivo (${archiveSettings.format.ext.uppercase()})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Progress
        item {
            Material3TaskProgressCard(
                progress = progress,
                onDismiss = { viewModel.clearArchiveFiles() },
                onOpenResult = { viewModel.showPreview(it) },
                onSaveToDownloads = { viewModel.saveFileToPublicDownloads(it) },
                onShareResult = { viewModel.shareFile(it) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

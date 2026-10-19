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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.data.model.ArchiveType
import com.example.data.model.FileCategory
import com.example.data.model.ProcessedFile
import com.example.ui.components.CustomFilePickerDialog
import com.example.ui.components.FileCategoryIcon
import com.example.ui.components.FormatBadge
import com.example.ui.components.InteractiveFileDropZone
import com.example.ui.components.Material3TaskProgressCard
import com.example.ui.components.OperationCircularProgressCard
import com.example.ui.components.OperationProgressDialog
import com.example.ui.components.PickerFilterMode
import com.example.ui.components.SampleFilesBar
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber
import com.example.ui.viewmodel.MainViewModel
import java.io.File

@Composable
fun DecompressScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val selectedArchive by viewModel.selectedArchiveFile.collectAsState()
    val entries by viewModel.archiveEntries.collectAsState()
    val selectedPaths by viewModel.selectedEntryPaths.collectAsState()
    val progress by viewModel.extractionProgress.collectAsState()
    val lastExtractedFiles by viewModel.lastExtractedFiles.collectAsState()
    val sampleFiles by viewModel.sampleFiles.collectAsState()

    val archivePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.selectArchiveUri(uri)
        }
    }

    var showCustomPicker by remember { mutableStateOf(false) }

    if (showCustomPicker) {
        CustomFilePickerDialog(
            initialDirectory = viewModel.repository.samplesDir,
            defaultFilter = PickerFilterMode.ARCHIVES_ONLY,
            onDismiss = { showCustomPicker = false },
            onFileSelected = { file ->
                viewModel.selectArchiveFile(file)
            },
            onLaunchSystemPicker = {
                showCustomPicker = false
                archivePickerLauncher.launch("*/*")
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick sample archives bar
        item {
            val archiveSamples = sampleFiles.filter {
                val ext = it.extension.lowercase()
                ext in listOf("zip", "7z", "rar", "iso")
            }
            if (archiveSamples.isNotEmpty()) {
                SampleFilesBar(
                    samples = archiveSamples,
                    onSelectSample = { viewModel.selectArchiveFile(it) },
                    onReloadSamples = { viewModel.reloadSamples() }
                )
            }
        }

        // Archive Picker Card
        item {
            if (selectedArchive == null) {
                InteractiveFileDropZone(
                    onFilesDropped = { uris ->
                        viewModel.handleDroppedUris(uris)
                    },
                    onFileSelected = { file ->
                        viewModel.handleDroppedFile(file)
                    },
                    onOpenCustomPicker = { showCustomPicker = true },
                    onOpenSystemPicker = { archivePickerLauncher.launch("*/*") },
                    title = "Zona de Arrastre de Archivos",
                    subtitle = "Arrastra imágenes ISO, ZIP, RAR o 7z aquí o toca para explorar",
                    acceptedFormats = listOf("ISO", "ZIP", "RAR", "7Z"),
                    sampleFiles = sampleFiles.filter {
                        val ext = it.extension.lowercase()
                        ext in listOf("zip", "7z", "rar", "iso")
                    }
                )
            } else {
                val archive = selectedArchive!!
                val ext = archive.extension.uppercase()
                val typeColor = when (ext) {
                    "ISO" -> VibrantAmber
                    "RAR" -> Color(0xFFEF4444)
                    "7Z" -> Color(0xFF10B981)
                    else -> NeonCyan
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, typeColor.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = typeColor.copy(alpha = 0.15f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (ext == "ISO") Icons.Default.DiscFull else Icons.Default.FolderZip,
                                contentDescription = null,
                                tint = typeColor,
                                modifier = Modifier
                                    .padding(10.dp)
                                    .size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = archive.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FormatBadge(ext, color = typeColor)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ProcessedFile.formatBytes(archive.length()),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = " • ${entries.size} elementos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = { viewModel.clearSelectedArchive() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar archivo")
                        }
                    }
                }
            }
        }

        // Archive Contents Inspection
        if (selectedArchive != null && entries.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contenido del archivo (${selectedPaths.size}/${entries.filter { !it.isDirectory }.size} seleccionados)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    val allSelected = selectedPaths.size == entries.filter { !it.isDirectory }.size
                    Text(
                        text = if (allSelected) "Deseleccionar todo" else "Seleccionar todo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { viewModel.selectAllEntries(!allSelected) }
                            .padding(4.dp)
                    )
                }
            }

            // Entries List
            items(entries) { entry ->
                val isSelected = selectedPaths.contains(entry.path)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            if (!entry.isDirectory) {
                                viewModel.toggleEntrySelection(entry.path)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!entry.isDirectory) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { viewModel.toggleEntrySelection(entry.path) },
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = VibrantAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        FileCategoryIcon(
                            category = if (entry.isDirectory) FileCategory.OTHER else entry.category,
                            size = 20.dp
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = entry.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (entry.isDirectory) "Carpeta" else ProcessedFile.formatBytes(entry.uncompressedSize),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Extraction Action Buttons
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.startExtraction(onlySelected = true) },
                        enabled = selectedPaths.isNotEmpty(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_extract_selected")
                    ) {
                        Text("Extraer Seleccionados (${selectedPaths.size})")
                    }

                    Button(
                        onClick = { viewModel.startExtraction(onlySelected = false) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_extract_all")
                    ) {
                        Icon(Icons.Default.Unarchive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Extraer Todo")
                    }
                }
            }
        }

        // Active Extraction Progress Card
        item {
            Material3TaskProgressCard(
                progress = progress,
                onDismiss = { /* keep state */ },
                onOpenResult = { viewModel.showPreview(it) },
                onSaveToDownloads = { viewModel.saveFileToPublicDownloads(it) },
                onShareResult = { file -> viewModel.shareFile(file) }
            )
        }

        // Extracted Files Section
        if (lastExtractedFiles.isNotEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = SuccessGreen)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Archivos Extraídos (${lastExtractedFiles.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }

                            FilledTonalButton(
                                onClick = { viewModel.shareMultipleFiles(lastExtractedFiles) },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("btn_share_all_extracted")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir todos", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Compartir Todo", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        lastExtractedFiles.forEach { file ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.showPreview(file) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FileCategoryIcon(category = FileCategory.fromFileName(file.name), size = 18.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = ProcessedFile.formatBytes(file.length()),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.showPreview(file) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Ver archivo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.saveFileToPublicDownloads(file) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = "Guardar en Descargas", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { viewModel.shareFile(file) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Share, contentDescription = "Compartir", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

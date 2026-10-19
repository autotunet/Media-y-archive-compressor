package com.example.ui.components

import android.os.Environment
import com.example.core.permission.StoragePermissionManager
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DiscFull
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ArchiveType
import com.example.data.model.FileCategory
import com.example.data.model.ProcessedFile
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ArchiveFileIconType(val label: String, val color: Color) {
    ZIP("ZIP", Color(0xFF0284C7)),
    RAR("RAR", Color(0xFFE11D48)),
    SEVEN_ZIP("7Z", Color(0xFF059669)),
    ISO("ISO", Color(0xFFD97706)),
    OTHER_ARCHIVE("ARCHIVE", Color(0xFF7C3AED));

    companion object {
        fun fromExtension(ext: String): ArchiveFileIconType? {
            return when (ext.lowercase()) {
                "zip" -> ZIP
                "rar" -> RAR
                "7z" -> SEVEN_ZIP
                "iso" -> ISO
                "tar", "gz", "bz2", "xz" -> OTHER_ARCHIVE
                else -> null
            }
        }
    }
}

@Composable
fun ArchiveFormatVisualBadge(
    archiveType: ArchiveFileIconType,
    size: Dp = 40.dp
) {
    val icon: ImageVector = when (archiveType) {
        ArchiveFileIconType.ZIP -> Icons.Default.FolderZip
        ArchiveFileIconType.RAR -> Icons.Default.Layers
        ArchiveFileIconType.SEVEN_ZIP -> Icons.Default.Inventory2
        ArchiveFileIconType.ISO -> Icons.Default.DiscFull
        ArchiveFileIconType.OTHER_ARCHIVE -> Icons.Default.Archive
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = archiveType.color.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, archiveType.color.copy(alpha = 0.45f)),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = archiveType.label,
                tint = archiveType.color,
                modifier = Modifier.size((size.value * 0.58f).dp)
            )
        }
    }
}

enum class PickerFilterMode(val label: String) {
    ARCHIVES_ONLY("Archivos (ISO, ZIP, RAR, 7Z)"),
    ALL_FILES("Todos los archivos"),
    VIDEOS("Videos"),
    AUDIOS("Audios"),
    DOCUMENTS("Documentos")
}

/**
 * Custom file picker UI for browsing internal and storage directories with
 * tailored icons for archive formats (zip, rar, 7z, iso).
 */
@Composable
fun CustomFilePickerDialog(
    initialDirectory: File,
    isMultiSelect: Boolean = false,
    defaultFilter: PickerFilterMode = PickerFilterMode.ALL_FILES,
    onDismiss: () -> Unit,
    onFileSelected: (File) -> Unit,
    onMultipleFilesSelected: (List<File>) -> Unit = {},
    onLaunchSystemPicker: () -> Unit
) {
    val context = LocalContext.current
    var currentDirectory by remember { mutableStateOf(initialDirectory) }
    var searchQuery by remember { mutableStateOf("") }
    var filterMode by remember { mutableStateOf(defaultFilter) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    val permissionState = remember { StoragePermissionManager.checkPermissionState(context) }

    // Quick accessible root folders across all storage directories
    val rootLocations = remember(context) {
        val list = mutableListOf<Pair<String, File>>()
        val discovered = StoragePermissionManager.getPhoneStorageLocations(context)
        discovered.forEach { info ->
            list.add(info.label to info.file)
        }
        if (list.none { it.second.absolutePath == initialDirectory.absolutePath }) {
            list.add(0, "Carpeta Actual" to initialDirectory)
        }
        list
    }

    val fileItems = remember(currentDirectory, searchQuery, filterMode) {
        val all = currentDirectory.listFiles()?.toList() ?: emptyList()
        val dirs = all.filter { it.isDirectory && !it.name.startsWith(".") }.sortedBy { it.name.lowercase() }
        val files = all.filter { it.isFile && !it.name.startsWith(".") }.filter { file ->
            val ext = file.extension.lowercase()
            val matchesFilter = when (filterMode) {
                PickerFilterMode.ARCHIVES_ONLY -> ext in listOf("zip", "rar", "7z", "iso", "tar", "gz")
                PickerFilterMode.VIDEOS -> ext in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp")
                PickerFilterMode.AUDIOS -> ext in listOf("mp3", "wav", "m4a", "aac", "ogg", "flac")
                PickerFilterMode.DOCUMENTS -> ext in listOf("pdf", "doc", "docx", "txt", "md", "xls", "xlsx", "csv")
                PickerFilterMode.ALL_FILES -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else file.name.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }.sortedBy { it.name.lowercase() }

        dirs + files
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("custom_file_picker_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header with back and close buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val canGoUp = currentDirectory.parentFile != null && currentDirectory.parentFile?.canRead() == true
                    IconButton(
                        onClick = {
                            currentDirectory.parentFile?.let { currentDirectory = it }
                        },
                        enabled = canGoUp
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Carpeta superior",
                            tint = if (canGoUp) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMultiSelect) "Seleccionar Archivos" else "Explorador de Archivos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentDirectory.name.ifEmpty { currentDirectory.absolutePath },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar archivo...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda", modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("file_picker_search"),
                    singleLine = true
                )

                // Storage permission banner if full storage access is not granted
                if (!permissionState.hasAllFilesAccess) {
                    Spacer(modifier = Modifier.height(10.dp))
                    StoragePermissionCalloutBanner(
                        onGrantClick = {
                            val intent = StoragePermissionManager.getAllFilesAccessIntent(context)
                            context.startActivity(intent)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick directory chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(rootLocations) { (name, dir) ->
                        val isCurrent = currentDirectory.absolutePath == dir.absolutePath
                        FilterChip(
                            selected = isCurrent,
                            onClick = { currentDirectory = dir },
                            label = { Text(name) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isCurrent) Icons.Default.FolderOpen else Icons.Default.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                // Filter format chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(PickerFilterMode.values()) { mode ->
                        val isSelected = filterMode == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { filterMode = mode },
                            label = {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Files and Folders List
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (fileItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Carpeta vacía o sin coincidencias",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(fileItems) { item ->
                                if (item.isDirectory) {
                                    DirectoryListItem(
                                        dir = item,
                                        onClick = { currentDirectory = item }
                                    )
                                } else {
                                    val isChecked = selectedFiles.contains(item)
                                    FileListItemWithArchiveIcon(
                                        file = item,
                                        isMultiSelect = isMultiSelect,
                                        isSelected = isChecked,
                                        onToggleSelect = {
                                            selectedFiles = if (isChecked) {
                                                selectedFiles - item
                                            } else {
                                                selectedFiles + item
                                            }
                                        },
                                        onClick = {
                                            if (isMultiSelect) {
                                                selectedFiles = if (isChecked) {
                                                    selectedFiles - item
                                                } else {
                                                    selectedFiles + item
                                                }
                                            } else {
                                                onFileSelected(item)
                                                onDismiss()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onLaunchSystemPicker,
                        modifier = Modifier.testTag("btn_launch_system_picker")
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Explorador Sistema")
                    }

                    if (isMultiSelect) {
                        Button(
                            onClick = {
                                onMultipleFilesSelected(selectedFiles.toList())
                                onDismiss()
                            },
                            enabled = selectedFiles.isNotEmpty(),
                            modifier = Modifier.testTag("btn_confirm_selected_files")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Seleccionar (${selectedFiles.size})")
                        }
                    } else {
                        OutlinedButton(onClick = onDismiss) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectoryListItem(
    dir: File,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = VibrantAmber.copy(alpha = 0.15f),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = VibrantAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dir.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val count = dir.listFiles()?.size ?: 0
                Text(
                    text = "$count elementos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun FileListItemWithArchiveIcon(
    file: File,
    isMultiSelect: Boolean,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onClick: () -> Unit
) {
    val ext = file.extension.lowercase()
    val archiveType = ArchiveFileIconType.fromExtension(ext)
    val category = FileCategory.fromFileName(file.name)

    val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val dateStr = remember(file) { sdf.format(Date(file.lastModified())) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("file_item_${file.name}")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMultiSelect) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelect() },
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Visual Icon with custom Archive styling
            if (archiveType != null) {
                ArchiveFormatVisualBadge(archiveType = archiveType, size = 38.dp)
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        FileCategoryIcon(category = category, size = 20.dp)
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (archiveType != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = archiveType.color.copy(alpha = 0.2f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = archiveType.label,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = archiveType.color,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Text(
                        text = "${ProcessedFile.formatBytes(file.length())} • $dateStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.ArchiveCreationSettings
import com.example.data.model.ArchiveOutputFormat
import com.example.data.model.AudioCompressionSettings
import com.example.data.model.AudioPreset
import com.example.data.model.CompressionLevelPreset
import com.example.data.model.MediaMetadata
import com.example.data.model.VideoCompressionSettings
import com.example.data.model.VideoPreset
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber

/**
 * Settings dialog for Archive compression tasks (ZIP / 7Z).
 * Allows users to choose output format and compression level (store, fastest, normal, best).
 */
@Composable
fun ArchiveCompressionSettingsDialog(
    initialSettings: ArchiveCreationSettings,
    totalFilesCount: Int,
    totalFilesSizeBytes: Long = 0L,
    onDismiss: () -> Unit,
    onConfirm: (ArchiveCreationSettings) -> Unit
) {
    var format by remember { mutableStateOf(initialSettings.format) }
    var archiveName by remember { mutableStateOf(initialSettings.archiveName) }
    var selectedLevelPreset by remember { mutableStateOf(initialSettings.levelPreset) }
    var compressionLevel by remember { mutableStateOf(initialSettings.compressionLevel) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .padding(vertical = 12.dp)
                .testTag("dialog_compression_settings")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ajustes de Compresión",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$totalFilesCount archivos seleccionados",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Output Format Selection
                    Column {
                        Text(
                            text = "Formato de Salida",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ArchiveOutputFormat.values().forEach { fmt ->
                                val isSelected = format == fmt
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { format = fmt }
                                        .testTag("chip_format_${fmt.ext}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (fmt == ArchiveOutputFormat.ZIP) Icons.Default.FolderZip else Icons.Default.Inventory2,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = fmt.label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (fmt == ArchiveOutputFormat.ZIP) "Universal" else "Máxima tasa",
                                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Archive Name
                    Column {
                        Text(
                            text = "Nombre del Archivo",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = archiveName,
                            onValueChange = { archiveName = it },
                            placeholder = { Text("Nombre del archivo") },
                            trailingIcon = {
                                Text(
                                    text = ".${format.ext}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_archive_name"),
                            singleLine = true
                        )
                    }

                    // 3. Compression Level Strategy
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Nivel y Algoritmo de Compresión",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CompressionLevelPreset.values().forEach { preset ->
                                val isSelected = selectedLevelPreset == preset
                                val levelColor = when (preset) {
                                    CompressionLevelPreset.STORE -> VibrantAmber
                                    CompressionLevelPreset.FASTEST -> DeepCyan
                                    CompressionLevelPreset.NORMAL -> NeonCyan
                                    CompressionLevelPreset.BEST_COMPRESSION -> SuccessGreen
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) levelColor.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.5.dp,
                                        if (isSelected) levelColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            selectedLevelPreset = preset
                                            compressionLevel = preset.level
                                        }
                                        .testTag("preset_level_${preset.name.lowercase()}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = if (isSelected) levelColor else MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.surface,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = when (preset) {
                                                            CompressionLevelPreset.STORE -> Icons.Default.Inventory2
                                                            CompressionLevelPreset.FASTEST -> Icons.Default.Bolt
                                                            CompressionLevelPreset.NORMAL -> Icons.Default.Speed
                                                            CompressionLevelPreset.BEST_COMPRESSION -> Icons.Default.RocketLaunch
                                                        },
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = preset.title,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) levelColor else MaterialTheme.colorScheme.onSurface
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = levelColor.copy(alpha = 0.18f)
                                                ) {
                                                    Text(
                                                        text = preset.speedTag,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = levelColor,
                                                        fontWeight = FontWeight.SemiBold,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${preset.subtitle} • Estimado: ${preset.estimatedRatio}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Fine-tuning slider for ZIP
                        if (format == ArchiveOutputFormat.ZIP) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Ajuste manual de Deflater (Nivel $compressionLevel / 9):",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Slider(
                                value = compressionLevel.toFloat(),
                                onValueChange = {
                                    compressionLevel = it.toInt()
                                },
                                valueRange = 0f..9f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Pre-compression size preview UI element
                    if (totalFilesSizeBytes > 0L) {
                        val estimatedArchiveSize = remember(totalFilesSizeBytes, format, compressionLevel) {
                            val ratio = when (format) {
                                ArchiveOutputFormat.ZIP -> when (compressionLevel) {
                                    0 -> 1.0
                                    in 1..3 -> 0.70
                                    in 4..6 -> 0.52
                                    else -> 0.44
                                }
                                ArchiveOutputFormat.SEVEN_ZIP -> when (compressionLevel) {
                                    0 -> 1.0
                                    in 1..3 -> 0.58
                                    in 4..6 -> 0.38
                                    else -> 0.30
                                }
                            }
                            (totalFilesSizeBytes * ratio).toLong().coerceIn(1024L, totalFilesSizeBytes)
                        }

                        CompressionSizePreviewCard(
                            originalSizeBytes = totalFilesSizeBytes,
                            calculatedSizeBytes = estimatedArchiveSize,
                            algorithmOrPresetLabel = "${format.label} • Nivel $compressionLevel (${selectedLevelPreset.title})"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            val finalSettings = ArchiveCreationSettings(
                                format = format,
                                archiveName = archiveName.trim().ifEmpty { "Archivo_Comprimido" },
                                compressionLevel = compressionLevel,
                                levelPreset = selectedLevelPreset
                            )
                            onConfirm(finalSettings)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirm_compression")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Comprimir")
                    }
                }
            }
        }
    }
}

/**
 * Settings dialog for Media (Audio and Video) compression tasks.
 */
@Composable
fun MediaCompressionSettingsDialog(
    media: MediaMetadata,
    initialVideoSettings: VideoCompressionSettings,
    initialAudioSettings: AudioCompressionSettings,
    onDismiss: () -> Unit,
    onConfirmVideo: (VideoCompressionSettings) -> Unit,
    onConfirmAudio: (AudioCompressionSettings) -> Unit
) {
    var videoPreset by remember { mutableStateOf(initialVideoSettings.preset) }
    var audioPreset by remember { mutableStateOf(initialAudioSettings.preset) }
    var isMono by remember { mutableStateOf(initialAudioSettings.isMono) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
                .testTag("dialog_media_settings")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ajustes de Optimización",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = media.fileName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (media.isVideo) {
                        Text(
                            text = "Calidad y Tasa de Reducción",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        VideoPreset.values().forEach { preset ->
                            val isSelected = videoPreset == preset
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { videoPreset = preset }
                                    .testTag("video_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
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
                                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "Perfil de Compresión de Audio",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )

                        AudioPreset.values().forEach { preset ->
                            val isSelected = audioPreset == preset
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) DeepEmerald.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isSelected) DeepEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { audioPreset = preset }
                                    .testTag("audio_preset_${preset.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
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
                                        Icon(Icons.Default.Check, contentDescription = null, tint = DeepEmerald)
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Canal Mono (Voz / Podcasts)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Reduce significativamente el tamaño del archivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = isMono, onCheckedChange = { isMono = it })
                        }
                    }

                    // Pre-compression size preview UI element
                    val calculatedMediaSize = remember(media, videoPreset, audioPreset, isMono) {
                        if (media.isVideo) {
                            CompressionSizeCalculator.calculateEstimatedVideoSize(media, videoPreset)
                        } else {
                            CompressionSizeCalculator.calculateEstimatedAudioSize(media, audioPreset, isMono)
                        }
                    }
                    val mediaPresetLabel = if (media.isVideo) {
                        "${videoPreset.title} (${videoPreset.desc})"
                    } else {
                        "${audioPreset.title}${if (isMono) " + Modo Mono" else ""}"
                    }

                    CompressionSizePreviewCard(
                        originalSizeBytes = media.fileSizeBytes,
                        calculatedSizeBytes = calculatedMediaSize,
                        algorithmOrPresetLabel = mediaPresetLabel,
                        accentColor = if (media.isVideo) DeepCyan else DeepEmerald
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar")
                    }
                    Button(
                        onClick = {
                            if (media.isVideo) {
                                onConfirmVideo(initialVideoSettings.copy(preset = videoPreset))
                            } else {
                                onConfirmAudio(initialAudioSettings.copy(preset = audioPreset, isMono = isMono))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_confirm_media_compression")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Comprimir")
                    }
                }
            }
        }
    }
}

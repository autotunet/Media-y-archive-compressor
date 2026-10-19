package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ArchiveOutputFormat
import com.example.data.model.AudioPreset
import com.example.data.model.MediaMetadata
import com.example.data.model.ProcessedFile
import com.example.data.model.VideoPreset
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber
import java.io.File
import kotlin.math.max

/**
 * Calculators for estimated compressed sizes prior to task initiation.
 */
object CompressionSizeCalculator {

    /**
     * Estimates output size for multi-file archive creations (ZIP / 7Z).
     * Accounts for file extensions (plain text vs media) and compression levels.
     */
    fun calculateEstimatedArchiveSize(
        files: List<File>,
        format: ArchiveOutputFormat,
        compressionLevel: Int
    ): Long {
        if (files.isEmpty()) return 0L
        val totalBytes = files.sumOf { if (it.exists()) it.length() else 0L }
        if (totalBytes <= 0L) return 1024L

        // Store level has no compression
        if (compressionLevel == 0) {
            return (totalBytes + (files.size * 128L)).coerceAtLeast(1024L)
        }

        // Weighted compressibility ratio based on extensions
        var totalWeightedRatio = 0.0
        files.forEach { file ->
            val ext = file.extension.lowercase()
            val fileRatio = when (ext) {
                // Highly compressible text and data
                "txt", "log", "json", "xml", "csv", "html", "md", "sql", "java", "kt", "c", "cpp", "py", "sh" -> 0.22
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx" -> 0.45
                // Raw uncompressed audio or bitmap
                "wav", "bmp", "tga", "raw" -> 0.48
                // Pre-compressed media files (minimal gains)
                "mp4", "mkv", "avi", "mov", "mp3", "aac", "ogg", "flac", "jpg", "jpeg", "png", "webp", "zip", "7z", "gz", "tar", "rar" -> 0.94
                // General binaries or unknown
                else -> 0.60
            }
            val weight = if (totalBytes > 0) file.length().toDouble() / totalBytes else 1.0 / files.size
            totalWeightedRatio += fileRatio * weight
        }

        // Adjust for algorithm and level
        val levelFactor = when (compressionLevel) {
            in 1..3 -> 1.15
            in 4..6 -> 1.00
            in 7..8 -> 0.92
            else -> 0.85
        }

        val formatMultiplier = when (format) {
            ArchiveOutputFormat.ZIP -> 1.0
            ArchiveOutputFormat.SEVEN_ZIP -> 0.80 // LZMA2 is ~20% tighter than Deflate
        }

        val estimatedRatio = (totalWeightedRatio * levelFactor * formatMultiplier).coerceIn(0.08, 0.98)
        val estimatedBytes = (totalBytes * estimatedRatio).toLong()

        // Overhead for archive header
        val headerOverhead = (files.size * 96L) + 512L
        return (estimatedBytes + headerOverhead).coerceIn(1024L, (totalBytes * 0.99).toLong().coerceAtLeast(1024L))
    }

    /**
     * Estimates output size for video compression tasks based on preset or bitrate and duration.
     */
    fun calculateEstimatedVideoSize(
        media: MediaMetadata,
        preset: VideoPreset
    ): Long {
        val origBytes = media.fileSizeBytes
        if (origBytes <= 0L) return 0L

        // If duration is available, calculate exact expected bitrate
        if (media.durationMs > 1000L) {
            val durationSec = media.durationMs / 1000.0
            val targetBitrateKbps = when (preset) {
                VideoPreset.MAX_COMPRESSION -> 500 + 64 // 564 kbps total
                VideoPreset.BALANCED -> 1100 + 128 // 1228 kbps total
                VideoPreset.HIGH_QUALITY -> 2200 + 192 // 2392 kbps total
                VideoPreset.CUSTOM -> 1000 + 128
            }
            val calculatedBytes = ((targetBitrateKbps * 1000L / 8.0) * durationSec).toLong()
            return calculatedBytes.coerceIn(10_240L, (origBytes * 0.85).toLong())
        }

        // Fallback reduction factors
        val ratio = when (preset) {
            VideoPreset.MAX_COMPRESSION -> 0.28 // ~72% savings
            VideoPreset.BALANCED -> 0.48 // ~52% savings
            VideoPreset.HIGH_QUALITY -> 0.72 // ~28% savings
            VideoPreset.CUSTOM -> 0.50
        }
        return (origBytes * ratio).toLong().coerceAtLeast(10_240L)
    }

    /**
     * Estimates output size for audio compression tasks.
     */
    fun calculateEstimatedAudioSize(
        media: MediaMetadata,
        preset: AudioPreset,
        isMono: Boolean
    ): Long {
        val origBytes = media.fileSizeBytes
        if (origBytes <= 0L) return 0L

        val monoMultiplier = if (isMono) 0.65 else 1.0

        if (media.durationMs > 1000L) {
            val durationSec = media.durationMs / 1000.0
            val bitrateKbps = when (preset) {
                AudioPreset.VOICE_PODCAST -> 64
                AudioPreset.BALANCED -> 96
                AudioPreset.HIGH_QUALITY -> 128
            }
            val calculatedBytes = (((bitrateKbps * 1000L / 8.0) * durationSec) * monoMultiplier).toLong()
            return calculatedBytes.coerceIn(4096L, (origBytes * 0.90).toLong())
        }

        val baseRatio = when (preset) {
            AudioPreset.VOICE_PODCAST -> 0.30
            AudioPreset.BALANCED -> 0.52
            AudioPreset.HIGH_QUALITY -> 0.74
        }
        val finalRatio = (baseRatio * monoMultiplier).coerceIn(0.15, 0.90)
        return (origBytes * finalRatio).toLong().coerceAtLeast(4096L)
    }
}

/**
 * Material 3 UI component that displays the original and calculated compressed file sizes
 * before the user starts the compression task.
 */
@Composable
fun CompressionSizePreviewCard(
    originalSizeBytes: Long,
    calculatedSizeBytes: Long,
    algorithmOrPresetLabel: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
    accentColor: Color = SuccessGreen
) {
    if (originalSizeBytes <= 0L) return

    val validCompressedSize = calculatedSizeBytes.coerceIn(1L, originalSizeBytes)
    val savedBytes = max(0L, originalSizeBytes - validCompressedSize)
    val savingsPercent = ((savedBytes.toDouble() / originalSizeBytes) * 100.0).toInt().coerceIn(0, 99)
    val compressedRatioFloat = (validCompressedSize.toFloat() / originalSizeBytes.toFloat()).coerceIn(0.05f, 1f)

    val animatedBarProgress by animateFloatAsState(
        targetValue = compressedRatioFloat,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "compressed_ratio_anim"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = androidx.compose.foundation.BorderStroke(
            1.2.dp,
            accentColor.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("compression_size_preview_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Title and Savings Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = accentColor.copy(alpha = 0.18f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Estimación de Tamaño Comprimido",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // High-visibility savings badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = accentColor.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "-$savingsPercent% ESPACIO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.testTag("text_savings_badge")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Comparison Row: Original Size -> Arrow -> Calculated Size
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Original Size column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ORIGINAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = ProcessedFile.formatBytes(originalSizeBytes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.testTag("text_original_size")
                    )
                }

                // Directional Indicator
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Comprime hacia",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Calculated Compressed Size column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ESTIMADO COMPRIMIDO",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = ProcessedFile.formatBytes(validCompressedSize),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        modifier = Modifier.testTag("text_compressed_size")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visual comparison proportion bar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Tamaño final: ${(animatedBarProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Ahorro: -${ProcessedFile.formatBytes(savedBytes)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Dual visual meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor.copy(alpha = 0.22f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedBarProgress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Context footer explanation
            Text(
                text = "Calculado según: $algorithmOrPresetLabel",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

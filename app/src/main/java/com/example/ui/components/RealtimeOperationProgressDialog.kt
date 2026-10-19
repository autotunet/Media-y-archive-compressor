package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.FileCategory
import com.example.data.model.ProcessedFile
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber
import com.example.ui.viewmodel.OperationProgress
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

/**
 * High-visibility real-time progress dialog that provides detailed feedback
 * while compression or extraction operations are running.
 */
@Composable
fun RealtimeOperationProgressDialog(
    progress: OperationProgress,
    onMinimizeToBackground: () -> Unit,
    onCancel: () -> Unit,
    onOpenResult: ((File) -> Unit)? = null,
    onSaveToDownloads: ((File) -> Unit)? = null,
    onShareResult: ((File) -> Unit)? = null
) {
    if (progress is OperationProgress.Idle) return

    val isRunning = progress is OperationProgress.Running
    val isSuccess = progress is OperationProgress.Success
    val isError = progress is OperationProgress.Error

    Dialog(
        onDismissRequest = {
            if (isRunning) onMinimizeToBackground() else onCancel()
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = !isRunning,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                when {
                    isSuccess -> SuccessGreen.copy(alpha = 0.5f)
                    isError -> ErrorRed.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
                .testTag("realtime_operation_progress_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (progress) {
                    is OperationProgress.Running -> {
                        RunningProgressContent(
                            running = progress,
                            onMinimize = onMinimizeToBackground,
                            onCancel = onCancel
                        )
                    }

                    is OperationProgress.Success -> {
                        SuccessProgressContent(
                            success = progress,
                            onDismiss = onCancel,
                            onOpen = onOpenResult,
                            onSave = onSaveToDownloads,
                            onShare = onShareResult
                        )
                    }

                    is OperationProgress.Error -> {
                        ErrorProgressContent(
                            error = progress,
                            onDismiss = onCancel
                        )
                    }

                    OperationProgress.Idle -> Unit
                }
            }
        }
    }
}

@Composable
private fun RunningProgressContent(
    running: OperationProgress.Running,
    onMinimize: () -> Unit,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_and_rotate")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spin_gear"
    )

    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    // Elapsed time ticker
    var elapsedSeconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val elapsedFormatted = remember(elapsedSeconds) {
        val min = elapsedSeconds / 60
        val sec = elapsedSeconds % 60
        String.format(Locale.US, "%02d:%02d", min, sec)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = running.progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "dialog_bar_anim"
    )

    val etaFormatted = remember(animatedProgress, elapsedSeconds) {
        if (animatedProgress > 0.08f && animatedProgress < 0.99f && elapsedSeconds > 1) {
            val totalSeconds = (elapsedSeconds / animatedProgress).toInt()
            val remaining = (totalSeconds - elapsedSeconds).coerceAtLeast(1)
            val min = remaining / 60
            val sec = remaining % 60
            if (min > 0) "~${min}m ${sec}s" else "~${sec}s"
        } else {
            "--"
        }
    }

    // Header with animated spinner and title
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(rotation)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = running.operationName.ifEmpty { "Procesando archivos" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "En ejecución en tiempo real",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Percentage Badge
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "${(animatedProgress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("dialog_progress_percentage_badge")
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Real-Time Progress Bar with rounded pill shape and gradient
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                RoundedCornerShape(7.dp)
            )
            .testTag("dialog_realtime_linear_bar")
    ) {
        // Active filled progress gradient
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(DeepCyan, NeonCyan, ElectricEmerald)
                    )
                )
        )
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Telemetry Statistics Panel
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Status message / file
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val fileCat = if (running.currentFileName.isNotEmpty()) {
                    FileCategory.fromFileName(running.currentFileName)
                } else FileCategory.ARCHIVE

                FileCategoryIcon(category = fileCat, size = 18.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = running.message.ifEmpty { "Comprimiendo contenido..." },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Bytes and item count row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (running.totalBytes > 0L) {
                    val procStr = ProcessedFile.formatBytes(running.bytesProcessed.coerceAtLeast(0L))
                    val totalStr = ProcessedFile.formatBytes(running.totalBytes)
                    Text(
                        text = "Procesado: $procStr / $totalStr",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Procesando flujo de datos...",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (running.totalItems > 1) {
                    Text(
                        text = "Elemento ${running.currentItem} de ${running.totalItems}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Time & ETA row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Transcurrido: $elapsedFormatted",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (etaFormatted != "--") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Restante: $etaFormatted",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Step checkpoints milestone
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val step = when {
            animatedProgress < 0.15f -> 1
            animatedProgress < 0.95f -> 2
            else -> 3
        }

        StepItem(number = 1, label = "Preparación", isActive = step >= 1, isCurrent = step == 1)
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (step >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
        StepItem(number = 2, label = "Procesando", isActive = step >= 2, isCurrent = step == 2)
        Box(
            modifier = Modifier
                .width(28.dp)
                .height(2.dp)
                .background(if (step >= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
        )
        StepItem(number = 3, label = "Guardado", isActive = step >= 3, isCurrent = step == 3)
    }

    Spacer(modifier = Modifier.height(22.dp))

    // Control buttons: Background vs Cancel
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick = onCancel,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
            modifier = Modifier
                .weight(1f)
                .testTag("btn_cancel_operation")
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Cancelar")
        }

        Button(
            onClick = onMinimize,
            modifier = Modifier
                .weight(1f)
                .testTag("btn_minimize_to_background")
        ) {
            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Segundo plano")
        }
    }
}

@Composable
private fun StepItem(
    number: Int,
    label: String,
    isActive: Boolean,
    isCurrent: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            border = if (isCurrent) androidx.compose.foundation.BorderStroke(2.dp, NeonCyan) else null,
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$number",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SuccessProgressContent(
    success: OperationProgress.Success,
    onDismiss: () -> Unit,
    onOpen: ((File) -> Unit)?,
    onSave: ((File) -> Unit)?,
    onShare: ((File) -> Unit)?
) {
    Surface(
        shape = CircleShape,
        color = SuccessGreen.copy(alpha = 0.18f),
        modifier = Modifier.size(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = SuccessGreen,
                modifier = Modifier.size(38.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = "¡Operación Completada!",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
        text = success.message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    if (success.resultFile != null) {
        val file = success.resultFile
        Spacer(modifier = Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FileCategoryIcon(category = FileCategory.fromFileName(file.name), size = 24.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Tamaño: ${ProcessedFile.formatBytes(file.length())}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Action buttons
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (success.resultFile != null && onOpen != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onOpen(success.resultFile)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Abrir")
                }
            }

            if (success.resultFile != null && onSave != null) {
                OutlinedButton(
                    onClick = {
                        onSave(success.resultFile)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Descargas")
                }
            }
        }

        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar")
        }
    }
}

@Composable
private fun ErrorProgressContent(
    error: OperationProgress.Error,
    onDismiss: () -> Unit
) {
    Surface(
        shape = CircleShape,
        color = ErrorRed.copy(alpha = 0.18f),
        modifier = Modifier.size(56.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = null,
                tint = ErrorRed,
                modifier = Modifier.size(34.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
        text = "Fallo en la Operación",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = ErrorRed,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = error.errorMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(20.dp))

    Button(
        onClick = onDismiss,
        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Aceptar")
    }
}

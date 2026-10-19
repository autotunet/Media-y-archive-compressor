package com.example.ui.components

import android.content.ClipData
import android.content.ClipDescription
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.DragEvent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import com.example.data.model.FileCategory
import com.example.ui.theme.DeepCyan
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VibrantAmber
import java.io.File
import kotlin.math.roundToInt

/**
 * Interactive File Drop Zone that provides:
 * 1. Native Android system drag-and-drop support (split-screen, external file manager, desktop drag in emulator)
 * 2. Visual highlight states (dashed border, glowing pulse, bouncing arrow, scale animation)
 * 3. In-app interactive draggable chips to easily test and demonstrate the drag-and-drop workflow
 * 4. Tap-to-pick fallback opening internal or system file picker
 */
@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalLayoutApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun InteractiveFileDropZone(
    onFilesDropped: (List<Uri>) -> Unit,
    onFileSelected: (File) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onOpenSystemPicker: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Zona de Arrastre Inteligente",
    subtitle: String = "Arrastra archivos aquí desde cualquier app o toca para explorar",
    acceptedFormats: List<String> = listOf("ISO", "ZIP", "RAR", "7Z", "MP4", "MP3", "DOCS"),
    sampleFiles: List<File> = emptyList(),
    bannerMessage: String? = null
) {
    val context = LocalContext.current
    var isSystemDragActive by remember { mutableStateOf(false) }
    var isSystemDragHovered by remember { mutableStateOf(false) }
    var isInAppDragHovered by remember { mutableStateOf(false) }
    var lastDroppedName by remember { mutableStateOf<String?>(null) }

    val isHovered = isSystemDragHovered || isInAppDragHovered
    val isActive = isSystemDragActive || isHovered

    // Haptic feedback function
    val triggerHaptic = {
        try {
            val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(40)
                }
            }
        } catch (_: Exception) {}
    }

    // Compose Native Drag & Drop Target
    val dropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isSystemDragActive = true
            }

            override fun onEntered(event: DragAndDropEvent) {
                isSystemDragHovered = true
                triggerHaptic()
            }

            override fun onMoved(event: DragAndDropEvent) {
                isSystemDragHovered = true
            }

            override fun onExited(event: DragAndDropEvent) {
                isSystemDragHovered = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isSystemDragActive = false
                isSystemDragHovered = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                isSystemDragHovered = false
                isSystemDragActive = false
                val androidDragEvent = event.toAndroidDragEvent()
                val clipData = androidDragEvent.clipData ?: return false
                val uriList = mutableListOf<Uri>()

                for (i in 0 until clipData.itemCount) {
                    val item = clipData.getItemAt(i)
                    item.uri?.let { uriList.add(it) }
                }

                if (uriList.isNotEmpty()) {
                    triggerHaptic()
                    lastDroppedName = uriList.first().lastPathSegment ?: "archivo"
                    onFilesDropped(uriList)
                    return true
                }
                return false
            }
        }
    }

    // Infinite transition for glowing highlight pulse when hovering
    val infiniteTransition = rememberInfiniteTransition(label = "dropZonePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounceOffset"
    )

    // Animated scale and colors
    val cardScale by animateFloatAsState(
        targetValue = if (isHovered) 1.03f else 1.0f,
        animationSpec = tween(250),
        label = "cardScale"
    )

    val borderColor by animateColorAsState(
        targetValue = when {
            isHovered -> NeonCyan
            isActive -> ElectricEmerald
            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        },
        animationSpec = tween(300),
        label = "borderColor"
    )

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isHovered -> NeonCyan.copy(alpha = 0.16f)
            isActive -> ElectricEmerald.copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(300),
        label = "backgroundColor"
    )

    // Bounds tracking for in-app drag simulation
    var dropZoneBoundsInWindow by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("interactive_drop_zone_container")
    ) {
        // Main Drop Zone Card with dashed border canvas and drag target
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .scale(cardScale)
                .onGloballyPositioned { coordinates ->
                    dropZoneBoundsInWindow = coordinates.boundsInWindow()
                }
                .shadow(
                    elevation = if (isHovered) 12.dp else 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    spotColor = if (isHovered) NeonCyan else Color.Transparent
                )
                .clip(RoundedCornerShape(20.dp))
                .background(backgroundColor)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        val mimeTypes = event.toAndroidDragEvent().clipDescription
                        mimeTypes != null
                    },
                    target = dropTarget
                )
                .clickable { onOpenCustomPicker() }
                .padding(2.dp)
        ) {
            // Custom Canvas for dashed outline border
            Canvas(modifier = Modifier.matchParentSize()) {
                val strokeWidth = if (isHovered) 3.5.dp.toPx() else 1.8.dp.toPx()
                val dashLength = if (isHovered) 16.dp.toPx() else 10.dp.toPx()
                val gapLength = if (isHovered) 8.dp.toPx() else 6.dp.toPx()

                drawRoundRect(
                    color = borderColor,
                    size = size,
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(
                            floatArrayOf(dashLength, gapLength),
                            phase = if (isHovered) pulseAlpha * 40f else 0f
                        )
                    )
                )
            }

            // Drop zone interior content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Interactive State Badge
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isHovered) NeonCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(
                        1.dp,
                        if (isHovered) NeonCyan.copy(alpha = pulseAlpha) else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isHovered) ElectricEmerald else NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHovered) "¡ZONA ACTIVA: SUELTA AHORA!" else "ZONA DE ARRASTRE Y SOLTADO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isHovered) NeonCyan else MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                // Animated Center Icon with Glow Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = if (isHovered) {
                                    listOf(NeonCyan.copy(alpha = 0.4f), Color.Transparent)
                                } else {
                                    listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), Color.Transparent)
                                }
                            )
                        )
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isHovered) NeonCyan else MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .size(54.dp)
                            .offset(y = if (isHovered) bounceOffset.dp else 0.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    isHovered -> Icons.Default.FileDownload
                                    isActive -> Icons.Default.ArrowDownward
                                    else -> Icons.Default.FileUpload
                                },
                                contentDescription = "Arrastrar archivo",
                                tint = if (isHovered) Color.Black else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Title & Subtitle with high-visibility call to action
                Text(
                    text = if (isHovered) "¡Suelta tu archivo aquí!" else title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isHovered) NeonCyan else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (isHovered) "Se detectará el tipo de archivo automáticamente" else subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isHovered) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Supported Format Badges
                FlowRow(
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    acceptedFormats.forEach { format ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            border = BorderStroke(0.7.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.padding(horizontal = 3.dp)
                        ) {
                            Text(
                                text = format,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons inside the Drop Zone
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onOpenCustomPicker,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_drop_zone_browse_storage")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Explorar",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenSystemPicker,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("btn_drop_zone_system_picker")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sistema",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        // Feedback notification banner when a file has just been dropped
        if (bannerMessage != null || lastDroppedName != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = ElectricEmerald.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, ElectricEmerald.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = ElectricEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bannerMessage ?: "✓ Archivo recibido: ${lastDroppedName} listo para procesar",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // In-App Interactive Drag Simulation Bar
        // Allows users to test and experience the drag-and-drop animation directly on single-window mobile & emulator
        if (sampleFiles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            InAppDraggableSampleBar(
                sampleFiles = sampleFiles.take(5),
                dropZoneBounds = dropZoneBoundsInWindow,
                onHoverChange = { hovered ->
                    isInAppDragHovered = hovered
                    if (hovered) triggerHaptic()
                },
                onFileDroppedIntoZone = { file ->
                    triggerHaptic()
                    lastDroppedName = file.name
                    onFileSelected(file)
                }
            )
        }
    }
}

/**
 * Interactive bar containing draggable file items that can be dragged directly
 * into the drop zone or tapped for instant selection.
 */
@Composable
private fun InAppDraggableSampleBar(
    sampleFiles: List<File>,
    dropZoneBounds: androidx.compose.ui.geometry.Rect?,
    onHoverChange: (Boolean) -> Unit,
    onFileDroppedIntoZone: (File) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Layers,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Arrastra o toca un archivo de prueba para probar la zona:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            sampleFiles.forEach { file ->
                DraggableSampleFileChip(
                    file = file,
                    dropZoneBounds = dropZoneBounds,
                    onHoverChange = onHoverChange,
                    onDropInZone = { onFileDroppedIntoZone(file) },
                    onTap = { onFileDroppedIntoZone(file) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * An individual chip that can be dragged into the drop zone with real-time feedback
 * or tapped to select immediately.
 */
@Composable
private fun DraggableSampleFileChip(
    file: File,
    dropZoneBounds: androidx.compose.ui.geometry.Rect?,
    onHoverChange: (Boolean) -> Unit,
    onDropInZone: () -> Unit,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var chipBoundsInWindow by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }

    val cat = FileCategory.fromFileName(file.name)
    val badgeColor = when (cat) {
        FileCategory.ARCHIVE -> DeepCyan
        FileCategory.VIDEO -> NeonCyan
        FileCategory.AUDIO -> ElectricEmerald
        else -> VibrantAmber
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                chipBoundsInWindow = coordinates.boundsInWindow()
            }
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .scale(if (isDragging) 1.1f else 1.0f)
            .shadow(if (isDragging) 8.dp else 0.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(if (isDragging) badgeColor.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface)
            .border(
                1.dp,
                if (isDragging) badgeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                RoundedCornerShape(10.dp)
            )
            .clickable { onTap() }
            .pointerInput(file, dropZoneBounds) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                        // Check if current position intersects with drop zone bounds
                        val currentBounds = chipBoundsInWindow
                        if (currentBounds != null && dropZoneBounds != null) {
                            val currentCenterX = currentBounds.left + offsetX + currentBounds.width / 2
                            val currentCenterY = currentBounds.top + offsetY + currentBounds.height / 2
                            if (dropZoneBounds.contains(androidx.compose.ui.geometry.Offset(currentCenterX, currentCenterY))) {
                                onDropInZone()
                            }
                        }
                        offsetX = 0f
                        offsetY = 0f
                        onHoverChange(false)
                    },
                    onDragCancel = {
                        isDragging = false
                        offsetX = 0f
                        offsetY = 0f
                        onHoverChange(false)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offsetX += dragAmount.x
                        offsetY += dragAmount.y

                        val currentBounds = chipBoundsInWindow
                        if (currentBounds != null && dropZoneBounds != null) {
                            val currentCenterX = currentBounds.left + offsetX + currentBounds.width / 2
                            val currentCenterY = currentBounds.top + offsetY + currentBounds.height / 2
                            val isInside = dropZoneBounds.contains(
                                androidx.compose.ui.geometry.Offset(currentCenterX, currentCenterY)
                            )
                            onHoverChange(isInside)
                        }
                    }
                )
            }
            .padding(horizontal = 6.dp, vertical = 6.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = badgeColor.copy(alpha = 0.15f),
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Text(
                    text = file.extension.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            Text(
                text = file.name,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

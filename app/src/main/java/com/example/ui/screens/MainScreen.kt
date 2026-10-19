package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderZip
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.graphics.Color
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import com.example.core.permission.StoragePermissionManager
import com.example.ui.components.AppSettingsDialog
import com.example.ui.components.BottomAdBanner
import com.example.ui.components.OngoingTaskTopBar
import com.example.ui.components.PreviewDialog
import com.example.ui.components.StoragePermissionCalloutBanner
import com.example.ui.components.StoragePermissionDialog
import com.example.ui.theme.ElectricEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.VibrantAmber
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.OperationProgress

@OptIn(
    ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = true,
    onShowInterstitial: () -> Unit = {}
) {
    val currentTab by viewModel.currentTab.collectAsState()
    val previewFile by viewModel.previewFile.collectAsState()
    val mediaProgress by viewModel.mediaCompressionProgress.collectAsState()
    val archiveProgress by viewModel.archiveCreationProgress.collectAsState()
    val extractionProgress by viewModel.extractionProgress.collectAsState()
    val dropBannerMessage by viewModel.dropBannerMessage.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()

    val context = LocalContext.current
    val permissionState by viewModel.storagePermissionState.collectAsState()
    var showPermissionDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    val standardPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionState()
    }

    var isGlobalDragActive by remember { mutableStateOf(false) }
    var isGlobalDragHovered by remember { mutableStateOf(false) }

    val globalDropTarget = remember {
        object : DragAndDropTarget {
            override fun onStarted(event: DragAndDropEvent) {
                isGlobalDragActive = true
            }
            override fun onEntered(event: DragAndDropEvent) {
                isGlobalDragHovered = true
            }
            override fun onMoved(event: DragAndDropEvent) {
                isGlobalDragHovered = true
            }
            override fun onExited(event: DragAndDropEvent) {
                isGlobalDragHovered = false
            }
            override fun onEnded(event: DragAndDropEvent) {
                isGlobalDragActive = false
                isGlobalDragHovered = false
            }
            override fun onDrop(event: DragAndDropEvent): Boolean {
                isGlobalDragActive = false
                isGlobalDragHovered = false
                val androidDragEvent = event.toAndroidDragEvent()
                val clipData = androidDragEvent.clipData ?: return false
                val uriList = mutableListOf<android.net.Uri>()
                for (i in 0 until clipData.itemCount) {
                    val item = clipData.getItemAt(i)
                    item.uri?.let { uriList.add(it) }
                }
                if (uriList.isNotEmpty()) {
                    viewModel.handleDroppedUris(uriList)
                    return true
                }
                return false
            }
        }
    }

    androidx.compose.runtime.LaunchedEffect(dropBannerMessage) {
        if (dropBannerMessage != null) {
            kotlinx.coroutines.delay(4500)
            viewModel.dismissDropBanner()
        }
    }

    // Trigger interstitial ad when an operation completes successfully
    androidx.compose.runtime.LaunchedEffect(mediaProgress) {
        if (mediaProgress is OperationProgress.Success) {
            onShowInterstitial()
        }
    }
    androidx.compose.runtime.LaunchedEffect(archiveProgress) {
        if (archiveProgress is OperationProgress.Success) {
            onShowInterstitial()
        }
    }
    androidx.compose.runtime.LaunchedEffect(extractionProgress) {
        if (extractionProgress is OperationProgress.Success) {
            onShowInterstitial()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CompresorPro",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "ISO • ZIP • RAR • 7Z • Video • Audio",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Quick Theme Toggle Button (Light/Dark switch)
                    IconButton(
                        onClick = { viewModel.toggleTheme(isDarkTheme) },
                        modifier = Modifier.testTag("btn_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Cambiar a Modo Claro" else "Cambiar a Modo Oscuro",
                            tint = if (isDarkTheme) VibrantAmber else MaterialTheme.colorScheme.primary
                        )
                    }

                    // Application Settings Button (allows switching theme mode, storage info, etc.)
                    IconButton(
                        onClick = { showSettingsDialog = true },
                        modifier = Modifier.testTag("btn_app_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Ajustes de la Aplicación",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showPermissionDialog = true },
                        modifier = Modifier.testTag("btn_permissions_manager")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Gestor de Permisos",
                                tint = if (permissionState.canReadWriteAnywhere) SuccessGreen else VibrantAmber
                            )
                            if (!permissionState.canReadWriteAnywhere) {
                                Surface(
                                    shape = CircleShape,
                                    color = VibrantAmber,
                                    modifier = Modifier
                                        .size(8.dp)
                                        .align(Alignment.TopEnd)
                                ) {}
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.reloadSamples() },
                        modifier = Modifier.testTag("btn_refresh_samples")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerar archivos de prueba",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                BottomAdBanner(modifier = Modifier.fillMaxWidth())
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentTab == MainTab.COMPRESS,
                        onClick = { viewModel.setTab(MainTab.COMPRESS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.COMPRESS) Icons.Default.FolderZip else Icons.Outlined.FolderZip,
                                contentDescription = "Comprimir"
                            )
                        },
                        label = { Text("Comprimir") },
                        modifier = Modifier.testTag("tab_compress")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.DECOMPRESS,
                        onClick = { viewModel.setTab(MainTab.DECOMPRESS) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.DECOMPRESS) Icons.Default.Unarchive else Icons.Outlined.Unarchive,
                                contentDescription = "Descomprimir"
                            )
                        },
                        label = { Text("Descomprimir") },
                        modifier = Modifier.testTag("tab_decompress")
                    )

                    NavigationBarItem(
                        selected = currentTab == MainTab.HISTORY,
                        onClick = { viewModel.setTab(MainTab.HISTORY) },
                        icon = {
                            Icon(
                                imageVector = if (currentTab == MainTab.HISTORY) Icons.Default.Folder else Icons.Outlined.Folder,
                                contentDescription = "Mis Archivos"
                            )
                        },
                        label = { Text("Mis Archivos") },
                        modifier = Modifier.testTag("tab_history")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event.toAndroidDragEvent().clipDescription != null
                    },
                    target = globalDropTarget
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Drop Notification Banner
                AnimatedVisibility(
                    visible = dropBannerMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = ElectricEmerald,
                        contentColor = Color.Black,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = dropBannerMessage ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissDropBanner() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Storage Permission Callout if full storage access is not yet granted
                if (!permissionState.canReadWriteAnywhere) {
                    StoragePermissionCalloutBanner(
                        onGrantClick = {
                            val intent = StoragePermissionManager.getAllFilesAccessIntent(context)
                            context.startActivity(intent)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }

                // Persistent Real-time Progress Bar for active operations across screens
                if (mediaProgress is OperationProgress.Running) {
                    OngoingTaskTopBar(
                        progress = mediaProgress,
                        onTap = { viewModel.setTab(MainTab.COMPRESS) }
                    )
                } else if (archiveProgress is OperationProgress.Running) {
                    OngoingTaskTopBar(
                        progress = archiveProgress,
                        onTap = { viewModel.setTab(MainTab.COMPRESS) }
                    )
                } else if (extractionProgress is OperationProgress.Running) {
                    OngoingTaskTopBar(
                        progress = extractionProgress,
                        onTap = { viewModel.setTab(MainTab.DECOMPRESS) }
                    )
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        MainTab.COMPRESS -> CompressScreen(viewModel = viewModel)
                        MainTab.DECOMPRESS -> DecompressScreen(viewModel = viewModel)
                        MainTab.HISTORY -> HistoryScreen(viewModel = viewModel)
                    }
                }
            }

            // Global Drag Hover Overlay
            AnimatedVisibility(
                visible = isGlobalDragHovered,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(24.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeonCyan),
                        shadowElevation = 16.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Surface(
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = NeonCyan.copy(alpha = 0.2f),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.FileDownload,
                                        contentDescription = null,
                                        tint = NeonCyan,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "¡Suelta tu archivo aquí!",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "CompresorPro identificará el formato y abrirá la opción adecuada",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Preview modal
        if (previewFile != null) {
            PreviewDialog(
                file = previewFile!!,
                onDismiss = { viewModel.showPreview(null) },
                onOpenExternal = {
                    previewFile?.let { viewModel.openFile(it) }
                },
                onShare = {
                    previewFile?.let { viewModel.shareFile(it) }
                },
                onSaveToDownloads = {
                    previewFile?.let { viewModel.saveFileToPublicDownloads(it) }
                }
            )
        }

        // Storage Permissions Manager Dialog
        if (showPermissionDialog) {
            StoragePermissionDialog(
                permissionState = permissionState,
                onDismiss = { showPermissionDialog = false },
                onRequestStandardPermissions = {
                    standardPermissionsLauncher.launch(
                        StoragePermissionManager.getRuntimePermissionsToRequest()
                    )
                },
                onRequestAllFilesAccess = {
                    val intent = StoragePermissionManager.getAllFilesAccessIntent(context)
                    context.startActivity(intent)
                }
            )
        }

        // Application Settings Dialog with Theme Switcher
        if (showSettingsDialog) {
            AppSettingsDialog(
                currentThemeMode = themeMode,
                isDarkTheme = isDarkTheme,
                permissionState = permissionState,
                onSelectThemeMode = { newMode ->
                    viewModel.setThemeMode(newMode)
                },
                onOpenPermissions = {
                    showSettingsDialog = false
                    showPermissionDialog = true
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

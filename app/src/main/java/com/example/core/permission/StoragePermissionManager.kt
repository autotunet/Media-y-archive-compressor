package com.example.core.permission

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.io.File

data class StoragePermissionState(
    val hasAllFilesAccess: Boolean,
    val hasMediaImagesAccess: Boolean,
    val hasMediaVideoAccess: Boolean,
    val hasMediaAudioAccess: Boolean,
    val hasLegacyStorageAccess: Boolean,
    val hasNotificationAccess: Boolean,
    val canReadWriteAnywhere: Boolean
)

data class StorageDirectoryInfo(
    val label: String,
    val file: File,
    val isPrimary: Boolean,
    val isAccessible: Boolean,
    val totalSpaceFormatted: String = "",
    val freeSpaceFormatted: String = ""
)

object StoragePermissionManager {

    /**
     * Checks full status of all relevant Android permissions.
     */
    fun checkPermissionState(context: Context): StoragePermissionState {
        val hasAllFilesAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasMediaImages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasMediaVideo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasMediaAudio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val hasLegacyStorage = hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) &&
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q || hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE))

        val hasNotification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true
        }

        val canReadWriteAnywhere = hasAllFilesAccess || hasLegacyStorage

        return StoragePermissionState(
            hasAllFilesAccess = hasAllFilesAccess,
            hasMediaImagesAccess = hasMediaImages,
            hasMediaVideoAccess = hasMediaVideo,
            hasMediaAudioAccess = hasMediaAudio,
            hasLegacyStorageAccess = hasLegacyStorage,
            hasNotificationAccess = hasNotification,
            canReadWriteAnywhere = canReadWriteAnywhere
        )
    }

    private fun hasPermission(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns runtime permissions array suitable for standard ActivityResultContracts.RequestMultiplePermissions().
     */
    fun getRuntimePermissionsToRequest(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }

        return permissions.toTypedArray()
    }

    /**
     * Intent to open the "All files access" system settings screen on Android 11+ (API 30+).
     */
    fun getAllFilesAccessIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } catch (e: Exception) {
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        } else {
            getAppSettingsIntent(context)
        }
    }

    /**
     * Intent to open app info settings in system settings.
     */
    fun getAppSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Enumerates all phone storage directories, including primary internal storage,
     * standard shared folders (Downloads, Documents, DCIM, Music, Movies, Pictures),
     * and secondary SD cards/external storage volumes.
     */
    fun getPhoneStorageLocations(context: Context): List<StorageDirectoryInfo> {
        val locations = mutableListOf<StorageDirectoryInfo>()

        // 1. Primary Internal Storage Root (/storage/emulated/0)
        try {
            val internalRoot = Environment.getExternalStorageDirectory()
            if (internalRoot != null && internalRoot.exists()) {
                val total = formatBytes(internalRoot.totalSpace)
                val free = formatBytes(internalRoot.freeSpace)
                locations.add(
                    StorageDirectoryInfo(
                        label = "Almacenamiento Interno",
                        file = internalRoot,
                        isPrimary = true,
                        isAccessible = internalRoot.canRead(),
                        totalSpaceFormatted = total,
                        freeSpaceFormatted = free
                    )
                )
            }
        } catch (_: Exception) {}

        // 2. Standard Public Folders
        val standardFolders = listOf(
            "Descargas" to Environment.DIRECTORY_DOWNLOADS,
            "Documentos" to Environment.DIRECTORY_DOCUMENTS,
            "Cámara y Fotos (DCIM)" to Environment.DIRECTORY_DCIM,
            "Música" to Environment.DIRECTORY_MUSIC,
            "Películas y Videos" to Environment.DIRECTORY_MOVIES,
            "Imágenes" to Environment.DIRECTORY_PICTURES
        )

        for ((label, dirType) in standardFolders) {
            try {
                val dir = Environment.getExternalStoragePublicDirectory(dirType)
                if (dir != null && (dir.exists() || dir.mkdirs())) {
                    locations.add(
                        StorageDirectoryInfo(
                            label = label,
                            file = dir,
                            isPrimary = false,
                            isAccessible = dir.canRead()
                        )
                    )
                }
            } catch (_: Exception) {}
        }

        // 3. App-specific storage directory inside filesDir
        val appBaseDir = File(context.filesDir, "CompresorPro")
        if (appBaseDir.exists() || appBaseDir.mkdirs()) {
            locations.add(
                StorageDirectoryInfo(
                    label = "CompresorPro (Archivos App)",
                    file = appBaseDir,
                    isPrimary = false,
                    isAccessible = true
                )
            )
        }

        // 4. Samples directory
        val samplesDir = File(appBaseDir, "muestras")
        if (samplesDir.exists() || samplesDir.mkdirs()) {
            locations.add(
                StorageDirectoryInfo(
                    label = "Archivos de Muestra",
                    file = samplesDir,
                    isPrimary = false,
                    isAccessible = true
                )
            )
        }

        // 5. External Media / SD Cards (from ContextCompat.getExternalFilesDirs)
        try {
            val externalDirs = ContextCompat.getExternalFilesDirs(context, null)
            externalDirs.filterNotNull().forEachIndexed { index, dir ->
                if (index > 0) { // Index 0 is primary, 1+ are physical SD cards
                    // Climb up to the root of the SD card
                    var root: File? = dir
                    while (root != null && root.parentFile != null && root.parentFile?.name != "storage") {
                        root = root.parentFile
                    }
                    if (root != null && root.exists()) {
                        locations.add(
                            StorageDirectoryInfo(
                                label = "Tarjeta SD / USB ($index)",
                                file = root,
                                isPrimary = false,
                                isAccessible = root.canRead(),
                                totalSpaceFormatted = formatBytes(root.totalSpace),
                                freeSpaceFormatted = formatBytes(root.freeSpace)
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        return locations
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}

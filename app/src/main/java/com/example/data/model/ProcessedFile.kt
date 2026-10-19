package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class OperationType(val label: String) {
    COMPRESSED_VIDEO("Video comprimido"),
    COMPRESSED_AUDIO("Audio comprimido"),
    CREATED_ARCHIVE("Archivo creado"),
    EXTRACTED_ARCHIVE("Archivo extraído")
}

data class ProcessedFile(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val originalSizeBytes: Long = -1L,
    val operation: OperationType,
    val category: FileCategory = FileCategory.fromFileName(name),
    val timestamp: Long = System.currentTimeMillis()
) {
    val formattedSize: String
        get() = formatBytes(sizeBytes)

    val formattedOriginalSize: String
        get() = if (originalSizeBytes > 0) formatBytes(originalSizeBytes) else ""

    val savingsPercent: Int?
        get() {
            if (originalSizeBytes > 0 && originalSizeBytes > sizeBytes) {
                val diff = originalSizeBytes - sizeBytes
                return ((diff.toDouble() / originalSizeBytes.toDouble()) * 100).toInt()
            }
            return null
        }

    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    companion object {
        fun formatBytes(bytes: Long): String {
            if (bytes <= 0) return "0 B"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
                else -> "$bytes B"
            }
        }
    }
}

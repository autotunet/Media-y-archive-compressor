package com.example.data.model

enum class ArchiveType(val extension: String, val displayName: String) {
    ZIP("zip", "ZIP"),
    RAR("rar", "RAR"),
    SEVEN_ZIP("7z", "7-Zip"),
    ISO("iso", "Imagen ISO"),
    UNKNOWN("", "Desconocido");

    companion object {
        fun fromExtension(ext: String): ArchiveType {
            return when (ext.lowercase().trimStart('.')) {
                "zip" -> ZIP
                "rar" -> RAR
                "7z" -> SEVEN_ZIP
                "iso" -> ISO
                else -> UNKNOWN
            }
        }
    }
}

enum class FileCategory(val label: String) {
    VIDEO("Video"),
    AUDIO("Audio"),
    DOCUMENT("Documento"),
    IMAGE("Imagen"),
    ARCHIVE("Archivo comprimido"),
    OTHER("Otro");

    companion object {
        fun fromFileName(name: String): FileCategory {
            val ext = name.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "mp4", "mkv", "avi", "mov", "webm", "flv", "3gp", "wmv" -> VIDEO
                "mp3", "m4a", "wav", "aac", "ogg", "flac", "wma", "opus" -> AUDIO
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "md" -> DOCUMENT
                "jpg", "jpeg", "png", "webp", "gif", "bmp", "svg" -> IMAGE
                "zip", "rar", "7z", "iso", "tar", "gz", "bz2" -> ARCHIVE
                else -> OTHER
            }
        }
    }
}

data class ArchiveEntryInfo(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val uncompressedSize: Long,
    val compressedSize: Long = -1L,
    val category: FileCategory = FileCategory.fromFileName(name),
    val isSelected: Boolean = true
)

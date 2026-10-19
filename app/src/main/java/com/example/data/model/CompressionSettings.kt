package com.example.data.model

data class MediaMetadata(
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val mimeType: String,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bitrateBps: Int = 0,
    val audioChannels: Int = 2,
    val audioSampleRate: Int = 44100
) {
    val durationFormatted: String
        get() {
            if (durationMs <= 0) return "--:--"
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return if (minutes >= 60) {
                val hours = minutes / 60
                val remMinutes = minutes % 60
                String.format("%d:%02d:%02d", hours, remMinutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val resolutionFormatted: String
        get() = if (width > 0 && height > 0) "${width}x${height}" else ""
}

enum class VideoPreset(val title: String, val desc: String, val scale: Float, val bitrateKbps: Int) {
    MAX_COMPRESSION("WhatsApp / Web", "Hasta 75% ahorro (480p / 800k)", 0.5f, 800),
    BALANCED("Equilibrado", "Hasta 55% ahorro (720p / 1500k)", 0.75f, 1500),
    HIGH_QUALITY("Alta Calidad", "Hasta 35% ahorro (1080p / 2500k)", 1.0f, 2500),
    CUSTOM("Personalizado", "Ajustar parámetros manualmente", 1.0f, 1500)
}

enum class AudioPreset(val title: String, val desc: String, val bitrateKbps: Int, val isMono: Boolean) {
    VOICE_PODCAST("Voz / Podcast", "Máximo ahorro, mono 64 kbps", 64, true),
    BALANCED("Música Equilibrada", "Calidad óptima, estéreo 96 kbps", 96, false),
    HIGH_QUALITY("Fidelidad Alta", "Calidad CD, estéreo 128 kbps", 128, false)
}

data class VideoCompressionSettings(
    val preset: VideoPreset = VideoPreset.BALANCED,
    val targetWidth: Int = 1280,
    val targetHeight: Int = 720,
    val targetVideoBitrateKbps: Int = 1500,
    val targetAudioBitrateKbps: Int = 96,
    val keepAudio: Boolean = true
)

data class AudioCompressionSettings(
    val preset: AudioPreset = AudioPreset.BALANCED,
    val targetBitrateKbps: Int = 96,
    val targetSampleRate: Int = 44100,
    val isMono: Boolean = false
)

enum class ArchiveOutputFormat(val ext: String, val label: String) {
    ZIP("zip", "ZIP (.zip)"),
    SEVEN_ZIP("7z", "7-Zip (.7z)")
}

enum class CompressionLevelPreset(
    val title: String,
    val subtitle: String,
    val level: Int,
    val estimatedRatio: String,
    val speedTag: String
) {
    STORE("Almacenar (Store)", "Sin compresión, empaquetado instantáneo", 0, "100% tamaño", "Instantáneo"),
    FASTEST("Más rápido (Fastest)", "Compresión ligera con máxima velocidad", 2, "~80% - 85%", "Muy rápido"),
    NORMAL("Equilibrado (Normal)", "Balance óptimo entre tamaño y CPU", 6, "~55% - 65%", "Equilibrado"),
    BEST_COMPRESSION("Máxima compresión (Best)", "Mayor reducción de espacio para compartir", 9, "~35% - 50%", "Intensivo")
}

data class ArchiveCreationSettings(
    val format: ArchiveOutputFormat = ArchiveOutputFormat.ZIP,
    val archiveName: String = "Archivo_Comprimido",
    val compressionLevel: Int = 6,
    val levelPreset: CompressionLevelPreset = CompressionLevelPreset.NORMAL
)

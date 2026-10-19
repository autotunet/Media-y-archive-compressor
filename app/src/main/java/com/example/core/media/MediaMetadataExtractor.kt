package com.example.core.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.model.MediaMetadata
import java.io.File

class MediaMetadataExtractor(private val context: Context) {

    fun extract(file: File): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            extractInternal(file.name, file.absolutePath, file.length(), retriever)
        } catch (e: Exception) {
            e.printStackTrace()
            val isVid = file.extension.lowercase() in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp")
            MediaMetadata(
                fileName = file.name,
                filePath = file.absolutePath,
                fileSizeBytes = file.length(),
                mimeType = if (isVid) "video/*" else "audio/*",
                isVideo = isVid
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    fun extract(uri: Uri, fileName: String, fileSize: Long): MediaMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            extractInternal(fileName, uri.toString(), fileSize, retriever)
        } catch (e: Exception) {
            e.printStackTrace()
            val isVid = fileName.extensionLower() in listOf("mp4", "mkv", "mov", "avi", "webm", "3gp")
            MediaMetadata(
                fileName = fileName,
                filePath = uri.toString(),
                fileSizeBytes = fileSize,
                mimeType = if (isVid) "video/*" else "audio/*",
                isVideo = isVid
            )
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun extractInternal(
        fileName: String,
        path: String,
        fileSize: Long,
        retriever: MediaMetadataRetriever
    ): MediaMetadata {
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        val durationMs = durationStr?.toLongOrNull() ?: 0L

        val widthStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
        val heightStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
        val width = widthStr?.toIntOrNull() ?: 0
        val height = heightStr?.toIntOrNull() ?: 0

        val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
        val bitrate = bitrateStr?.toIntOrNull() ?: 0

        val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            ?: if (width > 0 || fileName.extensionLower() in listOf("mp4", "mkv", "mov", "avi", "webm")) "video/mp4" else "audio/mpeg"

        val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) != null || width > 0

        return MediaMetadata(
            fileName = fileName,
            filePath = path,
            fileSizeBytes = fileSize,
            mimeType = mimeType,
            isVideo = hasVideo,
            durationMs = durationMs,
            width = width,
            height = height,
            bitrateBps = bitrate
        )
    }

    private fun String.extensionLower(): String = substringAfterLast('.', "").lowercase()
}

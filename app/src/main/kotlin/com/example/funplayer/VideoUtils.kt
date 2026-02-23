package com.example.funplayer

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal fun copySmbToTempFile(smbUri: String, maxBytes: Long = SMB_TEMP_COPY_LIMIT): File? {
    return try {
        val smbFile = SmbFile(smbUri)
        if (!smbFile.exists() || smbFile.isDirectory) return null
        val suffix = smbUri.substringAfterLast('.', "mp4").take(4)
        val tempFile = File.createTempFile("smb_video_", ".$suffix")
        smbFile.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                val buffer = ByteArray(64 * 1024)
                var copied: Long = 0
                while (copied < maxBytes || maxBytes <= 0) {
                    val n = input.read(buffer)
                    if (n <= 0) break
                    output.write(buffer, 0, n)
                    copied += n
                }
            }
        }
        tempFile
    } catch (_: Exception) {
        null
    }
}

internal fun loadVideoFirstFrame(context: android.content.Context, uriString: String): Bitmap? {
    if (uriString.startsWith("smb:", ignoreCase = true)) {
        NasDataCache.thumbnailCache.get(uriString)?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        if (uriString.startsWith("smb:", ignoreCase = true)) {
            // 对于封面/元数据，只需要下载视频头部（3MB 足够）
            val tempFile = copySmbToTempFile(uriString, maxBytes = SMB_METADATA_COPY_LIMIT)
            if (tempFile == null) return null
            try {
                retriever.setDataSource(tempFile.absolutePath)
                val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
                val scaled = Bitmap.createScaledBitmap(frame, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true)
                if (frame != scaled) frame.recycle()
                NasDataCache.thumbnailCache.put(uriString, scaled)
                scaled
            } finally {
                tempFile.delete()
            }
        } else {
            val uri = android.net.Uri.parse(uriString)
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC) ?: return null
            val scaled = Bitmap.createScaledBitmap(frame, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, true)
            if (frame != scaled) frame.recycle()
            scaled
        }
    } catch (_: Exception) {
        null
    } finally {
        retriever.release()
    }
}

internal fun getVideoDurationFormatted(context: android.content.Context, uri: android.net.Uri): String {
    return getVideoDurationFormattedFromUri(context, uri.toString())
}

internal fun getVideoDurationFormattedFromUri(context: android.content.Context, uriString: String): String {
    if (uriString.startsWith("smb:", ignoreCase = true)) {
        NasDataCache.durationCache.get(uriString)?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        val result = if (uriString.startsWith("smb:", ignoreCase = true)) {
            // 对于时长提取，只需要下载视频头部（3MB 足够）
            val tempFile = copySmbToTempFile(uriString, maxBytes = SMB_METADATA_COPY_LIMIT)
            if (tempFile == null) return "0:00"
            try {
                retriever.setDataSource(tempFile.absolutePath)
                formatDurationFromRetriever(retriever)
            } finally {
                tempFile.delete()
            }
        } else {
            retriever.setDataSource(context, android.net.Uri.parse(uriString))
            formatDurationFromRetriever(retriever)
        }
        if (uriString.startsWith("smb:", ignoreCase = true)) NasDataCache.durationCache.put(uriString, result)
        result
    } catch (_: Exception) {
        "0:00"
    } finally {
        retriever.release()
    }
}

internal fun formatDurationFromRetriever(retriever: MediaMetadataRetriever): String {
    val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

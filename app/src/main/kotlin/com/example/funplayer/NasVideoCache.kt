package com.example.funplayer

import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * NAS 视频缓存池：将 SMB 视频拉到本地缓存目录，播放时优先使用缓存以减少网络卡顿与 seek 慢的问题。
 * 缓存按 URI 做 key，超出容量时按文件最后修改时间删除最旧的条目。
 */
internal object NasVideoCache {

    private fun cacheDir(context: android.content.Context): File {
        val base = context.externalCacheDir ?: context.cacheDir
        val dir = File(base, NAS_VIDEO_CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun cacheKey(smbUri: String): String {
        val normalized = normalizeSmbUri(smbUri)
        val bytes = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray(Charsets.UTF_8))
        return bytes.take(16).joinToString("") { "%02x".format(it) }
    }

    private fun extensionFromUri(smbUri: String): String =
        smbUri.substringAfterLast('.', "mp4").take(4).lowercase()

    /** 若该 SMB URI 已有完整缓存文件则返回本地 File，否则返回 null。 */
    fun getCachedFile(context: android.content.Context, smbUri: String): File? {
        if (!smbUri.startsWith("smb:", ignoreCase = true)) return null
        val dir = cacheDir(context)
        val key = cacheKey(smbUri)
        val ext = extensionFromUri(smbUri)
        val file = File(dir, "$key.$ext")
        return if (file.isFile && file.canRead()) file else null
    }

    /** 返回可用于播放的 URI：若已有缓存则返回 file:// 路径，否则返回 null（调用方继续用 smb）。 */
    fun getPlayUriIfCached(context: android.content.Context, smbUri: String): String? =
        getCachedFile(context, smbUri)?.let { "file://${it.absolutePath}" }

    private fun evictUntilUnderLimit(dir: File, maxBytes: Long) {
        var total = dir.listFiles()?.sumOf { it.length() } ?: 0L
        if (total <= maxBytes) return
        val byAge = dir.listFiles()?.filter { it.isFile }?.sortedBy { it.lastModified() } ?: return
        for (f in byAge) {
            if (total <= maxBytes) break
            total -= f.length()
            f.delete()
        }
    }

    /**
     * 在后台将 SMB 视频完整复制到缓存池。调用后即可返回；复制完成后下次播放会走缓存。
     * 复制前会按容量淘汰最旧文件。
     */
    suspend fun cacheToLocal(context: android.content.Context, smbUri: String): Boolean =
        withContext(Dispatchers.IO) {
            if (!smbUri.startsWith("smb:", ignoreCase = true)) return@withContext false
            val dir = cacheDir(context)
            evictUntilUnderLimit(dir, NAS_VIDEO_CACHE_MAX_BYTES)
            val key = cacheKey(smbUri)
            val ext = extensionFromUri(smbUri)
            val outFile = File(dir, "$key.$ext")
            if (outFile.isFile && outFile.length() > 0) {
                DevLog.log("SMB", "NasVideoCache 已存在: ${smbUri.takeLast(50)}")
                return@withContext true
            }
            val ctx = getSmbContext(context)
            val normalized = normalizeSmbUri(smbUri)
            runCatching {
                val smbFile = jcifs.smb.SmbFile(normalized, ctx)
                if (!smbFile.exists() || smbFile.isDirectory) {
                    DevLog.log("SMB", "NasVideoCache 源不存在: ${smbUri.takeLast(50)}")
                    return@withContext false
                }
                smbFile.inputStream.use { input ->
                    FileOutputStream(outFile).use { output ->
                        val buf = ByteArray(256 * 1024)
                        var copied = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n <= 0) break
                            output.write(buf, 0, n)
                            copied += n
                        }
                        DevLog.log("SMB", "NasVideoCache 写入完成: ${smbUri.takeLast(50)} ${copied} 字节")
                    }
                }
                true
            }.getOrElse { e ->
                DevLog.log("SMB", "NasVideoCache 失败: ${smbUri.takeLast(50)} ${e.message}")
                outFile.delete()
                false
            }
        }
}

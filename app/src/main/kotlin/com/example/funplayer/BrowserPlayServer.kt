package com.example.funplayer

import android.content.Context
import android.net.Uri
import fi.iki.elonen.NanoHTTPD
import jcifs.CIFSContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.InputStream
import java.util.Locale

private const val MIME_HTML = "text/html; charset=utf-8"
private const val MIME_VIDEO = "video/mp4"
private const val MIME_PLAIN = "text/plain"

/**
 * 本地 HTTP 服务：提供浏览器播放页与视频流，并接收页面上报的播放时间以同步脚本到设备。
 * GET / -> 播放器 HTML（video + 定时 POST 当前时间）
 * GET /video -> 视频流（支持 Range）
 * POST /time -> 请求体为当前播放时间(ms)，用于脚本同步
 */
internal class BrowserPlayServer(
    port: Int,
    private val appContext: Context,
    private val videoUri: String,
    private val funscriptData: FunscriptData?,
    private val scope: CoroutineScope
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri?.lowercase(Locale.ROOT) ?: ""
        val method = session.method

        when {
            method == Method.GET && (uri == "/" || uri == "") -> return servePlayerPage()
            method == Method.GET && uri == "/video" -> return serveVideo(session.headers)
            method == Method.GET && uri == "/split" -> return newFixedLengthResponse(Response.Status.OK, MIME_PLAIN, if (BrowserPlayServerHolder.splitMode) "1" else "0")
            method == Method.POST && uri == "/split" -> return handleSplitPost(session)
            method == Method.POST && uri == "/time" -> return handleTimePost(session)
            else -> return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAIN, "Not Found")
        }
    }

    private fun servePlayerPage(): Response {
        return try {
            val inputStream = appContext.assets.open("player.html")
            val html = inputStream.bufferedReader().use { it.readText() }
            newFixedLengthResponse(Response.Status.OK, MIME_HTML, html)
        } catch (e: Exception) {
            DevLog.log("BrowserPlay", "servePlayerPage: ${e.message}")
            // 如果读取文件失败，返回一个简单的错误页面
            newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_HTML, 
                "<html><body><h1>Error loading player page</h1></body></html>")
        }
    }

    private fun serveVideo(headers: Map<String, String>): Response {
        val (stream, totalLength) = openVideoStream() ?: run {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAIN, "Cannot open video")
        }
        val rangeHeader = headers["range"]
        if (!rangeHeader.isNullOrBlank() && rangeHeader.lowercase(Locale.ROOT).startsWith("bytes=")) {
            val range = parseRange(rangeHeader, totalLength) ?: return streamResponse(stream, totalLength, 0, totalLength)
            val (start, end) = range
            if (start > 0) {
                stream.skip(start)
            }
            val length = if (totalLength >= 0) (end - start + 1).coerceAtLeast(0) else -1L
            return newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, MIME_VIDEO, stream, length).apply {
                addHeader("Accept-Ranges", "bytes")
                addHeader("Content-Range", "bytes $start-${if (totalLength >= 0) end else "*"}/${if (totalLength >= 0) totalLength else "*"}")
            }
        }
        return streamResponse(stream, totalLength, 0, totalLength)
    }

    private fun streamResponse(stream: InputStream, totalLength: Long, start: Long, end: Long): Response {
        val length = if (totalLength >= 0) (end - start + 1).coerceAtLeast(0) else -1L
        val resp = if (length >= 0) newFixedLengthResponse(Response.Status.OK, MIME_VIDEO, stream, length)
        else newChunkedResponse(Response.Status.OK, MIME_VIDEO, stream)
        if (totalLength >= 0) resp.addHeader("Accept-Ranges", "bytes")
        return resp
    }

    private fun parseRange(rangeHeader: String, totalLength: Long): Pair<Long, Long>? {
        val s = rangeHeader.removePrefix("bytes=").trim()
        val parts = s.split("-")
        if (parts.size != 2) return null
        val startStr = parts[0].trim()
        val endStr = parts[1].trim()
        val start = if (startStr.isEmpty()) 0L else startStr.toLongOrNull() ?: return null
        val end = if (endStr.isEmpty()) {
            if (totalLength < 0) return null
            totalLength - 1
        } else endStr.toLongOrNull() ?: return null
        if (start < 0 || end < start) return null
        if (totalLength >= 0 && end >= totalLength) return start to (totalLength - 1)
        return start to end
    }

    private fun openVideoStream(): Pair<InputStream, Long>? {
        return try {
            when {
                videoUri.startsWith("file:", ignoreCase = true) -> {
                    val path = Uri.parse(videoUri).path ?: return null
                    val file = java.io.File(path)
                    if (!file.exists() || !file.canRead()) null else file.inputStream() to file.length()
                }
                videoUri.startsWith("content:", ignoreCase = true) -> {
                    val uri = Uri.parse(videoUri)
                    val stream = appContext.contentResolver.openInputStream(uri) ?: return null
                    val len = try {
                        appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: -1L
                    } catch (_: Exception) { -1L }
                    stream to (if (len > 0) len else -1L)
                }
                videoUri.startsWith("smb:", ignoreCase = true) -> {
                    val ctx: CIFSContext = getSmbContext(appContext)
                    val normalized = normalizeSmbUri(videoUri)
                    val smbFile = jcifs.smb.SmbFile(normalized, ctx)
                    if (!smbFile.exists() || smbFile.isDirectory) null else smbFile.inputStream to smbFile.length()
                }
                else -> null
            }
        } catch (e: Exception) {
            DevLog.log("BrowserPlay", "openVideoStream: ${e.message}")
            null
        }
    }

    private fun handleTimePost(session: IHTTPSession): Response {
        try {
            val body = session.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            val posMs = body.trim().toLongOrNull() ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAIN, "Bad time")
            val script = funscriptData
            val ctx = appContext
            scope.launch(Dispatchers.Main) {
                if (!getConnectionEnabled(ctx)) return@launch
                val cmd = buildAxisCommandFromScript(ctx, script, posMs)
                if (cmd.isNotEmpty()) sendAxisCommand(ctx, cmd)
            }
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAIN, "OK")
        } catch (e: Exception) {
            DevLog.log("BrowserPlay", "handleTimePost: ${e.message}")
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAIN, "Error")
        }
    }

    private fun handleSplitPost(session: IHTTPSession): Response {
        try {
            val body = session.inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            BrowserPlayServerHolder.splitMode = body.trim() == "1"
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAIN, "OK")
        } catch (e: Exception) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAIN, "Error")
        }
    }
}

/** 当前浏览器播放服务实例，用于启动/停止与获取播放 URL。分屏状态与 app 内统一，由软件内分屏按钮控制。 */
internal object BrowserPlayServerHolder {
    private var server: BrowserPlayServer? = null

    @Volatile
    var playbackUrl: String? = null
        private set

    /** 与 app 内 vrSplitMode 同步，浏览器页通过 GET /split 轮询此值。 */
    @Volatile
    var splitMode: Boolean = false

    fun start(
        context: Context,
        videoUri: String,
        funscriptData: FunscriptData?,
        scope: CoroutineScope,
        initialSplitMode: Boolean = false
    ): String? {
        stop()
        splitMode = initialSplitMode
        val port = 0
        val s = BrowserPlayServer(port, context.applicationContext, videoUri, funscriptData, scope)
        return try {
            s.start()
            server = s
            val portActual = s.listeningPort
            if (portActual in 1..65535) {
                playbackUrl = "http://127.0.0.1:$portActual/"
                playbackUrl
            } else null
        } catch (e: Exception) {
            DevLog.log("BrowserPlay", "start: ${e.message}")
            null
        }
    }

    fun stop() {
        try {
            server?.stop()
        } catch (_: Exception) { }
        server = null
        playbackUrl = null
    }
}

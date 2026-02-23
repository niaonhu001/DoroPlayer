@file:Suppress("DEPRECATION")
package com.example.funplayer

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import jcifs.smb.SmbFile
import jcifs.smb.SmbRandomAccessFile
import java.io.IOException

/**
 * Media3 DataSource 实现，从 SMB (smb://) URI 读取数据，用于 NAS 方式下的视频播放。
 * 使用 SmbRandomAccessFile 实现真正的随机访问，支持高效 seek。
 * 添加缓冲层以减少碎片化的小读取。
 */
@UnstableApi
class SmbDataSource : DataSource {

    companion object {
        private const val BUFFER_SIZE = 256 * 1024 // 256KB 缓冲区
    }

    private var randomAccessFile: SmbRandomAccessFile? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0
    private var openThread: Thread? = null
    private var readCount = 0

    // 缓冲区相关
    private var buffer: ByteArray? = null
    private var bufferPosition: Long = 0      // 缓冲区在文件中的起始位置
    private var bufferOffset = 0              // 当前读取位置在缓冲区中的偏移
    private var bufferLength = 0              // 缓冲区中有效数据长度

    override fun open(dataSpec: DataSpec): Long {
        val currentThread = Thread.currentThread()
        openThread = currentThread
        val callId = System.currentTimeMillis()
        val fileName = dataSpec.uri.lastPathSegment ?: "unknown"
        DevLog.log("SmbDataSource", "[$callId] [${currentThread.name}] open() called - file: $fileName, position: ${dataSpec.position}, length: ${dataSpec.length}")

        uri = dataSpec.uri
        val uriString = dataSpec.uri.toString()
        if (!uriString.startsWith("smb:", ignoreCase = true)) {
            throw UnsupportedOperationException("SmbDataSource only supports smb:// URIs")
        }
        val smbFile = SmbFile(uriString)
        if (!smbFile.exists() || smbFile.isDirectory) {
            throw IOException("SMB file does not exist or is directory")
        }

        // 使用 SmbRandomAccessFile 实现真正的随机访问
        DevLog.log("SmbDataSource", "[$callId] [$fileName] Creating SmbRandomAccessFile...")
        randomAccessFile = SmbRandomAccessFile(smbFile, "r")

        // 初始化缓冲区
        buffer = ByteArray(BUFFER_SIZE)
        bufferPosition = -1  // 标记缓冲区无效
        bufferOffset = 0
        bufferLength = 0

        // 真正的 seek：直接定位到指定位置
        val position = dataSpec.position
        if (position > 0) {
            DevLog.log("SmbDataSource", "[$callId] [$fileName] Seeking to position: $position")
            randomAccessFile!!.seek(position)
            // invalidate buffer after seek
            bufferPosition = -1
        }

        val length = dataSpec.length
        val fileLength = randomAccessFile!!.length()
        bytesRemaining = when {
            length != C.LENGTH_UNSET.toLong() -> length
            position >= fileLength -> 0
            else -> fileLength - position
        }
        val result = if (bytesRemaining == Long.MAX_VALUE) C.LENGTH_UNSET.toLong() else bytesRemaining
        DevLog.log("SmbDataSource", "[$callId] [$fileName] open() complete - bytesRemaining: $bytesRemaining, fileLength: $fileLength, bufferSize: $BUFFER_SIZE")
        return result
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        val currentThread = Thread.currentThread()
        readCount++
        val fileName = uri?.lastPathSegment ?: "unknown"

        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT

        val toRead = length.coerceAtMost(if (bytesRemaining == Long.MAX_VALUE) length else bytesRemaining.toInt())

        // 尝试从缓冲区读取
        var bytesRead = 0
        var targetOffset = offset

        while (bytesRead < toRead) {
            // 检查缓冲区是否有效且有数据
            if (bufferPosition < 0 || bufferOffset >= bufferLength) {
                // 需要重新填充缓冲区
                refillBuffer()
                if (bufferLength == 0) {
                    // 文件结束
                    break
                }
            }

            // 从缓冲区复制到目标
            val available = bufferLength - bufferOffset
            val needed = toRead - bytesRead
            val toCopy = available.coerceAtMost(needed)

            System.arraycopy(buffer!!, bufferOffset, target, targetOffset, toCopy)
            bufferOffset += toCopy
            bytesRead += toCopy
            targetOffset += toCopy

            // 更新文件位置
            bufferPosition += toCopy
        }

        if (readCount % 500 == 0 || readCount <= 3) {
            DevLog.log("SmbDataSource", "[$fileName] [read#$readCount] [${currentThread.name}] requested: $toRead, actually read: $bytesRead, bytesRemaining: $bytesRemaining")
        }

        if (bytesRead == 0 && bytesRemaining > 0) {
            DevLog.log("SmbDataSource", "[$fileName] [read#$readCount] Unexpected end of stream!")
            throw IOException("Unexpected end of stream")
        }

        if (bytesRemaining != Long.MAX_VALUE) bytesRemaining -= bytesRead
        return bytesRead
    }

    private fun refillBuffer() {
        val raf = randomAccessFile ?: return
        val currentFilePos = raf.filePointer

        // 如果缓冲区无效，定位到当前文件位置
        if (bufferPosition < 0) {
            bufferPosition = currentFilePos
        }

        // 移动到缓冲区起始位置
        raf.seek(bufferPosition)

        // 读取一大块数据到缓冲区
        bufferLength = raf.read(buffer!!)
        bufferOffset = 0

        // 每100次缓冲操作记录一次
        if (readCount % 500 == 0) {
            DevLog.log("SmbDataSource", "Buffer refilled: position=$bufferPosition, length=$bufferLength")
        }
    }

    override fun getUri(): Uri? = uri

    override fun addTransferListener(transferListener: TransferListener) {
        // SMB 播放暂不上报传输进度，忽略 listener
    }

    override fun close() {
        val currentThread = Thread.currentThread()
        val fileName = uri?.lastPathSegment ?: "unknown"
        DevLog.log("SmbDataSource", "[$fileName] [${currentThread.name}] close() called, openThread was: ${openThread?.name}, total reads: $readCount")
        uri = null
        try {
            randomAccessFile?.close()
            DevLog.log("SmbDataSource", "[$fileName] [${currentThread.name}] SmbRandomAccessFile closed successfully")
        } catch (e: Exception) {
            DevLog.log("SmbDataSource", "[$fileName] [${currentThread.name}] Error closing SmbRandomAccessFile: ${e.message}")
        }
        randomAccessFile = null
        buffer = null
        readCount = 0
    }

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbDataSource()
    }
}

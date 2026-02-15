@file:Suppress("DEPRECATION")
package com.example.funplayer

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import jcifs.smb.SmbFile
import java.io.InputStream

/**
 * Media3 DataSource 实现，从 SMB (smb://) URI 读取数据，用于 NAS 方式下的视频播放。
 */
class SmbDataSource : DataSource {

    private var inputStream: InputStream? = null
    private var uri: Uri? = null
    private var bytesRemaining: Long = 0

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val uriString = dataSpec.uri.toString()
        if (!uriString.startsWith("smb:", ignoreCase = true)) {
            throw UnsupportedOperationException("SmbDataSource only supports smb:// URIs")
        }
        val smbFile = SmbFile(uriString)
        if (!smbFile.exists() || smbFile.isDirectory) {
            throw java.io.IOException("SMB file does not exist or is directory")
        }
        inputStream = smbFile.inputStream
        val position = dataSpec.position
        if (position > 0) {
            var skipped = 0L
            while (skipped < position) {
                val n = inputStream!!.skip(position - skipped)
                if (n <= 0) break
                skipped += n
            }
        }
        val length = dataSpec.length
        bytesRemaining = if (length != C.LENGTH_UNSET.toLong()) length else Long.MAX_VALUE
        return if (bytesRemaining == Long.MAX_VALUE) C.LENGTH_UNSET.toLong() else bytesRemaining
    }

    override fun read(target: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val toRead = length.coerceAtMost(if (bytesRemaining == Long.MAX_VALUE) length else bytesRemaining.toInt())
        val read = inputStream!!.read(target, offset, toRead)
        if (read == -1) {
            if (bytesRemaining != Long.MAX_VALUE) throw java.io.IOException("Unexpected end of stream")
            return C.RESULT_END_OF_INPUT
        }
        if (bytesRemaining != Long.MAX_VALUE) bytesRemaining -= read
        return read
    }

    override fun getUri(): Uri? = uri

    override fun addTransferListener(transferListener: TransferListener) {
        // SMB 播放暂不上报传输进度，忽略 listener
    }

    override fun close() {
        uri = null
        try {
            inputStream?.close()
        } catch (_: Exception) { }
        inputStream = null
    }

    class Factory : DataSource.Factory {
        override fun createDataSource(): DataSource = SmbDataSource()
    }
}

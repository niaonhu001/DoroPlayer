package com.example.funplayer

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf

/** 多文件 funscript 命名：轴 id -> 文件名后缀（null 表示 视频名.funscript 即 L0） */
internal val MULTI_FUNSCRIPT_AXIS_SUFFIXES = listOf(
    "L0" to null,
    "L1" to "surge",
    "L2" to "sway",
    "R0" to "twist",
    "R1" to "roll",
    "R2" to "pitch"
)

internal val AXIS_NAMES = listOf("L0", "L1", "L2", "R0", "R1", "R2")

internal enum class ConnectionType(@StringRes val nameResId: Int) {
    Serial(R.string.serial_connection),
    BluetoothSerial(R.string.bt_serial_connection),
    TCP(R.string.tcp_connection),
    UDP(R.string.udp_connection),
    TheHandy(R.string.handy_connection),
    TcodeBLE(R.string.TcodeBLE)
}

data class VideoItem(
    val id: Int,
    val name: String,
    val duration: String,
    val tags: List<String>,
    val uri: String? = null,
    val funscriptUri: String? = null,
    val funscriptUrisByAxis: Map<String, String>? = null,
    val tagFileUri: String? = null,
    val parentFolderUri: String? = null
)

enum class MainTab(val label: String) {
    Device("设备"),
    Home("主页"),
    Settings("设置")
}

sealed class Screen {
    object Main : Screen()
    data class VideoDetail(val videoId: Int) : Screen()
}

internal data class DevLogEntry(val time: Long, val tag: String, val msg: String)

internal object DevLog {
    private const val MAX_ENTRIES = 500
    val entries = mutableStateListOf<DevLogEntry>()
    fun log(tag: String, msg: String) {
        android.util.Log.d(tag, msg)
        synchronized(entries) {
            entries.add(DevLogEntry(System.currentTimeMillis(), tag, msg))
            while (entries.size > MAX_ENTRIES) entries.removeAt(0)
        }
    }

    fun clear() {
        synchronized(entries) { entries.clear() }
    }
}

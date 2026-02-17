@file:Suppress("DEPRECATION")
package com.example.funplayer

import android.annotation.SuppressLint
import android.app.Activity
import androidx.annotation.StringRes
import android.content.Context
import android.content.res.Configuration
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.content.ContextWrapper
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.os.Bundle
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.documentfile.provider.DocumentFile
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.location.LocationManager
import android.hardware.usb.UsbManager
import android.os.Build
import android.os.ParcelUuid
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.funplayer.handyplug.Handyplug
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.json.JSONArray
import org.json.JSONObject
import jcifs.smb.SmbFile
import java.io.File
import java.io.FileOutputStream
import java.util.LinkedHashMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.funplayer.ui.theme.FunPlayerTheme
import java.util.Locale
import java.util.UUID

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        val locale = when (lang) {
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "en" -> Locale.ENGLISH
            else -> null
        }
        if (locale != null) {
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            super.attachBaseContext(ContextWrapper(newBase.createConfigurationContext(config)))
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDark) }

            FunPlayerTheme(darkTheme = isDarkTheme) {
                VideoApp(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = { isDarkTheme = it }
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onVideosScanned: (List<VideoItem>) -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit
) = AppSettingsScreenImpl(
    isDarkTheme = isDarkTheme,
    onThemeChange = onThemeChange,
    onVideosScanned = onVideosScanned,
    onDeveloperModeChange = onDeveloperModeChange
)

// ================== 数据模型与导航状态 ==================

data class VideoItem(
    val id: Int,
    val name: String,
    val duration: String,
    val tags: List<String>,
    val uri: String? = null,  // 内容 URI，用于播放与扫描来源
    val funscriptUri: String? = null,  // 单文件脚本 URI（视频名.funscript），无则 null
    /** 多文件脚本：轴 id -> 脚本 URI。格式为 视频名.轴名.funscript，L0 缺省轴名为 视频名.funscript */
    val funscriptUrisByAxis: Map<String, String>? = null,
    /** 同目录下的 视频名.tag 文件 URI，用于读写标签；无则 null */
    val tagFileUri: String? = null,
    /** 视频所在目录的 Document tree URI，用于在无 .tag 时创建新文件 */
    val parentFolderUri: String? = null
)

/** 多文件 funscript 命名：轴 id -> 文件名后缀（null 表示 视频名.funscript 即 L0） */
private val MULTI_FUNSCRIPT_AXIS_SUFFIXES = listOf(
    "L0" to null,
    "L1" to "surge",
    "L2" to "sway",
    "R0" to "twist",
    "R1" to "roll",
    "R2" to "pitch"
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

// ================== Funscript 解析与热力图 ==================

private data class FunscriptAction(val at: Long, val pos: Int)
private data class FunscriptAxis(val id: String, val actions: List<FunscriptAction>)
private data class FunscriptData(val durationSec: Float, val axes: List<FunscriptAxis>)

/** 从 URI 字符串（content 或 smb://）读取 JSON 文本；失败返回 null */
private fun readJsonFromUriString(context: android.content.Context, uriString: String): String? {
    return when {
        uriString.startsWith("smb:", ignoreCase = true) -> try {
            SmbFile(uriString).inputStream.use { it.reader(Charsets.UTF_8).readText() }
        } catch (_: Exception) { null }
        else -> try {
            context.contentResolver.openInputStream(android.net.Uri.parse(uriString))?.reader(Charsets.UTF_8)?.use { it.readText() }
        } catch (_: Exception) { null }
    }
}

/** 从 content 或 smb URI 加载并解析 .funscript，支持多轴 (axes) 或单轴 (根 actions)。
 * 根节点的 "actions" 无 id，解析为 L0；"axes" 数组中每项带 id（如 L1、L2），一并加入。结果会写入内存缓存。 */
private fun loadFunscriptFromUri(context: android.content.Context, uriString: String?): FunscriptData? {
    if (uriString == null) return null
    if (uriString.startsWith("smb:", ignoreCase = true)) {
        NasDataCache.getFunscript(uriString)?.let { return it }
    }
    return try {
        val json = readJsonFromUriString(context, uriString) ?: return null
        val root = JSONObject(json)
        val durationSec = root.optJSONObject("metadata")?.optDouble("duration", 0.0)?.toFloat() ?: 0f
        val axesList = mutableListOf<FunscriptAxis>()
        if (root.has("actions")) {
            val actionsArr = root.getJSONArray("actions")
            val actions = (0 until actionsArr.length()).map { j ->
                val o = actionsArr.getJSONObject(j)
                FunscriptAction(o.optLong("at", 0L), o.optInt("pos", 0))
            }.sortedBy { it.at }
            if (actions.isNotEmpty()) {
                axesList.add(FunscriptAxis("L0", actions))
            }
        }
        if (root.has("axes")) {
            val axes = root.getJSONArray("axes")
            for (i in 0 until axes.length()) {
                val ax = axes.getJSONObject(i)
                val id = ax.optString("id", "?").takeIf { it.isNotEmpty() } ?: "?"
                val actionsArr = ax.optJSONArray("actions") ?: continue
                val actions = (0 until actionsArr.length()).map { j ->
                    val o = actionsArr.getJSONObject(j)
                    FunscriptAction(o.optLong("at", 0L), o.optInt("pos", 0))
                }.sortedBy { it.at }
                axesList.add(FunscriptAxis(id, actions))
            }
        }
        if (axesList.isEmpty()) return null
        val totalMs = if (durationSec > 0) (durationSec * 1000).toLong() else axesList.flatMap { it.actions }.maxOfOrNull { it.at } ?: 1L
        val data = FunscriptData(durationSec, axesList)
        if (uriString.startsWith("smb:", ignoreCase = true)) NasDataCache.putFunscript(uriString, data)
        data
    } catch (_: Exception) {
        null
    }
}

/** 多文件 funscript：按轴 id -> URI 分别读取，每文件仅解析根 "actions" 与 metadata.duration，合并为一份多轴 FunscriptData。支持 content 与 smb；结果写入内存缓存。 */
private fun loadFunscriptMultiFromUris(context: android.content.Context, urisByAxis: Map<String, String>): FunscriptData? {
    if (urisByAxis.isEmpty()) return null
    val cacheKey = urisByAxis.toSortedMap().entries.joinToString(",") { "${it.key}=${it.value}" }
    val allSmb = urisByAxis.values.all { it.startsWith("smb:", ignoreCase = true) }
    if (allSmb) {
        NasDataCache.getFunscript(cacheKey)?.let { return it }
    }
    return try {
        val axesList = mutableListOf<FunscriptAxis>()
        var maxDurationSec = 0f
        for ((axisId, uriString) in urisByAxis) {
            val json = readJsonFromUriString(context, uriString) ?: continue
            val root = JSONObject(json)
            val durationSec = root.optJSONObject("metadata")?.optDouble("duration", 0.0)?.toFloat() ?: 0f
            if (durationSec > maxDurationSec) maxDurationSec = durationSec
            if (!root.has("actions")) continue
            val actionsArr = root.getJSONArray("actions")
            val actions = (0 until actionsArr.length()).map { j ->
                val o = actionsArr.getJSONObject(j)
                FunscriptAction(o.optLong("at", 0L), o.optInt("pos", 0))
            }.sortedBy { it.at }
            if (actions.isNotEmpty()) axesList.add(FunscriptAxis(axisId, actions))
        }
        if (axesList.isEmpty()) return null
        val data = FunscriptData(maxDurationSec, axesList)
        if (allSmb) NasDataCache.putFunscript(cacheKey, data)
        data
    } catch (_: Exception) {
        null
    }
}

/**
 * 根据当前播放时间与脚本数据生成轴控制指令。
 * 格式：轴名+目标位置+I+运动时间(ms)，如 L087I2000；多轴直接拼接，如 L093I2031L185I3256。
 * 脚本位置 0–100 会按设备设置中的输出范围映射到各轴 [min%, max%]。
 */
private fun buildAxisCommandFromScript(
    context: android.content.Context,
    script: FunscriptData?,
    currentPositionMs: Long
): String {
    if (script == null) return ""
    val savedRanges = getAxisRanges(context)
    val defaultRanges = script.axes.associate { it.id to (0f to 100f) }
    val ranges = defaultRanges + savedRanges
    val sb = StringBuilder()
    for (axis in script.axes) {
        val next = axis.actions.firstOrNull { it.at >= currentPositionMs } ?: continue
        val (minR, maxR) = ranges[axis.id] ?: (0f to 100f)
        val posClamped = next.pos.coerceIn(0, 100)
        val t = posClamped / 100f
        val mappedFloat = minR + t * (maxR - minR)
        val mapped = kotlin.math.round(mappedFloat).toInt().coerceIn(0, 100)
        val durationMs = (next.at - currentPositionMs).coerceIn(1L, 60_000L)
        sb.append(axis.id).append(mapped).append('I').append(durationMs)
    }
    return sb.toString()
}

/** 返回指定轴在当前时间的下一目标：位置 0.0–1.0（已按输出范围映射）、运动时长 ms；无则 null */
private fun getAxisPositionAndDurationForHandy(
    context: android.content.Context,
    script: FunscriptData?,
    axisId: String,
    currentPositionMs: Long
): Pair<Double, Long>? {
    if (script == null) return null
    val axis = script.axes.find { it.id == axisId } ?: return null
    val next = axis.actions.firstOrNull { it.at >= currentPositionMs } ?: return null
    val ranges = getAxisRanges(context)
    val (minR, maxR) = ranges[axisId] ?: (0f to 100f)
    val posClamped = next.pos.coerceIn(0, 100)
    val t = posClamped / 100f
    val mappedFloat = minR + t * (maxR - minR)
    val position = (mappedFloat / 100.0).coerceIn(0.0, 1.0)
    val durationMs = (next.at - currentPositionMs).coerceIn(1L, 60_000L)
    return position to durationMs
}

/**
 * 根据各轴目标位置（0–100）生成轴控制指令字符串。
 * 会按设备设置中的输出范围映射到 [min%, max%]，durationMs 为运动时长。
 */
private fun buildAxisCommandFromPositions(
    context: android.content.Context,
    positions: Map<String, Int>,
    durationMs: Long
): String {
    if (positions.isEmpty()) return ""
    val ranges = getAxisRanges(context)
    val sb = StringBuilder()
    val dur = durationMs.coerceIn(1L, 60_000L)
    for (axisId in AXIS_NAMES) {
        val posRaw = positions[axisId] ?: continue
        val (minR, maxR) = ranges[axisId] ?: (0f to 100f)
        val t = (posRaw.coerceIn(0, 100) / 100f)
        val mapped = kotlin.math.round(minR + t * (maxR - minR)).toInt().coerceIn(0, 100)
        sb.append(axisId).append(mapped).append('I').append(dur)
    }
    return sb.toString()
}

/** 解析轴指令字符串为 (轴id, 位置, 时长ms) 列表，供 The Handy 等单轴发送使用 */
private fun parseAxisCommandSegments(command: String): List<Triple<String, Int, Long>> {
    if (command.isEmpty()) return emptyList()
    val regex = Regex("(L0|L1|L2|R0|R1|R2)(\\d+)I(\\d+)")
    return regex.findAll(command).map { m ->
        Triple(m.groupValues[1], m.groupValues[2].toIntOrNull() ?: 0, m.groupValues[3].toLongOrNull() ?: 500L)
    }.toList()
}

/** 通过当前连接方式发送轴控制指令字符串（含前缀/尾缀或 Handy 线性协议）。 */
private suspend fun sendAxisCommand(context: android.content.Context, axisCommand: String): Boolean {
    if (axisCommand.isEmpty()) return false
    val connType = getConnectionType(context)
    if (connType != ConnectionType.UDP && connType != ConnectionType.TCP &&
        connType != ConnectionType.Serial && connType != ConnectionType.BluetoothSerial &&
        connType != ConnectionType.TheHandy && connType != ConnectionType.JoyPlay
    ) return false
    return withContext(Dispatchers.IO) {
        when (connType) {
            ConnectionType.UDP -> {
                val prefix = getSendFormatPrefix(context)
                val suffix = getSendFormatSuffix(context)
                sendUdpMessage(getDeviceIp(context), getDevicePort(context), prefix + axisCommand + suffix)
            }
            ConnectionType.TCP -> {
                val prefix = getSendFormatPrefix(context)
                val suffix = getSendFormatSuffix(context)
                sendTcpMessage(getDeviceIp(context), getDevicePort(context), prefix + axisCommand + suffix)
            }
            ConnectionType.Serial -> {
                val prefix = getSendFormatPrefix(context)
                val suffix = getSendFormatSuffix(context)
                sendSerialMessage(context, getSerialDeviceId(context), getBaudRate(context), prefix + axisCommand + suffix)
            }
            ConnectionType.BluetoothSerial -> {
                val prefix = getSendFormatPrefix(context)
                val suffix = getSendFormatSuffix(context)
                sendBluetoothSerialMessage(context, getBtSerialDeviceAddress(context), prefix + axisCommand + suffix)
            }
            ConnectionType.TheHandy -> {
                val axisId = getHandyAxis(context)
                val segment = parseAxisCommandSegments(axisCommand).firstOrNull { it.first == axisId } ?: return@withContext false
                val (_, pos, dur) = segment
                val ranges = getAxisRanges(context)
                val (minR, maxR) = ranges[axisId] ?: (0f to 100f)
                val t = (pos.coerceIn(0, 100) / 100f)
                val mappedFloat = minR + t * (maxR - minR)
                val position = (mappedFloat / 100.0).coerceIn(0.0, 1.0)
                val payload = buildHandyLinearPayloadFromPosition(context, axisId, position, dur.toInt())
                if (payload != null) HandyBleClient.write(context, getHandyDeviceAddress(context), payload) else false
            }
            ConnectionType.JoyPlay -> {
                val prefix = getSendFormatPrefix(context)
                val suffix = getSendFormatSuffix(context)
                val message = prefix + axisCommand + suffix
                HandyBleClient.write(context, getJoyPlayDeviceAddress(context), message.toByteArray(Charsets.UTF_8), useWriteWithResponse = false)
            }
            else -> false
        }
    }
}

/** 从单轴目标位置与时长构建 Handy 线性指令（用于设备控制滑块）。 */
private fun buildHandyLinearPayloadFromPosition(
    context: android.content.Context,
    axisId: String,
    position: Double,
    durationMs: Int
): ByteArray? {
    val vector = Handyplug.LinearCmd.Vector.newBuilder()
        .setIndex(0)
        .setDuration(durationMs.coerceIn(1, 65535))
        .setPosition(position.coerceIn(0.0, 1.0))
        .build()
    val linearCmd = Handyplug.LinearCmd.newBuilder()
        .setId(1)
        .setDeviceIndex(0)
        .addVectors(vector)
        .build()
    val handyMsg = Handyplug.HandyMessage.newBuilder()
        .setLinearCmd(linearCmd)
        .build()
    return Handyplug.Payload.newBuilder()
        .addMessages(handyMsg)
        .build()
        .toByteArray()
}

/** 按时间分段计算某轴的运动强度（每段内位置变化量之和，归一化 0f..1f），segmentCount 为时间分段数 */
private fun computeAxisHeatmap(actions: List<FunscriptAction>, totalMs: Long, segmentCount: Int): FloatArray {
    if (totalMs <= 0 || segmentCount <= 0) return FloatArray(0)
    val segLen = totalMs / segmentCount.coerceAtLeast(1)
    val intensity = FloatArray(segmentCount)
    for (i in 1 until actions.size) {
        val (at, pos) = actions[i]
        val prevPos = actions[i - 1].pos
        val delta = kotlin.math.abs(pos - prevPos)
        val segIndex = ((at + actions[i - 1].at) / 2 / segLen).toInt().coerceIn(0, segmentCount - 1)
        intensity[segIndex] += delta
    }
    val max = intensity.maxOrNull() ?: 1f
    if (max > 0) for (i in intensity.indices) intensity[i] = (intensity[i] / max).coerceIn(0f, 1f)
    return intensity
}

// ================== 视频库路径持久化与扫描 ==================

private const val PREFS_NAME = "funplayer_settings"
private const val KEY_VIDEO_LIBRARY_URI = "video_library_uri"
private const val KEY_VIDEO_LIBRARY_DISPLAY_NAME = "video_library_display_name"
private const val KEY_VIDEO_LIBRARY_SOURCE = "video_library_source" // "local" | "nas"
private const val KEY_NAS_HOST = "nas_host"
private const val KEY_NAS_SHARE = "nas_share"
private const val KEY_NAS_USER = "nas_user"
private const val KEY_NAS_PASSWORD = "nas_password"
private const val KEY_NAS_PORT = "nas_port"
private const val KEY_NAS_SUBPATH = "nas_subpath"
private const val KEY_VIDEO_TAGS = "video_tags"
private const val KEY_DEVELOPER_MODE = "developer_mode"

/** 连接测试时发送的固定轴控制指令 */
private const val CONNECTION_TEST_MESSAGE = "L050I900L150I900L250I900R050I900R150I900R250I900"
private const val KEY_CONNECTION_TYPE = "device_connection_type"
private const val KEY_CONNECTION_ENABLED = "device_connection_enabled"
private const val KEY_DEVICE_IP = "device_ip"
private const val KEY_DEVICE_PORT = "device_port"
private const val KEY_SEND_FORMAT_PREFIX = "send_format_prefix"
private const val KEY_SEND_FORMAT_SUFFIX = "send_format_suffix"
private const val KEY_SERIAL_DEVICE = "serial_device_id"
private const val KEY_BAUD_RATE = "serial_baud_rate"
private const val KEY_AXIS_RANGES = "axis_ranges"
private const val KEY_HANDY_KEY = "handy_connection_key"
private const val KEY_HANDY_AXIS = "handy_axis"
private const val KEY_HANDY_DEVICE_ADDRESS = "handy_device_address"
private const val KEY_JOYPLAY_DEVICE_ADDRESS = "joyplay_device_address"
private const val KEY_BT_SERIAL_DEVICE_ADDRESS = "bt_serial_device_address"
private const val KEY_APP_LANGUAGE = "app_language"

private val HANDY_SERVICE_UUID: UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
private val HANDY_CHARACTERISTIC_UUID: UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")
private val BT_SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

/** 开发者模式下的 Log 条目 */
private data class DevLogEntry(val time: Long, val tag: String, val msg: String)

/** 开发者模式 Log 缓冲，悬浮窗口读取并显示 */
private object DevLog {
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

private fun getIsDeveloperMode(context: android.content.Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getBoolean(KEY_DEVELOPER_MODE, false)

private fun setDeveloperMode(context: android.content.Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_DEVELOPER_MODE, enabled).apply()
}

/** 可选值：system / zh / en */
private fun getAppLanguage(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_APP_LANGUAGE, "system") ?: "system"

private fun setAppLanguage(context: android.content.Context, value: String) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_APP_LANGUAGE, value).apply()
}

private fun getConnectionEnabled(context: android.content.Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getBoolean(KEY_CONNECTION_ENABLED, false)

private fun getConnectionType(context: android.content.Context): ConnectionType {
    val name = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_CONNECTION_TYPE, ConnectionType.Serial.name) ?: ConnectionType.Serial.name
    return try {
        ConnectionType.valueOf(name)
    } catch (_: Exception) {
        ConnectionType.Serial
    }
}

private fun getDeviceIp(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

private fun getDevicePort(context: android.content.Context): Int {
    val p = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_DEVICE_PORT, "8080") ?: "8080"
    return p.toIntOrNull()?.coerceIn(1, 65535) ?: 8080
}

private fun getSerialDeviceId(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_SERIAL_DEVICE, "") ?: ""

private fun getBaudRate(context: android.content.Context): Int {
    val s = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_BAUD_RATE, "9600") ?: "9600"
    return s.toIntOrNull()?.coerceIn(300, 4_000_000) ?: 9600
}

private fun getAxisRanges(context: android.content.Context): Map<String, Pair<Float, Float>> {
    val raw = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_AXIS_RANGES, null) ?: return emptyMap()
    val result = mutableMapOf<String, Pair<Float, Float>>()
    raw.split(";").forEach { entry ->
        val parts = entry.split("=")
        if (parts.size == 2) {
            val axis = parts[0].trim()
            val range = parts[1].split(",")
            if (range.size == 2) {
                val minV = range[0].trim().toFloatOrNull()?.coerceIn(0f, 100f) ?: 0f
                val maxV = range[1].trim().toFloatOrNull()?.coerceIn(0f, 100f) ?: 100f
                result[axis] = (minOf(minV, maxV) to maxOf(minV, maxV))
            }
        }
    }
    return result
}

private fun saveAxisRanges(context: android.content.Context, ranges: Map<String, Pair<Float, Float>>) {
    val raw = ranges.entries.joinToString(";") { (axis, pair) ->
        "${axis}=${pair.first},${pair.second}"
    }
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_AXIS_RANGES, raw)
        .apply()
}

private fun getHandyKey(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_HANDY_KEY, "") ?: ""

private fun getHandyAxis(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_HANDY_AXIS, "L0") ?: "L0"

private fun getHandyDeviceAddress(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_HANDY_DEVICE_ADDRESS, "") ?: ""

private fun getJoyPlayDeviceAddress(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_JOYPLAY_DEVICE_ADDRESS, "") ?: ""

private fun getBtSerialDeviceAddress(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_BT_SERIAL_DEVICE_ADDRESS, "") ?: ""

private fun getSendFormatPrefix(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_SEND_FORMAT_PREFIX, "") ?: ""

private fun getSendFormatSuffix(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_SEND_FORMAT_SUFFIX, "") ?: ""

private fun saveConnectionSettings(
    context: android.content.Context,
    connectionEnabled: Boolean,
    connectionType: ConnectionType,
    ipAddress: String,
    port: String,
    sendFormatPrefix: String,
    sendFormatSuffix: String,
    serialDeviceId: String = "",
    baudRate: String = "9600",
    btSerialDeviceAddress: String = "",
    handyDeviceAddress: String = "",
    handyKey: String = "",
    handyAxis: String = "L0",
    joyPlayDeviceAddress: String = ""
) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_CONNECTION_ENABLED, connectionEnabled)
        .putString(KEY_CONNECTION_TYPE, connectionType.name)
        .putString(KEY_DEVICE_IP, ipAddress)
        .putString(KEY_DEVICE_PORT, port)
        .putString(KEY_SEND_FORMAT_PREFIX, sendFormatPrefix)
        .putString(KEY_SEND_FORMAT_SUFFIX, sendFormatSuffix)
        .putString(KEY_SERIAL_DEVICE, serialDeviceId)
        .putString(KEY_BAUD_RATE, baudRate)
        .putString(KEY_BT_SERIAL_DEVICE_ADDRESS, btSerialDeviceAddress)
        .putString(KEY_HANDY_DEVICE_ADDRESS, handyDeviceAddress)
        .putString(KEY_HANDY_KEY, handyKey)
        .putString(KEY_HANDY_AXIS, handyAxis)
        .putString(KEY_JOYPLAY_DEVICE_ADDRESS, joyPlayDeviceAddress)
        .apply()
}

private fun sendUdpMessage(host: String, port: Int, message: String): Boolean {
    if (message.isEmpty()) return true
    return try {
        java.net.DatagramSocket().use { socket ->
            val bytes = message.toByteArray(Charsets.UTF_8)
            val packet = java.net.DatagramPacket(bytes, bytes.size, java.net.InetAddress.getByName(host), port)
            socket.send(packet)
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun sendTcpMessage(host: String, port: Int, message: String): Boolean {
    if (message.isEmpty()) return true
    return try {
        java.net.Socket(host, port).use { socket ->
            socket.getOutputStream().use { os ->
                os.write(message.toByteArray(Charsets.UTF_8))
                os.flush()
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

private fun sendTcpBytes(host: String, port: Int, bytes: ByteArray): Boolean {
    if (bytes.isEmpty()) return true
    return try {
        java.net.Socket(host, port).use { socket ->
            socket.getOutputStream().use { os ->
                os.write(bytes)
                os.flush()
            }
        }
        true
    } catch (_: Exception) {
        false
    }
}

/** 构建 The Handy 的 LinearCmd protobuf 载荷（仅当指定轴有下一目标时返回非 null） */
/** 构建 The Handy 连接测试用的一条线性指令（50% 位置，900ms） */
private fun buildHandyTestPayload(): ByteArray {
    val vector = Handyplug.LinearCmd.Vector.newBuilder()
        .setIndex(0)
        .setDuration(900)
        .setPosition(0.5)
        .build()
    val linearCmd = Handyplug.LinearCmd.newBuilder()
        .setId(1)
        .setDeviceIndex(0)
        .addVectors(vector)
        .build()
    val handyMsg = Handyplug.HandyMessage.newBuilder()
        .setLinearCmd(linearCmd)
        .build()
    return Handyplug.Payload.newBuilder()
        .addMessages(handyMsg)
        .build()
        .toByteArray()
}

private fun buildHandyLinearPayload(
    context: android.content.Context,
    script: FunscriptData?,
    axisId: String,
    currentPositionMs: Long
): ByteArray? {
    val posDur = getAxisPositionAndDurationForHandy(context, script, axisId, currentPositionMs) ?: return null
    val (position, durationMs) = posDur
    val vector = Handyplug.LinearCmd.Vector.newBuilder()
        .setIndex(0)
        .setDuration(durationMs.toInt().coerceIn(1, 65535))
        .setPosition(position)
        .build()
    val linearCmd = Handyplug.LinearCmd.newBuilder()
        .setId(1)
        .setDeviceIndex(0)
        .addVectors(vector)
        .build()
    val handyMsg = Handyplug.HandyMessage.newBuilder()
        .setLinearCmd(linearCmd)
        .build()
    val payload = Handyplug.Payload.newBuilder()
        .addMessages(handyMsg)
        .build()
    return payload.toByteArray()
}

private fun sendBluetoothSerialMessage(context: android.content.Context, address: String, message: String): Boolean {
    if (message.isEmpty() || address.isBlank()) return true
    if (Build.VERSION.SDK_INT >= 31 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
    val adapter: BluetoothAdapter = manager.adapter ?: return false
    val device = try { adapter.getRemoteDevice(address) } catch (_: Exception) { return false }
    // RFCOMM (SPP)
    val socket: BluetoothSocket = try { device.createRfcommSocketToServiceRecord(BT_SPP_UUID) } catch (_: Exception) { return false }
    return try {
        try { adapter.cancelDiscovery() } catch (_: Exception) { }
        socket.connect()
        socket.outputStream.use { os ->
            os.write(message.toByteArray(Charsets.UTF_8))
            os.flush()
        }
        true
    } catch (_: Exception) {
        false
    } finally {
        try { socket.close() } catch (_: Exception) { }
    }
}

private object HandyBleClient {
    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var connectedAddress: String? = null
    private var pendingWrite: kotlinx.coroutines.CompletableDeferred<Boolean>? = null
    /** 协商后的 MTU，用于无响应写入分片；默认 23（BLE 最小值），onMtuChanged 后更新 */
    private var negotiatedMtu: Int = 23

    private fun hasPermission(context: android.content.Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun closeInternal() {
        try { gatt?.close() } catch (_: Exception) { }
        gatt = null
        txCharacteristic = null
        connectedAddress = null
        pendingWrite = null
        negotiatedMtu = 23
    }

    /** @param useWriteWithResponse false 时仅用无响应写入（JoyPlay 方式不需要带响应） */
    suspend fun write(context: android.content.Context, address: String, payload: ByteArray, useWriteWithResponse: Boolean = true): Boolean {
        if (address.isBlank() || payload.isEmpty()) {
            DevLog.log("Handy", "连接/写入失败: 设备地址为空或载荷为空")
            return false
        }
        if (Build.VERSION.SDK_INT >= 31 && !hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            DevLog.log("Handy", "连接失败: 缺少 BLUETOOTH_CONNECT 权限")
            return false
        }
        val forJoyPlay = !useWriteWithResponse
        val ok = ensureConnected(context, address, forJoyPlay)
        if (!ok) return false
        val g = gatt
        val ch = txCharacteristic
        if (g == null || ch == null) {
            DevLog.log("Handy", "写入失败: GATT 或特征为空，连接可能已断开")
            return false
        }
        // The Handy 时连接后稍作延迟再写；JoyPlay 与 eciot 一致不延迟
        if (useWriteWithResponse) kotlinx.coroutines.delay(150L)
        val written = writeChunked(g, ch, payload, useWriteWithResponse)
        if (!written) DevLog.log("Handy", "写入失败: 未完成或超时")
        return written
    }

    /** @param forJoyPlay true 时与 eciot_bletool 一致：connectGatt 不传 TRANSPORT_LE */
    private suspend fun ensureConnected(context: android.content.Context, address: String, forJoyPlay: Boolean = false): Boolean {
        if (gatt != null && txCharacteristic != null && connectedAddress == address) return true
        closeInternal()

        val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
        if (manager == null) {
            DevLog.log("Handy", "连接失败: 无法获取 BluetoothManager")
            return false
        }
        val adapter = manager.adapter
        if (adapter == null) {
            DevLog.log("Handy", "连接失败: 蓝牙适配器不可用")
            return false
        }
        val device: BluetoothDevice = try {
            adapter.getRemoteDevice(address)
        } catch (e: Exception) {
            DevLog.log("Handy", "连接失败: 无效设备地址 $address, ${e.message}")
            return false
        }

        /** 单次连接尝试：先直连(false)，可选 autoConnect。超时 25 秒以兼容慢速机型； */
        suspend fun tryConnect(autoConnect: Boolean): Boolean? {
            return withTimeoutOrNull(25_000L) {
                kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
                    val callback = object : BluetoothGattCallback() {
                        private var finished = false

                        private fun finish(value: Boolean) {
                            if (finished) return
                            finished = true
                            cont.resume(value)
                        }

                        override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                            if (status == BluetoothGatt.GATT_SUCCESS && mtu > 0) {
                                negotiatedMtu = mtu
                                DevLog.log("Handy", "MTU 协商: $mtu")
                            }
                        }

                        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                            if (status != BluetoothGatt.GATT_SUCCESS) {
                                DevLog.log("Handy", "连接失败: onConnectionStateChange status=$status (非 GATT_SUCCESS)")
                                closeInternal()
                                finish(false)
                                return
                            }
                            if (newState == BluetoothProfile.STATE_CONNECTED) {
                                try { g.requestMtu(247) } catch (_: Exception) { }
                                g.discoverServices()
                            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                                if (finished) DevLog.log("Handy", "BLE 连接已断开，下次发送将自动重连")
                                else DevLog.log("Handy", "连接断开: STATE_DISCONNECTED")
                                closeInternal()
                                finish(false)
                            }
                        }

                        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                            if (status != BluetoothGatt.GATT_SUCCESS) {
                                DevLog.log("Handy", "连接失败: onServicesDiscovered status=$status (非 GATT_SUCCESS)")
                                closeInternal()
                                finish(false)
                                return
                            }
                            val service: BluetoothGattService? = g.getService(HANDY_SERVICE_UUID)
                            val ch: BluetoothGattCharacteristic? = service?.getCharacteristic(HANDY_CHARACTERISTIC_UUID)
                            if (ch == null) {
                                DevLog.log("Handy", "连接失败: 未找到 Handy 服务或特征 UUID (Service=$HANDY_SERVICE_UUID, Char=$HANDY_CHARACTERISTIC_UUID)")
                                closeInternal()
                                finish(false)
                                return
                            }
                            txCharacteristic = ch
                            finish(true)
                        }

                        override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                            if (status != BluetoothGatt.GATT_SUCCESS) {
                                DevLog.log("Handy", "写入回调: status=$status (非 GATT_SUCCESS)")
                            }
                            pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
                            pendingWrite = null
                        }
                    }

                    @Suppress("DEPRECATION")
                    val g = if (forJoyPlay) {
                        device.connectGatt(context, autoConnect, callback)
                    } else if (Build.VERSION.SDK_INT >= 23) {
                        device.connectGatt(context, autoConnect, callback, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device.connectGatt(context, autoConnect, callback)
                    }
                    gatt = g
                    connectedAddress = address

                    cont.invokeOnCancellation {
                        closeInternal()
                    }
                }
            }
        }

        var result = tryConnect(autoConnect = false)
        if (result != true) {
            DevLog.log("Handy", "首次连接未成功，关闭后重试一次(直连)")
            closeInternal()
            kotlinx.coroutines.delay(500L)
            result = tryConnect(autoConnect = false)
        }
        if (result != true) {
            DevLog.log("Handy", "直连两次未成，尝试后台连接(autoConnect=true)")
            closeInternal()
            kotlinx.coroutines.delay(300L)
            result = tryConnect(autoConnect = true)
        }
        if (result != true) {
            DevLog.log("Handy", "连接失败: 连接超时(25s)或未完成，已尝试直连与后台连接")
        }
        return result == true
    }

    /** 整包一次写入，不分片。JoyPlay(useWriteWithResponse=false) ；The Handy 先带响应再无响应+重试 */
    private suspend fun writeChunked(g: BluetoothGatt, ch: BluetoothGattCharacteristic, payload: ByteArray, useWriteWithResponse: Boolean): Boolean {
        if (!useWriteWithResponse) return writeOnceNoResponseEciotStyle(g, ch, payload)
        suspend fun tryWriteNoRsp(data: ByteArray, index: Int): Boolean {
            var delayMs = 80L
            repeat(4) { attempt ->
                if (attempt > 0) kotlinx.coroutines.delay(delayMs).also { delayMs = (delayMs * 2).coerceAtMost(400L) }
                if (writeOnceNoResponse(g, ch, data, index)) return true
            }
            return false
        }
        if (writeOnceWithResponse(g, ch, payload, 1)) return true
        return tryWriteNoRsp(payload, 1)
    }

    /** 立即返回，不等回调、不重试 */
    @Suppress("DEPRECATION")
    private fun writeOnceNoResponseEciotStyle(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray): Boolean {
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return try {
            g.writeCharacteristic(ch)
        } catch (e: Exception) {
            DevLog.log("Handy", "写入失败(JoyPlay/eciot): ${e.message}")
            false
        }
    }

    /** 与 JoyPlay 参考 control(cmd) 一致：单包时用「带响应写入」writeValue，更接近 Web/常见实现 */
    @Suppress("DEPRECATION")
    private suspend fun writeOnceWithResponse(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray, chunkIndex: Int = 0): Boolean {
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val started = try { g.writeCharacteristic(ch) } catch (e: Exception) {
            DevLog.log("Handy", "写入(带响应)失败(第${chunkIndex}包): ${e.message}")
            return false
        }
        if (!started) return false
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingWrite = deferred
        val ok = kotlinx.coroutines.withTimeoutOrNull(800L) { deferred.await() }
        pendingWrite = null
        return ok == true
    }

    /** 部分机型顺序或队列敏感 */
    @Suppress("DEPRECATION")
    private suspend fun writeOnceNoResponse(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray, chunkIndex: Int = 0): Boolean {
        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        val started = try { g.writeCharacteristic(ch) } catch (e: Exception) {
            DevLog.log("Handy", "写入失败(第${chunkIndex}包): writeCharacteristic 异常 ${e.message} → 判断: 发不出去")
            return false
        }
        if (!started) {
            DevLog.log("Handy", "写入失败(第${chunkIndex}包): writeCharacteristic 返回 false → 判断: 发不出去(可能 MTU 过大或队列满)")
            return false
        }
        // 等待 onCharacteristicWrite 再发下一包，避免部分机型队列满导致连续 false；若超时未回调则视为已发出（部分机型 NO_RESPONSE 不回调）
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingWrite = deferred
        val ok = kotlinx.coroutines.withTimeoutOrNull(500L) {
            deferred.await()
        }
        pendingWrite = null
        when {
            ok == true -> return true
            ok == false -> { DevLog.log("Handy", "写入(第${chunkIndex}包): status 非 GATT_SUCCESS"); return false }
            else -> { /* 超时未回调，部分栈对 NO_RESPONSE 不回调，视为已发出 */ return true }
        }
    }
}

private fun sendSerialMessage(context: android.content.Context, deviceId: String, baudRate: Int, message: String): Boolean {
    if (message.isEmpty() || deviceId.isBlank()) return true
    val parts = deviceId.trim().split(":")
    if (parts.size < 2) return false
    val vid = parts[0].trim().toIntOrNull(16) ?: return false
    val pid = parts[1].trim().toIntOrNull(16) ?: return false
    val usbManager = context.getSystemService(android.content.Context.USB_SERVICE) as? UsbManager ?: return false
    val device = usbManager.deviceList.values.find { it.vendorId == vid && it.productId == pid } ?: return false
    val connection = usbManager.openDevice(device) ?: return false
    return try {
        val driver = UsbSerialProber.getDefaultProber().probeDevice(device) ?: run {
            connection.close()
            return false
        }
        val port = driver.ports.firstOrNull() ?: run {
            connection.close()
            return false
        }
        port.open(connection)
        port.setParameters(baudRate, UsbSerialPort.DATABITS_8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        port.write(message.toByteArray(Charsets.UTF_8), 5000)
        port.close()
        true
    } catch (_: Exception) {
        try { connection.close() } catch (_: Exception) { }
        false
    }
}

/** 通过当前所选连接方式发送连接测试指令，返回是否发送成功 */
private suspend fun sendConnectionTest(context: android.content.Context): Boolean {
    val connType = getConnectionType(context)
    if (connType != ConnectionType.UDP && connType != ConnectionType.TCP &&
        connType != ConnectionType.Serial && connType != ConnectionType.BluetoothSerial &&
        connType != ConnectionType.TheHandy && connType != ConnectionType.JoyPlay) return false
    return when (connType) {
        ConnectionType.UDP -> {
            val prefix = getSendFormatPrefix(context)
            val suffix = getSendFormatSuffix(context)
            sendUdpMessage(getDeviceIp(context), getDevicePort(context), prefix + CONNECTION_TEST_MESSAGE + suffix)
        }
        ConnectionType.TCP -> {
            val prefix = getSendFormatPrefix(context)
            val suffix = getSendFormatSuffix(context)
            sendTcpMessage(getDeviceIp(context), getDevicePort(context), prefix + CONNECTION_TEST_MESSAGE + suffix)
        }
        ConnectionType.Serial -> {
            val prefix = getSendFormatPrefix(context)
            val suffix = getSendFormatSuffix(context)
            sendSerialMessage(context, getSerialDeviceId(context), getBaudRate(context), prefix + CONNECTION_TEST_MESSAGE + suffix)
        }
        ConnectionType.BluetoothSerial -> {
            val prefix = getSendFormatPrefix(context)
            val suffix = getSendFormatSuffix(context)
            sendBluetoothSerialMessage(context, getBtSerialDeviceAddress(context), prefix + CONNECTION_TEST_MESSAGE + suffix)
        }
        ConnectionType.TheHandy -> {
            val payload = buildHandyTestPayload()
            HandyBleClient.write(context, getHandyDeviceAddress(context), payload)
        }
        ConnectionType.JoyPlay -> {
            val prefix = getSendFormatPrefix(context)
            val suffix = getSendFormatSuffix(context)
            val message = prefix + CONNECTION_TEST_MESSAGE + suffix
            HandyBleClient.write(context, getJoyPlayDeviceAddress(context), message.toByteArray(Charsets.UTF_8), useWriteWithResponse = false)
        }
        else -> false
    }
}

/** 返回 (显示名称, deviceId 即 "vid:pid") 列表，用于串口下拉选择 */
private fun getAvailableSerialPorts(context: android.content.Context): List<Pair<String, String>> {
    val usbManager = context.getSystemService(android.content.Context.USB_SERVICE) as? UsbManager ?: return emptyList()
    val drivers = try {
        UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
    } catch (_: Exception) {
        return emptyList()
    }
    return drivers.map { driver ->
        val d = driver.device
        val vid = d.vendorId.toString(16).lowercase()
        val pid = d.productId.toString(16).lowercase()
        val id = "$vid:$pid"
        val name = d.productName?.takeIf { it.isNotEmpty() } ?: d.deviceName ?: context.getString(R.string.usb_serial)
        Pair("$name ($id)", id)
    }
}

private fun getStoredVideoLibraryUri(context: android.content.Context): String? =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_LIBRARY_URI, null)

private fun getStoredVideoLibraryDisplayName(context: android.content.Context): String? =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_LIBRARY_DISPLAY_NAME, null)

private fun getVideoLibrarySource(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_LIBRARY_SOURCE, "local") ?: "local"

private fun setVideoLibrarySource(context: android.content.Context, source: String) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_VIDEO_LIBRARY_SOURCE, source).apply()
}

private fun getNasHost(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_HOST, "") ?: ""
private fun getNasShare(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_SHARE, "") ?: ""
private fun getNasUser(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_USER, "") ?: ""
private fun getNasPassword(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_PASSWORD, "") ?: ""
private fun getNasPort(context: android.content.Context): Int =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_PORT, "445")?.toIntOrNull() ?: 445
private fun getNasSubpath(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_SUBPATH, "") ?: ""

private fun saveVideoLibrary(context: android.content.Context, uri: android.net.Uri, displayName: String?) {
    context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_VIDEO_LIBRARY_URI, uri.toString())
        .putString(KEY_VIDEO_LIBRARY_DISPLAY_NAME, displayName ?: uri.lastPathSegment ?: context.getString(R.string.unknown))
        .putString(KEY_VIDEO_LIBRARY_SOURCE, "local")
        .apply()
}

/** 通过 SMB 列出指定路径下的子文件夹名称（仅目录）。路径为空或 "/" 表示共享根目录。 */
private suspend fun listSmbFolders(
    host: String,
    share: String,
    user: String,
    password: String,
    port: Int,
    subpath: String
): Result<List<String>> = withContext(Dispatchers.IO) {
    runCatching {
        val path = subpath.trim().trimStart('/')
        val pathPart = if (path.isEmpty()) "" else "$path/"
        val auth = when {
            user.isNotEmpty() && password.isNotEmpty() -> {
                val encUser = java.net.URLEncoder.encode(user, "UTF-8")
                val encPass = java.net.URLEncoder.encode(password, "UTF-8")
                "$encUser:$encPass@"
            }
            else -> ""
        }
        val url = "smb://$auth$host:$port/$share/$pathPart"
        val smbFile = SmbFile(url)
        if (!smbFile.exists() || !smbFile.isDirectory()) return@runCatching emptyList<String>()
        smbFile.listFiles()
            ?.filter { it.isDirectory() && !it.name.equals(".", true) && !it.name.equals("..", true) }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()
    }
}

/** 构建 SMB 基础 URL（不含末尾斜杠的目录路径），auth 已编码。 */
private fun buildSmbBaseUrl(host: String, share: String, user: String, password: String, port: Int, pathPart: String): String {
    val auth = when {
        user.isNotEmpty() && password.isNotEmpty() -> {
            val encUser = java.net.URLEncoder.encode(user, "UTF-8")
            val encPass = java.net.URLEncoder.encode(password, "UTF-8")
            "$encUser:$encPass@"
        }
        else -> ""
    }
    return "smb://$auth$host:$port/$share/$pathPart"
}

/**
 * 从 NAS SMB 路径递归扫描视频文件，返回 VideoItem 列表。
 * uri 为 SMB 地址（如 smb://host/share/path/video.mp4），时长在 NAS 上暂不解析填 ""。
 */
private suspend fun collectVideosFromSmb(
    context: android.content.Context,
    host: String,
    share: String,
    user: String,
    password: String,
    port: Int,
    subpath: String
): List<VideoItem> = withContext(Dispatchers.IO) {
    val list = mutableListOf<VideoItem>()
    val path = subpath.trim().trimStart('/')
    val pathPart = if (path.isEmpty()) "" else "$path/"
    val baseUrl = buildSmbBaseUrl(host, share, user, password, port, pathPart)
    var id = 0
    fun walk(dirUrl: String) {
        runCatching {
            val smbDir = SmbFile(dirUrl)
            if (!smbDir.exists() || !smbDir.isDirectory()) return@runCatching
            smbDir.listFiles()?.forEach { file ->
                val name = file.name ?: return@forEach
                if (name.equals(".", true) || name.equals("..", true)) return@forEach
                if (file.isDirectory()) {
                    val base = dirUrl.trimEnd('/')
                    val childUrl = "$base/$name/"
                    walk(childUrl)
                } else {
                    val ext = name.substringAfterLast('.', "").lowercase()
                    if (ext in VIDEO_EXTENSIONS) {
                        val base = dirUrl.trimEnd('/')
                        val videoUrl = "$base/$name"
                        if (!videoUrl.startsWith("smb://")) return@forEach
                        val baseName = name.substringBeforeLast(".")
                        val parentUrl = videoUrl.substringBeforeLast("/") + "/"
                        val siblingFiles = runCatching {
                            SmbFile(parentUrl).listFiles()?.filter { !it.isDirectory() }?.map { it.name to it.url.toString() }?.toMap() ?: emptyMap()
                        }.getOrNull() ?: emptyMap()
                        val multiMap = mutableMapOf<String, String>()
                        for ((axisId, suffix) in MULTI_FUNSCRIPT_AXIS_SUFFIXES) {
                            val fileName = if (suffix == null) "$baseName.funscript" else "$baseName.$suffix.funscript"
                            siblingFiles[fileName]?.let { multiMap[axisId] = it }
                        }
                        val singleFile = siblingFiles["$baseName.funscript"]
                        val (scriptUri, urisByAxis) = if (multiMap.size > 1) {
                            Pair(multiMap["L0"] ?: multiMap.values.first(), multiMap)
                        } else {
                            Pair(singleFile, null)
                        }
                        val tagFileUri = siblingFiles["$baseName.tag"]
                        list.add(VideoItem(
                            id = id++,
                            name = name,
                            duration = "",
                            tags = emptyList(),
                            uri = videoUrl,
                            funscriptUri = scriptUri,
                            funscriptUrisByAxis = urisByAxis,
                            tagFileUri = tagFileUri,
                            parentFolderUri = parentUrl
                        ))
                    }
                }
            }
        }
    }
    walk(baseUrl)
    val savedTags = loadAllTags(context)
    list.map { item ->
        val tags = item.tagFileUri?.let { loadTagsFromSmbTagFile(it) } ?: savedTags[item.uri] ?: emptyList()
        item.copy(tags = tags)
    }
}

/** 从 SMB 上的 .tag 文件读取标签（URI 为 smb://...）。 */
private fun loadTagsFromSmbTagFile(smbTagUri: String): List<String>? {
    return try {
        val smbFile = SmbFile(smbTagUri)
        if (!smbFile.exists()) return null
        smbFile.inputStream.use { stream ->
            val json = stream.reader(Charsets.UTF_8).readText()
            val root = JSONObject(json)
            root.optJSONArray("tags")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveNasSettings(
    context: android.content.Context,
    host: String,
    share: String,
    user: String,
    password: String,
    port: Int,
    subpath: String
) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_VIDEO_LIBRARY_SOURCE, "nas")
        .putString(KEY_NAS_HOST, host)
        .putString(KEY_NAS_SHARE, share)
        .putString(KEY_NAS_USER, user)
        .putString(KEY_NAS_PASSWORD, password)
        .putString(KEY_NAS_PORT, port.toString())
        .putString(KEY_NAS_SUBPATH, subpath)
        .apply()
}

// ================== .tag 文件格式（每视频一个文件，与视频同目录） ==================
// 文件名：视频名.tag，例如 卡路里.mp4 对应 卡路里.tag
// 内容：JSON，UTF-8
//   {
//     "version": 1,
//     "tags": ["标签1", "标签2"]
//   }
// version 保留扩展用；tags 为字符串数组，顺序可保留。若存在 .tag 文件则优先使用，否则回退到 SharedPreferences。

private const val TAG_FILE_VERSION = 1

private fun loadTagsFromTagFile(context: android.content.Context, tagFileUri: String?): List<String>? {
    if (tagFileUri == null) return null
    return try {
        val uri = android.net.Uri.parse(tagFileUri)
        val json = context.contentResolver.openInputStream(uri)?.reader(Charsets.UTF_8)?.use { it.readText() } ?: return null
        val root = JSONObject(json)
        root.optJSONArray("tags")?.let { arr ->
            (0 until arr.length()).map { arr.getString(it) }
        } ?: emptyList()
    } catch (_: Exception) {
        null
    }
}

private fun saveTagsToTagFile(context: android.content.Context, tagFileUri: String?, tags: List<String>): Boolean {
    if (tagFileUri == null) return false
    return try {
        val uri = android.net.Uri.parse(tagFileUri)
        val root = JSONObject().put("version", TAG_FILE_VERSION).put("tags", JSONArray(tags))
        context.contentResolver.openOutputStream(uri, "w")?.writer(Charsets.UTF_8)?.use { it.write(root.toString()) } != null
    } catch (_: Exception) {
        false
    }
}

/** 从 SharedPreferences 读取所有视频的标签（key 为视频 uri） */
private fun loadAllTags(context: android.content.Context): Map<String, List<String>> {
    val json = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_TAGS, "{}") ?: "{}"
    return try {
        val obj = JSONObject(json)
        obj.keys().asSequence().associate { key ->
            key to (obj.optJSONArray(key)?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList())
        }
    } catch (_: Exception) {
        emptyMap()
    }
}

/** 保存某个视频的标签：优先写入 .tag 文件（存在则写，不存在则尝试在同目录创建），并回写 SharedPreferences 以兼容列表展示。 */
private fun saveTagsForVideo(context: android.content.Context, video: VideoItem, tags: List<String>) {
    val uri = video.uri ?: return
    val tagFileUriToUse = video.tagFileUri ?: run {
        // 先查找同目录是否已有 视频名.tag，避免多次保存时重复 createFile 产生多个文件
        val parentUri = video.parentFolderUri ?: return@run null
        val baseName = video.name.substringBeforeLast(".")
        val parent = DocumentFile.fromTreeUri(context, android.net.Uri.parse(parentUri)) ?: return@run null
        val existing = parent.findFile("$baseName.tag")
        if (existing != null) existing.uri.toString()
        else {
            val created = parent.createFile("application/octet-stream", "$baseName.tag") ?: return@run null
            created.uri.toString()
        }
    }
    if (tagFileUriToUse != null) {
        saveTagsToTagFile(context, tagFileUriToUse, tags)
    }
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val current = loadAllTags(context).toMutableMap()
    current[uri] = tags
    val obj = JSONObject()
    current.forEach { (k, v) -> obj.put(k, JSONArray(v)) }
    prefs.edit().putString(KEY_VIDEO_TAGS, obj.toString()).apply()
}

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "flv")

private const val THUMBNAIL_WIDTH = 320
private const val THUMBNAIL_HEIGHT = 180
/** NAS/SMB 视频为获取封面和时长时，最多拷贝到本地的字节数（避免大文件长时间下载） */
private const val SMB_TEMP_COPY_LIMIT = 20L * 1024 * 1024  // 20 MB

/** NAS 方式下封面、时长、脚本等数据的软件内存缓存，获取完成后复用避免重复拉取 */
private object NasDataCache {
    private const val THUMBNAIL_CACHE_BYTES = 15 * 1024 * 1024  // 约 15MB 封面
    private const val DURATION_CACHE_MAX_ENTRIES = 500
    private const val FUNSCRIPT_CACHE_MAX_ENTRIES = 100

    val thumbnailCache = object : LruCache<String, Bitmap>(THUMBNAIL_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) value.byteCount else value.width * value.height * 4
        }
    }
    val durationCache = LruCache<String, String>(DURATION_CACHE_MAX_ENTRIES)
    private val funscriptCache = object : LinkedHashMap<String, FunscriptData>(FUNSCRIPT_CACHE_MAX_ENTRIES + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FunscriptData>?) = size > FUNSCRIPT_CACHE_MAX_ENTRIES
    }
    @Synchronized fun getFunscript(key: String): FunscriptData? = funscriptCache[key]
    @Synchronized fun putFunscript(key: String, data: FunscriptData) { funscriptCache[key] = data }
}

/**
 * 将 SMB 文件拷贝到临时文件，最多拷贝 maxBytes 字节（0 表示不限制）。
 * 返回临时文件，调用方负责在不用时删除；失败返回 null。
 */
private fun copySmbToTempFile(smbUri: String, maxBytes: Long = SMB_TEMP_COPY_LIMIT): File? {
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

private fun loadVideoFirstFrame(context: android.content.Context, uriString: String): Bitmap? {
    if (uriString.startsWith("smb:", ignoreCase = true)) {
        NasDataCache.thumbnailCache.get(uriString)?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        if (uriString.startsWith("smb:", ignoreCase = true)) {
            val tempFile = copySmbToTempFile(uriString)
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

/** 从视频 URI 读取时长（毫秒），格式化为 "M:SS" 或 "H:MM:SS"，失败返回 "0:00"。支持 content URI 与 smb:// */
private fun getVideoDurationFormatted(context: android.content.Context, uri: android.net.Uri): String {
    return getVideoDurationFormattedFromUri(context, uri.toString())
}

/** 从视频 URI 字符串获取格式化时长，支持 content 与 smb://；NAS 会先拷贝到临时文件再解析，结果写入内存缓存 */
private fun getVideoDurationFormattedFromUri(context: android.content.Context, uriString: String): String {
    if (uriString.startsWith("smb:", ignoreCase = true)) {
        NasDataCache.durationCache.get(uriString)?.let { return it }
    }
    val retriever = MediaMetadataRetriever()
    return try {
        val result = if (uriString.startsWith("smb:", ignoreCase = true)) {
            val tempFile = copySmbToTempFile(uriString)
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

private fun formatDurationFromRetriever(retriever: MediaMetadataRetriever): String {
    val ms = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
    val totalSec = (ms / 1000).toInt().coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

private fun collectVideosFromTree(context: android.content.Context, treeUri: android.net.Uri): List<VideoItem> {
    val list = mutableListOf<VideoItem>()
    val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return list
    var id = 0
    fun walk(dir: DocumentFile) {
        dir.listFiles().forEach { file ->
            if (file.isDirectory) walk(file)
            else {
                val name = file.name ?: return@forEach
                val ext = name.substringAfterLast('.', "").lowercase()
                if (ext in VIDEO_EXTENSIONS) {
                    val uri = file.uri.toString()
                    val duration = getVideoDurationFormatted(context, file.uri)
                    val baseName = name.substringBeforeLast(".")
                    val siblingFiles = file.parentFile?.listFiles() ?: emptyArray()
                    // 多脚本：每个轴一个文件（如 video.funscript + video.surge.funscript）；合并脚本：一个文件含多轴（video.funscript 内带 axes 或 actions）
                    val multiMap = mutableMapOf<String, String>()
                    for ((axisId, suffix) in MULTI_FUNSCRIPT_AXIS_SUFFIXES) {
                        val fileName = if (suffix == null) "$baseName.funscript" else "$baseName.$suffix.funscript"
                        val found = siblingFiles.find { it.name == fileName }
                        if (found != null) multiMap[axisId] = found.uri.toString()
                    }
                    val singleFile = siblingFiles.find { it.name == "$baseName.funscript" }?.uri?.toString()
                    val (scriptUri, urisByAxis) = if (multiMap.size > 1) {
                        // 存在至少两个轴文件 -> 多脚本方式（各轴分开）
                        Pair(multiMap["L0"] ?: multiMap.values.first(), multiMap)
                    } else {
                        // 仅有一个 baseName.funscript 或没有 -> 单文件合并脚本方式（由 loadFunscriptFromUri 解析 axes/actions）
                        Pair(singleFile, null)
                    }
                    val tagFile = siblingFiles.find { it.name == "$baseName.tag" }
                    val tagFileUri = tagFile?.uri?.toString()
                    val parentFolderUri = file.parentFile?.uri?.toString()
                    list.add(VideoItem(
                        id = id++,
                        name = name,
                        duration = duration,
                        tags = emptyList(),
                        uri = uri,
                        funscriptUri = scriptUri,
                        funscriptUrisByAxis = urisByAxis,
                        tagFileUri = tagFileUri,
                        parentFolderUri = parentFolderUri
                    ))
                }
            }
        }
    }
    walk(tree)
    val savedTags = loadAllTags(context)
    return list.map { item ->
        val tags = if (item.tagFileUri != null) {
            loadTagsFromTagFile(context, item.tagFileUri) ?: savedTags[item.uri] ?: emptyList()
        } else {
            savedTags[item.uri] ?: emptyList()
        }
        item.copy(tags = tags)
    }
}

// ================== 顶层应用 ==================

@Composable
fun VideoApp(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
    ) {

    val context = LocalContext.current
    var isDeveloperMode by remember { mutableStateOf(getIsDeveloperMode(context)) }
    var videos by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Main) }
    var searchText by remember { mutableStateOf("") }
    var selectedTags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showLogWindow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (isDeveloperMode) DevLog.log("App", "开发者模式已开启，可点击 Log 悬浮窗查看日志")
    }
    // 启动时根据视频库来源自动扫描：本地用 Document tree URI，NAS 用 SMB 参数
    LaunchedEffect(Unit) {
        when (getVideoLibrarySource(context)) {
            "local" -> {
                val uri = getStoredVideoLibraryUri(context)
                if (uri != null) {
                    val list = withContext(Dispatchers.IO) {
                        collectVideosFromTree(context, android.net.Uri.parse(uri))
                    }
                    videos = list
                }
            }
            "nas" -> {
                val host = getNasHost(context)
                val share = getNasShare(context)
                if (host.isNotBlank() && share.isNotBlank()) {
                    val list = withContext(Dispatchers.IO) {
                        collectVideosFromSmb(
                            context,
                            host,
                            share,
                            getNasUser(context),
                            getNasPassword(context),
                            getNasPort(context),
                            getNasSubpath(context)
                        )
                    }
                    videos = list
                }
            }
            else -> { }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Main -> {
                    MainScaffold(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it },
                    videos = videos,
                    searchText = searchText,
                    onSearchTextChange = { searchText = it },
                    selectedTags = selectedTags,
                    onTagToggled = { tag ->
                        selectedTags = if (tag == TAG_KEY_ALL) emptySet()
                        else if (tag in selectedTags) selectedTags - tag
                        else selectedTags + tag
                    },
                    onVideoClick = { videoId ->
                        currentScreen = Screen.VideoDetail(videoId)
                    },
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onVideosUpdate = { videos = it },
                    onDeveloperModeChange = { isDeveloperMode = it }
                )
            }

            is Screen.VideoDetail -> {
                val video = videos.firstOrNull { it.id == screen.videoId }
                if (video != null) {
                    VideoDetailScreen(
                        video = video,
                        onBack = { currentScreen = Screen.Main },
                        onTagsChanged = { newTags ->
                            videos = videos.map {
                                if (it.id == video.id) it.copy(tags = newTags) else it
                            }
                            saveTagsForVideo(context, video, newTags)
                        }
                    )
                } else {
                    currentScreen = Screen.Main
                }
            }
        }
        }

        if (isDeveloperMode) {
            if (showLogWindow) {
                DevLogOverlay(
                    onClose = { showLogWindow = false },
                    onClear = { DevLog.clear() },
                    modifier = Modifier.zIndex(999f)
                )
            } else {
                FloatingActionButton(
                    onClick = { showLogWindow = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .zIndex(998f),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(stringResource(R.string.dev_log_fab), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun DevLogOverlay(
    onClose: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val entries = DevLog.entries
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.dev_log_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClear) {
                        Text(stringResource(R.string.dev_log_clear))
                    }
                    TextButton(onClick = onClose) {
                        Text(stringResource(R.string.dev_log_close))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(entries.size, key = { it }) { i ->
                        val e = entries.getOrNull(i) ?: return@items
                        Text(
                            text = "${android.text.format.DateFormat.format("HH:mm:ss.SSS", e.time)} [${e.tag}] ${e.msg}",
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

// ================== 主 Scaffold 和底部导航 ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScaffold(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    videos: List<VideoItem>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onVideoClick: (Int) -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onVideosUpdate: (List<VideoItem>) -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavigationBar(currentTab = currentTab, onTabSelected = onTabSelected)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentTab) {
                MainTab.Device -> DeviceSettingsScreen()
                MainTab.Home -> HomeScreen(
                    videos = videos,
                    searchText = searchText,
                    onSearchTextChange = onSearchTextChange,
                    selectedTags = selectedTags,
                    onTagToggled = onTagToggled,
                    onVideoClick = onVideoClick
                )
                MainTab.Settings -> AppSettingsScreen(
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onVideosScanned = onVideosUpdate,
                    onDeveloperModeChange = onDeveloperModeChange
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomNavigationBar(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentTab == MainTab.Device,
            onClick = { onTabSelected(MainTab.Device) },
            icon = { Icon(Icons.Filled.Devices, contentDescription = stringResource(R.string.tab_device)) },
            label = { Text(stringResource(R.string.tab_device)) }
        )
        NavigationBarItem(
            selected = currentTab == MainTab.Home,
            onClick = { onTabSelected(MainTab.Home) },
            icon = { Icon(Icons.Filled.Home, contentDescription = stringResource(R.string.tab_home)) },
            label = { Text(stringResource(R.string.tab_home)) }
        )
        NavigationBarItem(
            selected = currentTab == MainTab.Settings,
            onClick = { onTabSelected(MainTab.Settings) },
            icon = { Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.tab_settings)) },
            label = { Text(stringResource(R.string.tab_settings)) }
        )
    }
}

// ================== 主页：搜索 + 标签 + 双列网格 ==================

/** 筛选栏单行最多显示的标签个数，超出后显示省略号并弹窗选择 */
private const val MAX_VISIBLE_TAGS_IN_ROW = 6
/** 标签“全部”的逻辑 key，与语言无关 */
private const val TAG_KEY_ALL = "all"

@Composable
private fun HomeScreen(
    videos: List<VideoItem>,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    selectedTags: Set<String>,
    onTagToggled: (String) -> Unit,
    onVideoClick: (Int) -> Unit
) {
    val allTags = remember(videos) {
        buildSet<String> {
            videos.forEach { addAll(it.tags) }
        }.toList().sorted()
    }

    val visibleTags = allTags.take(MAX_VISIBLE_TAGS_IN_ROW)
    val showMoreChip = allTags.size > MAX_VISIBLE_TAGS_IN_ROW
    var showAllTagsDialog by remember { mutableStateOf(false) }

    // 交集筛选：选中多个标签时，只显示同时包含所有选中标签的视频
    val filteredVideos = videos.filter { video ->
        val matchSearch = searchText.isBlank() ||
                video.name.contains(searchText, ignoreCase = true) ||
                video.tags.any { it.contains(searchText, ignoreCase = true) }

        val matchTag = selectedTags.isEmpty() || selectedTags.all { it in video.tags }
        matchSearch && matchTag
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = searchText,
            onValueChange = onSearchTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            placeholder = { Text(stringResource(R.string.search_placeholder)) },
            singleLine = true
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            FilterChipItem(
                text = stringResource(R.string.tag_all),
                selected = selectedTags.isEmpty(),
                onClick = { onTagToggled(TAG_KEY_ALL) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            visibleTags.forEach { tag ->
                FilterChipItem(
                    text = tag,
                    selected = tag in selectedTags,
                    onClick = { onTagToggled(tag) }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            if (showMoreChip) {
                FilterChipItem(
                    text = "…",
                    selected = false,
                    onClick = { showAllTagsDialog = true }
                )
            }
        }

        if (showAllTagsDialog) {
            AlertDialog(
                onDismissRequest = { showAllTagsDialog = false },
                title = { Text(stringResource(R.string.tag_select_title)) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilterChipItem(
                                text = stringResource(R.string.tag_all),
                                selected = selectedTags.isEmpty(),
                                onClick = { onTagToggled(TAG_KEY_ALL) }
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            allTags.forEach { tag ->
                                FilterChipItem(
                                    text = tag,
                                    selected = tag in selectedTags,
                                    onClick = { onTagToggled(tag) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAllTagsDialog = false }) {
                        Text(stringResource(R.string.done))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredVideos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 72.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.video_empty_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 72.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredVideos, key = { it.id }) { video ->
                    VideoCard(
                        video = video,
                        onClick = { onVideoClick(video.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChipItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(text) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            labelColor = if (selected) Color.White
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var thumbnail by remember(video.uri) { mutableStateOf<Bitmap?>(null) }
    var loadedDuration by remember(video.uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(video.uri) {
        if (video.uri != null) {
            val b = withContext(Dispatchers.IO) { loadVideoFirstFrame(context, video.uri!!) }
            thumbnail = b
        }
    }
    LaunchedEffect(video.uri) {
        if (video.uri != null && video.duration.isEmpty() && video.uri.startsWith("smb:", ignoreCase = true)) {
            val dur = withContext(Dispatchers.IO) { getVideoDurationFormattedFromUri(context, video.uri) }
            loadedDuration = dur
        }
    }

    val displayDuration = video.duration.ifEmpty { loadedDuration ?: "…" }

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16 / 9f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF263238)),
            contentAlignment = Alignment.Center
        ) {
            if (thumbnail != null) {
                Image(
                    bitmap = thumbnail!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(stringResource(R.string.cover), color = Color.White.copy(alpha = 0.7f))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = video.name,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = stringResource(R.string.duration_format, displayDuration),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            video.tags.take(3).forEach { tag ->
                TagChip(text = tag)
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(text, fontSize = 11.sp)
    }
}

// ================== 嵌入式视频播放器（ExoPlayer） ==================

@Composable
private fun VideoPlayerEmbed(
    videoUri: String?,
    modifier: Modifier = Modifier,
    onVideoSizeKnown: ((width: Int, height: Int) -> Unit)? = null,
    onPlaybackPosition: ((positionMs: Long) -> Unit)? = null
) {
    val context = LocalContext.current

    if (videoUri == null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_play_uri),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
        }
        return
    }

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            if (videoUri.startsWith("smb:", ignoreCase = true)) {
                val mediaItem = MediaItem.fromUri(android.net.Uri.parse(videoUri))
                val mediaSource = ProgressiveMediaSource.Factory(SmbDataSource.Factory())
                    .createMediaSource(mediaItem)
                setMediaSource(mediaSource)
            } else {
                setMediaItem(MediaItem.fromUri(android.net.Uri.parse(videoUri)))
            }
            prepare()
            playWhenReady = false  // 进入详情页不自动播放
        }
    }

    DisposableEffect(videoUri) {
        onDispose {
            exoPlayer.release()
        }
    }

    if (onVideoSizeKnown != null) {
        DisposableEffect(exoPlayer, onVideoSizeKnown) {
            val listener = object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        onVideoSizeKnown(videoSize.width, videoSize.height)
                    }
                }
            }
            exoPlayer.addListener(listener)
            onDispose { exoPlayer.removeListener(listener) }
        }
    }

    onPlaybackPosition?.let { callback ->
        LaunchedEffect(exoPlayer) {
            while (true) {
                delay(100)
                callback(exoPlayer.currentPosition)
            }
        }
    }

    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(12.dp)),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
                controllerShowTimeoutMs = 3000
            }
        }
    )
}

// ================== 视频详情页（全屏） ==================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoDetailScreen(
    video: VideoItem,
    onBack: () -> Unit,
    onTagsChanged: (List<String>) -> Unit
) {
    // 左/右滑或返回键：若正在全屏则先退出全屏，否则返回视频选择页
    var isFullscreen by remember { mutableStateOf(false) }
    BackHandler {
        if (isFullscreen) isFullscreen = false else onBack()
    }

    val activity = LocalContext.current as? Activity
    // 全屏时的方向：true=横屏，false=竖屏；进入全屏默认横屏
    var fullscreenIsLandscape by remember { mutableStateOf(true) }

    // 系统栏与窗口：全屏时隐藏，非全屏时恢复
    SideEffect {
        activity ?: return@SideEffect
        val controller = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (isFullscreen) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    // 全屏时的屏幕方向：用 LaunchedEffect 确保在状态变化时立即生效（含旋转按钮）
    LaunchedEffect(isFullscreen, fullscreenIsLandscape) {
        activity ?: return@LaunchedEffect
        if (isFullscreen) {
            val orientation = if (fullscreenIsLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            }
            activity.requestedOrientation = orientation
        }
    }

    val context = LocalContext.current
    var localTags by remember(video) { mutableStateOf(video.tags) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteMode by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var loadedDuration by remember(video.uri) { mutableStateOf<String?>(null) }

    LaunchedEffect(video.uri) {
        if (video.uri != null && video.duration.isEmpty() && video.uri.startsWith("smb:", ignoreCase = true)) {
            val dur = withContext(Dispatchers.IO) { getVideoDurationFormattedFromUri(context, video.uri) }
            loadedDuration = dur
        }
    }
    val detailDisplayDuration = video.duration.ifEmpty { loadedDuration ?: "…" }

    val scriptKey = video.funscriptUri to video.funscriptUrisByAxis
    var funscriptData by remember(scriptKey) { mutableStateOf<FunscriptData?>(null) }
    var funscriptHeatmaps by remember(scriptKey) { mutableStateOf<Map<String, FloatArray>?>(null) }
    var playbackPositionMs by remember { mutableStateOf(0L) }
    LaunchedEffect(video.funscriptUri, video.funscriptUrisByAxis) {
        val multiUris = video.funscriptUrisByAxis
        val data = if (multiUris != null && multiUris.isNotEmpty()) {
            withContext(Dispatchers.IO) { loadFunscriptMultiFromUris(context, multiUris) }
        } else {
            val uri = video.funscriptUri
            if (uri == null) {
                funscriptData = null
                funscriptHeatmaps = null
                return@LaunchedEffect
            }
            withContext(Dispatchers.IO) { loadFunscriptFromUri(context, uri) }
        } ?: run {
            funscriptData = null
            funscriptHeatmaps = null
            return@LaunchedEffect
        }
        val totalMs = if (data.durationSec > 0) (data.durationSec * 1000).toLong()
            else data.axes.flatMap { it.actions }.maxOfOrNull { it.at } ?: 1L
        val totalMsCoerced = totalMs.coerceAtLeast(1L)
        val segmentCount = 120
        val heatmaps = data.axes.associate { axis ->
            axis.id to computeAxisHeatmap(axis.actions, totalMsCoerced, segmentCount)
        }
        funscriptData = data
        funscriptHeatmaps = heatmaps
    }

    LaunchedEffect(localTags) {
        onTagsChanged(localTags)
    }

    val axisCommandForSend = buildAxisCommandFromScript(context, funscriptData, playbackPositionMs)
    LaunchedEffect(axisCommandForSend) {
        if (axisCommandForSend.isEmpty()) return@LaunchedEffect
        if (!getConnectionEnabled(context)) return@LaunchedEffect
        val connType = getConnectionType(context)
        if (connType != ConnectionType.UDP &&
            connType != ConnectionType.TCP &&
            connType != ConnectionType.Serial &&
            connType != ConnectionType.BluetoothSerial &&
            connType != ConnectionType.TheHandy &&
            connType != ConnectionType.JoyPlay
        ) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            when (connType) {
                ConnectionType.UDP -> {
                    val prefix = getSendFormatPrefix(context)
                    val suffix = getSendFormatSuffix(context)
                    sendUdpMessage(getDeviceIp(context), getDevicePort(context), prefix + axisCommandForSend + suffix)
                }
                ConnectionType.TCP -> {
                    val prefix = getSendFormatPrefix(context)
                    val suffix = getSendFormatSuffix(context)
                    sendTcpMessage(getDeviceIp(context), getDevicePort(context), prefix + axisCommandForSend + suffix)
                }
                ConnectionType.Serial -> {
                    val prefix = getSendFormatPrefix(context)
                    val suffix = getSendFormatSuffix(context)
                    sendSerialMessage(context, getSerialDeviceId(context), getBaudRate(context), prefix + axisCommandForSend + suffix)
                }
                ConnectionType.BluetoothSerial -> {
                    val prefix = getSendFormatPrefix(context)
                    val suffix = getSendFormatSuffix(context)
                    sendBluetoothSerialMessage(context, getBtSerialDeviceAddress(context), prefix + axisCommandForSend + suffix)
                }
                ConnectionType.TheHandy -> {
                    val payload = buildHandyLinearPayload(context, funscriptData, getHandyAxis(context), playbackPositionMs)
                    if (payload != null) {
                        HandyBleClient.write(context, getHandyDeviceAddress(context), payload)
                    }
                }
                ConnectionType.JoyPlay -> {
                    val prefix = getSendFormatPrefix(context)
                    val suffix = getSendFormatSuffix(context)
                    val message = prefix + axisCommandForSend + suffix
                    HandyBleClient.write(context, getJoyPlayDeviceAddress(context), message.toByteArray(Charsets.UTF_8), useWriteWithResponse = false)
                }
                else -> { }
            }
        }
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    title = { Text(stringResource(R.string.video_detail_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(if (isFullscreen) 0.dp else 16.dp)
                .then(
                    if (isFullscreen) Modifier.fillMaxSize()
                    else Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                )
        ) {
            Box(
                modifier = if (isFullscreen) Modifier.fillMaxSize().background(Color.Black)
                else Modifier.fillMaxWidth().aspectRatio(16 / 9f)
            ) {
                VideoPlayerEmbed(
                    videoUri = video.uri,
                    modifier = Modifier.fillMaxSize(),
                    onPlaybackPosition = { playbackPositionMs = it }
                )
                if (isFullscreen) {
                    IconButton(
                        onClick = { isFullscreen = false },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .zIndex(2f)
                    ) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.exit_fullscreen),
                            tint = Color.White
                        )
                    }
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .zIndex(2f),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(
                            onClick = { fullscreenIsLandscape = !fullscreenIsLandscape }
                        ) {
                            Icon(
                                Icons.Filled.ScreenRotation,
                                contentDescription = stringResource(R.string.rotate_fullscreen),
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { isFullscreen = false }) {
                            Icon(
                                Icons.Filled.FullscreenExit,
                                contentDescription = stringResource(R.string.exit_fullscreen),
                                tint = Color.White
                            )
                        }
                    }
                } else {
                    IconButton(
                        onClick = { isFullscreen = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Fullscreen,
                            contentDescription = stringResource(R.string.fullscreen_play),
                            tint = Color.White
                        )
                    }
                }
            }

            if (!isFullscreen) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = video.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.video_duration_format, detailDisplayDuration),
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.label_tags),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                localTags.forEach { tag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(tag, fontSize = 12.sp)
                        if (showDeleteMode) {
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "×",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .clickable {
                                        localTags = localTags.filterNot { it == tag }
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                AssistChip(
                    onClick = { showAddDialog = true },
                    label = { Text(stringResource(R.string.add_tag_btn)) }
                )

                Spacer(modifier = Modifier.width(8.dp))

                AssistChip(
                    onClick = { showDeleteMode = !showDeleteMode },
                    label = { Text(if (showDeleteMode) stringResource(R.string.done) else stringResource(R.string.add_tag_done_btn)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.script_heatmap),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val heatmaps = funscriptHeatmaps
            val scriptData = funscriptData
            if (scriptData != null && heatmaps != null && heatmaps.isNotEmpty()) {
                val coolColor = Color(0xFF2196F3)
                val hotColor = Color(0xFFF44336)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    scriptData.axes.forEach { axis ->
                        val intensities = heatmaps[axis.id] ?: return@forEach
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = axis.id,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.width(28.dp)
                            )
                            Canvas(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                            ) {
                                val w = size.width
                                val h = size.height
                                val segW = w / intensities.size.coerceAtLeast(1)
                                intensities.forEachIndexed { i, t ->
                                    val color = lerp(coolColor, hotColor, t)
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(i * segW, 0f),
                                        size = Size(segW + 1f, h)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(
                            if (video.funscriptUri == null && video.funscriptUrisByAxis.isNullOrEmpty())
                                R.string.no_script else R.string.script_loading
                        ),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (getIsDeveloperMode(context) && axisCommandForSend.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.axis_command),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = axisCommandForSend,
                        fontSize = 12.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text(stringResource(R.string.add_tag_dialog_title)) },
                text = {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        label = { Text(stringResource(R.string.tag_name_label)) }
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val trimmed = newTagText.trim()
                            if (trimmed.isNotEmpty() && !localTags.contains(trimmed)) {
                                localTags = localTags + trimmed
                            }
                            newTagText = ""
                            showAddDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
    }
}

// ================== 设备设置页和软件设置页 ==================

private enum class ConnectionType(@StringRes val nameResId: Int) {
    Serial(R.string.serial_connection),
    BluetoothSerial(R.string.bt_serial_connection),
    TCP(R.string.tcp_connection),
    UDP(R.string.udp_connection),
    TheHandy(R.string.handy_connection),
    JoyPlay(R.string.joyplay_connection)
}

private val AXIS_NAMES = listOf("L0", "L1", "L2", "R0", "R1", "R2")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDeveloperMode = getIsDeveloperMode(context)
    val activity = context as? Activity
    var connectionTestInProgress by remember { mutableStateOf(false) }

    var handyScanning by remember { mutableStateOf(false) }
    var handyDeviceExpanded by remember { mutableStateOf(false) }
    var handyDeviceOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var handyScanTick by remember { mutableStateOf(0) }
    var joyPlayScanTick by remember { mutableStateOf(0) }
    var joyPlayScanning by remember { mutableStateOf(false) }
    var pendingBleScan by remember { mutableStateOf<ConnectionType?>(null) }

    val handyPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            when (pendingBleScan) {
                ConnectionType.TheHandy -> handyScanTick++
                ConnectionType.JoyPlay -> joyPlayScanTick++
                else -> { }
            }
            pendingBleScan = null
        } else {
            Toast.makeText(context, context.getString(R.string.bt_scan_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }

    // BLE 权限：Android 12 (API 31)–16 需 BLUETOOTH_SCAN/CONNECT；Android 6–11 需定位，部分 12+ 机型也需定位才有扫描结果
    fun handyRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= 31) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun handyHasAllPermissions(): Boolean {
        return handyRequiredPermissions().all { p ->
            ContextCompat.checkSelfPermission(context, p) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun handyStartScan() {
        if (handyScanning) return
        if (!handyHasAllPermissions()) {
            pendingBleScan = ConnectionType.TheHandy
            handyPermLauncher.launch(handyRequiredPermissions())
            return
        }
        handyScanTick++
    }

    fun joyPlayStartScan() {
        if (joyPlayScanning) return
        if (!handyHasAllPermissions()) {
            pendingBleScan = ConnectionType.JoyPlay
            handyPermLauncher.launch(handyRequiredPermissions())
            return
        }
        joyPlayScanTick++
    }

    var connectionEnabled by remember { mutableStateOf(getConnectionEnabled(context)) }
    var connectionType by remember { mutableStateOf(getConnectionType(context)) }
    var connectionExpanded by remember { mutableStateOf(false) }

    var ipAddress by remember { mutableStateOf(getDeviceIp(context)) }
    var port by remember { mutableStateOf(context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_DEVICE_PORT, "8080") ?: "8080") }
    var serialDeviceId by remember { mutableStateOf(getSerialDeviceId(context)) }
    var serialPortExpanded by remember { mutableStateOf(false) }
    var serialPortOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var baudRate by remember { mutableStateOf(context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_BAUD_RATE, "9600") ?: "9600") }
    var btSerialDeviceAddress by remember { mutableStateOf(getBtSerialDeviceAddress(context)) }
    var btSerialDeviceExpanded by remember { mutableStateOf(false) }
    var btSerialDeviceOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var handyAxis by remember { mutableStateOf(getHandyAxis(context)) }
    var handyAxisExpanded by remember { mutableStateOf(false) }
    var handyDeviceAddress by remember { mutableStateOf(getHandyDeviceAddress(context)) }
    var handyBondedOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var handyKey by remember { mutableStateOf(getHandyKey(context)) }
    var joyPlayDeviceAddress by remember { mutableStateOf(getJoyPlayDeviceAddress(context)) }
    var joyPlayDeviceExpanded by remember { mutableStateOf(false) }
    var joyPlayDeviceOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var joyPlayBondedOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var sendFormatPrefix by remember { mutableStateOf(getSendFormatPrefix(context)) }
    var sendFormatSuffix by remember { mutableStateOf(getSendFormatSuffix(context)) }
    var connectionDetailsExpanded by remember { mutableStateOf(true) }
    var outputRangeExpanded by remember { mutableStateOf(true) }
    var deviceControlExpanded by remember { mutableStateOf(true) }
    var manualAxisPositions by remember { mutableStateOf(AXIS_NAMES.associateWith { 50 }) }
    var sliderSendJob by remember { mutableStateOf<Job?>(null) }
    var selectedScriptUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedScriptName by remember { mutableStateOf<String?>(null) }
    var standaloneScriptData by remember { mutableStateOf<FunscriptData?>(null) }
    var scriptPlayPositionMs by remember { mutableStateOf(0L) }
    var scriptPlaying by remember { mutableStateOf(false) }
    var scriptPlayJob by remember { mutableStateOf<Job?>(null) }

    val scriptPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            selectedScriptUris = uris.map { it.toString() }
            selectedScriptName = if (uris.size == 1) {
                DocumentFile.fromSingleUri(context, uris[0])?.name ?: uris[0].lastPathSegment?.substringAfterLast('/') ?: context.getString(R.string.unknown)
            } else {
                context.getString(R.string.device_control_multi_script_format, uris.size)
            }
            standaloneScriptData = null
            scriptPlayPositionMs = 0L
        }
    }
    LaunchedEffect(selectedScriptUris) {
        if (selectedScriptUris.isEmpty()) {
            standaloneScriptData = null
            scriptPlayPositionMs = 0L
            return@LaunchedEffect
        }
        val data = if (selectedScriptUris.size == 1) {
            withContext(Dispatchers.IO) { loadFunscriptFromUri(context, selectedScriptUris[0]) }
        } else {
            val urisByAxis = mutableMapOf<String, String>()
            for (uriStr in selectedScriptUris) {
                val name = DocumentFile.fromSingleUri(context, android.net.Uri.parse(uriStr))?.name ?: uriStr.substringAfterLast('/')
                if (!name.endsWith(".funscript", ignoreCase = true)) continue
                val rest = name.removeSuffix(".funscript").removeSuffix(".FUNSCRIPT")
                val axisId = if ('.' in rest) rest.substringAfterLast('.') else "L0"
                urisByAxis[axisId] = uriStr
            }
            if (urisByAxis.isEmpty()) null else withContext(Dispatchers.IO) { loadFunscriptMultiFromUris(context, urisByAxis) }
        }
        standaloneScriptData = data
        scriptPlayPositionMs = 0L
    }

    LaunchedEffect(connectionEnabled, connectionType, ipAddress, port, sendFormatPrefix, sendFormatSuffix, serialDeviceId, baudRate, btSerialDeviceAddress, handyAxis, handyKey, handyDeviceAddress, joyPlayDeviceAddress) {
        saveConnectionSettings(
            context,
            connectionEnabled,
            connectionType,
            ipAddress,
            port,
            sendFormatPrefix,
            sendFormatSuffix,
            serialDeviceId,
            baudRate,
            btSerialDeviceAddress,
            handyDeviceAddress,
            handyKey,
            handyAxis,
            joyPlayDeviceAddress
        )
    }

    // Handy 模式：预加载已配对设备（与蓝牙串口一致），便于在 BLE 扫不到时仍能选到已配对的 Handy
    LaunchedEffect(connectionType) {
        if (connectionType == ConnectionType.TheHandy) {
            if (Build.VERSION.SDK_INT >= 31 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                handyBondedOptions = emptyList()
                return@LaunchedEffect
            }
            val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bonded = manager?.adapter?.bondedDevices?.toList().orEmpty()
            handyBondedOptions = bonded
                .mapNotNull { dev ->
                    val addr = dev.address ?: return@mapNotNull null
                    val name = (dev.name?.trim()?.takeIf { it.isNotEmpty() }
                        ?: context.getString(R.string.bluetooth_device))
                    name to addr
                }
                .distinctBy { it.second }
                .sortedBy { it.first }
            if (handyDeviceOptions.isEmpty()) handyDeviceOptions = handyBondedOptions
        }
        if (connectionType == ConnectionType.JoyPlay) {
            if (Build.VERSION.SDK_INT >= 31 &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                joyPlayBondedOptions = emptyList()
                return@LaunchedEffect
            }
            val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val bonded = manager?.adapter?.bondedDevices?.toList().orEmpty()
            joyPlayBondedOptions = bonded
                .mapNotNull { dev ->
                    val addr = dev.address ?: return@mapNotNull null
                    val name = (dev.name?.trim()?.takeIf { it.isNotEmpty() }
                        ?: context.getString(R.string.bluetooth_device))
                    name to addr
                }
                .distinctBy { it.second }
                .sortedBy { it.first }
            if (joyPlayDeviceOptions.isEmpty()) joyPlayDeviceOptions = joyPlayBondedOptions
        }
    }

    LaunchedEffect(handyScanTick) {
        if (handyScanTick <= 0) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return@LaunchedEffect
        }
        val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val scanner = manager?.adapter?.bluetoothLeScanner ?: return@LaunchedEffect

        handyScanning = true
        // 保留已配对设备，再叠加 BLE 扫描结果（避免只显示“未扫描到设备”）
        handyDeviceOptions = handyBondedOptions

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device ?: return
                val address = dev.address ?: return
                val name = (dev.name ?: result.scanRecord?.deviceName ?: "").trim()
                if (name.isEmpty()) return  // 空名称设备过滤，不加入列表
                activity?.runOnUiThread {
                    handyDeviceOptions = (handyDeviceOptions + (name to address))
                        .distinctBy { it.second }
                        .sortedBy { it.first }
                }
            }
        }

        try {
            scanner.startScan(null, settings, callback)
            delay(10_000L)
        } catch (_: Exception) {
            // ignore
        } finally {
            try { scanner.stopScan(callback) } catch (_: Exception) { }
            handyScanning = false
        }
    }

    // 与 eciot_bletool 完全一致：BluetoothAdapter.getDefaultAdapter() + startLeScan(LeScanCallback)
    LaunchedEffect(joyPlayScanTick) {
        if (joyPlayScanTick <= 0) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return@LaunchedEffect
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            DevLog.log("JoyPlay", "需要定位权限才能扫描 BLE")
            return@LaunchedEffect
        }
        @Suppress("DEPRECATION")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            DevLog.log("JoyPlay", "设备不支持蓝牙")
            return@LaunchedEffect
        }
        if (!bluetoothAdapter.isEnabled) {
            DevLog.log("JoyPlay", "请先打开蓝牙")
            return@LaunchedEffect
        }
        // 与 eciot 一致：检查定位开关，未开时很多机型 BLE 扫描无结果
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as? LocationManager
        val locationOn = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        if (!locationOn) {
            DevLog.log("JoyPlay", "请打开系统定位（GPS 或网络位置）后再扫描")
            activity?.runOnUiThread {
                Toast.makeText(context, context.getString(R.string.ble_need_location_hint), Toast.LENGTH_LONG).show()
            }
        }

        joyPlayScanning = true
        joyPlayDeviceOptions = joyPlayBondedOptions

        // 与 eciot_bletool 相同的 LeScanCallback，过滤空名称设备（仅显示有名称的 BLE 设备）
        val leScanCallback = @SuppressLint("MissingPermission")
        object : LeScanCallback {
            override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
                try {
                    val name = device.name?.trim() ?: ""
                    val mac = device.address ?: return
                    if (mac.isEmpty()) return
                    if (name.isEmpty()) return  // 空名称设备过滤，不加入列表
                    DevLog.log("JoyPlay", "BLE 发现: $name rssi=$rssi")
                    activity?.runOnUiThread {
                        joyPlayDeviceOptions = (joyPlayDeviceOptions + (name to mac))
                            .distinctBy { it.second }
                            .sortedBy { it.first }
                    }
                } catch (e: Throwable) {
                    DevLog.log("JoyPlay", "LeScanCallback: ${e.message}")
                }
            }
        }

        @Suppress("DEPRECATION")
        val started = bluetoothAdapter.startLeScan(leScanCallback)
        DevLog.log("JoyPlay", if (started) "开始 BLE 扫描 (eciot startLeScan)..." else "startLeScan 返回 false")

        try {
            delay(12_000L)
        } finally {
            @Suppress("DEPRECATION")
            bluetoothAdapter.stopLeScan(leScanCallback)
            joyPlayScanning = false
            DevLog.log("JoyPlay", "扫描结束，共 ${joyPlayDeviceOptions.size} 个设备")
        }
    }

    var axisRanges by remember {
        mutableStateOf(
            AXIS_NAMES.associateWith { axisName ->
                getAxisRanges(context)[axisName] ?: (0f to 100f)
            }
        )
    }
    LaunchedEffect(axisRanges) {
        saveAxisRanges(context, axisRanges)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.device_settings_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        // ---------- 连接设置 ----------
        SettingsCard(title = stringResource(R.string.connection_settings)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(stringResource(R.string.connection_switch), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Switch(
                        checked = connectionEnabled,
                        onCheckedChange = { connectionEnabled = it }
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (connectionTestInProgress) return@OutlinedButton
                        connectionTestInProgress = true
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { sendConnectionTest(context) }
                            connectionTestInProgress = false
Toast.makeText(
                            context,
                            if (ok) context.getString(R.string.connection_test_sent) else context.getString(R.string.connection_test_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                        }
                    },
                    enabled = !connectionTestInProgress
                ) {
                    Text(if (connectionTestInProgress) stringResource(R.string.connection_test_sending) else stringResource(R.string.connection_test))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { connectionDetailsExpanded = !connectionDetailsExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.connection_type_label), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(
                    imageVector = if (connectionDetailsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (connectionDetailsExpanded) "收起" else "展开"
                )
            }
            AnimatedVisibility(
                visible = connectionDetailsExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(8.dp))
            ExposedDropdownMenuBox(
                expanded = connectionExpanded,
                onExpandedChange = { connectionExpanded = it }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = stringResource(connectionType.nameResId),
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text(stringResource(R.string.connection_type_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = connectionExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = connectionExpanded,
                    onDismissRequest = { connectionExpanded = false }
                ) {
                    ConnectionType.entries.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(stringResource(type.nameResId)) },
                            onClick = {
                                connectionType = type
                                connectionExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when (connectionType) {
                ConnectionType.Serial -> {
                    LaunchedEffect(connectionType, serialPortExpanded) {
                        if (connectionType == ConnectionType.Serial) {
                            serialPortOptions = getAvailableSerialPorts(context)
                        }
                    }
                    ExposedDropdownMenuBox(
                        expanded = serialPortExpanded,
                        onExpandedChange = { serialPortExpanded = it }
                    ) {
                        val selectedDisplay = serialPortOptions.find { it.second == serialDeviceId }?.first
                            ?: if (serialDeviceId.isNotEmpty()) serialDeviceId else stringResource(R.string.select_serial_device)
                        OutlinedTextField(
                            readOnly = true,
                            value = if (serialPortOptions.isEmpty() && serialDeviceId.isEmpty()) stringResource(R.string.serial_no_device) else selectedDisplay,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(R.string.serial_device)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serialPortExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = serialPortExpanded,
                            onDismissRequest = { serialPortExpanded = false }
                        ) {
                            if (serialPortOptions.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.serial_no_device)) },
                                    onClick = { serialPortExpanded = false }
                                )
                            } else {
                                serialPortOptions.forEach { (display, deviceId) ->
                                    DropdownMenuItem(
                                        text = { Text(display) },
                                        onClick = {
                                            serialDeviceId = deviceId
                                            serialPortExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    if (isDeveloperMode) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = baudRate,
                            onValueChange = { baudRate = it },
                            label = { Text(stringResource(R.string.baud_rate)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ConnectionType.BluetoothSerial -> {
                    LaunchedEffect(connectionType, btSerialDeviceExpanded) {
                        if (connectionType != ConnectionType.BluetoothSerial) return@LaunchedEffect
                        if (Build.VERSION.SDK_INT >= 31 &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            btSerialDeviceOptions = emptyList()
                            return@LaunchedEffect
                        }
                        val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
                        val bonded = manager?.adapter?.bondedDevices?.toList().orEmpty()
                        btSerialDeviceOptions = bonded
                            .mapNotNull { dev ->
                                val addr = dev.address ?: return@mapNotNull null
                                val name = (dev.name?.trim()?.takeIf { it.isNotEmpty() }
                                    ?: context.getString(R.string.bluetooth_device))
                                name to addr
                            }
                            .distinctBy { it.second }
                            .sortedBy { it.first }
                    }
                    ExposedDropdownMenuBox(
                        expanded = btSerialDeviceExpanded,
                        onExpandedChange = { btSerialDeviceExpanded = it }
                    ) {
                        val selectedDisplay = btSerialDeviceOptions.find { it.second == btSerialDeviceAddress }?.first
                            ?: if (btSerialDeviceAddress.isNotEmpty()) btSerialDeviceAddress else stringResource(R.string.select_bt_device)
                        OutlinedTextField(
                            readOnly = true,
                            value = if (btSerialDeviceOptions.isEmpty() && btSerialDeviceAddress.isEmpty()) stringResource(R.string.bt_serial_no_device) else selectedDisplay,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(R.string.bt_serial_device)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = btSerialDeviceExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = btSerialDeviceExpanded,
                            onDismissRequest = { btSerialDeviceExpanded = false }
                        ) {
                            if (btSerialDeviceOptions.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.bt_serial_no_device)) },
                                    onClick = { btSerialDeviceExpanded = false }
                                )
                            } else {
                                btSerialDeviceOptions.forEach { (display, addr) ->
                                    DropdownMenuItem(
                                        text = { Text(display) },
                                        onClick = {
                                            btSerialDeviceAddress = addr
                                            btSerialDeviceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = btSerialDeviceAddress,
                        onValueChange = { btSerialDeviceAddress = it },
                        label = { Text(stringResource(R.string.device_address_mac)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.mac_placeholder)) }
                    )
                    if (isDeveloperMode) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = baudRate,
                            onValueChange = { baudRate = it },
                            label = { Text(stringResource(R.string.baud_rate_bt_hint)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ConnectionType.TCP, ConnectionType.UDP -> {
                    OutlinedTextField(
                        value = ipAddress,
                        onValueChange = { ipAddress = it },
                        label = { Text(stringResource(R.string.ip_address)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(stringResource(R.string.port_number)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (isDeveloperMode) {
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.send_format_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = sendFormatPrefix,
                            onValueChange = { sendFormatPrefix = it },
                            label = { Text(stringResource(R.string.prefix)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = sendFormatSuffix,
                            onValueChange = { sendFormatSuffix = it },
                            label = { Text(stringResource(R.string.suffix)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                ConnectionType.TheHandy -> {
                    Text(stringResource(R.string.handy_bluetooth_device), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { handyStartScan() },
                            enabled = !handyScanning
                        ) {
                            Text(if (handyScanning) stringResource(R.string.scanning) else stringResource(R.string.scan))
                        }
                        Text(
                            text = if (handyScanning) stringResource(R.string.scanning_devices) else "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ExposedDropdownMenuBox(
                        expanded = handyDeviceExpanded,
                        onExpandedChange = { handyDeviceExpanded = it }
                    ) {
                        val selectedDisplay = handyDeviceOptions.find { it.second == handyDeviceAddress }?.first
                            ?: if (handyDeviceAddress.isNotEmpty()) handyDeviceAddress else stringResource(R.string.select_handy_device)
                        OutlinedTextField(
                            readOnly = true,
                            value = if (handyDeviceOptions.isEmpty() && handyDeviceAddress.isEmpty()) stringResource(R.string.handy_no_device) else selectedDisplay,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(R.string.serial_device)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = handyDeviceExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = handyDeviceExpanded,
                            onDismissRequest = { handyDeviceExpanded = false }
                        ) {
                            if (handyDeviceOptions.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.handy_no_device)) },
                                    onClick = { handyDeviceExpanded = false }
                                )
                            } else {
                                handyDeviceOptions.forEach { (display, address) ->
                                    DropdownMenuItem(
                                        text = { Text(display) },
                                        onClick = {
                                            handyDeviceAddress = address
                                            handyDeviceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = handyDeviceAddress,
                        onValueChange = { handyDeviceAddress = it },
                        label = { Text(stringResource(R.string.device_address_mac)) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.mac_placeholder)) }
                    )
                    Spacer(Modifier.height(12.dp))
                    ExposedDropdownMenuBox(
                        expanded = handyAxisExpanded,
                        onExpandedChange = { handyAxisExpanded = it }
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = handyAxis,
                            onValueChange = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            label = { Text(stringResource(R.string.axis)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = handyAxisExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = handyAxisExpanded,
                            onDismissRequest = { handyAxisExpanded = false }
                        ) {
                            AXIS_NAMES.forEach { axis ->
                                DropdownMenuItem(
                                    text = { Text(axis) },
                                    onClick = {
                                        handyAxis = axis
                                        handyAxisExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = handyKey,
                        onValueChange = { handyKey = it },
                        label = { Text(stringResource(R.string.handy_key)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ConnectionType.JoyPlay -> {
                Text(stringResource(R.string.handy_bluetooth_device), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { joyPlayStartScan() },
                        enabled = !joyPlayScanning
                    ) {
                        Text(if (joyPlayScanning) stringResource(R.string.scanning) else stringResource(R.string.scan))
                    }
                    Text(
                        text = if (joyPlayScanning) stringResource(R.string.scanning_devices) else "",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = joyPlayDeviceExpanded,
                    onExpandedChange = { joyPlayDeviceExpanded = it }
                ) {
                    val selectedDisplay = joyPlayDeviceOptions.find { it.second == joyPlayDeviceAddress }?.first
                        ?: if (joyPlayDeviceAddress.isNotEmpty()) joyPlayDeviceAddress else stringResource(R.string.select_handy_device)
                    OutlinedTextField(
                        readOnly = true,
                        value = if (joyPlayDeviceOptions.isEmpty() && joyPlayDeviceAddress.isEmpty()) stringResource(R.string.handy_no_device) else selectedDisplay,
                        onValueChange = {},
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text(stringResource(R.string.serial_device)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = joyPlayDeviceExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = joyPlayDeviceExpanded,
                        onDismissRequest = { joyPlayDeviceExpanded = false }
                    ) {
                        if (joyPlayDeviceOptions.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.handy_no_device)) },
                                onClick = { joyPlayDeviceExpanded = false }
                            )
                        } else {
                            joyPlayDeviceOptions.forEach { (display, address) ->
                                DropdownMenuItem(
                                    text = { Text(display) },
                                    onClick = {
                                        joyPlayDeviceAddress = address
                                        joyPlayDeviceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = joyPlayDeviceAddress,
                    onValueChange = { joyPlayDeviceAddress = it },
                    label = { Text(stringResource(R.string.device_address_mac)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.mac_placeholder)) }
                )
                if (isDeveloperMode) {
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.send_format_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sendFormatPrefix,
                        onValueChange = { sendFormatPrefix = it },
                        label = { Text(stringResource(R.string.prefix)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sendFormatSuffix,
                        onValueChange = { sendFormatSuffix = it },
                        label = { Text(stringResource(R.string.suffix)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                }
            }
                }
            }
        }

        // ---------- 输出范围设置 ----------
        SettingsCard(title = stringResource(R.string.output_range_settings)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { outputRangeExpanded = !outputRangeExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.output_range_title), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(
                    imageVector = if (outputRangeExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (outputRangeExpanded) "收起" else "展开"
                )
            }
            AnimatedVisibility(
                visible = outputRangeExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(8.dp))
            AXIS_NAMES.forEach { axisName ->
                val range = axisRanges[axisName] ?: (0f to 100f)
                var minV by remember(axisName) { mutableStateOf(range.first) }
                var maxV by remember(axisName) { mutableStateOf(range.second) }
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$axisName", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            "${minV.toInt()}% – ${maxV.toInt()}%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RangeSlider(
                        value = minV..maxV,
                        onValueChange = { r ->
                            minV = r.start
                            maxV = r.endInclusive
                            axisRanges = axisRanges + (axisName to (minV to maxV))
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
                }
            }
        }

        // ---------- 设备控制 ----------
        SettingsCard(title = stringResource(R.string.device_control_title)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { deviceControlExpanded = !deviceControlExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.device_control_axis_sliders), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Icon(
                    imageVector = if (deviceControlExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (deviceControlExpanded) "收起" else "展开"
                )
            }
            AnimatedVisibility(
                visible = deviceControlExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.height(8.dp))
            AXIS_NAMES.forEach { axisName ->
                val pos = manualAxisPositions[axisName] ?: 50
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$axisName", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text("${pos}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(
                        value = pos.toFloat(),
                        onValueChange = {
                            manualAxisPositions = manualAxisPositions + (axisName to it.toInt())
                            sliderSendJob?.cancel()
                            sliderSendJob = scope.launch {
                                while (isActive) {
                                    val cmd = buildAxisCommandFromPositions(context, manualAxisPositions, 500L)
                                    if (cmd.isNotEmpty()) sendAxisCommand(context, cmd)
                                    delay(100L)
                                }
                            }
                        },
                        onValueChangeFinished = {
                            sliderSendJob?.cancel()
                            sliderSendJob = null
                        },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.device_control_script_section), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { scriptPickerLauncher.launch(arrayOf("*/*")) }) {
                    Text(stringResource(R.string.device_control_select_script))
                }
                Text(
                    text = selectedScriptName ?: stringResource(R.string.device_control_no_script),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (scriptPlaying) {
                            scriptPlayJob?.cancel()
                            scriptPlaying = false
                        } else {
                            val data = standaloneScriptData
                            if (data == null) {
                                Toast.makeText(context, context.getString(R.string.device_control_no_script), Toast.LENGTH_SHORT).show()
                                return@OutlinedButton
                            }
                            val totalMs = if (data.durationSec > 0) (data.durationSec * 1000).toLong()
                                else data.axes.flatMap { it.actions }.maxOfOrNull { it.at } ?: 1L
                            scriptPlaying = true
                            scriptPlayJob = scope.launch {
                                var pos = scriptPlayPositionMs
                                while (pos <= totalMs && isActive) {
                                    val cmd = buildAxisCommandFromScript(context, data, pos)
                                    if (cmd.isNotEmpty()) sendAxisCommand(context, cmd)
                                    delay(100L)
                                    pos += 100L
                                    scriptPlayPositionMs = pos
                                }
                                scriptPlaying = false
                            }
                        }
                    },
                    enabled = standaloneScriptData != null
                ) {
                    Text(if (scriptPlaying) stringResource(R.string.device_control_stop_script) else stringResource(R.string.device_control_play_script))
                }
            }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NasFolderBrowserDialog(
    host: String,
    share: String,
    user: String,
    password: String,
    port: Int,
    initialSubpath: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    var currentPath by remember(initialSubpath) { mutableStateOf(initialSubpath.trim().trimStart('/')) }
    var folders by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val pathSegments = remember(currentPath) { currentPath.split("/").filter { it.isNotEmpty() } }

    LaunchedEffect(host, share, currentPath) {
        if (host.isBlank() || share.isBlank()) {
            error = "请先填写服务器地址和共享名"
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        val result = listSmbFolders(host, share, user, password, port, currentPath)
        loading = false
        result.fold(
            onSuccess = { folders = it; error = null },
            onFailure = { error = it.message ?: "连接失败" }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nas_folder_browser_title)) },
        text = {
            Column(modifier = Modifier.widthIn(max = 320.dp)) {
                Text(
                    text = if (currentPath.isEmpty()) "//$host/$share" else "//$host/$share/$currentPath",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            if (pathSegments.isNotEmpty()) {
                                currentPath = pathSegments.dropLast(1).joinToString("/")
                            }
                        },
                        enabled = pathSegments.isNotEmpty()
                    ) {
                        Text(stringResource(R.string.nas_parent_folder))
                    }
                    OutlinedButton(onClick = { onSelect(currentPath); onDismiss() }) {
                        Text(stringResource(R.string.nas_select_current_folder))
                    }
                }
                Spacer(Modifier.height(12.dp))
                when {
                    loading -> Text(stringResource(R.string.nas_loading), fontSize = 14.sp)
                    error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.heightIn(max = 280.dp)
                    ) {
                        items(folders) { name ->
                            OutlinedButton(
                                onClick = { currentPath = if (currentPath.isEmpty()) name else "$currentPath/$name" },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSettingsScreenImpl(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onVideosScanned: (List<VideoItem>) -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var videoLibrarySource by remember { mutableStateOf(getVideoLibrarySource(context)) }
    var displayPath by remember {
        mutableStateOf(
            if (getVideoLibrarySource(context) == "nas") {
                val h = getNasHost(context)
                val s = getNasShare(context)
                val p = getNasSubpath(context).trim().trimStart('/')
                if (h.isNotEmpty() && s.isNotEmpty()) "//$h/$s" + if (p.isNotEmpty()) "/$p" else ""
                else context.getString(R.string.unselected)
            } else {
                getStoredVideoLibraryDisplayName(context) ?: getStoredVideoLibraryUri(context) ?: context.getString(R.string.unselected)
            }
        )
    }
    var isScanning by remember { mutableStateOf(false) }
    var nasHost by remember { mutableStateOf(getNasHost(context)) }
    var nasShare by remember { mutableStateOf(getNasShare(context)) }
    var nasUser by remember { mutableStateOf(getNasUser(context)) }
    var nasPassword by remember { mutableStateOf(getNasPassword(context)) }
    var nasPortStr by remember { mutableStateOf(getNasPort(context).toString()) }
    var nasSubpath by remember { mutableStateOf(getNasSubpath(context)) }
    var showNasFolderBrowser by remember { mutableStateOf(false) }
    var nasSettingsExpanded by remember { mutableStateOf(true) }
    var nasTestInProgress by remember { mutableStateOf(false) }

    var showDeveloperPasswordDialog by remember { mutableStateOf(false) }
    var developerPasswordInput by remember { mutableStateOf("") }
    var developerPasswordError by remember { mutableStateOf<String?>(null) }
    var isDeveloperMode by remember { mutableStateOf(getIsDeveloperMode(context)) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val doc = DocumentFile.fromTreeUri(context, uri)
            val name = doc?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: context.getString(R.string.unknown)
            saveVideoLibrary(context, uri, name)
            displayPath = name
            // 设置路径后自动扫描一次
            isScanning = true
            scope.launch {
                val list = withContext(Dispatchers.IO) {
                    collectVideosFromTree(context, uri)
                }
                onVideosScanned(list)
                isScanning = false
            }
        }
    }

    // NAS 相关信息改为自动保存，无需点击保存按钮
    LaunchedEffect(videoLibrarySource, nasHost, nasShare, nasUser, nasPassword, nasPortStr, nasSubpath) {
        if (videoLibrarySource == "nas") {
            saveNasSettings(
                context,
                nasHost.trim(),
                nasShare.trim(),
                nasUser.trim(),
                nasPassword,
                nasPortStr.toIntOrNull() ?: 445,
                nasSubpath.trim()
            )
            val p = nasSubpath.trim().trimStart('/')
            displayPath = if (nasHost.isNotEmpty() && nasShare.isNotEmpty()) "//${nasHost.trim()}/${nasShare.trim()}" + if (p.isNotEmpty()) "/$p" else "" else context.getString(R.string.unselected)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.app_settings_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        val videoLibrarySourceLocalLabel = stringResource(R.string.video_library_source_local)
        val videoLibrarySourceNasLabel = stringResource(R.string.video_library_source_nas)
        var videoLibrarySourceExpanded by remember { mutableStateOf(false) }
        SettingsCard(title = stringResource(R.string.video_library_path)) {
            Text(
                text = stringResource(R.string.current_path_format, displayPath.ifEmpty { context.getString(R.string.unselected) }),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(
                expanded = videoLibrarySourceExpanded,
                onExpandedChange = { videoLibrarySourceExpanded = it }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = if (videoLibrarySource == "local") videoLibrarySourceLocalLabel else videoLibrarySourceNasLabel,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text(stringResource(R.string.video_library_source_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = videoLibrarySourceExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = videoLibrarySourceExpanded,
                    onDismissRequest = { videoLibrarySourceExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(videoLibrarySourceLocalLabel) },
                        onClick = {
                            videoLibrarySource = "local"
                            setVideoLibrarySource(context, "local")
                            displayPath = getStoredVideoLibraryDisplayName(context) ?: getStoredVideoLibraryUri(context) ?: context.getString(R.string.unselected)
                            videoLibrarySourceExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(videoLibrarySourceNasLabel) },
                        onClick = {
                            videoLibrarySource = "nas"
                            setVideoLibrarySource(context, "nas")
                            val p = nasSubpath.trim().trimStart('/')
                            displayPath = if (nasHost.isNotEmpty() && nasShare.isNotEmpty()) "//$nasHost/$nasShare" + if (p.isNotEmpty()) "/$p" else "" else context.getString(R.string.unselected)
                            videoLibrarySourceExpanded = false
                            if (nasHost.isNotBlank() && nasShare.isNotBlank()) {
                                isScanning = true
                                scope.launch {
                                    val list = collectVideosFromSmb(
                                        context,
                                        nasHost.trim(),
                                        nasShare.trim(),
                                        nasUser.trim(),
                                        nasPassword,
                                        nasPortStr.toIntOrNull() ?: 445,
                                        nasSubpath.trim()
                                    )
                                    onVideosScanned(list)
                                    isScanning = false
                                    Toast.makeText(context, context.getString(R.string.nas_scan_done_format, list.size), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            if (videoLibrarySource == "local") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { folderPickerLauncher.launch(null) }) {
                        Text(stringResource(R.string.select_folder))
                    }
                    OutlinedButton(
                        onClick = {
                            val storedUri = getStoredVideoLibraryUri(context)
                            if (storedUri == null) return@OutlinedButton
                            isScanning = true
                            scope.launch {
                                val list = withContext(Dispatchers.IO) {
                                    collectVideosFromTree(context, android.net.Uri.parse(storedUri))
                                }
                                onVideosScanned(list)
                                isScanning = false
                            }
                        },
                        enabled = !isScanning && getStoredVideoLibraryUri(context) != null
                    ) {
                        Text(if (isScanning) stringResource(R.string.scanning) else stringResource(R.string.rescan))
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { nasSettingsExpanded = !nasSettingsExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.nas_connection_settings),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Icon(
                        imageVector = if (nasSettingsExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (nasSettingsExpanded) "收起" else "展开"
                    )
                }
                if (nasSettingsExpanded) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nasHost,
                        onValueChange = { nasHost = it },
                        label = { Text(stringResource(R.string.nas_server_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nasShare,
                        onValueChange = { nasShare = it },
                        label = { Text(stringResource(R.string.nas_share_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nasUser,
                        onValueChange = { nasUser = it },
                        label = { Text(stringResource(R.string.nas_username_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nasPassword,
                        onValueChange = { nasPassword = it },
                        label = { Text(stringResource(R.string.nas_password_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nasPortStr,
                        onValueChange = { nasPortStr = it },
                        label = { Text(stringResource(R.string.nas_port_hint)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = nasSubpath,
                            onValueChange = { nasSubpath = it },
                            label = { Text(stringResource(R.string.nas_subpath_hint)) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("/Videos") }
                        )
                        OutlinedButton(
                            onClick = { showNasFolderBrowser = true },
                            enabled = nasHost.isNotBlank() && nasShare.isNotBlank()
                        ) {
                            Text(stringResource(R.string.nas_browse_folder))
                        }
                    }
                    if (showNasFolderBrowser) {
                        NasFolderBrowserDialog(
                            host = nasHost.trim(),
                            share = nasShare.trim(),
                            user = nasUser.trim(),
                            password = nasPassword,
                            port = nasPortStr.toIntOrNull() ?: 445,
                            initialSubpath = nasSubpath.trim(),
                            onDismiss = { showNasFolderBrowser = false },
                            onSelect = { path ->
                                nasSubpath = path
                                showNasFolderBrowser = false
                                if (nasHost.isNotBlank() && nasShare.isNotBlank()) {
                                    isScanning = true
                                    scope.launch {
                                        val list = collectVideosFromSmb(
                                            context,
                                            nasHost.trim(),
                                            nasShare.trim(),
                                            nasUser.trim(),
                                            nasPassword,
                                            nasPortStr.toIntOrNull() ?: 445,
                                            path.trim()
                                        )
                                        onVideosScanned(list)
                                        isScanning = false
                                        Toast.makeText(context, context.getString(R.string.nas_scan_done_format, list.size), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (nasTestInProgress || nasHost.isBlank() || nasShare.isBlank()) return@OutlinedButton
                                nasTestInProgress = true
                                scope.launch {
                                    val result = listSmbFolders(
                                        nasHost.trim(),
                                        nasShare.trim(),
                                        nasUser.trim(),
                                        nasPassword,
                                        nasPortStr.toIntOrNull() ?: 445,
                                        ""
                                    )
                                    nasTestInProgress = false
                                    val msg = if (result.isSuccess) context.getString(R.string.nas_test_success) else context.getString(R.string.nas_test_failed)
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !nasTestInProgress && nasHost.isNotBlank() && nasShare.isNotBlank()
                        ) {
                            Text(if (nasTestInProgress) stringResource(R.string.scanning) else stringResource(R.string.nas_connection_test))
                        }
                        OutlinedButton(
                            onClick = {
                                if (isScanning || nasHost.isBlank() || nasShare.isBlank()) return@OutlinedButton
                                isScanning = true
                                scope.launch {
                                    val list = collectVideosFromSmb(
                                        context,
                                        nasHost.trim(),
                                        nasShare.trim(),
                                        nasUser.trim(),
                                        nasPassword,
                                        nasPortStr.toIntOrNull() ?: 445,
                                        nasSubpath.trim()
                                    )
                                    onVideosScanned(list)
                                    isScanning = false
                                    Toast.makeText(
                                        context,
                                        context.getString(R.string.nas_scan_done_format, list.size),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            enabled = !isScanning && nasHost.isNotBlank() && nasShare.isNotBlank()
                        ) {
                            Text(if (isScanning) stringResource(R.string.scanning) else stringResource(R.string.nas_scan_folder))
                        }
                    }
                }
            }
        }

        SettingsCard(title = stringResource(R.string.player_settings)) {
            Text(stringResource(R.string.default_play_behavior), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.play_stop_example), color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(12.dp))

            Text(stringResource(R.string.subtitle_settings), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.subtitle_default_on))
                Spacer(Modifier.width(8.dp))
                Switch(checked = true, onCheckedChange = { /* TODO */ })
            }
        }

        SettingsCard(title = stringResource(R.string.language_settings)) {
            val languageOptions = listOf(
                "system" to stringResource(R.string.follow_system),
                "zh" to stringResource(R.string.simplified_chinese),
                "en" to stringResource(R.string.english)
            )
            var appLanguage by remember { mutableStateOf(getAppLanguage(context)) }
            var languageExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = languageExpanded,
                onExpandedChange = { languageExpanded = it }
            ) {
                OutlinedTextField(
                    readOnly = true,
                    value = languageOptions.find { it.first == appLanguage }?.second ?: appLanguage,
                    onValueChange = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    label = { Text(stringResource(R.string.interface_language)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                )
                ExposedDropdownMenu(
                    expanded = languageExpanded,
                    onDismissRequest = { languageExpanded = false }
                ) {
                    languageOptions.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = {
                                if (value != appLanguage) {
                                    appLanguage = value
                                    setAppLanguage(context, value)
                                    languageExpanded = false
                                    (context as? Activity)?.recreate()
                                } else {
                                    languageExpanded = false
                                }
                            }
                        )
                    }
                }
            }
        }

        SettingsCard(title = stringResource(R.string.theme_appearance)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.dark_mode))
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeChange
                )
            }
        }

        SettingsCard(title = stringResource(R.string.developer_mode)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isDeveloperMode) stringResource(R.string.developer_mode_on_hint) else stringResource(R.string.developer_mode_off_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Switch(
                    checked = isDeveloperMode,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            showDeveloperPasswordDialog = true
                        } else {
                            isDeveloperMode = false
                            setDeveloperMode(context, false)
                            onDeveloperModeChange(false)
                        }
                    }
                )
            }
        }
    }

    if (showDeveloperPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeveloperPasswordDialog = false
                developerPasswordInput = ""
                developerPasswordError = null
            },
            title = { Text(stringResource(R.string.developer_dialog_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = developerPasswordInput,
                        onValueChange = {
                            developerPasswordInput = it
                            developerPasswordError = null
                        },
                        label = { Text(stringResource(R.string.password)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    developerPasswordError?.let { err ->
                        Spacer(Modifier.height(8.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (developerPasswordInput == "doroplayer") {
                            isDeveloperMode = true
                            setDeveloperMode(context, true)
                            onDeveloperModeChange(true)
                            showDeveloperPasswordDialog = false
                            developerPasswordInput = ""
                            developerPasswordError = null
                            Toast.makeText(context, context.getString(R.string.developer_entered), Toast.LENGTH_SHORT).show()
                        } else {
                            developerPasswordError = context.getString(R.string.password_error)
                        }
                    }
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeveloperPasswordDialog = false
                        developerPasswordInput = ""
                        developerPasswordError = null
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

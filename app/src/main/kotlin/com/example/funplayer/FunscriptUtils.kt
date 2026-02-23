package com.example.funplayer

import android.graphics.Bitmap
import android.os.Build
import android.util.LruCache
import com.example.funplayer.handyplug.Handyplug
import jcifs.smb.SmbFile
import org.json.JSONObject
import java.util.LinkedHashMap

internal data class FunscriptAction(val at: Long, val pos: Int)
internal data class FunscriptAxis(val id: String, val actions: List<FunscriptAction>)
internal data class FunscriptData(val durationSec: Float, val axes: List<FunscriptAxis>)

/** NAS 方式下封面、时长、脚本等数据的软件内存缓存 */
internal object NasDataCache {
    private const val THUMBNAIL_CACHE_BYTES = 15 * 1024 * 1024
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

internal fun readJsonFromUriString(context: android.content.Context, uriString: String): String? {
    return when {
        uriString.startsWith("smb:", ignoreCase = true) -> try {
            SmbFile(uriString).inputStream.use { it.reader(Charsets.UTF_8).readText() }
        } catch (_: Exception) { null }
        else -> try {
            context.contentResolver.openInputStream(android.net.Uri.parse(uriString))?.reader(Charsets.UTF_8)?.use { it.readText() }
        } catch (_: Exception) { null }
    }
}

internal fun loadFunscriptFromUri(context: android.content.Context, uriString: String?): FunscriptData? {
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
        val data = FunscriptData(durationSec, axesList)
        if (uriString.startsWith("smb:", ignoreCase = true)) NasDataCache.putFunscript(uriString, data)
        data
    } catch (_: Exception) {
        null
    }
}

internal fun loadFunscriptMultiFromUris(context: android.content.Context, urisByAxis: Map<String, String>): FunscriptData? {
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

internal fun buildAxisCommandFromScript(
    context: android.content.Context,
    script: FunscriptData?,
    currentPositionMs: Long
): String {
    if (script == null) return ""
    val savedRanges = getAxisRanges(context)
    val defaultRanges = script.axes.associate { it.id to (0f to 100f) }
    val ranges = defaultRanges + savedRanges
    val sb = StringBuilder()
    for ((index, axis) in script.axes.withIndex()) {
        // 使用二分查找替代线性遍历，大幅提升性能
        val next = axis.actions.binarySearchBy(currentPositionMs, selector = { it.at })
            .let { if (it < 0) -(it + 1) else it }
            .let { if (it < axis.actions.size) axis.actions[it] else null }
            ?: continue

        val (minR, maxR) = ranges[axis.id] ?: (0f to 100f)
        val posClamped = next.pos.coerceIn(0, 100)
        val t = posClamped / 100f
        val mappedFloat = minR + t * (maxR - minR)
        val mapped = kotlin.math.round(mappedFloat).toInt().coerceIn(0, 100)
        val durationMs = (next.at - currentPositionMs).coerceIn(1L, 60_000L)
        if (index > 0) sb.append(' ')
        sb.append(axis.id).append(mapped).append('I').append(durationMs)
    }
    return sb.toString()
}

internal fun getAxisPositionAndDurationForHandy(
    context: android.content.Context,
    script: FunscriptData?,
    axisId: String,
    currentPositionMs: Long
): Pair<Double, Long>? {
    if (script == null) return null
    val axis = script.axes.find { it.id == axisId } ?: return null
    // 使用二分查找替代线性遍历
    val next = axis.actions.binarySearchBy(currentPositionMs, selector = { it.at })
        .let { if (it < 0) -(it + 1) else it }
        .let { if (it < axis.actions.size) axis.actions[it] else null }
        ?: return null

    val ranges = getAxisRanges(context)
    val (minR, maxR) = ranges[axisId] ?: (0f to 100f)
    val posClamped = next.pos.coerceIn(0, 100)
    val t = posClamped / 100f
    val mappedFloat = minR + t * (maxR - minR)
    val position = (mappedFloat / 100.0).coerceIn(0.0, 1.0)
    val durationMs = (next.at - currentPositionMs).coerceIn(1L, 60_000L)
    return position to durationMs
}

internal fun buildAxisCommandFromPositions(
    context: android.content.Context,
    positions: Map<String, Int>,
    durationMs: Long
): String {
    if (positions.isEmpty()) return ""
    val ranges = getAxisRanges(context)
    val sb = StringBuilder()
    val dur = durationMs.coerceIn(1L, 60_000L)
    var index = 0
    for (axisId in AXIS_NAMES) {
        val posRaw = positions[axisId] ?: continue
        val (minR, maxR) = ranges[axisId] ?: (0f to 100f)
        val t = (posRaw.coerceIn(0, 100) / 100f)
        val mapped = kotlin.math.round(minR + t * (maxR - minR)).toInt().coerceIn(0, 100)
        if (index > 0) sb.append(' ')
        sb.append(axisId).append(mapped).append('I').append(dur)
        index++
    }
    return sb.toString()
}

internal fun parseAxisCommandSegments(command: String): List<Triple<String, Int, Long>> {
    if (command.isEmpty()) return emptyList()
    val regex = Regex("(L0|L1|L2|R0|R1|R2)(\\d+)I(\\d+)")
    return regex.findAll(command).map { m ->
        Triple(m.groupValues[1], m.groupValues[2].toIntOrNull() ?: 0, m.groupValues[3].toLongOrNull() ?: 500L)
    }.toList()
}

internal fun buildHandyLinearPayloadFromPosition(
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

internal fun computeAxisHeatmap(actions: List<FunscriptAction>, totalMs: Long, segmentCount: Int): FloatArray {
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

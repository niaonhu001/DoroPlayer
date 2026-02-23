package com.example.funplayer

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION

internal fun getIsDeveloperMode(context: android.content.Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getBoolean(KEY_DEVELOPER_MODE, false)

internal fun setDeveloperMode(context: android.content.Context, enabled: Boolean) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putBoolean(KEY_DEVELOPER_MODE, enabled).apply()
}

internal fun getAppLanguage(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_APP_LANGUAGE, "system") ?: "system"

internal fun setAppLanguage(context: android.content.Context, value: String) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_APP_LANGUAGE, value).apply()
}

internal fun getConnectionEnabled(context: android.content.Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getBoolean(KEY_CONNECTION_ENABLED, false)

internal fun getConnectionType(context: android.content.Context): ConnectionType {
    val name = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_CONNECTION_TYPE, ConnectionType.Serial.name) ?: ConnectionType.Serial.name
    return try {
        ConnectionType.valueOf(name)
    } catch (_: Exception) {
        ConnectionType.Serial
    }
}

internal fun getDeviceIp(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_DEVICE_IP, "192.168.1.1") ?: "192.168.1.1"

internal fun getDevicePort(context: android.content.Context): Int {
    val p = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_DEVICE_PORT, "8080") ?: "8080"
    return p.toIntOrNull()?.coerceIn(1, 65535) ?: 8080
}

internal fun getSerialDeviceId(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_SERIAL_DEVICE, "") ?: ""

internal fun getBaudRate(context: android.content.Context): Int {
    val s = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_BAUD_RATE, "9600") ?: "9600"
    return s.toIntOrNull()?.coerceIn(300, 4_000_000) ?: 9600
}

internal fun getAxisRanges(context: android.content.Context): Map<String, Pair<Float, Float>> {
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

internal fun saveAxisRanges(context: android.content.Context, ranges: Map<String, Pair<Float, Float>>) {
    val raw = ranges.entries.joinToString(";") { (axis, pair) ->
        "${axis}=${pair.first},${pair.second}"
    }
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_AXIS_RANGES, raw)
        .apply()
}

internal fun getHandyKey(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_HANDY_KEY, "") ?: ""

internal fun getHandyAxis(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_HANDY_AXIS, "L0") ?: "L0"

internal fun getHandyDeviceAddress(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_HANDY_DEVICE_ADDRESS, "") ?: ""

internal fun getTcodeBLEDeviceAddress(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_TCODEBLE_DEVICE_ADDRESS, "") ?: ""

internal fun getBtSerialDeviceAddress(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_BT_SERIAL_DEVICE_ADDRESS, "") ?: ""

internal fun getSendFormatPrefix(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_SEND_FORMAT_PREFIX, "") ?: ""

internal fun getSendFormatSuffix(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_SEND_FORMAT_SUFFIX, "") ?: ""

internal fun saveConnectionSettings(
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
    tcodeBLEDeviceAddress: String = ""
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
        .putString(KEY_TCODEBLE_DEVICE_ADDRESS, tcodeBLEDeviceAddress)
        .apply()
}

internal fun getStoredVideoLibraryUri(context: android.content.Context): String? =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_LIBRARY_URI, null)

internal fun getStoredVideoLibraryDisplayName(context: android.content.Context): String? =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_LIBRARY_DISPLAY_NAME, null)

internal fun getVideoLibrarySource(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        .getString(KEY_VIDEO_LIBRARY_SOURCE, "local") ?: "local"

internal fun setVideoLibrarySource(context: android.content.Context, source: String) {
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_VIDEO_LIBRARY_SOURCE, source).apply()
}

internal fun getNasHost(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_HOST, "") ?: ""
internal fun getNasShare(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_SHARE, "") ?: ""
internal fun getNasUser(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_USER, "") ?: ""
internal fun getNasPassword(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_PASSWORD, "") ?: ""
internal fun getNasPort(context: android.content.Context): Int =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_PORT, "445")?.toIntOrNull() ?: 445
internal fun getNasSubpath(context: android.content.Context): String =
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_NAS_SUBPATH, "") ?: ""

internal fun saveVideoLibrary(context: android.content.Context, uri: android.net.Uri, displayName: String?) {
    context.contentResolver.takePersistableUriPermission(uri, FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).edit()
        .putString(KEY_VIDEO_LIBRARY_URI, uri.toString())
        .putString(KEY_VIDEO_LIBRARY_DISPLAY_NAME, displayName ?: uri.lastPathSegment ?: context.getString(R.string.unknown))
        .putString(KEY_VIDEO_LIBRARY_SOURCE, "local")
        .apply()
}

package com.example.funplayer

import android.content.Context
import jcifs.CIFSContext
import jcifs.config.PropertyConfiguration
import jcifs.context.BaseContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile

/**
 * SMB 辅助函数：提供 SMB 连接上下文和 URI 规范化功能
 */

/**
 * 获取 SMB 连接上下文，使用用户配置的认证信息
 */
internal fun getSmbContext(context: Context): CIFSContext {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val user = prefs.getString(KEY_NAS_USER, "") ?: ""
    val password = prefs.getString(KEY_NAS_PASSWORD, "") ?: ""
    val domain = "" // 可根据需要添加域名配置

    val auth = NtlmPasswordAuthenticator(domain, user, password)
    val properties = java.util.Properties()
    val config = PropertyConfiguration(properties)
    return BaseContext(config).withCredentials(auth)
}

/**
 * 规范化 SMB URI，确保格式正确
 * 将 smb://host/share/path 转换为 smb://host/share/path 格式
 */
internal fun normalizeSmbUri(uri: String): String {
    var normalized = uri.trim()

    // 确保以 smb:// 开头
    if (!normalized.startsWith("smb://", ignoreCase = true)) {
        if (normalized.startsWith("smb:", ignoreCase = true)) {
            normalized = "smb://" + normalized.removePrefix("smb:").removePrefix("//")
        } else {
            normalized = "smb://$normalized"
        }
    }

    // 移除重复的斜杠
    normalized = normalized.replace("([^/])/+", "$1/")

    // 确保路径不以斜杠结尾（除非是根路径）
    if (normalized.length > 6 && normalized.endsWith("/")) {
        normalized = normalized.dropLast(1)
    }

    return normalized
}

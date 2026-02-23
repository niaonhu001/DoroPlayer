package com.example.funplayer

import androidx.documentfile.provider.DocumentFile
import jcifs.smb.SmbFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "m4v", "3gp", "flv")

internal suspend fun listSmbFolders(
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

internal fun buildSmbBaseUrl(host: String, share: String, user: String, password: String, port: Int, pathPart: String): String {
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

internal suspend fun collectVideosFromSmb(
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
    // BFS: 使用队列遍历目录
    val queue = ArrayDeque<String>()
    queue.add(baseUrl)
    while (queue.isNotEmpty()) {
        val dirUrl = queue.removeFirst()
        runCatching {
            val smbDir = SmbFile(dirUrl)
            if (!smbDir.exists() || !smbDir.isDirectory()) return@runCatching
            smbDir.listFiles()?.forEach { file ->
                val name = file.name ?: return@forEach
                if (name.equals(".", true) || name.equals("..", true)) return@forEach
                if (file.isDirectory()) {
                    // BFS: 将子目录加入队列末尾
                    val base = dirUrl.trimEnd('/')
                    val childUrl = "$base/$name/"
                    DevLog.log("SMB Walk", "enqueue: $childUrl")
                    queue.add(childUrl)
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
    val savedTags = loadAllTags(context)
    list.map { item ->
        val tags = item.tagFileUri?.let { loadTagsFromSmbTagFile(it) } ?: savedTags[item.uri] ?: emptyList()
        item.copy(tags = tags)
    }
}

internal fun loadTagsFromSmbTagFile(smbTagUri: String): List<String>? {
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

internal fun saveTagsToSmbTagFile(smbTagUri: String, tags: List<String>): Boolean {
    return try {
        val smbFile = SmbFile(smbTagUri)
        // 如果文件不存在，创建新文件
        if (!smbFile.exists()) {
            smbFile.createNewFile()
        }
        val root = JSONObject().put("version", TAG_FILE_VERSION).put("tags", JSONArray(tags))
        smbFile.outputStream.use { stream ->
            stream.writer(Charsets.UTF_8).write(root.toString())
        }
        true
    } catch (_: Exception) {
        false
    }
}

internal fun saveNasSettings(
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

internal fun loadTagsFromTagFile(context: android.content.Context, tagFileUri: String?): List<String>? {
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

internal fun saveTagsToTagFile(context: android.content.Context, tagFileUri: String?, tags: List<String>): Boolean {
    if (tagFileUri == null) return false
    return try {
        val uri = android.net.Uri.parse(tagFileUri)
        val root = JSONObject().put("version", TAG_FILE_VERSION).put("tags", JSONArray(tags))
        context.contentResolver.openOutputStream(uri, "w")?.writer(Charsets.UTF_8)?.use { it.write(root.toString()) } != null
    } catch (_: Exception) {
        false
    }
}

internal fun loadAllTags(context: android.content.Context): Map<String, List<String>> {
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

internal fun saveTagsForVideo(context: android.content.Context, video: VideoItem, tags: List<String>) {
    val uri = video.uri ?: return
    // 判断是否为 SMB URI
    val isSmbUri = uri.startsWith("smb://")

    val tagFileUriToUse = video.tagFileUri ?: run {
        if (isSmbUri) {
            // SMB: 构建 tag 文件 URI
            val parentUri = video.parentFolderUri ?: return@run null
            val baseName = video.name.substringBeforeLast(".")
            "$parentUri$baseName.tag"
        } else {
            // 本地 SAF: 使用 DocumentFile 创建
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
    }
    if (tagFileUriToUse != null) {
        if (isSmbUri) {
            saveTagsToSmbTagFile(tagFileUriToUse, tags)
        } else {
            saveTagsToTagFile(context, tagFileUriToUse, tags)
        }
    }
    val prefs = context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
    val current = loadAllTags(context).toMutableMap()
    current[uri] = tags
    val obj = JSONObject()
    current.forEach { (k, v) -> obj.put(k, JSONArray(v)) }
    prefs.edit().putString(KEY_VIDEO_TAGS, obj.toString()).apply()
}

internal fun collectVideosFromTree(context: android.content.Context, treeUri: android.net.Uri): List<VideoItem> {
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
                    val multiMap = mutableMapOf<String, String>()
                    for ((axisId, suffix) in MULTI_FUNSCRIPT_AXIS_SUFFIXES) {
                        val fileName = if (suffix == null) "$baseName.funscript" else "$baseName.$suffix.funscript"
                        val found = siblingFiles.find { it.name == fileName }
                        if (found != null) multiMap[axisId] = found.uri.toString()
                    }
                    val singleFile = siblingFiles.find { it.name == "$baseName.funscript" }?.uri?.toString()
                    val (scriptUri, urisByAxis) = if (multiMap.size > 1) {
                        Pair(multiMap["L0"] ?: multiMap.values.first(), multiMap)
                    } else {
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

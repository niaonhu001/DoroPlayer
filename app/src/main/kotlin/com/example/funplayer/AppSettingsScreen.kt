package com.example.funplayer

import androidx.compose.material3.ExperimentalMaterial3Api
import kotlin.OptIn
import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppSettingsScreen(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onVideosScanned: (List<VideoItem>) -> Unit,
    onDeveloperModeChange: (Boolean) -> Unit
) {
    AppSettingsScreenImpl(
        isDarkTheme = isDarkTheme,
        onThemeChange = onThemeChange,
        onVideosScanned = onVideosScanned,
        onDeveloperModeChange = onDeveloperModeChange
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
    val scope = androidx.compose.runtime.rememberCoroutineScope()
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
    var nasDiscoveryInProgress by remember { mutableStateOf(false) }
    var showNasDiscoveryDialog by remember { mutableStateOf(false) }
    var discoveredSmbDevices by remember { mutableStateOf<List<SMBDiscoverer.SmbDevice>>(emptyList()) }

    var showDeveloperPasswordDialog by remember { mutableStateOf(false) }
    var developerPasswordInput by remember { mutableStateOf("") }
    var developerPasswordError by remember { mutableStateOf<String?>(null) }
    var isDeveloperMode by remember { mutableStateOf(getIsDeveloperMode(context)) }

    val folderPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val doc = DocumentFile.fromTreeUri(context, uri)
            val name = doc?.name ?: uri.lastPathSegment?.substringAfterLast('/') ?: context.getString(R.string.unknown)
            saveVideoLibrary(context, uri, name)
            displayPath = name
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
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
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
                            onSelect = { path: String ->
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
                                showNasDiscoveryDialog = true
                                nasDiscoveryInProgress = true
                                scope.launch {
                                    val discoverer = SMBDiscoverer(context)
                                    val result = discoverer.discoverSmbDevices(
                                        timeoutMs = 8000,
                                        scanLocalNetwork = true  // 先使用 SSDP，更快
                                    )
                                    result.fold(
                                        onSuccess = { devices ->
                                            discoveredSmbDevices = devices
                                        },
                                        onFailure = { error ->
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.nas_discovery_failed) + ": ${error.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                            discoveredSmbDevices = emptyList()
                                        }
                                    )
                                    nasDiscoveryInProgress = false
                                }
                            },
                            enabled = !nasDiscoveryInProgress
                        ) {
                            Text(if (nasDiscoveryInProgress) stringResource(R.string.nas_discovering) else stringResource(R.string.nas_discovery))
                        }
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
                            enabled = !nasTestInProgress && !nasDiscoveryInProgress && nasHost.isNotBlank() && nasShare.isNotBlank()
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
                            enabled = !isScanning && !nasDiscoveryInProgress && nasHost.isNotBlank() && nasShare.isNotBlank()
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
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
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

    if (showNasDiscoveryDialog) {
        NasDiscoveryDialog(
            devices = discoveredSmbDevices,
            isLoading = nasDiscoveryInProgress,
            onDismiss = { showNasDiscoveryDialog = false },
            onDeviceSelected = { device: SMBDiscoverer.SmbDevice ->
                nasHost = device.address
                showNasDiscoveryDialog = false
                // 可选：如果用户提供了凭据，可以尝试自动获取共享列表
                if (nasUser.isNotBlank() && nasPassword.isNotBlank()) {
                    scope.launch {
                        val discoverer = SMBDiscoverer(context)
                        val sharesResult = discoverer.verifySmbDevice(
                            device = device,
                            user = nasUser.trim(),
                            password = nasPassword,
                            port = nasPortStr.toIntOrNull() ?: 445
                        )
                        sharesResult.fold(
                            onSuccess = { shares ->
                                if (shares.size == 1) {
                                    nasShare = shares.first()
                                }
                            },
                            onFailure = {
                                // 静默失败，用户仍需手动输入共享名
                            }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun NasDiscoveryDialog(
    devices: List<SMBDiscoverer.SmbDevice>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onDeviceSelected: (SMBDiscoverer.SmbDevice) -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.nas_select_device)) },
        text = {
            Column(modifier = Modifier.widthIn(max = 400.dp)) {
                when {
                    isLoading -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.nas_discovering))
                        }
                    }
                    devices.isEmpty() -> {
                        Text(
                            stringResource(R.string.nas_no_devices_found),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        val deviceCount = devices.count()
                        Text(
                            stringResource(R.string.nas_devices_found_format, deviceCount),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(devices) { device ->
                                OutlinedButton(
                                    onClick = { onDeviceSelected(device) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            device.displayName,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (device.name != null && device.name != device.address) {
                                            Text(
                                                device.address,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
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

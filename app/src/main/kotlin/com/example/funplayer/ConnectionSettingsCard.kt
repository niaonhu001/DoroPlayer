@file:Suppress("DEPRECATION")

package com.example.funplayer

import android.Manifest
import android.bluetooth.BluetoothManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Help
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlin.OptIn

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ConnectionSettingsCard(
    // 连接启用状态
    connectionEnabled: Boolean,
    // 连接启用状态变更回调
    onConnectionEnabledChange: (Boolean) -> Unit,
    // 连接类型
    connectionType: ConnectionType,
    // 连接类型变更回调
    onConnectionTypeChange: (ConnectionType) -> Unit,
    // IP地址
    ipAddress: String,
    // IP地址变更回调
    onIpAddressChange: (String) -> Unit,
    // 端口号
    port: String,
    // 端口号变更回调
    onPortChange: (String) -> Unit,
    // 串口设备ID
    serialDeviceId: String,
    // 串口设备ID变更回调
    onSerialDeviceIdChange: (String) -> Unit,
    // 波特率
    baudRate: String,
    // 波特率变更回调
    onBaudRateChange: (String) -> Unit,
    // 蓝牙串口设备地址
    btSerialDeviceAddress: String,
    // 蓝牙串口设备地址变更回调
    onBtSerialDeviceAddressChange: (String) -> Unit,
    // Handy设备地址
    handyDeviceAddress: String,
    // Handy设备地址变更回调
    onHandyDeviceAddressChange: (String) -> Unit,
    // Handy轴名称
    handyAxis: String,
    // Handy轴名称变更回调
    onHandyAxisChange: (String) -> Unit,
    // Handy密钥
    handyKey: String,
    // Handy密钥变更回调
    onHandyKeyChange: (String) -> Unit,
    // TcodeBLE设备地址
    tcodeBLEDeviceAddress: String,
    // TcodeBLE设备地址变更回调
    onTcodeBLEDeviceAddressChange: (String) -> Unit,
    // 发送格式前缀
    sendFormatPrefix: String,
    // 发送格式前缀变更回调
    onSendFormatPrefixChange: (String) -> Unit,
    // 发送格式后缀
    sendFormatSuffix: String,
    // 发送格式后缀变更回调
    onSendFormatSuffixChange: (String) -> Unit,
    // 连接测试请求回调
    onConnectionTest: () -> Unit,
    // 连接测试进行中状态
    connectionTestInProgress: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDeveloperMode = getIsDeveloperMode(context)

    var showHelpDialog by remember { mutableStateOf(false) }

    var connectionExpanded by remember { mutableStateOf(false) }
    var connectionDetailsExpanded by remember { mutableStateOf(true) }
    var serialPortExpanded by remember { mutableStateOf(false) }
    var serialPortOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var btSerialDeviceExpanded by remember { mutableStateOf(false) }
    var btSerialDeviceOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var handyDeviceExpanded by remember { mutableStateOf(false) }
    var handyDeviceOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var handyAxisExpanded by remember { mutableStateOf(false) }
    var tcodeBLEDeviceExpanded by remember { mutableStateOf(false) }
    var tcodeBLEDeviceOptions by remember {
        mutableStateOf<List<Triple<String, String, Int>>>(
            emptyList()
        )
    }

    val bluetoothScanner = remember { BluetoothScanner(context) }
    val permissionManager = remember { BluetoothPermissionManager(context) }

    var handyScanning by remember { mutableStateOf(false) }
    var tcodeBLEScanning by remember { mutableStateOf(false) }
    var pendingBleScan by remember { mutableStateOf<ConnectionType?>(null) }
    var handyScanTick by remember { mutableStateOf(0) }
    var tcodeBLEScanTick by remember { mutableStateOf(0) }

    val handyPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.all { it }
        if (granted) {
            when (pendingBleScan) {
                ConnectionType.TheHandy -> handyScanTick++
                ConnectionType.TcodeBLE -> tcodeBLEScanTick++
                else -> {}
            }
            pendingBleScan = null
        } else {
            Toast.makeText(
                context,
                context.getString(R.string.bt_scan_permission_denied),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun handyStartScan() {
        if (handyScanning) return
        if (!permissionManager.hasAllPermissions()) {
            pendingBleScan = ConnectionType.TheHandy
            handyPermLauncher.launch(permissionManager.getRequiredPermissions())
            return
        }
        handyScanTick++
    }

    fun tcodeBLEStartScan() {
        if (tcodeBLEScanning) return
        if (!permissionManager.hasAllPermissions()) {
            pendingBleScan = ConnectionType.TcodeBLE
            handyPermLauncher.launch(permissionManager.getRequiredPermissions())
            return
        }
        tcodeBLEScanTick++
    }

    LaunchedEffect(connectionType) {
        if (connectionType == ConnectionType.TheHandy) {
            val bonded = bluetoothScanner.getBondedDevices()
            handyDeviceOptions = bonded
            if (handyDeviceOptions.isEmpty()) handyDeviceOptions = bonded
        }
    }

    LaunchedEffect(handyScanTick) {
        if (handyScanTick <= 0) return@LaunchedEffect
        bluetoothScanner.scanHandyDevices(
            onDeviceFound = { name, address ->
                handyDeviceOptions = (handyDeviceOptions + (name to address))
                    .distinctBy { it.second }
                    .sortedBy { it.first }
            },
            onScanStart = { handyScanning = true },
            onScanEnd = { handyScanning = false }
        )
    }

    LaunchedEffect(tcodeBLEScanTick) {
        if (tcodeBLEScanTick <= 0) return@LaunchedEffect
        bluetoothScanner.scanTcodeBLEDevices(
            onDeviceFound = { name, address, rssi ->
                tcodeBLEDeviceOptions = (tcodeBLEDeviceOptions + Triple(name, address, rssi))
                    .distinctBy { it.second }
                    .sortedByDescending { it.third } //根据rssi信号强度排序
            },
            onScanStart = { tcodeBLEScanning = true },
            onScanEnd = { tcodeBLEScanning = false }
        )
    }

    LaunchedEffect(connectionType, serialPortExpanded) {
        if (connectionType == ConnectionType.Serial) {
            serialPortOptions = getAvailableSerialPorts(context)
        }
    }

    LaunchedEffect(connectionType, btSerialDeviceExpanded) {
        if (connectionType != ConnectionType.BluetoothSerial) return@LaunchedEffect
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            btSerialDeviceOptions = emptyList()
            return@LaunchedEffect
        }
        val manager =
            context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bonded = manager?.adapter?.bondedDevices?.toList().orEmpty()
        btSerialDeviceOptions = bonded
            .mapNotNull { dev ->
                val addr = dev.address ?: return@mapNotNull null
                val name = dev.name?.trim()?.takeIf { it.isNotEmpty() }
                    ?: context.getString(R.string.bluetooth_device)
                name to addr
            }
            .distinctBy { it.second }
            .sortedBy { it.first }
    }

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
                Text(
                    stringResource(R.string.connection_switch),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Switch(
                    checked = connectionEnabled,
                    onCheckedChange = onConnectionEnabledChange
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Help,
                        contentDescription = stringResource(R.string.connection_test_tooltip),
                        tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(27.dp)
                    )
                }
                OutlinedButton(
                    onClick = onConnectionTest,
                    enabled = !connectionTestInProgress
                ) {
                    Text(
                        if (connectionTestInProgress) stringResource(R.string.connection_test_sending) else stringResource(
                            R.string.connection_test
                        )
                    )
                }
            }
        }

        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                title = { Text(stringResource(R.string.connection_test)) },
                text = { Text(stringResource(R.string.connection_test_tooltip)) },
                confirmButton = {
                    TextButton(
                        onClick = { showHelpDialog = false }
                    ) {
                        Text(stringResource(R.string.ok))
                    }
                }
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .let { modifier ->
                    var m = modifier
                    // Add clickable modifier if needed
                    m
                },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.connection_type_label),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            // Add expand/collapse icon here if needed
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
                        ConnectionType.entries
                            //暂时不显示the handy
                            .filter { it != ConnectionType.TheHandy }
                            .forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(type.nameResId)) },
                                    onClick = {
                                        onConnectionTypeChange(type)
                                        connectionExpanded = false
                                    }
                                )
                            }
                    }
                }

                Spacer(Modifier.height(12.dp))

                when (connectionType) {
                    ConnectionType.Serial -> {
                        SerialPortSettings(
                            serialDeviceId = serialDeviceId,
                            onSerialDeviceIdChange = onSerialDeviceIdChange,
                            serialPortOptions = serialPortOptions,
                            serialPortExpanded = serialPortExpanded,
                            onSerialPortExpandedChange = { serialPortExpanded = it },
                            baudRate = baudRate,
                            onBaudRateChange = onBaudRateChange,
                            isDeveloperMode = isDeveloperMode
                        )
                    }

                    ConnectionType.BluetoothSerial -> {
                        BluetoothSerialSettings(
                            btSerialDeviceAddress = btSerialDeviceAddress,
                            onBtSerialDeviceAddressChange = onBtSerialDeviceAddressChange,
                            btSerialDeviceOptions = btSerialDeviceOptions,
                            btSerialDeviceExpanded = btSerialDeviceExpanded,
                            onBtSerialDeviceExpandedChange = { btSerialDeviceExpanded = it },
                            baudRate = baudRate,
                            onBaudRateChange = onBaudRateChange,
                            isDeveloperMode = isDeveloperMode
                        )
                    }

                    ConnectionType.TCP, ConnectionType.UDP -> {
                        NetworkSettings(
                            connectionType = connectionType,
                            ipAddress = ipAddress,
                            onIpAddressChange = onIpAddressChange,
                            port = port,
                            onPortChange = onPortChange,
                            sendFormatPrefix = sendFormatPrefix,
                            onSendFormatPrefixChange = onSendFormatPrefixChange,
                            sendFormatSuffix = sendFormatSuffix,
                            onSendFormatSuffixChange = onSendFormatSuffixChange,
                            isDeveloperMode = isDeveloperMode
                        )
                    }

                    ConnectionType.TheHandy -> {
                        HandySettings(
                            handyDeviceAddress = handyDeviceAddress,
                            onHandyDeviceAddressChange = onHandyDeviceAddressChange,
                            handyDeviceOptions = handyDeviceOptions,
                            handyDeviceExpanded = handyDeviceExpanded,
                            onHandyDeviceExpandedChange = { handyDeviceExpanded = it },
                            handyAxis = handyAxis,
                            onHandyAxisChange = onHandyAxisChange,
                            handyAxisExpanded = handyAxisExpanded,
                            onHandyAxisExpandedChange = { handyAxisExpanded = it },
                            handyKey = handyKey,
                            onHandyKeyChange = onHandyKeyChange,
                            handyScanning = handyScanning,
                            onHandyStartScan = { handyStartScan() }
                        )
                    }

                    ConnectionType.TcodeBLE -> {
                        TcodeBLESettings(
                            tcodeBLEDeviceAddress = tcodeBLEDeviceAddress,
                            onTcodeBLEDeviceAddressChange = onTcodeBLEDeviceAddressChange,
                            tcodeBLEDeviceOptions = tcodeBLEDeviceOptions,
                            tcodeBLEDeviceExpanded = tcodeBLEDeviceExpanded,
                            onTcodeBLEDeviceExpandedChange = { tcodeBLEDeviceExpanded = it },
                            tcodeBLEScanning = tcodeBLEScanning,
                            onTcodeBLEStartScan = { tcodeBLEStartScan() },
                            onTcodeBLEStopScan = {
                                bluetoothScanner.stopTcodeBLEScan(); tcodeBLEScanning = false
                            },
                            sendFormatPrefix = sendFormatPrefix,
                            onSendFormatPrefixChange = onSendFormatPrefixChange,
                            sendFormatSuffix = sendFormatSuffix,
                            onSendFormatSuffixChange = onSendFormatSuffixChange,
                            isDeveloperMode = isDeveloperMode
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SerialPortSettings(
    // 串口设备ID
    serialDeviceId: String,
    // 串口设备ID变更回调
    onSerialDeviceIdChange: (String) -> Unit,
    // 可用串口设备列表
    serialPortOptions: List<Pair<String, String>>,
    // 串口选择菜单展开状态
    serialPortExpanded: Boolean,
    // 串口选择菜单展开状态变更回调
    onSerialPortExpandedChange: (Boolean) -> Unit,
    // 波特率
    baudRate: String,
    // 波特率变更回调
    onBaudRateChange: (String) -> Unit,
    // 是否为开发者模式
    isDeveloperMode: Boolean
) {
    val context = LocalContext.current

    ExposedDropdownMenuBox(
        expanded = serialPortExpanded,
        onExpandedChange = onSerialPortExpandedChange
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
            onDismissRequest = { onSerialPortExpandedChange(false) }
        ) {
            if (serialPortOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.serial_no_device)) },
                    onClick = { onSerialPortExpandedChange(false) }
                )
            } else {
                serialPortOptions.forEach { (display, deviceId) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            onSerialDeviceIdChange(deviceId)
                            onSerialPortExpandedChange(false)
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
            onValueChange = onBaudRateChange,
            label = { Text(stringResource(R.string.baud_rate)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothSerialSettings(
    // 蓝牙串口设备地址
    btSerialDeviceAddress: String,
    // 蓝牙串口设备地址变更回调
    onBtSerialDeviceAddressChange: (String) -> Unit,
    // 可用蓝牙设备列表
    btSerialDeviceOptions: List<Pair<String, String>>,
    // 蓝牙设备选择菜单展开状态
    btSerialDeviceExpanded: Boolean,
    // 蓝牙设备选择菜单展开状态变更回调
    onBtSerialDeviceExpandedChange: (Boolean) -> Unit,
    // 波特率
    baudRate: String,
    // 波特率变更回调
    onBaudRateChange: (String) -> Unit,
    // 是否为开发者模式
    isDeveloperMode: Boolean
) {
    ExposedDropdownMenuBox(
        expanded = btSerialDeviceExpanded,
        onExpandedChange = onBtSerialDeviceExpandedChange
    ) {
        val selectedDisplay =
            btSerialDeviceOptions.find { it.second == btSerialDeviceAddress }?.first
                ?: if (btSerialDeviceAddress.isNotEmpty()) btSerialDeviceAddress else stringResource(
                    R.string.select_bt_device
                )
        OutlinedTextField(
            readOnly = true,
            value = if (btSerialDeviceOptions.isEmpty() && btSerialDeviceAddress.isEmpty()) stringResource(
                R.string.bt_serial_no_device
            ) else selectedDisplay,
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
            onDismissRequest = { onBtSerialDeviceExpandedChange(false) }
        ) {
            if (btSerialDeviceOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.bt_serial_no_device)) },
                    onClick = { onBtSerialDeviceExpandedChange(false) }
                )
            } else {
                btSerialDeviceOptions.forEach { (display, addr) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            onBtSerialDeviceAddressChange(addr)
                            onBtSerialDeviceExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = btSerialDeviceAddress,
        onValueChange = onBtSerialDeviceAddressChange,
        label = { Text(stringResource(R.string.device_address_mac)) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.mac_placeholder)) }
    )
    if (isDeveloperMode) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = baudRate,
            onValueChange = onBaudRateChange,
            label = { Text(stringResource(R.string.baud_rate_bt_hint)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkSettings(
    // 连接类型
    connectionType: ConnectionType,
    // IP地址
    ipAddress: String,
    // IP地址变更回调
    onIpAddressChange: (String) -> Unit,
    // 端口号
    port: String,
    // 端口号变更回调
    onPortChange: (String) -> Unit,
    // 发送格式前缀
    sendFormatPrefix: String,
    // 发送格式前缀变更回调
    onSendFormatPrefixChange: (String) -> Unit,
    // 发送格式后缀
    sendFormatSuffix: String,
    // 发送格式后缀变更回调
    onSendFormatSuffixChange: (String) -> Unit,
    // 是否为开发者模式
    isDeveloperMode: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var mdnsDiscovering by remember { mutableStateOf(false) }
    var mdnsDeviceOptions by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    var mdnsDeviceExpanded by remember { mutableStateOf(false) }

    val mDnsDiscoverer = remember { MDnsDiscoverer(context) }

    fun startMDnsDiscovery() {
        if (mdnsDiscovering) return
        mdnsDiscovering = true
        mdnsDeviceOptions = emptyList()

        scope.launch {
            try {
                val serviceType = when (connectionType) {
                    ConnectionType.TCP -> "_tcode._tcp"
                    ConnectionType.UDP -> "_tcode._udp"
                    else -> "_tcode._tcp"
                }

                // 只发现单个服务类型，避免冲突
                val discoveredService = mDnsDiscoverer.discoverTCodeService(8000)

                if (discoveredService != null) {
                    mdnsDeviceOptions = listOf(
                        "${discoveredService.serviceName} (${discoveredService.host}:${discoveredService.port})" to discoveredService.host
                    )
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.mdns_device_found,
                            discoveredService.serviceName
                        ),
                        Toast.LENGTH_SHORT
                    ).show()

                    // 自动填充发现的设备
                    onIpAddressChange(discoveredService.host)
                    onPortChange(discoveredService.port.toString())
                } else {
                    Toast.makeText(
                        context,
                        context.getString(R.string.mdns_no_device),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    context.getString(R.string.mdns_discovery_failed) + ": ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                mdnsDiscovering = false
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = { startMDnsDiscovery() },
            enabled = !mdnsDiscovering
        ) {
            Text(
                if (mdnsDiscovering) context.getString(R.string.mdns_discovering) else context.getString(
                    R.string.mdns_discovery
                )
            )
        }

        if (mdnsDiscovering) {
            Text(
                text = context.getString(R.string.mdns_discovering),
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    Spacer(Modifier.height(8.dp))

    if (mdnsDeviceOptions.isNotEmpty()) {
        ExposedDropdownMenuBox(
            expanded = mdnsDeviceExpanded,
            onExpandedChange = { mdnsDeviceExpanded = it }
        ) {
            val selectedDisplay = mdnsDeviceOptions.find { it.second == ipAddress }?.first
                ?: if (ipAddress.isNotEmpty()) ipAddress else context.getString(R.string.mdns_discovery)

            OutlinedTextField(
                readOnly = true,
                value = selectedDisplay,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                label = { Text(context.getString(R.string.mdns_discovery)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mdnsDeviceExpanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            ExposedDropdownMenu(
                expanded = mdnsDeviceExpanded,
                onDismissRequest = { mdnsDeviceExpanded = false }
            ) {
                mdnsDeviceOptions.forEach { (display, host) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            onIpAddressChange(host)
                            mdnsDeviceExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    OutlinedTextField(
        value = ipAddress,
        onValueChange = onIpAddressChange,
        label = { Text(stringResource(R.string.ip_address)) },
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = port,
        onValueChange = onPortChange,
        label = { Text(stringResource(R.string.port_number)) },
        modifier = Modifier.fillMaxWidth()
    )
    if (isDeveloperMode) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.send_format_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sendFormatPrefix,
            onValueChange = onSendFormatPrefixChange,
            label = { Text(stringResource(R.string.prefix)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sendFormatSuffix,
            onValueChange = onSendFormatSuffixChange,
            label = { Text(stringResource(R.string.suffix)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandySettings(
    // Handy设备地址
    handyDeviceAddress: String,
    // Handy设备地址变更回调
    onHandyDeviceAddressChange: (String) -> Unit,
    // 可用Handy设备列表
    handyDeviceOptions: List<Pair<String, String>>,
    // Handy设备选择菜单展开状态
    handyDeviceExpanded: Boolean,
    // Handy设备选择菜单展开状态变更回调
    onHandyDeviceExpandedChange: (Boolean) -> Unit,
    // Handy轴名称
    handyAxis: String,
    // Handy轴名称变更回调
    onHandyAxisChange: (String) -> Unit,
    // Handy轴选择菜单展开状态
    handyAxisExpanded: Boolean,
    // Handy轴选择菜单展开状态变更回调
    onHandyAxisExpandedChange: (Boolean) -> Unit,
    // Handy密钥
    handyKey: String,
    // Handy密钥变更回调
    onHandyKeyChange: (String) -> Unit,
    // Handy扫描进行中状态
    handyScanning: Boolean,
    // Handy开始扫描回调
    onHandyStartScan: () -> Unit
) {
    Text(
        stringResource(R.string.handy_bluetooth_device),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onHandyStartScan,
            enabled = !handyScanning
        ) {
            Text(if (handyScanning) stringResource(R.string.scanning) else stringResource(R.string.scan))
        }
        Text(
            text = if (handyScanning) stringResource(R.string.scanning_devices) else "",
            fontSize = 12.sp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(
        expanded = handyDeviceExpanded,
        onExpandedChange = onHandyDeviceExpandedChange
    ) {
        val selectedDisplay = handyDeviceOptions.find { it.second == handyDeviceAddress }?.first
            ?: if (handyDeviceAddress.isNotEmpty()) handyDeviceAddress else stringResource(R.string.select_handy_device)
        OutlinedTextField(
            readOnly = true,
            value = if (handyDeviceOptions.isEmpty() && handyDeviceAddress.isEmpty()) stringResource(
                R.string.handy_no_device
            ) else selectedDisplay,
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
            onDismissRequest = { onHandyDeviceExpandedChange(false) }
        ) {
            if (handyDeviceOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.handy_no_device)) },
                    onClick = { onHandyDeviceExpandedChange(false) }
                )
            } else {
                handyDeviceOptions.forEach { (display, address) ->
                    DropdownMenuItem(
                        text = { Text(display) },
                        onClick = {
                            onHandyDeviceAddressChange(address)
                            onHandyDeviceExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = handyDeviceAddress,
        onValueChange = onHandyDeviceAddressChange,
        label = { Text(stringResource(R.string.device_address_mac)) },
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.mac_placeholder)) }
    )
    Spacer(Modifier.height(12.dp))
    ExposedDropdownMenuBox(
        expanded = handyAxisExpanded,
        onExpandedChange = onHandyAxisExpandedChange
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
            onDismissRequest = { onHandyAxisExpandedChange(false) }
        ) {
            AXIS_NAMES.forEach { axis ->
                DropdownMenuItem(
                    text = { Text(axis) },
                    onClick = {
                        onHandyAxisChange(axis)
                        onHandyAxisExpandedChange(false)
                    }
                )
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = handyKey,
        onValueChange = onHandyKeyChange,
        label = { Text(stringResource(R.string.handy_key)) },
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TcodeBLESettings(
    // TcodeBLE设备地址
    tcodeBLEDeviceAddress: String,
    // TcodeBLE设备地址变更回调
    onTcodeBLEDeviceAddressChange: (String) -> Unit,
    // 可用TcodeBLE设备列表 (name, address, rssi)
    tcodeBLEDeviceOptions: List<Triple<String, String, Int>>,
    // TcodeBLE设备选择菜单展开状态
    tcodeBLEDeviceExpanded: Boolean,
    // TcodeBLE设备选择菜单展开状态变更回调
    onTcodeBLEDeviceExpandedChange: (Boolean) -> Unit,
    // TcodeBLE扫描进行中状态
    tcodeBLEScanning: Boolean,
    // TcodeBLE开始扫描回调
    onTcodeBLEStartScan: () -> Unit,
    // TcodeBLE停止扫描回调
    onTcodeBLEStopScan: () -> Unit,
    // 发送格式前缀
    sendFormatPrefix: String,
    // 发送格式前缀变更回调
    onSendFormatPrefixChange: (String) -> Unit,
    // 发送格式后缀
    sendFormatSuffix: String,
    // 发送格式后缀变更回调
    onSendFormatSuffixChange: (String) -> Unit,
    // 是否为开发者模式
    isDeveloperMode: Boolean
) {
    // 当开始扫描时自动展开菜单
    LaunchedEffect(tcodeBLEScanning) {
        if (tcodeBLEScanning && !tcodeBLEDeviceExpanded) {
            onTcodeBLEDeviceExpandedChange(true)
        }
    }

    Text(
        stringResource(R.string.handy_bluetooth_device),
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onTcodeBLEStartScan,
            enabled = !tcodeBLEScanning
        ) {
            Text(if (tcodeBLEScanning) stringResource(R.string.scanning) else stringResource(R.string.scan))
        }
        Text(
            text = if (tcodeBLEScanning) stringResource(R.string.scanning_devices) else "",
            fontSize = 12.sp,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Spacer(Modifier.height(8.dp))
    ExposedDropdownMenuBox(
        expanded = tcodeBLEDeviceExpanded,
        onExpandedChange = onTcodeBLEDeviceExpandedChange
    ) {
        val selectedDisplay = tcodeBLEDeviceOptions.find { it.second == tcodeBLEDeviceAddress }
            ?.let { (name, _, rssi) ->
                if (rssi != 0) "$name (rssi: $rssi)" else name
            }
            ?: if (tcodeBLEDeviceAddress.isNotEmpty()) tcodeBLEDeviceAddress else stringResource(R.string.select_handy_device)
        OutlinedTextField(
            readOnly = true,
            value = if (tcodeBLEDeviceOptions.isEmpty() && tcodeBLEDeviceAddress.isEmpty()) stringResource(
                R.string.handy_no_device
            ) else selectedDisplay,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            label = { Text(stringResource(R.string.ble_device)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tcodeBLEDeviceExpanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(
            expanded = tcodeBLEDeviceExpanded,
            onDismissRequest = { onTcodeBLEDeviceExpandedChange(false) }
        ) {
            if (tcodeBLEDeviceOptions.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.handy_no_device)) },
                    onClick = { onTcodeBLEDeviceExpandedChange(false) }
                )
            } else {
                tcodeBLEDeviceOptions.forEach { (name, address, rssi) ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("$name ($address)")
                                if (rssi != 0) {
                                    Text(
                                        text = "rssi: $rssi",
                                        fontSize = 12.sp,
                                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onTcodeBLEDeviceAddressChange(address)
                            onTcodeBLEDeviceExpandedChange(false)
                            // 停止蓝牙扫描
                            if (tcodeBLEScanning) {
                                onTcodeBLEStopScan()
                            }
                        }
                    )
                }
            }
        }
    }
//    Spacer(Modifier.height(8.dp))
//    OutlinedTextField(
//        value = tcodeBLEDeviceAddress,
//        onValueChange = onTcodeBLEDeviceAddressChange,
//        label = { Text(stringResource(R.string.device_address_mac)) },
//        modifier = Modifier.fillMaxWidth(),
//        placeholder = { Text(stringResource(R.string.mac_placeholder)) }
//    )
    if (isDeveloperMode) {
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.send_format_title),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sendFormatPrefix,
            onValueChange = onSendFormatPrefixChange,
            label = { Text(stringResource(R.string.prefix)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = sendFormatSuffix,
            onValueChange = onSendFormatSuffixChange,
            label = { Text(stringResource(R.string.suffix)) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
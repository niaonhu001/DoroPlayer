package com.example.funplayer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun DeviceSettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var connectionEnabled by remember { mutableStateOf(getConnectionEnabled(context)) }
    var connectionType by remember { mutableStateOf(getConnectionType(context)) }
    var ipAddress by remember { mutableStateOf(getDeviceIp(context)) }
    var port by remember { mutableStateOf(context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_DEVICE_PORT, "8080") ?: "8080") }
    var serialDeviceId by remember { mutableStateOf(getSerialDeviceId(context)) }
    var baudRate by remember { mutableStateOf(context.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE).getString(KEY_BAUD_RATE, "9600") ?: "9600") }
    var btSerialDeviceAddress by remember { mutableStateOf(getBtSerialDeviceAddress(context)) }
    var handyAxis by remember { mutableStateOf(getHandyAxis(context)) }
    var handyDeviceAddress by remember { mutableStateOf(getHandyDeviceAddress(context)) }
    var handyKey by remember { mutableStateOf(getHandyKey(context)) }
    var tcodeBLEDeviceAddress by remember { mutableStateOf(getTcodeBLEDeviceAddress(context)) }
    var sendFormatPrefix by remember { mutableStateOf(getSendFormatPrefix(context)) }
    var sendFormatSuffix by remember { mutableStateOf(getSendFormatSuffix(context)) }
    
    var axisRanges by remember {
        mutableStateOf(
            AXIS_NAMES.associateWith { axisName ->
                getAxisRanges(context)[axisName] ?: (0f to 100f)
            }
        )
    }
    
    var connectionTestInProgress by remember { mutableStateOf(false) }
    
    LaunchedEffect(
        connectionEnabled, connectionType, ipAddress, port, sendFormatPrefix, sendFormatSuffix,
        serialDeviceId, baudRate, btSerialDeviceAddress, handyAxis, handyKey, handyDeviceAddress, tcodeBLEDeviceAddress
    ) {
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
            tcodeBLEDeviceAddress
        )
    }
    
    fun handleConnectionTest() {
        if (connectionTestInProgress) return
        connectionTestInProgress = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) { sendConnectionTest(context) }
            connectionTestInProgress = false
            android.widget.Toast.makeText(
                context,
                if (ok) context.getString(R.string.connection_test_sent) else context.getString(R.string.connection_test_failed),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    fun handleSendCommand(command: String) {
        scope.launch {
            sendAxisCommand(context, command)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(stringResource(R.string.device_settings_title), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        
        ConnectionSettingsCard(
            connectionEnabled = connectionEnabled,
            onConnectionEnabledChange = { connectionEnabled = it },
            connectionType = connectionType,
            onConnectionTypeChange = { connectionType = it },
            ipAddress = ipAddress,
            onIpAddressChange = { ipAddress = it },
            port = port,
            onPortChange = { port = it },
            serialDeviceId = serialDeviceId,
            onSerialDeviceIdChange = { serialDeviceId = it },
            baudRate = baudRate,
            onBaudRateChange = { baudRate = it },
            btSerialDeviceAddress = btSerialDeviceAddress,
            onBtSerialDeviceAddressChange = { btSerialDeviceAddress = it },
            handyDeviceAddress = handyDeviceAddress,
            onHandyDeviceAddressChange = { handyDeviceAddress = it },
            handyAxis = handyAxis,
            onHandyAxisChange = { handyAxis = it },
            handyKey = handyKey,
            onHandyKeyChange = { handyKey = it },
            tcodeBLEDeviceAddress = tcodeBLEDeviceAddress,
            onTcodeBLEDeviceAddressChange = { tcodeBLEDeviceAddress = it },
            sendFormatPrefix = sendFormatPrefix,
            onSendFormatPrefixChange = { sendFormatPrefix = it },
            sendFormatSuffix = sendFormatSuffix,
            onSendFormatSuffixChange = { sendFormatSuffix = it },
            onConnectionTest = { handleConnectionTest() },
            connectionTestInProgress = connectionTestInProgress
        )
        
        OutputRangeSettingsCard(
            axisRanges = axisRanges,
            onAxisRangesChange = { newRanges -> axisRanges = newRanges }
        )
        
        DeviceControlSettingsCard(
            onSendCommand = { command -> handleSendCommand(command) }
        )
    }
}
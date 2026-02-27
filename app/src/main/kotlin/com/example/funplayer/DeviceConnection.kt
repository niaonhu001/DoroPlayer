@file:Suppress("DEPRECATION")
package com.example.funplayer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.funplayer.handyplug.Handyplug
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

internal fun sendUdpMessage(host: String, port: Int, message: String): Boolean {
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

/**
 * TCP 连接池，用于维护长连接
 */
internal object TcpConnectionPool {
    private data class ConnectionKey(val host: String, val port: Int)

    private data class PooledConnection(
        val socket: java.net.Socket,
        val outputStream: java.io.OutputStream,
        var lastUsed: Long = System.currentTimeMillis()
    )

    private val connections = mutableMapOf<ConnectionKey, PooledConnection>()
    private val lock = Any()

    /**
     * 发送 TCP 消息，使用连接池保持长连接
     */
    fun sendMessage(host: String, port: Int, message: String): Boolean {
        if (message.isEmpty()) return true
        return sendBytes(host, port, message.toByteArray(Charsets.UTF_8))
    }

    /**
     * 发送 TCP 字节数组，使用连接池保持长连接
     */
    fun sendBytes(host: String, port: Int, bytes: ByteArray): Boolean {
        if (bytes.isEmpty()) return true

        val key = ConnectionKey(host, port)

        synchronized(lock) {
            try {
                // 获取或创建连接
                val conn = getOrCreateConnection(key)

                // 检查连接是否仍然有效
                if (conn.socket.isClosed || !conn.socket.isConnected || conn.socket.isOutputShutdown) {
                    // 连接已失效，移除并重新创建
                    removeConnection(key)
                    val newConn = createConnection(host, port) ?: return false
                    connections[key] = newConn
                    newConn.outputStream.write(bytes)
                    newConn.outputStream.flush()
                    newConn.lastUsed = System.currentTimeMillis()
                    return true
                }

                // 发送数据
                conn.outputStream.write(bytes)
                conn.outputStream.flush()
                conn.lastUsed = System.currentTimeMillis()
                return true

            } catch (e: Exception) {
                DevLog.log("TCP", "发送失败: ${e.message}")
                // 发生错误时移除连接
                removeConnection(key)
                return false
            }
        }
    }

    /**
     * 获取或创建连接
     */
    private fun getOrCreateConnection(key: ConnectionKey): PooledConnection {
        val existing = connections[key]
        if (existing != null && !existing.socket.isClosed && existing.socket.isConnected) {
            return existing
        }

        // 创建新连接
        val newConn = createConnection(key.host, key.port) ?: throw Exception("无法创建 TCP 连接")
        connections[key] = newConn
        return newConn
    }

    /**
     * 创建新的 TCP 连接
     */
    private fun createConnection(host: String, port: Int): PooledConnection? {
        return try {
            DevLog.log("TCP", "创建新连接: $host:$port")
            val socket = java.net.Socket()
            // 设置连接超时和读写超时
            socket.connect(java.net.InetSocketAddress(host, port), 5000)
            socket.soTimeout = 10000
            socket.tcpNoDelay = true
            socket.keepAlive = true

            val outputStream = socket.getOutputStream()
            PooledConnection(socket, outputStream)
        } catch (e: Exception) {
            DevLog.log("TCP", "连接失败: $host:$port, ${e.message}")
            null
        }
    }

    /**
     * 移除并关闭连接
     */
    private fun removeConnection(key: ConnectionKey) {
        val conn = connections.remove(key)
        if (conn != null) {
            try {
                conn.outputStream.close()
            } catch (_: Exception) { }
            try {
                conn.socket.close()
            } catch (_: Exception) { }
        }
    }

    /**
     * 关闭所有连接
     */
    fun closeAll() {
        synchronized(lock) {
            connections.keys.toList().forEach { key ->
                removeConnection(key)
            }
        }
    }

    /**
     * 关闭特定连接
     */
    fun close(host: String, port: Int) {
        val key = ConnectionKey(host, port)
        synchronized(lock) {
            removeConnection(key)
        }
    }

    /**
     * 清理超过指定时间未使用的连接
     */
    fun cleanup(idleTimeoutMs: Long = 60000) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            connections.keys.toList().forEach { key ->
                val conn = connections[key]
                if (conn != null && (now - conn.lastUsed) > idleTimeoutMs) {
                    DevLog.log("TCP", "清理闲置连接: ${key.host}:${key.port}")
                    removeConnection(key)
                }
            }
        }
    }
}

// 兼容旧接口
internal fun sendTcpMessage(host: String, port: Int, message: String): Boolean {
    return TcpConnectionPool.sendMessage(host, port, message)
}

internal fun sendTcpBytes(host: String, port: Int, bytes: ByteArray): Boolean {
    return TcpConnectionPool.sendBytes(host, port, bytes)
}

internal fun buildHandyTestPayload(): ByteArray {
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

internal fun buildHandyLinearPayload(
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

internal fun sendBluetoothSerialMessage(context: android.content.Context, address: String, message: String): Boolean {
    if (message.isEmpty() || address.isBlank()) return true
    if (Build.VERSION.SDK_INT >= 31 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != android.content.pm.PackageManager.PERMISSION_GRANTED
    ) {
        return false
    }
    val manager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as? BluetoothManager ?: return false
    val adapter: BluetoothAdapter = manager.adapter ?: return false
    val device = try { adapter.getRemoteDevice(address) } catch (_: Exception) { return false }
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

internal object HandyBleClient {
    private var gatt: BluetoothGatt? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null
    private var connectedAddress: String? = null
    private var pendingWrite: kotlinx.coroutines.CompletableDeferred<Boolean>? = null
    private var negotiatedMtu: Int = 23

    // 连接状态管理：null=未连接, Deferred=正在连接
    private var pendingConnection: kotlinx.coroutines.CompletableDeferred<Boolean>? = null

    // 连接就绪标志：只有在成功获取TX特征值后才为true
    @Volatile
    private var isConnectionReady: Boolean = false

    private fun hasPermission(context: android.content.Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private fun closeInternal() {
        try {
            gatt?.disconnect()
        } catch (_: Exception) { }
        try {
            gatt?.close()
        } catch (_: Exception) { }
        gatt = null
        txCharacteristic = null
        connectedAddress = null
        pendingWrite = null
        negotiatedMtu = 23
        pendingConnection = null
        isConnectionReady = false
    }

    suspend fun write(context: android.content.Context, address: String, payload: ByteArray, useWriteWithResponse: Boolean = true): Boolean {
        if (address.isBlank() || payload.isEmpty()) {
            DevLog.log("Handy", "连接/写入失败: 设备地址为空或载荷为空")
            return false
        }
        if (Build.VERSION.SDK_INT >= 31 && !hasPermission(context, Manifest.permission.BLUETOOTH_CONNECT)) {
            DevLog.log("Handy", "连接失败: 缺少 BLUETOOTH_CONNECT 权限")
            return false
        }

        // 如果正在连接中，直接返回false，不响应写入
        if (pendingConnection != null) {
            DevLog.log("Handy", "连接正在进行中，暂不响应写入请求")
            return false
        }

        // 检查是否已连接且就绪（只有在获取到TX特征值后才认为连接就绪）
        if (isConnectionReady && gatt != null && txCharacteristic != null && connectedAddress == address) {
            val g = gatt
            val ch = txCharacteristic
            if (g != null && ch != null) {
                if (useWriteWithResponse) kotlinx.coroutines.delay(150L)
                val written = writeChunked(g, ch, payload, useWriteWithResponse)
                if (!written) DevLog.log("Handy", "写入失败: 未完成或超时")
                return written
            }
        }

        // 触发新的连接
        val forTcodeBLE = !useWriteWithResponse
        val connectionDeferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingConnection = connectionDeferred

        val ok = connectOnce(context, address, forTcodeBLE)
        connectionDeferred.complete(ok)
        pendingConnection = null

        if (!ok) return false

        val g = gatt
        val ch = txCharacteristic
        if (g == null || ch == null) {
            DevLog.log("Handy", "写入失败: GATT 或特征为空，连接可能已断开")
            return false
        }
        if (useWriteWithResponse) kotlinx.coroutines.delay(150L)
        val written = writeChunked(g, ch, payload, useWriteWithResponse)
        if (!written) DevLog.log("Handy", "写入失败: 未完成或超时")
        return written
    }

    private suspend fun connectOnce(context: android.content.Context, address: String, forTcodeBLE: Boolean = false): Boolean {
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

        return withTimeoutOrNull(30_000L) {
            kotlinx.coroutines.suspendCancellableCoroutine<Boolean> { cont ->
                val callback = object : BluetoothGattCallback() {
                    private var finished = false

                    private fun finish(value: Boolean) {
                        if (finished) return
                        finished = true
                        cont.resume(value)
                    }

                    override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            if (status != BluetoothGatt.GATT_SUCCESS && status != 257 && status != 133) {
                                DevLog.log("Handy", "连接成功但 status=$status (已忽略)")
                            }
                            val res = g.requestMtu(200)
                            DevLog.log("HANDY","requestMtu called: $res")
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            if (status == 257 || status == 133) {
                                DevLog.log("Handy", "收到状态断开回调 (status=$status)，忽略此假性断开")
                                return
                            }
                            DevLog.log("Handy", "连接断开: status=$status")
                            if (finished) DevLog.log("Handy", "BLE 连接已断开，下次发送将自动重连")
                            else DevLog.log("Handy", "连接失败: STATE_DISCONNECTED")
                            closeInternal()
                            finish(false)
                        }
                    }

                    override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            DevLog.log("Handy", "onServicesDiscovered status=$status (非 GATT_SUCCESS)")
                        }

                        val service: BluetoothGattService? = g.getService(HANDY_SERVICE_UUID)
                        val ch: BluetoothGattCharacteristic? = service?.getCharacteristic(HANDY_CHARACTERISTIC_UUID)
                        if (ch == null) {
                            DevLog.log("Handy", "连接失败: 未找到 Handy 服务或特征 UUID")
                            closeInternal()
                            finish(false)
                            return
                        } else {
                            DevLog.log("Handy", "成功获取TX特征值")
                        }
                        txCharacteristic = ch

                        val res = g.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                        DevLog.log("HANDY","requestConnectionPriority: $res")

                        // 标记连接就绪，允许写入
                        isConnectionReady = true
                        DevLog.log("Handy", "连接就绪，可以开始写入")

                        finish(true)
                    }

                    override fun onCharacteristicWrite(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            DevLog.log("Handy", "写入回调: status=$status (非 GATT_SUCCESS)")
                        }
                        pendingWrite?.complete(status == BluetoothGatt.GATT_SUCCESS)
                        pendingWrite = null
                    }

                    override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) {
                        if (status == BluetoothGatt.GATT_SUCCESS) {
                            negotiatedMtu = mtu
                            DevLog.log("Handy", "MTU 协商成功: $mtu")
                        } else {
                            DevLog.log("Handy", "MTU 协商失败: status=$status, 使用默认 23")
                        }

                        g.setPreferredPhy(
                            BluetoothDevice.PHY_LE_1M_MASK,  // TX 首选1M
                            BluetoothDevice.PHY_LE_1M_MASK,  // RX 首选1M
                            BluetoothDevice.PHY_OPTION_NO_PREFERRED
                        )
                        g.discoverServices()
                    }
                }

                val g = if (forTcodeBLE) {
                    device.connectGatt(context, false, callback)
                } else if (Build.VERSION.SDK_INT >= 23) {
                    device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
                } else {
                    device.connectGatt(context, false, callback)
                }
                gatt = g
                connectedAddress = address

                cont.invokeOnCancellation {
                    closeInternal()
                }
            }
        } ?: false
    }

    private suspend fun writeChunked(g: BluetoothGatt, ch: BluetoothGattCharacteristic, payload: ByteArray, useWriteWithResponse: Boolean): Boolean {
        if (!useWriteWithResponse) return writeOnceNoResponseEciotStyle(g, ch, payload)

        // 打印实际发送的内容
        DevLog.log("Handy", "准备发送: ${payload.size} 字节，分块大小: 20 字节")

        // 计算可用 MTU（MTU - 3，3字节为协议头）
        // 使用协商后的 MTU，默认 23-3=20，如果协商成功则更大
        val availableMtu = negotiatedMtu - 3
        val chunkSize = availableMtu.coerceAtLeast(20)

        // 分块发送
        val totalChunks = (payload.size + chunkSize - 1) / chunkSize
        DevLog.log("Handy", "分为 $totalChunks 块，每块最大 $chunkSize 字节")

        for (i in 0 until totalChunks) {
            val start = i * chunkSize
            val end = minOf(start + chunkSize, payload.size)
            val chunk = payload.copyOfRange(start, end)
            val success = writeOnceWithResponse(g, ch, chunk, i + 1)
            if (!success) {
                DevLog.log("Handy", "第 ${i + 1}/$totalChunks 块发送失败")
                return false
            }
        }

        DevLog.log("Handy", "所有 $totalChunks 块发送成功")
        return true
    }

    private fun writeOnceNoResponseEciotStyle(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray): Boolean {
        DevLog.log("Handy", "准备发送2: ${bytes.size} 字节")

        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        return try {
            g.writeCharacteristic(ch)
        } catch (e: Exception) {
            DevLog.log("Handy", "写入失败(TcodeBLE/eciot): ${e.message}")
            false
        }
    }

    private suspend fun writeOnceWithResponse(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray, chunkIndex: Int = 0): Boolean {
//        DevLog.log("Handy", "准备发送3: ${bytes.size} 字节")

        ch.value = bytes
        ch.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val started = try { g.writeCharacteristic(ch) } catch (e: Exception) {
            DevLog.log("Handy", "写入(带响应)失败(第${chunkIndex}包): ${e.message}")
            return false
        }
        if (!started) return false
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingWrite = deferred
        val ok = withTimeoutOrNull(800L) { deferred.await() }
        pendingWrite = null
        return ok == true
    }

    private suspend fun writeOnceNoResponse(g: BluetoothGatt, ch: BluetoothGattCharacteristic, bytes: ByteArray, chunkIndex: Int = 0): Boolean {
//        DevLog.log("Handy", "准备发送4: ${bytes.size} 字节")

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
        val deferred = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingWrite = deferred
        val ok = withTimeoutOrNull(500L) { deferred.await() }
        pendingWrite = null
        when {
            ok == true -> return true
            ok == false -> { DevLog.log("Handy", "写入(第${chunkIndex}包): status 非 GATT_SUCCESS"); return false }
            else -> return true
        }
    }
}

internal fun sendSerialMessage(context: android.content.Context, deviceId: String, baudRate: Int, message: String): Boolean {
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

internal suspend fun sendConnectionTest(context: android.content.Context): Boolean {
    val connType = getConnectionType(context)
    if (connType != ConnectionType.UDP && connType != ConnectionType.TCP &&
        connType != ConnectionType.Serial && connType != ConnectionType.BluetoothSerial &&
        connType != ConnectionType.TheHandy && connType != ConnectionType.TcodeBLE) return false
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
        ConnectionType.TcodeBLE -> {
            val prefix = getSendFormatPrefix(context)
            val suffix = getSendFormatSuffix(context)
            val message = prefix + CONNECTION_TEST_MESSAGE + suffix
            HandyBleClient.write(context, getTcodeBLEDeviceAddress(context), message.toByteArray(Charsets.UTF_8), useWriteWithResponse = false)
        }
        else -> false
    }
}

internal suspend fun sendAxisCommand(context: android.content.Context, axisCommand: String): Boolean {
    if (axisCommand.isEmpty()) return false
    val connType = getConnectionType(context)
    if (connType != ConnectionType.UDP && connType != ConnectionType.TCP &&
        connType != ConnectionType.Serial && connType != ConnectionType.BluetoothSerial &&
        connType != ConnectionType.TheHandy && connType != ConnectionType.TcodeBLE
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
            ConnectionType.TcodeBLE -> {
                val prefix = getSendFormatPrefix(context)
                val suffix = getSendFormatSuffix(context)
                val message = prefix + axisCommand + suffix
                HandyBleClient.write(context, getTcodeBLEDeviceAddress(context), message.toByteArray(Charsets.UTF_8), useWriteWithResponse = false)
            }
            else -> false
        }
    }
}

internal fun getAvailableSerialPorts(context: android.content.Context): List<Pair<String, String>> {
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

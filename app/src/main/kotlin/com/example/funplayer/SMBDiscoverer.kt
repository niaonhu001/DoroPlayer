package com.example.funplayer

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * SMB/NAS 服务器发现工具
 * 使用多种方法发现本地网络上的 SMB 服务器
 */
class SMBDiscoverer(private val context: Context) {

    companion object {
        // SSDP 多播地址和端口
        private val SSDP_MULTICAST_ADDRESS = "239.255.255.250"
        private const val SSDP_PORT = 1900

        // SMB 端口
        private const val SMB_PORT = 445
    }

    private fun getSSDPSearchMessage(): String {
        return """
            M-SEARCH * HTTP/1.1
            HOST: 239.255.255.250:1900
            MAN: "ssdp:discover"
            MX: 3
            ST: ssdp:all

        """.trimIndent()
    }

    /**
     * 发现支持 SMB 的设备
     * 使用多种方法：
     * 1. SSDP 多播搜索
     * 2. 本地网络扫描（如果启用了该选项）
     *
     * @param timeoutMs 超时时间（毫秒）
     * @param scanLocalNetwork 是否扫描本地网络段（较慢但更可靠）
     * @return 发现的设备列表，包含 IP 地址和设备名称
     */
    suspend fun discoverSmbDevices(
        timeoutMs: Long = 5000,
        scanLocalNetwork: Boolean = false
    ): Result<List<SmbDevice>> = withContext(Dispatchers.IO) {
        DevLog.log("SMBDiscover", "开始扫描 SMB 设备，超时: ${timeoutMs}ms, 扫描本地网络: $scanLocalNetwork")

        runCatching {
            val devices = mutableSetOf<SmbDevice>()

            // 方法1: SSDP 搜索
            DevLog.log("SMBDiscover", "尝试 SSDP 搜索...")
            try {
                val ssdpDevices = discoverViaSSDP(timeoutMs = 3000)
                DevLog.log("SMBDiscover", "SSDP 搜索完成，发现 ${ssdpDevices.size} 个设备")
                devices.addAll(ssdpDevices)
            } catch (e: Exception) {
                DevLog.log("SMBDiscover", "SSDP 搜索失败: ${e.message}")
                // SSDP 失败，继续尝试其他方法
            }

            // 方法2: 本地网络扫描（可选）
            if (scanLocalNetwork) {
                DevLog.log("SMBDiscover", "开始本地网络扫描...")
                try {
                    val localDevices = scanLocalNetworkForSMB()
                    DevLog.log("SMBDiscover", "本地网络扫描完成，发现 ${localDevices.size} 个设备")
                    devices.addAll(localDevices)
                } catch (e: Exception) {
                    DevLog.log("SMBDiscover", "本地网络扫描失败: ${e.message}")
                    // 扫描失败，忽略
                }
            }

//            // 方法3: 尝试从已知的本地子网网关地址开始扫描
//            if (devices.isEmpty()) {
//                DevLog.log("SMBDiscover", "未发现设备，尝试扫描网关附近地址...")
//                try {
//                    val gatewayDevices = scanGatewayRange()
//                    DevLog.log("SMBDiscover", "网关扫描完成，发现 ${gatewayDevices.size} 个设备")
//                    devices.addAll(gatewayDevices)
//                } catch (e: Exception) {
//                    DevLog.log("SMBDiscover", "网关扫描失败: ${e.message}")
//                    // 扫描失败，忽略
//                }
//            }

            DevLog.log("SMBDiscover", "扫描完成，共发现 ${devices.size} 个设备")
            devices.toList()
        }.onFailure { e ->
            DevLog.log("SMBDiscover", "扫描异常: ${e.message}")
            DevLog.log("SMBDiscover", "异常堆栈: ${e.stackTraceToString()}")
        }
    }

    /**
     * 使用 SSDP 协议发现设备
     */
    private suspend fun discoverViaSSDP(timeoutMs: Long): List<SmbDevice> =
        suspendCancellableCoroutine { continuation ->
            DevLog.log("SMBDiscover", "SSDP: 创建 UDP socket")
            val socket = DatagramSocket()
            socket.soTimeout = 1000
            socket.broadcast = true  // 启用广播

            try {
                val multicastAddress = InetAddress.getByName(SSDP_MULTICAST_ADDRESS)
                val searchData = getSSDPSearchMessage().toByteArray(Charsets.UTF_8)
                val sendPacket = DatagramPacket(
                    searchData,
                    searchData.size,
                    multicastAddress,
                    SSDP_PORT
                )

                // 直接发送到多播地址，不需要设置特定接口
                // Android 会自动选择合适的网络接口
                try {
                    DevLog.log("SMBDiscover", "SSDP: 发送多播数据包到 $multicastAddress:$SSDP_PORT")
                    socket.send(sendPacket)
                    DevLog.log("SMBDiscover", "SSDP: 数据包已发送，大小: ${searchData.size} 字节")
                } catch (e: Exception) {
                    DevLog.log("SMBDiscover", "SSDP: 发送失败 - ${e.message}")
                }

                val devices = mutableListOf<SmbDevice>()
                val buffer = ByteArray(8192)
                val startTime = System.currentTimeMillis()
                val seenAddresses = mutableSetOf<String>()

                // 接收响应
                while (System.currentTimeMillis() - startTime < timeoutMs) {
                    try {
                        val receivePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(receivePacket)

                        val address = receivePacket.address.hostAddress ?: continue
                        if (address in seenAddresses) continue

                        // 只关心 IPv4 地址
                        if (receivePacket.address !is Inet4Address) continue

                        seenAddresses.add(address)
                        DevLog.log("SMBDiscover", "SSDP: 收到响应来自 $address, 大小: ${receivePacket.length} 字节")

                        // 解析响应
                        val responseText = String(buffer, 0, receivePacket.length, Charsets.UTF_8)
                        val deviceName = parseSSDPResponse(responseText, address)

                        DevLog.log("SMBDiscover", "SSDP: 解析设备 - 地址: $address, 名称: ${deviceName ?: "未知"}")
                        devices.add(SmbDevice(address = address, name = deviceName))
                    } catch (e: SocketTimeoutException) {
                        // 超时是正常的
                    } catch (e: Exception) {
                        DevLog.log("SMBDiscover", "SSDP: 接收数据包异常 - ${e.message}")
                    }
                }

                socket.close()
                DevLog.log("SMBDiscover", "SSDP: 搜索完成，发现 ${devices.size} 个设备")
                continuation.resume(devices)
            } catch (e: Exception) {
                socket.close()
                DevLog.log("SMBDiscover", "SSDP: 严重错误 - ${e.message}")
                DevLog.log("SMBDiscover", "SSDP: 堆栈 - ${e.stackTraceToString()}")
                if (continuation.isActive) {
                    continuation.resumeWithException(e)
                }
            }

            continuation.invokeOnCancellation {
                socket.close()
            }
        }

    /**
     * 扫描本地网络段寻找 SMB 服务器
     * 根据 IP 和子网掩码计算实际的子网范围
     * 使用8个并发线程加速扫描
     */
    private suspend fun scanLocalNetworkForSMB(): List<SmbDevice> = withContext(Dispatchers.IO) {
        coroutineScope {
            val allDevices = mutableListOf<SmbDevice>()
            val scanLock = Any()

            DevLog.log("SMBDiscover", "本地网络扫描: 开始（8并发）...")

            // 获取所有网络接口的信息
            val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()
            DevLog.log("SMBDiscover", "本地网络扫描: 发现 ${networkInterfaces.size} 个网络接口")

            // 收集所有需要扫描的IP地址
            val ipAddressesToScan = mutableListOf<IpToScan>()

            for (networkInterface in networkInterfaces) {
                if (!networkInterface.isUp || networkInterface.isLoopback) continue

                val interfaceAddresses = networkInterface.interfaceAddresses ?: continue
                DevLog.log("SMBDiscover", "本地网络扫描: 接口 ${networkInterface.name}, ${interfaceAddresses.size} 个地址")

                for (interfaceAddress in interfaceAddresses) {
                    val address = interfaceAddress.address
                    if (address !is Inet4Address || address.isLoopbackAddress) continue

                    val ipAddress = address.hostAddress ?: continue
                    val networkPrefixLength = interfaceAddress.networkPrefixLength

                    DevLog.log("SMBDiscover", "本地网络扫描: 处理地址 $ipAddress/$networkPrefixLength")

                    // 计算子网范围
                    val subnetInfo = calculateSubnetRange(ipAddress, networkPrefixLength.toInt())
                    if (subnetInfo != null) {
                        DevLog.log("SMBDiscover", "本地网络扫描: 子网 ${subnetInfo.networkAddress}/${subnetInfo.prefixLength}, 主机数: ${subnetInfo.hostCount}")

                        // 扫描子网范围内的 IP（限制扫描数量以避免过长时间）
                        val scanCount = minOf(subnetInfo.hostCount, 256) // 最多扫描 256 个地址
                        val step = if (subnetInfo.hostCount > scanCount) {
                            subnetInfo.hostCount / scanCount
                        } else {
                            1
                        }

                        DevLog.log("SMBDiscover", "本地网络扫描: 将扫描约 $scanCount 个地址，步进: $step")

                        for (offset in 0 until subnetInfo.hostCount step step) {
                            if (offset >= subnetInfo.hostCount) break

                            val ipToScan = subnetInfo.getFirstAddress(offset)
                            // 跳过本机 IP 和网络/广播地址
                            if (ipToScan == ipAddress || ipToScan == subnetInfo.networkAddress ||
                                ipToScan == subnetInfo.broadcastAddress) continue

                            ipAddressesToScan.add(IpToScan(ipToScan, ipAddress))
                        }
                    } else {
                        DevLog.log("SMBDiscover", "本地网络扫描: 无法计算子网范围 - $ipAddress/$networkPrefixLength")
                    }
                }
            }

            DevLog.log("SMBDiscover", "本地网络扫描: 共 ${ipAddressesToScan.size} 个地址待扫描，使用8个并发线程")

            // 使用8个并发线程扫描
            val concurrency = 8
            val chunkedIps = ipAddressesToScan.chunked(
                (ipAddressesToScan.size + concurrency - 1) / concurrency
            )

            val startTime = System.currentTimeMillis()

            chunkedIps.map { chunk ->
                async(Dispatchers.IO) {
                    chunk.forEach { ipToScan ->
                        try {
                            if (isSMBServerAvailable(ipToScan.ip, timeoutMs = 200)) {
                                synchronized(scanLock) {
                                    DevLog.log("SMBDiscover", "本地网络扫描: ✓ 发现 SMB 服务器 ${ipToScan.ip}")
                                    allDevices.add(SmbDevice(address = ipToScan.ip))
                                }
                            }
                        } catch (e: Exception) {
                            DevLog.log("SMBDiscover", "本地网络扫描: ✗ 扫描 ${ipToScan.ip} 失败 - ${e.message}")
                        }
                    }
                }
            }.awaitAll()

            val elapsed = System.currentTimeMillis() - startTime
            DevLog.log("SMBDiscover", "本地网络扫描: 完成，发现 ${allDevices.size} 个设备，耗时 ${elapsed}ms")

            allDevices
        }
    }

    /**
     * 待扫描的IP地址信息
     */
    private data class IpToScan(
        val ip: String,
        val localIp: String  // 本机IP，用于日志
    )

    /**
     * 扫描网关附近的 IP 地址
     * 根据 IP 和子网掩码计算，扫描本机附近的地址
     */
    private suspend fun scanGatewayRange(): List<SmbDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SmbDevice>()
        DevLog.log("SMBDiscover", "网关扫描: 开始...")

        // 获取所有网络接口的信息
        val networkInterfaces = NetworkInterface.getNetworkInterfaces().toList()

        for (networkInterface in networkInterfaces) {
            if (!networkInterface.isUp || networkInterface.isLoopback) continue

            val interfaceAddresses = networkInterface.interfaceAddresses ?: continue

            for (interfaceAddress in interfaceAddresses) {
                val address = interfaceAddress.address
                if (address !is Inet4Address || address.isLoopbackAddress) continue

                val ipAddress = address.hostAddress ?: continue
                val networkPrefixLength = interfaceAddress.networkPrefixLength

                DevLog.log("SMBDiscover", "网关扫描: 处理接口地址 $ipAddress/$networkPrefixLength")

                // 计算子网范围
                val subnetInfo = calculateSubnetRange(ipAddress, networkPrefixLength.toInt())
                if (subnetInfo != null) {
                    // 获取本机在子网中的偏移量
                    val localOffset = subnetInfo.getOffset(ipAddress)

                    // 扫描本机附近的 IP（前后各 20 个地址）
                    val startOffset = maxOf(1, localOffset - 20)
                    val endOffset = minOf(subnetInfo.hostCount - 2, localOffset + 20)

                    DevLog.log("SMBDiscover", "网关扫描: 扫描范围 [$startOffset - $endOffset]，本机偏移: $localOffset")

                    for (offset in startOffset..endOffset) {
                        val ipToScan = subnetInfo.getFirstAddress(offset)
                        if (ipToScan == ipAddress) continue // 跳过本机

                        try {
                            if (isSMBServerAvailable(ipToScan, timeoutMs = 200)) {
                                DevLog.log("SMBDiscover", "网关扫描: ✓ 发现 SMB 服务器 $ipToScan")
                                devices.add(SmbDevice(address = ipToScan))
                            }
                        } catch (e: Exception) {
                            DevLog.log("SMBDiscover", "网关扫描: ✗ 扫描 $ipToScan 失败 - ${e.message}")
                        }
                    }
                } else {
                    DevLog.log("SMBDiscover", "网关扫描: 无法计算子网 - $ipAddress/$networkPrefixLength")
                }
            }
        }

        DevLog.log("SMBDiscover", "网关扫描: 完成，发现 ${devices.size} 个设备")
        devices.distinctBy { it.address }
    }

    /**
     * 根据IP地址和子网掩码计算子网范围
     */
    private fun calculateSubnetRange(ipAddress: String, prefixLength: Int): SubnetInfo? {
        return try {
            val parts = ipAddress.split(".").map { it.toInt() }
            if (parts.size != 4) return null

            val ipInt = ((parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or (parts[2].toLong() shl 8) or parts[3].toLong()).toInt()
            val maskInt = if (prefixLength == 0) 0 else (0xFFFFFFFF.toInt() shl (32 - prefixLength))

            val networkInt = ipInt and maskInt
            val broadcastInt = networkInt or (0xFFFFFFFF.toInt() xor maskInt)

            val hostCount = ((broadcastInt.toLong() - networkInt - 1).toInt()).coerceAtLeast(0)

            SubnetInfo(
                networkAddress = intToIp(networkInt.toLong()),
                broadcastAddress = intToIp(broadcastInt.toLong()),
                prefixLength = prefixLength,
                hostCount = hostCount
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 将整数转换为IP地址字符串
     */
    private fun intToIp(ipInt: Long): String {
        return "${(ipInt shr 24) and 0xFF}.${(ipInt shr 16) and 0xFF}.${(ipInt shr 8) and 0xFF}.${ipInt and 0xFF}"
    }

    /**
     * 子网信息数据类
     */
    private data class SubnetInfo(
        val networkAddress: String,
        val broadcastAddress: String,
        val prefixLength: Int,
        val hostCount: Int,
        private val networkInt: Int = 0  // 存储网络地址的整数形式，用于计算偏移量
    ) {
        /**
         * 获取子网中指定偏移量的IP地址
         * offset: 1 表示第一个可用主机地址（网络地址 + 1）
         */
        fun getFirstAddress(offset: Int): String {
            val networkParts = networkAddress.split(".").map { it.toInt() }
            val ipInt = (networkParts[0].toLong() shl 24) or (networkParts[1].toLong() shl 16) or (networkParts[2].toLong() shl 8) or networkParts[3].toLong()

            val targetInt = (ipInt + offset).toLong()
            return "${(targetInt shr 24) and 0xFF}.${(targetInt shr 16) and 0xFF}.${(targetInt shr 8) and 0xFF}.${targetInt and 0xFF}"
        }

        /**
         * 获取IP地址在子网中的偏移量
         */
        fun getOffset(ipAddress: String): Int {
            val parts = ipAddress.split(".").map { it.toInt() }
            val ipInt = (parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or (parts[2].toLong() shl 8) or parts[3].toLong()

            val networkParts = networkAddress.split(".").map { it.toInt() }
            val networkInt = (networkParts[0].toLong() shl 24) or (networkParts[1].toLong() shl 16) or (networkParts[2].toLong() shl 8) or networkParts[3].toLong()

            return (ipInt - networkInt).toInt()
        }
    }

    /**
     * 检查指定 IP 是否有可用的 SMB 服务器
     */
    private suspend fun isSMBServerAvailable(
        ip: String,
        timeoutMs: Int = 500
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, SMB_PORT), timeoutMs)
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 解析 SSDP 响应，提取设备信息
     */
    private fun parseSSDPResponse(response: String, ipAddress: String): String? {
        return try {
            // 尝试提取服务器信息
            val serverPattern = """SERVER:\s*(.+?)\r?\n""".toRegex(RegexOption.IGNORE_CASE)
            val serverMatch = serverPattern.find(response)
            val server = serverMatch?.groupValues?.get(1)?.trim()

            // 尝试提取位置信息
            val locationPattern = """LOCATION:\s*(.+?)\r?\n""".toRegex(RegexOption.IGNORE_CASE)
            val locationMatch = locationPattern.find(response)
            val location = locationMatch?.groupValues?.get(1)?.trim()

            // 尝试提取设备名称
            val friendlyNamePattern = """<friendlyName>(.+?)</friendlyName>""".toRegex()
            val friendlyNameMatch = friendlyNamePattern.find(response)
            val friendlyName = friendlyNameMatch?.groupValues?.get(1)?.trim()

            when {
                friendlyName != null -> friendlyName
                server != null && server.contains("NAS", ignoreCase = true) -> server
                location != null -> {
                    // 从 URL 中提取主机名
                    val urlPattern = """https?://([^/:]+)""".toRegex()
                    urlPattern.find(location)?.groupValues?.get(1)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 尝试通过 SMB 连接验证设备并获取共享列表
     */
    suspend fun verifySmbDevice(
        device: SmbDevice,
        user: String = "",
        password: String = "",
        port: Int = 445
    ): Result<List<String>> = withContext(Dispatchers.IO) {
        DevLog.log("SMBDiscover", "验证设备: ${device.address}, 用户: ${if (user.isEmpty()) "匿名" else user}, 端口: $port")

        runCatching {
            val auth = when {
                user.isNotEmpty() && password.isNotEmpty() -> {
                    val encUser = java.net.URLEncoder.encode(user, "UTF-8")
                    val encPass = java.net.URLEncoder.encode(password, "UTF-8")
                    "$encUser:$encPass@"
                }
                else -> ""
            }

            // 尝试列出根共享
            val url = "smb://$auth${device.address}:$port/"
            DevLog.log("SMBDiscover", "验证设备: 连接 URL - $url")

            val smbFile = jcifs.smb.SmbFile(url)

            if (!smbFile.exists()) {
                DevLog.log("SMBDiscover", "验证设备: SMB 路径不存在或无权限")
                return@runCatching emptyList()
            }

            DevLog.log("SMBDiscover", "验证设备: 连接成功，列出共享...")

            val shares = smbFile.listFiles()
                ?.filter { it.isDirectory() }
                ?.filter { !it.name.equals(".", true) && !it.name.equals("..", true) }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()

            DevLog.log("SMBDiscover", "验证设备: 发现 ${shares.size} 个共享: ${shares.joinToString(", ")}")
            shares
        }.onFailure { e ->
            DevLog.log("SMBDiscover", "验证设备: 失败 - ${e.message}")
            DevLog.log("SMBDiscover", "验证设备: 堆栈 - ${e.stackTraceToString()}")
        }
    }

    /**
     * SMB 设备信息
     */
    data class SmbDevice(
        val address: String,    // IP 地址
        val name: String? = null // 设备名称（如果能解析到）
    ) {
        val displayName: String
            get() = if (name.isNullOrEmpty()) {
                address
            } else {
                "$name ($address)"
            }
    }
}

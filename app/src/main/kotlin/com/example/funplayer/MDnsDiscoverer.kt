package com.example.funplayer

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MDnsDiscoverer(private val context: Context) {
    
    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }
    
    private var currentDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var currentServiceTypes: List<String> = emptyList()
    
    private fun stopCurrentDiscovery() {
        val listener = currentDiscoveryListener
        if (listener != null) {
            try {
                // 停止所有正在进行的发现
                currentServiceTypes.forEach { serviceType ->
                    try {
                        nsdManager.stopServiceDiscovery(listener)
                    } catch (e: Exception) {
                        // 忽略单个停止失败的错误
                    }
                }
            } catch (e: Exception) {
                // 忽略停止失败
            } finally {
                currentDiscoveryListener = null
                currentServiceTypes = emptyList()
            }
        }
    }
    
    private fun cleanupStaleListeners() {
        // 尝试清理可能存在的僵尸监听器
        try {
            // 创建一个临时监听器来测试状态
            val testListener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(regType: String) {}
                override fun onServiceFound(service: NsdServiceInfo) {}
                override fun onServiceLost(service: NsdServiceInfo) {}
                override fun onDiscoveryStopped(serviceType: String) {}
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {}
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
            }
            
            // 尝试启动和立即停止一个发现来清理状态
            try {
                nsdManager.discoverServices("_cleanup._tcp.", NsdManager.PROTOCOL_DNS_SD, testListener)
                Thread.sleep(50)
                nsdManager.stopServiceDiscovery(testListener)
                Thread.sleep(50) // 给系统时间清理
            } catch (e: Exception) {
                // 忽略清理过程中的错误
            }
        } catch (e: Exception) {
            // 忽略清理错误
        }
    }
    
    suspend fun discoverTCodeService(timeoutMs: Long = 5000): DiscoveredService? = suspendCancellableCoroutine { continuation ->
        // 清理可能存在的僵尸监听器
        cleanupStaleListeners()
        
        // 停止之前的发现
        stopCurrentDiscovery()
        
        var discoveryListener: NsdManager.DiscoveryListener? = null
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                // 发现开始，可以在这里记录日志
            }
            
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == "_tcode._tcp." || service.serviceType == "_tcode._udp.") {
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            // 解析失败
                        }
                        
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val discoveredService = DiscoveredService(
                                serviceName = serviceInfo.serviceName,
                                serviceType = serviceInfo.serviceType.removeSuffix("."),
                                host = serviceInfo.host.hostAddress ?: return,
                                port = serviceInfo.port
                            )
                            
                            // 停止发现并返回结果
                            stopCurrentDiscovery()
                            if (continuation.isActive) {
                                continuation.resume(discoveredService)
                            }
                        }
                    })
                }
            }
            
            override fun onServiceLost(service: NsdServiceInfo) {
                // 服务丢失
            }
            
            override fun onDiscoveryStopped(serviceType: String) {
                // 发现停止
            }
            
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopCurrentDiscovery()
                if (continuation.isActive) {
                    val errorMsg = when (errorCode) {
                        NsdManager.FAILURE_ALREADY_ACTIVE -> "发现服务已在运行中，请稍后再试"
                        NsdManager.FAILURE_INTERNAL_ERROR -> "内部错误"
                        NsdManager.FAILURE_MAX_LIMIT -> "达到最大发现限制"
                        else -> "未知错误: $errorCode"
                    }
                    continuation.resumeWithException(Exception("mDNS 发现失败: $errorMsg"))
                }
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                // 停止发现失败，可以在这里记录日志
            }
        }
        
        // 开始发现服务
        try {
            // 保存当前 listener 引用和服务类型
            currentDiscoveryListener = discoveryListener
            currentServiceTypes = listOf("_tcode._tcp.")
            // 先尝试发现 TCP 服务
            nsdManager.discoverServices("_tcode._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            stopCurrentDiscovery()
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
        
        // 设置超时
        GlobalScope.launch {
            delay(timeoutMs)
            if (continuation.isActive) {
                stopCurrentDiscovery()
                continuation.resume(null)
            }
        }
        
        continuation.invokeOnCancellation {
            stopCurrentDiscovery()
        }
    }
    
    suspend fun discoverAllTCodeServices(timeoutMs: Long = 5000): List<DiscoveredService> = suspendCancellableCoroutine { continuation ->
        // 清理可能存在的僵尸监听器
        cleanupStaleListeners()
        
        // 停止之前的发现
        stopCurrentDiscovery()
        
        val discoveredServices = mutableListOf<DiscoveredService>()
        var resolveCount = 0
        var discoveryListener: NsdManager.DiscoveryListener? = null
        
        fun checkCompletion() {
            if (resolveCount == 0) {
                GlobalScope.launch {
                    delay(500) // 等待一小段时间以确保发现完成
                    stopCurrentDiscovery()
                }
            }
        }
        
        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                // 发现开始
            }
            
            override fun onServiceFound(service: NsdServiceInfo) {
                if (service.serviceType == "_tcode._tcp." || service.serviceType == "_tcode._udp.") {
                    resolveCount++
                    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            resolveCount--
                            checkCompletion()
                        }
                        
                        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                            val discoveredService = DiscoveredService(
                                serviceName = serviceInfo.serviceName,
                                serviceType = serviceInfo.serviceType.removeSuffix("."),
                                host = serviceInfo.host.hostAddress ?: return,
                                port = serviceInfo.port
                            )
                            
                            synchronized(discoveredServices) {
                                if (!discoveredServices.any { it.host == discoveredService.host && it.port == discoveredService.port }) {
                                    discoveredServices.add(discoveredService)
                                }
                            }
                            
                            resolveCount--
                            checkCompletion()
                        }
                    })
                }
            }
            
            override fun onServiceLost(service: NsdServiceInfo) {
                // 服务丢失
            }
            
            override fun onDiscoveryStopped(serviceType: String) {
                if (continuation.isActive) {
                    continuation.resume(discoveredServices.toList())
                }
            }
            
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                stopCurrentDiscovery()
                if (continuation.isActive) {
                    continuation.resumeWithException(Exception("Failed to start discovery: $errorCode"))
                }
            }
            
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                // 停止发现失败
            }
        }
        
        // 同时发现 TCP 和 UDP 服务
        try {
            // 保存当前 listener 引用和服务类型
            currentDiscoveryListener = discoveryListener
            currentServiceTypes = listOf("_tcode._tcp.", "_tcode._udp.")
            
            // 分别启动 TCP 和 UDP 发现，避免同时启动导致冲突
            try {
                nsdManager.discoverServices("_tcode._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
                Thread.sleep(100) // 添加小延迟避免冲突
                nsdManager.discoverServices("_tcode._udp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            } catch (e: Exception) {
                stopCurrentDiscovery()
                throw e
            }
        } catch (e: Exception) {
            stopCurrentDiscovery()
            if (continuation.isActive) {
                continuation.resumeWithException(e)
            }
        }
        
        // 设置超时
        GlobalScope.launch {
            delay(timeoutMs)
            if (continuation.isActive) {
                stopCurrentDiscovery()
                continuation.resume(discoveredServices.toList())
            }
        }
        
        continuation.invokeOnCancellation {
            stopCurrentDiscovery()
        }
    }
    
    data class DiscoveredService(
        val serviceName: String,
        val serviceType: String,
        val host: String,
        val port: Int
    )
}
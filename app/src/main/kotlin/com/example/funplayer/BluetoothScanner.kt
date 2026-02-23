package com.example.funplayer

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.location.LocationManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class BluetoothScanner(private val context: Context) {

    private var handyScanning = false
    private var tcodeBLEScanning = false
    private var currentLeScanCallback: BluetoothAdapter.LeScanCallback? = null

    fun isHandyScanning() = handyScanning
    fun isTcodeBLEScanning() = tcodeBLEScanning

    fun stopTcodeBLEScan() {
        currentLeScanCallback?.let { callback ->
            @Suppress("DEPRECATION")
            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            try {
                bluetoothAdapter?.stopLeScan(callback)
                DevLog.log("TcodeBLE", "手动停止扫描")
            } catch (e: Exception) {
                DevLog.log("TcodeBLE", "停止扫描失败: ${e.message}")
            }
        }
        currentLeScanCallback = null
        tcodeBLEScanning = false
    }
    
    suspend fun scanHandyDevices(
        onDeviceFound: (String, String) -> Unit,
        onScanStart: () -> Unit = {},
        onScanEnd: () -> Unit = {}
    ) {
        if (handyScanning) return
        
        if (!hasBluetoothPermission()) {
            onScanEnd()
            return
        }
        
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val scanner = manager?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            onScanEnd()
            return
        }
        
        handyScanning = true
        onScanStart()
        
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val dev = result.device ?: return
                val address = dev.address ?: return
                val name = (dev.name ?: result.scanRecord?.deviceName ?: "").trim()
                if (name.isEmpty()) return
                
                onDeviceFound(name, address)
            }
        }
        
        try {
            scanner.startScan(null, settings, callback)
            delay(10_000L)
        } catch (_: Exception) {
            // ignore
        } finally {
            try { scanner.stopScan(callback) } catch (_: Exception) { }
            handyScanning = false
            onScanEnd()
        }
    }
    
    suspend fun scanTcodeBLEDevices(
        onDeviceFound: (String, String, Int) -> Unit,
        onScanStart: () -> Unit = {},
        onScanEnd: () -> Unit = {}
    ) {
        if (tcodeBLEScanning) return

        if (!hasBluetoothPermission()) {
            onScanEnd()
            return
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED) {
            DevLog.log("TcodeBLE", "需要定位权限才能扫描 BLE")
            onScanEnd()
            return
        }

        @Suppress("DEPRECATION")
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            DevLog.log("TcodeBLE", "设备不支持蓝牙")
            onScanEnd()
            return
        }

        if (!bluetoothAdapter.isEnabled) {
            DevLog.log("TcodeBLE", "请先打开蓝牙")
            onScanEnd()
            return
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        val locationOn = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        if (!locationOn) {
            DevLog.log("TcodeBLE", "请打开系统定位（GPS 或网络位置）后再扫描")
            Toast.makeText(context, context.getString(R.string.ble_need_location_hint), Toast.LENGTH_LONG).show()
        }

        tcodeBLEScanning = true
        onScanStart()

        val leScanCallback = @Suppress("DEPRECATION", "MissingPermission")
        object : LeScanCallback {
            override fun onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: ByteArray?) {
                // 检查是否还在扫描
                if (!tcodeBLEScanning) return

                try {
                    val name = device.name?.trim() ?: ""
                    val mac = device.address ?: return
                    if (mac.isEmpty() || name.isEmpty()) return

                    DevLog.log("TcodeBLE", "BLE 发现: $name rssi=$rssi")
                    onDeviceFound(name, mac, rssi)
                } catch (e: Throwable) {
                    DevLog.log("TcodeBLE", "LeScanCallback: ${e.message}")
                }
            }
        }

        currentLeScanCallback = leScanCallback

        @Suppress("DEPRECATION")
        val started = bluetoothAdapter.startLeScan(leScanCallback)
        DevLog.log("TcodeBLE", if (started) "开始 BLE 扫描..." else "startLeScan 返回 false")

        try {
            delay(12_000L)
        } finally {
            // 只有当仍然在扫描时才停止（避免重复停止）
            if (tcodeBLEScanning && currentLeScanCallback == leScanCallback) {
                @Suppress("DEPRECATION")
                bluetoothAdapter.stopLeScan(leScanCallback)
                currentLeScanCallback = null
                tcodeBLEScanning = false
                DevLog.log("TcodeBLE", "扫描结束")
                onScanEnd()
            }
        }
    }
    
    fun getBondedDevices(): List<Pair<String, String>> {
        if (Build.VERSION.SDK_INT >= 31 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != 
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bonded = manager?.adapter?.bondedDevices?.toList().orEmpty()
        
        return bonded
            .mapNotNull { dev ->
                val addr = dev.address ?: return@mapNotNull null
                val name = dev.name?.trim()?.takeIf { it.isNotEmpty() }
                    ?: context.getString(R.string.bluetooth_device)
                name to addr
            }
            .distinctBy { it.second }
            .sortedBy { it.first }
    }
    
    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 31) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == 
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
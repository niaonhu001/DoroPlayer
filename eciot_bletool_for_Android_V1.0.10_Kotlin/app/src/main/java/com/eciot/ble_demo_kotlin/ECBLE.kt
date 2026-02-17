package com.eciot.ble_demo_kotlin

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import pub.devrel.easypermissions.EasyPermissions

object ECBLE {
    private const val ECBLEChineseTypeUTF8 = "utf8"
    private const val ECBLEChineseTypeGBK = "gbk"
    private var ecBLEChineseType = ECBLEChineseTypeGBK
    fun setChineseTypeUTF8() {
        ecBLEChineseType = ECBLEChineseTypeUTF8
    }

    fun setChineseTypeGBK() {
        ecBLEChineseType = ECBLEChineseTypeGBK
    }

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var ecBluetoothAdapterStateChangeCallback: (ok: Boolean, errCode: Int, errMsg: String) -> Unit =
        { _, _, _ -> }

    fun onBluetoothAdapterStateChange(callback: (ok: Boolean, errCode: Int, errMsg: String) -> Unit) {
        ecBluetoothAdapterStateChangeCallback = callback
    }

    fun openBluetoothAdapter(ctx: AppCompatActivity) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            ecBluetoothAdapterStateChangeCallback(false, 10000, "此设备不支持蓝牙")
            return
        }
        if (!bluetoothAdapter!!.isEnabled) {
            ecBluetoothAdapterStateChangeCallback(false, 10001, "请打开设备蓝牙开关")
            return
        }
        val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!(gps || network)) {
            ecBluetoothAdapterStateChangeCallback(false, 10002, "请打开设备定位开关")
            return
        }

        var perms = arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (!EasyPermissions.hasPermissions(ctx, *perms)) {
            // 没有权限，进行权限请求
            EasyPermissions.requestPermissions(ctx, "请打开应用的定位权限", 0xECB001, *perms)
            return
        }
        //安卓12或以上，还需要蓝牙连接附近设备的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            perms = arrayOf(
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE,
                android.Manifest.permission.BLUETOOTH_CONNECT
            )
            if (!EasyPermissions.hasPermissions(ctx, *perms)) {
                // 没有蓝牙权限，进行权限请求
                EasyPermissions.requestPermissions(
                    ctx,
                    "请打开应用的蓝牙权限，允许应用使用蓝牙连接附近的设备",
                    0xECB002,
                    *perms
                )
                return
            }
        }

        ecBluetoothAdapterStateChangeCallback(true, 0, "")
        if (bluetoothGatt != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.BLUETOOTH_CONNECT
                    )
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            bluetoothGatt?.close()
        }
    }

    fun onPermissionsGranted(ctx: AppCompatActivity, requestCode: Int, perms: List<String?>) {
        if (requestCode == 0xECB001) {//获取定位权限失败
            openBluetoothAdapter(ctx)
        }
        if (requestCode == 0xECB002) {//获取蓝牙连接附近设备的权限失败
            openBluetoothAdapter(ctx)
        }
    }

    fun onPermissionsDenied(requestCode: Int, perms: List<String?>) {
        if (requestCode == 0xECB001) {//获取定位权限失败
            ecBluetoothAdapterStateChangeCallback(false, 10003, "请打开设备定位权限")
        }
        if (requestCode == 0xECB002) {//获取蓝牙连接附近设备的权限失败
            ecBluetoothAdapterStateChangeCallback(false, 10004, "请打开设备蓝牙权限")
        }
    }

    //--------------------------------------------------------------------------------------------
    private val deviceList: MutableList<BluetoothDevice> = ArrayList()
    private var scanFlag = false
    private var ecBluetoothDeviceFoundCallback: (id: String, name: String, mac: String, rssi: Int) -> Unit =
        { _, _, _, _ -> }

    fun onBluetoothDeviceFound(callback: (id: String, name: String, mac: String, rssi: Int) -> Unit) {
        ecBluetoothDeviceFoundCallback = callback
    }

    private val leScanCallback =
        LeScanCallback { bluetoothDevice: BluetoothDevice, rssi: Int, bytes: ByteArray? ->
            try {
//                Log.e("bytes",bytesToHexString(bytes))
//                if(bytes!=null) {
//                    val name2 = getBluetoothName(bytes)
//                }

                @SuppressLint("MissingPermission")
                val name = bluetoothDevice.getName()
                if (name == null || name.isEmpty()) return@LeScanCallback
                var mac = bluetoothDevice.getAddress()
                if (mac == null || mac.isEmpty()) return@LeScanCallback
                mac = mac.replace(":", "")

//                Log.e("bleDiscovery", name + "|" + mac +"|"+ rssi)
                var isExist = false
                for (tempDevice in deviceList) {
                    if (tempDevice.getAddress().replace(":", "") == mac) {
                        isExist = true
                        break
                    }
                }
                if (!isExist) {
                    deviceList.add(bluetoothDevice)
                }
                ecBluetoothDeviceFoundCallback(mac, name, mac, rssi)
            } catch (e: Throwable) {
                Log.e("LeScanCallback", "Throwable")
            }
        }

    fun startBluetoothDevicesDiscovery(ctx: Context) {
        if (!scanFlag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            if (bluetoothAdapter != null) {
                bluetoothAdapter?.startLeScan(leScanCallback)
                scanFlag = true
            }
        }
    }

    fun stopBluetoothDevicesDiscovery(ctx: Context) {
        if (scanFlag) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ActivityCompat.checkSelfPermission(
                        ctx,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
            }
            if (bluetoothAdapter != null) {
                bluetoothAdapter?.stopLeScan(leScanCallback)
                scanFlag = false
            }
        }
    }

    //--------------------------------------------------------------------------------------------
    private var bluetoothGatt: BluetoothGatt? = null
    private var connectFlag = false
    private var reconnectTime = 0
    private var ecBLEConnectionStateChangeCallback: (ok: Boolean, errCode: Int, errMsg: String) -> Unit =
        { _, _, _ -> }

    fun onBLEConnectionStateChange(callback: (ok: Boolean, errCode: Int, errMsg: String) -> Unit) {
        ecBLEConnectionStateChangeCallback = callback
    }

    private var connectCallback: (ok: Boolean, errCode: Int, errMsg: String) -> Unit =
        { _, _, _ -> }
    private var ecBLECharacteristicValueChangeCallback: (str: String, hexStr: String) -> Unit =
        { _, _ -> }

    fun onBLECharacteristicValueChange(callback: (str: String, hexStr: String) -> Unit) {
        ecBLECharacteristicValueChangeCallback = callback
    }

    private const val ecCharacteristicWriteUUID = "0000fff2-0000-1000-8000-00805f9b34fb"
    private const val ecCharacteristicNotifyUUID = "0000fff1-0000-1000-8000-00805f9b34fb"
    private var ecCharacteristicWrite: BluetoothGattCharacteristic? = null

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            Log.e("onConnectionStateChange", "status=$status|newState=$newState")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                gatt.close()
                if (connectFlag) {
                    ecBLEConnectionStateChangeCallback(
                        false, 10000, "onConnectionStateChange:$status|$newState"
                    )
                } else {
                    connectCallback(
                        false, 10000, "onConnectionStateChange:$status|$newState"
                    )
                }
                connectFlag = false
                return
            }
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
                connectCallback(true, 0, "")
                ecBLEConnectionStateChangeCallback(true, 0, "")
                connectFlag = true
                return
            }
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                gatt.close()
                if (connectFlag) {
                    ecBLEConnectionStateChangeCallback(false, 0, "")
                } else {
                    connectCallback(false, 0, "")
                }
                connectFlag = false
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.e("ble-service", "onServicesDiscovered")
            bluetoothGatt = gatt
            val bluetoothGattServices = gatt.getServices()
            Thread {
                try {
                    for (service in bluetoothGattServices) {
                        Log.e("ble-service", "UUID=" + service.uuid.toString())
                        val listGattCharacteristic = service.characteristics
                        for (characteristic in listGattCharacteristic) {
                            Log.e("ble-char", "UUID=:" + characteristic.uuid.toString())
                            //notify
//                            if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
//                                notifyBLECharacteristicValueChange(characteristic)
//                                Thread.sleep(800)
//                            }
                            //notify
                            if (characteristic.uuid.toString() == ecCharacteristicNotifyUUID) {
                                notifyBLECharacteristicValueChange(characteristic)
                            }
                            //write
                            if (characteristic.uuid.toString() == ecCharacteristicWriteUUID) {
                                ecCharacteristicWrite = characteristic
                            }
                        }
                    }
                } catch (ignored: Throwable) {
                }
            }.start()
            Thread {
                try {
                    Thread.sleep(300)
                    setMtu()
                } catch (ignored: Throwable) {
                }
            }.start()
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            super.onCharacteristicChanged(gatt, characteristic)
            val bytes = characteristic.value
            if (bytes != null) {
                var str = ""
                if (ecBLEChineseType == ECBLEChineseTypeGBK) {
                    try {
                        str = String(bytes, charset("GBK"))
                    } catch (ignored: Throwable) {
                    }
                } else {
                    str = String(bytes)
                }
                val strHex: String = bytesToHexString(bytes)
                Log.e("ble-receive", "读取成功[string]:$str")
                Log.e("ble-receive", "读取成功[hex]:$strHex")
                ecBLECharacteristicValueChangeCallback(str, strHex)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (BluetoothGatt.GATT_SUCCESS == status) {
                Log.e("BLEService", "onMtuChanged success MTU = $mtu")
            } else {
                Log.e("BLEService", "onMtuChanged fail ")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun notifyBLECharacteristicValueChange(characteristic: BluetoothGattCharacteristic) {
        val res = bluetoothGatt!!.setCharacteristicNotification(characteristic, true)
        if (!res) {
            return
        }
        for (dp in characteristic.descriptors) {
            dp.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            bluetoothGatt?.writeDescriptor(dp)
        }
    }

    @SuppressLint("MissingPermission")
    private fun setMtu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bluetoothGatt?.requestMtu(247)
        }
    }

    @SuppressLint("MissingPermission")
    fun closeBLEConnection() {
        if (bluetoothGatt != null) {
            bluetoothGatt?.disconnect()
        }
    }

    private fun _createBLEConnection(ctx: Context, id: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(
                    ctx,
                    android.Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                connectCallback(false, 10001, "permission error")
                return
            }
        }
        bluetoothGatt?.close()
        for (tempDevice in deviceList) {
            if (tempDevice.getAddress().replace(":", "") == id) {
                bluetoothGatt = tempDevice.connectGatt(ctx, false, bluetoothGattCallback)
                return
            }
        }
        connectCallback(false, 10002, "id error")
    }

    fun createBLEConnection(ctx: Context, id: String) {
        reconnectTime = 0
        connectCallback = { ok: Boolean, errCode: Int, errMsg: String ->
            Log.e("connectCallback", "$ok|$errCode|$errMsg")
            if (!ok) {
                reconnectTime += 1
                if (reconnectTime > 4) {
                    reconnectTime = 0
                    ecBLEConnectionStateChangeCallback(false, errCode, errMsg)
                } else {
                    Thread {
                        try {
                            Thread.sleep(300)
                            _createBLEConnection(ctx, id)
                        } catch (ignored: Throwable) {
                        }
                    }.start()
                }
            }
        }
        _createBLEConnection(ctx, id)
    }

    @SuppressLint("MissingPermission")
    fun writeBLECharacteristicValue(data: String, isHex: Boolean) {
        val byteArray: ByteArray = if (isHex) {
            hexStrToBytes(data)
        } else {
            if (ecBLEChineseType == ECBLEChineseTypeGBK) {
                try {
                    data.toByteArray(charset("GBK"))
                } catch (e: Throwable) {
                    return
                }
            } else {
                data.toByteArray()
            }
        }
        ecCharacteristicWrite?.setValue(byteArray)
        //设置回复形式
        ecCharacteristicWrite?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
        //开始写数据
        bluetoothGatt?.writeCharacteristic(ecCharacteristicWrite)
    }

    //--------------------------------------------------------------------------------------------

    private fun bytesToHexString(bytes: ByteArray?): String {
        if (bytes == null) return ""
        var str = ""
        for (b in bytes) {
            str += String.format("%02X", b)
        }
        return str
    }

    private fun hexStrToBytes(hexString: String): ByteArray {
        val byteArray = ByteArray(hexString.length / 2)
        for (i in byteArray.indices) {
            val high =
                (Character.digit(hexString[2 * i], 16) and 0xf).toByte()
            val low =
                (Character.digit(hexString[2 * i + 1], 16) and 0xf).toByte()
            byteArray[i] = (high * 16 + low).toByte()
        }
        return byteArray
    }

    private fun getBluetoothName(bytes: ByteArray): String {
        var i = 0
        while (i < 62) {
            if (i >= bytes.size) return ""
            val tempLen = bytes[i].toInt()
            val tempType = bytes[i + 1].toInt()
            if (tempLen == 0 || tempLen > 30) {
                return ""
            }
            if (tempType == 9 || tempType == 8) {
                val nameBytes = ByteArray(tempLen - 1)
                System.arraycopy(bytes, i + 2, nameBytes, 0, tempLen - 1)
                return String(nameBytes)
            }
            i += tempLen
            i++
        }
        return ""
    }
}
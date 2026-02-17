package com.eciot.ble_demo_kotlin

import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    class DeviceInfo(var id: String, var name: String, var mac: String, var rssi: Int)

    class Adapter(
        context: Context,
        private val myResource: Int,
        deviceListData: List<DeviceInfo>
    ) :
        ArrayAdapter<DeviceInfo>(context, myResource, deviceListData) {
        @SuppressLint("DefaultLocale")
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val deviceInfo: DeviceInfo? = getItem(position)
            val name = deviceInfo?.name ?: ""
            val rssi = deviceInfo?.rssi ?: 0

            @SuppressLint("ViewHolder")
            val view: View = LayoutInflater.from(context).inflate(myResource, parent, false)
            val headImg = view.findViewById<ImageView>(R.id.iv_type)
            if (name.isEmpty()) {
                headImg.setImageResource(R.drawable.ble)
            } else if (name.startsWith("@") && name.length == 11 || name.startsWith("BT_") && name.length == 15) {
                headImg.setImageResource(R.drawable.ecble)
            } else {
                headImg.setImageResource(R.drawable.ble)
            }
            view.findViewById<TextView>(R.id.tv_name).text = name
            view.findViewById<TextView>(R.id.tv_rssi).text = String.format("%d", rssi)
            val rssiImg = view.findViewById<ImageView>(R.id.iv_rssi)
            if (rssi >= -41)
                rssiImg.setImageResource(R.drawable.s5)
            else if (rssi >= -55)
                rssiImg.setImageResource(R.drawable.s4)
            else if (rssi >= -65)
                rssiImg.setImageResource(R.drawable.s3)
            else if (rssi >= -75)
                rssiImg.setImageResource(R.drawable.s2)
            else
                rssiImg.setImageResource(R.drawable.s1)
            return view
        }
    }

    private var deviceListData: MutableList<DeviceInfo> = ArrayList()
    private var deviceListDataShow: MutableList<DeviceInfo> = ArrayList()
    private lateinit var listViewAdapter: Adapter
    private lateinit var connectDialog: ProgressDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.e("onCreate","onCreate")
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        uiInit()

    }

    override fun onStart() {
        super.onStart()
        Log.e("onStart","onStart")
        deviceListData.clear()
        deviceListDataShow.clear()
        listViewAdapter.notifyDataSetChanged()
        openBluetoothAdapter()
    }

    override fun onStop() {
        super.onStop()
        Log.e("onStop", "onStop")
//        ECBLE.stopBluetoothDevicesDiscovery(this)
    }

    private fun uiInit() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.statusBarColor = 0xFF01a4ef.toInt()
        }
        val windowInsetsController = ViewCompat.getWindowInsetsController(window.decorView)
        windowInsetsController?.isAppearanceLightStatusBars = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.navigationBarColor = 0xFFFFFFFF.toInt()
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false)
        }

        val swipeRefreshLayout = findViewById<SwipeRefreshLayout>(R.id.swipe_layout)
        swipeRefreshLayout.setColorSchemeColors(0x01a4ef)
        swipeRefreshLayout.setOnRefreshListener {
            deviceListData.clear()
            deviceListDataShow.clear()
            listViewAdapter.notifyDataSetChanged()
            Handler().postDelayed({
                swipeRefreshLayout.isRefreshing = false
                //权限
                openBluetoothAdapter()
            }, 1000)
        }
        val listView = findViewById<ListView>(R.id.list_view)
        listViewAdapter = Adapter(this, R.layout.list_item, deviceListDataShow)
        listView.setAdapter(listViewAdapter)
        listView.onItemClickListener =
            OnItemClickListener { adapterView: AdapterView<*>?, view: View?, i: Int, l: Long ->
                showConnectDialog()
                val deviceInfo = listView.getItemAtPosition(i) as DeviceInfo
                ECBLE.onBLEConnectionStateChange { ok: Boolean, errCode: Int, errMsg: String ->
                    runOnUiThread {
                        hideConnectDialog()
                        if (ok) {
//                            ECBLE.stopBluetoothDevicesDiscovery(this)
                            startActivities(
                                arrayOf<Intent>(
                                    Intent().setClass(this, DeviceActivity::class.java)
                                )
                            )
                            overridePendingTransition(R.anim.jump_enter_anim, R.anim.jump_exit_anim)
                        } else {
                            showToast("蓝牙连接失败,errCode=$errCode,errMsg=$errMsg")
                            showAlert("提示","蓝牙连接失败,errCode=$errCode,errMsg=$errMsg") {}
                        }
                    }
                }
                ECBLE.createBLEConnection(this, deviceInfo.id)
            }
        listRefresh()
    }

    private fun listRefresh() {
        Handler().postDelayed({
            deviceListDataShow.clear()
            for (tempDevice in deviceListData) {
                deviceListDataShow.add(tempDevice)
            }
            listViewAdapter.notifyDataSetChanged()
            listRefresh()
        }, 400)
    }

    private fun showAlert(title:String,content: String, callback: () -> Unit) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("确定") { _, _ -> callback() }
                .setCancelable(false)
                .create().show()
        }
    }

    private fun showConnectDialog() {
        runOnUiThread {
            if (!::connectDialog.isInitialized){
                connectDialog = ProgressDialog(this@MainActivity)
                connectDialog.setMessage("连接中...")
                connectDialog.setCancelable(false)
            }
            connectDialog.show()
        }
    }

    private fun hideConnectDialog() {
        connectDialog.dismiss()
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun openBluetoothAdapter() {
        ECBLE.onBluetoothAdapterStateChange { ok: Boolean, errCode: Int, errMsg: String ->
            runOnUiThread {
                if (!ok) {
                    showAlert(
                        "提示",
                        "openBluetoothAdapter error,errCode=$errCode,errMsg=$errMsg"
                    ) {
                        if (errCode == 10001) {
                            //蓝牙开关没有打开
                            startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                        }
                        if (errCode == 10002) {
                            //定位开关没有打开
                            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        }
                        if (errCode == 10003) {
                            AppSettingsDialog.Builder(this)
                                .setTitle("提示")
                                .setRationale("请打开应用的定位权限")
                                .build().show()
                        }
                        //获取蓝牙连接附近设备的权限失败
                        if (errCode == 10004) {
                            AppSettingsDialog.Builder(this)
                                .setTitle("提示")
                                .setRationale("请打开应用的蓝牙权限，允许应用使用蓝牙连接附近的设备")
                                .build().show()
                        }
                    }
                } else {
//                    showToast("openBluetoothAdapter ok")
                    Log.e("openBluetoothAdapter", "ok")
                    startBluetoothDevicesDiscovery()
                }
            }
        }
        ECBLE.openBluetoothAdapter(this)
    }

    private fun startBluetoothDevicesDiscovery() {
        ECBLE.onBluetoothDeviceFound { id: String, name: String, mac: String, rssi: Int ->
            runOnUiThread {
//            Log.e("Discovery", "$name|$mac|$rssi")
                var isExist = false
                for (tempDevice in deviceListData) {
                    if (tempDevice.id == id) {
                        tempDevice.rssi = rssi
                        tempDevice.name = name
                        isExist = true
                        break
                    }
                }
                if (!isExist) {
                    deviceListData.add(DeviceInfo(id, name, mac, rssi))
                }
            }
        }
        ECBLE.startBluetoothDevicesDiscovery(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String?>) {
//        Log.e("onPermissionsGranted", "requestCode=$requestCode")
        ECBLE.onPermissionsGranted(this,requestCode,perms)
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String?>) {
        //获取权限失败
//        Log.e("onPermissionsDenied", "requestCode=$requestCode")
//        for (perm in perms) {
//            Log.e("perm:", perm!!)
//        }
        ECBLE.onPermissionsDenied(requestCode,perms)
    }

}

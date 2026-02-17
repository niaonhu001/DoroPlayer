package com.eciot.ble_demo_kotlin

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.RadioButton
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.regex.Pattern
import java.text.SimpleDateFormat
import java.util.Date

class DeviceActivity : AppCompatActivity() {

    private lateinit var scrollView: ScrollView
    private lateinit var receiveDataTextView: TextView
    private lateinit var scrollCheckBox: CheckBox
    private lateinit var hexRevCheckBox: CheckBox
    private lateinit var hexSendCheckBox: CheckBox
    private lateinit var sendDataEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_device)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_device)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

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

        scrollView = findViewById(R.id.sv_receive)
        receiveDataTextView = findViewById(R.id.tv_receive_data)
        scrollCheckBox = findViewById(R.id.cb_scroll)
        hexRevCheckBox = findViewById(R.id.cb_hex_rev)
        hexSendCheckBox = findViewById(R.id.cb_hex_send)
        sendDataEditText = findViewById(R.id.et_send)
        findViewById<View>(R.id.iv_back).setOnClickListener {
            finish()
            overridePendingTransition(R.anim.finish_enter_anim, R.anim.finish_exit_anim)
        }
        findViewById<View>(R.id.bt_send).setOnClickListener {
            var data = sendDataEditText.getText().toString()
            if (hexSendCheckBox.isChecked) {
                //send hex
                data = data.replace(" ", "").replace("\r", "").replace("\n", "")
                if (data.isEmpty()) {
                    showAlert("提示", "请输入要发送的数据") {}
                    return@setOnClickListener
                }
                if (data.length % 2 != 0) {
                    showAlert("提示", "长度错误，长度只能是双数") {}
                    return@setOnClickListener
                }
                if (data.length > 488) {
                    showAlert("提示", "最多只能发送244字节") {}
                    return@setOnClickListener
                }
                if (!Pattern.compile("^[0-9a-fA-F]+$").matcher(data).matches()) {
                    showAlert("提示", "格式错误，只能是0-9、a-f、A-F") {}
                    return@setOnClickListener
                }
                ECBLE.writeBLECharacteristicValue(data, true)
            } else {
                //send string
                if (data.isEmpty()) {
                    showAlert("提示", "请输入要发送的数据") {}
                    return@setOnClickListener
                }
                val tempSendData = data.replace("\n", "\r\n")
                if (tempSendData.length > 244) {
                    showAlert("提示", "最多只能发送244字节") {}
                    return@setOnClickListener
                }
                ECBLE.writeBLECharacteristicValue(tempSendData, false)
            }
        }
        findViewById<View>(R.id.bt_clear).setOnClickListener {
            receiveDataTextView.text = ""
        }
        findViewById<RadioButton>(R.id.rb_utf8).setOnCheckedChangeListener { _: CompoundButton?, b: Boolean ->
            if (b) {
                ECBLE.setChineseTypeUTF8()
            } else {
                ECBLE.setChineseTypeGBK()
            }
        }
        ECBLE.setChineseTypeGBK()

        ECBLE.onBLEConnectionStateChange { _: Boolean, _: Int, _: String? ->
            runOnUiThread {
                showToast("蓝牙断开连接")
                showAlert("提示", "蓝牙断开连接") {}
            }
        }
        ECBLE.onBLECharacteristicValueChange { str: String, strHex: String ->
            runOnUiThread {
                @SuppressLint("SimpleDateFormat")
                val timeStr: String =
                    SimpleDateFormat("[HH:mm:ss,SSS]: ").format(Date(System.currentTimeMillis()))
                val nowStr = receiveDataTextView.getText().toString()
                receiveDataTextView.setTextIsSelectable(false)
                if (hexRevCheckBox.isChecked) {
                    receiveDataTextView.text = String.format("%s%s%s\r\n",nowStr,timeStr,
                        strHex.replace("(.{2})".toRegex(),"$1 "))
                } else {
                    receiveDataTextView.text = String.format("%s%s%s\r\n",nowStr,timeStr,str)
                }
                receiveDataTextView.setTextIsSelectable(true)
                if (scrollCheckBox.isChecked) {
                    scrollView.post(Runnable { scrollView.fullScroll(ScrollView.FOCUS_DOWN) })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ECBLE.onBLECharacteristicValueChange { _, _ -> }
        ECBLE.onBLEConnectionStateChange { _, _, _ -> }
        ECBLE.closeBLEConnection()
    }

    private fun showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    private fun showAlert(title: String, content: String, callback: () -> Unit) {
        runOnUiThread {
            AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton("确定") { _, _ -> callback() }
                .setCancelable(false)
                .create().show()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.finish_enter_anim, R.anim.finish_exit_anim)
    }
}

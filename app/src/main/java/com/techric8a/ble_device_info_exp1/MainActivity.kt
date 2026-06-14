package com.techric8a.ble_device_info_exp1

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.techric8a.ble_device_info_exp1.ui.theme.BLEdeviceinfoexp1Theme
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 1)
        }

        setContent {
            BLEdeviceinfoexp1Theme {
                val context = LocalContext.current
                var connectedDeviceName by remember { mutableStateOf("未接続 or 切断済み") }
                var deviceAddress by remember { mutableStateOf("---") }
                var isBluetoothEnabled by remember {
                    mutableStateOf(BluetoothAdapter.getDefaultAdapter()?.isEnabled == true)
                }
                val adapter = BluetoothAdapter.getDefaultAdapter()
                val updateInfo = {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        val bondedDevices = adapter?.bondedDevices
                        if (bondedDevices?.isNotEmpty() == true) {
                            val device = bondedDevices.first()
                            connectedDeviceName = device.name ?: "unknown"
                            deviceAddress = device.address
                        } else {
                            connectedDeviceName = "ペアリング済みデバイスなし"
                            deviceAddress = "---"
                        }
                    } else {
                        connectedDeviceName = "権限がありません"
                    }
                }


                LaunchedEffect(isBluetoothEnabled) {
                    if (isBluetoothEnabled) {
                        updateInfo()
                    } else {
                        connectedDeviceName = "未接続 (Bluetooth OFF)"
                        deviceAddress = "---"
                    }
                }

                val receiver = remember {
                    object : BroadcastReceiver() {
                        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                        override fun onReceive(context: Context?, intent: Intent?) {
                            when (intent?.action) {
                                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                                    isBluetoothEnabled = (state == BluetoothAdapter.STATE_ON)
                                }
                                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                                    connectedDeviceName = device?.name ?: "不明なデバイス"
                                    deviceAddress = device?.address ?: "---"
                                }
                                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                                    connectedDeviceName = "切断されました"
                                    deviceAddress = "---"
                                }
                            }
                        }
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        Text(text = if (isBluetoothEnabled) "Bluetoothはオンです。快適なBluetoothライフを！"
                            else "Bluetoothは現在、無効です。"
                        )
                        Text(text = "デバイス名: $connectedDeviceName")
                        Text(text = "MACアドレス: $deviceAddress")
                    }
                }

                DisposableEffect(context) {
                    val filter = IntentFilter().apply {
                        addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                        addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                        addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                    }
                    context.registerReceiver(receiver, filter)
                    onDispose {
                        context.unregisterReceiver(receiver)
                    }
                }
            }
        }
    }
}

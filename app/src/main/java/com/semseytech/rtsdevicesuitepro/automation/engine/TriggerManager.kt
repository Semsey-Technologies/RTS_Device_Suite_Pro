package com.semseytech.rtsdevicesuitepro.automation.engine

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager

class TriggerManager(private val context: Context, private val engine: AutomationEngine) {

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    val ssid = wifiManager.connectionInfo.ssid?.replace("\"", "")
                    if (ssid != null && ssid != "<unknown ssid>") {
                        engine.onTrigger("SPECIFIC_WIFI_CONNECTED", ssid)
                        engine.onTrigger("WIFI_CONNECTED", ssid)
                    } else {
                        engine.onTrigger("WIFI_CONNECTED")
                    }
                }
                Intent.ACTION_BATTERY_CHANGED -> {
                    engine.onTrigger("BATTERY_LEVEL_ABOVE")
                    engine.onTrigger("BATTERY_LEVEL_BELOW")
                }
                Intent.ACTION_SCREEN_ON -> engine.onTrigger("SCREEN_ON")
                Intent.ACTION_SCREEN_OFF -> engine.onTrigger("SCREEN_OFF")
                Intent.ACTION_POWER_CONNECTED -> engine.onTrigger("POWER_CONNECTED")
                Intent.ACTION_POWER_DISCONNECTED -> engine.onTrigger("POWER_DISCONNECTED")
                Intent.ACTION_USER_PRESENT -> engine.onTrigger("SCREEN_UNLOCKED")
                Intent.ACTION_BOOT_COMPLETED -> engine.onTrigger("BOOT_COMPLETED")
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    if (state == BluetoothAdapter.STATE_ON) {
                        engine.onTrigger("BLUETOOTH_ON")
                    }
                }
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_BOOT_COMPLETED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(receiver, filter)
    }

    fun stop() {
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }
}

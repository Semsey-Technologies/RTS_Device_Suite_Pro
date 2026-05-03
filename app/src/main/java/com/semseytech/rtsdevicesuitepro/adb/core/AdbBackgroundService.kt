package com.semseytech.rtsdevicesuitepro.adb.core

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Foreground service to maintain ADB connection.
 */
class AdbBackgroundService : Service() {
    private val TAG = "AdbBackgroundService"
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var adbManager: AdbManager
    private lateinit var adbDiscovery: AdbDiscovery
    private var isAutoReconnectEnabled = true

    override fun onCreate() {
        super.onCreate()
        adbManager = AdbManager(this)
        adbDiscovery = AdbDiscovery(this)
        startForegroundService()
        observeSettings()
        startAutoReconnect()
    }

    private fun startForegroundService() {
        val channelId = "adb_connection_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "ADB Connection",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ADB Active")
            .setContentText("Maintaining connection to local ADB...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun observeSettings() {
        serviceScope.launch {
            adbManager.isEnabled.collectLatest { enabled ->
                isAutoReconnectEnabled = enabled
                if (!enabled) {
                    adbManager.disconnect()
                    stopSelf()
                }
            }
        }
    }

    private fun startAutoReconnect() {
        serviceScope.launch {
            while (isActive) {
                if (isAutoReconnectEnabled && adbManager.getStatus() != "Connected") {
                    Log.d(TAG, "Not connected. Searching for ADB services...")
                    
                    // Use discovery to find the correct port (it changes on restart)
                    adbDiscovery.discoverServices().collectLatest { services ->
                        val connectService = services.find { !it.type.contains("pairing") }
                        if (connectService != null) {
                            Log.d(TAG, "Found ADB service at ${connectService.port}. Attempting connection...")
                            val success = adbManager.connect(connectService.host, connectService.port)
                            if (success) {
                                Log.i(TAG, "Auto-reconnected successfully.")
                                // Once connected, stop collecting until disconnected
                                throw CancellationException("Connected")
                            }
                        }
                    }
                }
                delay(10000) // Retry every 10 seconds if not connected or discovery fails
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, AdbBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

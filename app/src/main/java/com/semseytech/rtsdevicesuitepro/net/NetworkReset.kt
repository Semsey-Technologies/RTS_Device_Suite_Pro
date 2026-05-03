package com.semseytech.rtsdevicesuitepro.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class NetworkReset(private val context: Context) {
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    suspend fun performReset(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Programmatically toggle WiFi on Android 9 and below
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = false
                delay(2000)
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
                delay(3000) // Wait for reconnection
            }

            // Re-bind to the current network to force routing update
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                connectivityManager.bindProcessToNetwork(null)
                connectivityManager.bindProcessToNetwork(activeNetwork)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}

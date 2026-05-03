package com.semseytech.rtsdevicesuitepro.adb.core

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class AdbService(val name: String, val host: String, val port: Int, val type: String)

class AdbDiscovery(private val context: Context) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val multicastLock: android.net.wifi.WifiManager.MulticastLock = 
        wifiManager.createMulticastLock("AdbDiscoveryLock")
    private val TAG = "AdbDiscovery"

    private fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun discoverServices(): Flow<List<AdbService>> = callbackFlow {
        if (!isWifiConnected()) {
            Log.w(TAG, "WiFi not connected. ADB discovery might not find local services.")
        }

        val services = mutableMapOf<String, AdbService>()
        
        Log.d(TAG, "Acquiring MulticastLock...")
        multicastLock.setReferenceCounted(true)
        multicastLock.acquire()

        fun createListener() = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(regType: String) {
                Log.d(TAG, "Discovery started: $regType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Service found: name=${serviceInfo.serviceName} type=${serviceInfo.serviceType}")
                
                // Normalizing type: mDNS types usually come as "_type._protocol." or "_type._protocol"
                // We want to keep the base part for matching in the UI
                val normalizedType = serviceInfo.serviceType.removePrefix(".").removeSuffix(".")

                try {
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                            Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: $errorCode")
                            // Retry resolution once for common transient failures
                            if (errorCode == NsdManager.FAILURE_ALREADY_ACTIVE) {
                                // Busy, can't retry immediately
                            }
                        }

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo) {
                            Log.d(TAG, "Service resolved: ${resolvedInfo.serviceName} at ${resolvedInfo.host}:${resolvedInfo.port}")
                            val service = AdbService(
                                name = resolvedInfo.serviceName,
                                host = resolvedInfo.host.hostAddress ?: "127.0.0.1",
                                port = resolvedInfo.port,
                                type = normalizedType
                            )
                            services[service.name] = service
                            trySend(services.values.toList())
                        }
                    })
                } catch (e: Exception) {
                    Log.e(TAG, "Error resolving service", e)
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                services.remove(serviceInfo.serviceName)
                trySend(services.values.toList())
            }

            override fun onDiscoveryStopped(regType: String) {
                Log.d(TAG, "Discovery stopped: $regType")
            }

            override fun onStartDiscoveryFailed(regType: String, errorCode: Int) {
                Log.e(TAG, "Start discovery failed: $errorCode")
            }

            override fun onStopDiscoveryFailed(regType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: $errorCode")
            }
        }

        val connectListener = createListener()
        val pairingListener = createListener()

        // Modern Android (11+) uses these specific mDNS service types for Wireless Debugging
        nsdManager.discoverServices("_adb_secure_connect._tcp", NsdManager.PROTOCOL_DNS_SD, connectListener)
        nsdManager.discoverServices("_adb_secure_pairing._tcp", NsdManager.PROTOCOL_DNS_SD, pairingListener)

        awaitClose {
            try {
                if (multicastLock.isHeld) {
                    multicastLock.release()
                    Log.d(TAG, "Released MulticastLock")
                }
                nsdManager.stopServiceDiscovery(connectListener)
                nsdManager.stopServiceDiscovery(pairingListener)
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping discovery", e)
            }
        }
    }
}

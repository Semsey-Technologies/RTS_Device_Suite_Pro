package com.semseytech.rtsdevicesuitepro.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.telephony.TelephonyManager
import android.telephony.CellInfoLte
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoGsm
import android.telephony.CellInfoCdma
import android.telephony.CellInfoNr
import android.os.Build
import android.text.format.Formatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.*

class NetworkInfoProvider(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

    fun getNetworkInfo(): NetworkInfo {
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        val lp = connectivityManager.getLinkProperties(activeNetwork)

        var localIp = "Unknown"
        var gateway = "Unknown"
        var subnetMask = "Unknown"
        val dnsServers = mutableListOf<String>()
        var linkSpeed = 0
        var frequency = 0
        var rssi = 0
        var ssid = "Unknown"
        var bssid = "Unknown"
        var networkType = "Unknown"

        var carrierName = "Unknown"
        var cellNetworkType = "Unknown"
        var cellDbm: Int? = null

        lp?.let {
            for (addr in it.linkAddresses) {
                if (!addr.address.isLoopbackAddress && addr.address.hostAddress?.contains(":") == false) {
                    localIp = addr.address.hostAddress ?: "Unknown"
                    subnetMask = calculateSubnetMask(addr.prefixLength)
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                it.dhcpServerAddress?.let { addr -> gateway = addr.hostAddress ?: "Unknown" }
            } else {
                try {
                    val dhcp = wifiManager.dhcpInfo
                    gateway = Formatter.formatIpAddress(dhcp.gateway)
                } catch (e: SecurityException) {
                    gateway = "Permission Denied"
                }
            }

            dnsServers.addAll(it.dnsServers.map { dns -> dns.hostAddress ?: "" }.filter { s -> s.isNotEmpty() })
        }

        caps?.let {
            networkType = when {
                it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                it.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                it.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
                else -> "Unknown"
            }
            
            if (it.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                try {
                    val wifiInfo = wifiManager.connectionInfo
                    linkSpeed = wifiInfo.linkSpeed
                    frequency = wifiInfo.frequency
                    rssi = wifiInfo.rssi
                    ssid = wifiInfo.ssid.removeSurrounding("\"")
                    bssid = wifiInfo.bssid ?: "Unknown"
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Some devices might still return 0 or invalid values
                        val snrValue = wifiInfo.currentSecurityType // Just a dummy check for API 31+
                        // Real SNR is often not exposed via standard WifiInfo
                    }
                } catch (e: SecurityException) {
                    ssid = "Permission Denied"
                }
            }

            if (it.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                carrierName = telephonyManager.networkOperatorName
                cellNetworkType = getCellNetworkType()
                cellDbm = getCellSignalStrength()
            }
        }

        return NetworkInfo(
            localIp = localIp,
            gateway = gateway,
            subnetMask = subnetMask,
            dnsServers = dnsServers,
            linkSpeed = linkSpeed,
            frequency = frequency,
            rssi = rssi,
            networkType = networkType,
            ssid = ssid,
            bssid = bssid,
            isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false,
            isMetered = caps?.let { !it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } ?: false,
            isRoaming = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING) == false,
            isValidated = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
            isCaptive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) ?: false,
            channel = freqToChannel(frequency),
            carrierName = carrierName,
            cellNetworkType = cellNetworkType,
            cellDbm = cellDbm,
            mobileDataEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try { telephonyManager.isDataEnabled } catch (e: Exception) { false }
            } else false
        )
    }

    private fun getCellNetworkType(): String {
        return try {
            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_EDGE, 
                TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT, 
                TelephonyManager.NETWORK_TYPE_IDEN -> "2G"
                TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_EVDO_0, 
                TelephonyManager.NETWORK_TYPE_EVDO_A, TelephonyManager.NETWORK_TYPE_HSDPA, 
                TelephonyManager.NETWORK_TYPE_HSUPA, TelephonyManager.NETWORK_TYPE_HSPA, 
                TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD, 
                TelephonyManager.NETWORK_TYPE_HSPAP -> "3G"
                TelephonyManager.NETWORK_TYPE_LTE -> "4G (LTE)"
                TelephonyManager.NETWORK_TYPE_NR -> "5G (NR)"
                else -> "Unknown"
            }
        } catch (e: SecurityException) {
            "Permission Denied"
        }
    }

    private fun getCellSignalStrength(): Int? {
        return try {
            val cellInfoList = telephonyManager.allCellInfo
            if (cellInfoList != null) {
                for (cellInfo in cellInfoList) {
                    if (cellInfo.isRegistered) {
                        return when (cellInfo) {
                            is CellInfoLte -> cellInfo.cellSignalStrength.dbm
                            is CellInfoGsm -> cellInfo.cellSignalStrength.dbm
                            is CellInfoWcdma -> cellInfo.cellSignalStrength.dbm
                            is CellInfoCdma -> cellInfo.cellSignalStrength.dbm
                            is CellInfoNr -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    cellInfo.cellSignalStrength.dbm
                                } else null
                            }
                            else -> null
                        }
                    }
                }
            }
            null
        } catch (e: SecurityException) {
            null
        }
    }

    private fun calculateSubnetMask(prefixLength: Int): String {
        var mask = 0xffffffff.toInt() shl (32 - prefixLength)
        return String.format(
            "%d.%d.%d.%d",
            (mask shr 24) and 0xff,
            (mask shr 16) and 0xff,
            (mask shr 8) and 0xff,
            mask and 0xff
        )
    }

    suspend fun getExternalIpInfo(): Pair<String, String> = withContext(Dispatchers.IO) {
        try {
            // Using ip-api.com for both IP and Geolocation
            val response = URL("http://ip-api.com/json").readText()
            val json = JSONObject(response)
            val ip = json.getString("query")
            val city = json.optString("city", "Unknown")
            val country = json.optString("country", "Unknown")
            val isp = json.optString("isp", "Unknown")
            Pair(ip, "$city, $country ($isp)")
        } catch (e: Exception) {
            Pair("Error", "Unknown")
        }
    }

    fun getWifiScanResults(): List<WifiScanResult> {
        return try {
            wifiManager.scanResults.map {
                WifiScanResult(
                    ssid = it.SSID,
                    level = it.level,
                    frequency = it.frequency,
                    channel = freqToChannel(it.frequency)
                )
            }.sortedByDescending { it.level }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    private fun freqToChannel(freq: Int): Int {
        return when {
            freq in 2412..2484 -> (freq - 2412) / 5 + 1
            freq in 5170..5825 -> (freq - 5170) / 5 + 34
            else -> 0
        }
    }
}

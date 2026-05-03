package com.semseytech.rtsdevicesuitepro.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.math.pow

class NetworkScanner(private val context: Context) {

    data class ScannedDevice(val name: String, val ip: String)

    suspend fun scanLocalNetwork(): List<ScannedDevice> = withContext(Dispatchers.IO) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return@withContext emptyList<ScannedDevice>()
        val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return@withContext emptyList<ScannedDevice>()

        val ipv4Address = linkProperties.linkAddresses.firstOrNull { 
            it.address.hostAddress?.contains(":") == false && !it.address.isLoopbackAddress 
        } ?: return@withContext emptyList<ScannedDevice>()

        val ipBytes = ipv4Address.address.address
        val prefixLength = ipv4Address.prefixLength
        
        val subnetMask = getSubnetMask(prefixLength)
        val networkAddress = getNetworkAddress(ipBytes, subnetMask)
        val broadcastAddress = getBroadcastAddress(networkAddress, subnetMask)

        val devices = mutableListOf<ScannedDevice>()
        val jobs = mutableListOf<Job>()

        val startIp = bytesToInt(networkAddress) + 1
        val endIp = bytesToInt(broadcastAddress) - 1

        for (i in startIp..endIp) {
            val targetIp = intToBytes(i)
            val ipString = InetAddress.getByAddress(targetIp).hostAddress ?: continue
            
            // Skip self
            if (ipString == ipv4Address.address.hostAddress) continue

            jobs.add(launch {
                if (isPortOpen(ipString, 445, 200)) {
                    val hostname = try {
                        InetAddress.getByName(ipString).canonicalHostName
                    } catch (e: Exception) {
                        ipString
                    }
                    synchronized(devices) {
                        devices.add(ScannedDevice(hostname, ipString))
                    }
                }
            })
            
            // Limit parallelism to avoid overwhelming the system
            if (jobs.size >= 50) {
                jobs.joinAll()
                jobs.clear()
            }
        }
        jobs.joinAll()
        devices
    }

    private fun isPortOpen(ip: String, port: Int, timeout: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun getSubnetMask(prefixLength: Int): Int {
        return if (prefixLength == 0) 0 else (-1 shl (32 - prefixLength))
    }

    private fun getNetworkAddress(ip: ByteArray, mask: Int): ByteArray {
        val ipInt = bytesToInt(ip)
        return intToBytes(ipInt and mask)
    }

    private fun getBroadcastAddress(network: ByteArray, mask: Int): ByteArray {
        val networkInt = bytesToInt(network)
        return intToBytes(networkInt or mask.inv())
    }

    private fun bytesToInt(bytes: ByteArray): Int {
        var result = 0
        for (i in 0 until 4) {
            result = result or ((bytes[i].toInt() and 0xFF) shl (8 * (3 - i)))
        }
        return result
    }

    private fun intToBytes(value: Int): ByteArray {
        return byteArrayOf(
            (value shr 24).toByte(),
            (value shr 16).toByte(),
            (value shr 8).toByte(),
            value.toByte()
        )
    }
}

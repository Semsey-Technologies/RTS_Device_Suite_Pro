package com.semseytech.rtsdevicesuitepro.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class LatencyTester {

    suspend fun testQuality(target: String = "8.8.8.8"): QualityMetrics = withContext(Dispatchers.IO) {
        val latencies = mutableListOf<Long>()
        var successCount = 0
        val samples = 5

        for (i in 0 until samples) {
            var latency = measureTcpLatency(target, 53)
            if (latency < 0) {
                // Fallback to HTTP HEAD
                latency = measureHttpLatency("http://www.google.com")
            }
            
            if (latency > 0) {
                latencies.add(latency)
                successCount++
            }
            Thread.sleep(100)
        }

        if (latencies.isEmpty()) return@withContext QualityMetrics()

        val avg = latencies.average().toLong()
        val min = latencies.minOrNull() ?: 0
        val max = latencies.maxOrNull() ?: 0
        
        var jitterSum = 0L
        for (i in 0 until latencies.size - 1) {
            jitterSum += Math.abs(latencies[i] - latencies[i + 1])
        }
        val jitter = if (latencies.size > 1) jitterSum / (latencies.size - 1) else 0

        val packetLoss = ((samples - successCount).toFloat() / samples) * 100

        QualityMetrics(
            latencyAvg = avg,
            latencyMin = min,
            latencyMax = max,
            jitter = jitter,
            packetLoss = packetLoss,
            dnsResolutionTime = measureDnsResolution(),
            latencyHistory = latencies
        )
    }

    private fun measureTcpLatency(host: String, port: Int): Long {
        return try {
            val start = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(host, port), 2000)
            val latency = System.currentTimeMillis() - start
            socket.close()
            latency
        } catch (e: Exception) {
            -1
        }
    }

    private fun measureHttpLatency(urlStr: String): Long {
        return try {
            val start = System.currentTimeMillis()
            val url = URL(urlStr)
            val connection = url.openConnection() as HttpsURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.connect()
            val latency = System.currentTimeMillis() - start
            connection.disconnect()
            latency
        } catch (e: Exception) {
            -1
        }
    }

    private fun measureDnsResolution(): Long {
        return try {
            val start = System.currentTimeMillis()
            java.net.InetAddress.getByName("google.com")
            System.currentTimeMillis() - start
        } catch (e: Exception) {
            0
        }
    }
}

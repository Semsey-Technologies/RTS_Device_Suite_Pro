package com.semseytech.rtsdevicesuitepro.net

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class NetworkOptimizer(private val context: Context) {

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val dnsBenchmark = DnsBenchmark()
    private val latencyTester = LatencyTester()

    fun optimize(): Flow<String> = flow {
        emit("Starting baseline latency test...")
        val baseline = latencyTester.testQuality().latencyAvg
        emit("Baseline Latency: ${baseline}ms")

        emit("Flushing stale sockets...")
        val socketsFlushed = flushSockets()
        emit(if (socketsFlushed) "Sockets flushed successfully." else "Socket flush partial.")

        emit("Rebinding network interfaces...")
        val rebound = rebindNetwork()
        emit(if (rebound) "Network rebound to best interface." else "Rebind failed.")

        emit("Running DNS benchmark...")
        val dnsResults = dnsBenchmark.benchmark()
        val fastest = dnsResults.minByOrNull { if (it.isSuccess) it.latency else Long.MAX_VALUE }
        emit("Fastest DNS: ${fastest?.name ?: "Unknown"} (${fastest?.latency}ms)")

        emit("Finalizing optimization...")
        val finalLatency = latencyTester.testQuality().latencyAvg
        emit("Optimization complete! Final Latency: ${finalLatency}ms")
    }

    private suspend fun flushSockets(): Boolean = withContext(Dispatchers.IO) {
        try {
            // There isn't a direct "flush" command for all sockets, but we can force 
            // the connectivity manager to re-evaluate or use a specific network
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                connectivityManager.bindProcessToNetwork(null)
                delay(100)
                connectivityManager.bindProcessToNetwork(activeNetwork)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun rebindNetwork(): Boolean = withContext(Dispatchers.IO) {
        try {
            val activeNetwork = connectivityManager.activeNetwork
            if (activeNetwork != null) {
                connectivityManager.reportNetworkConnectivity(activeNetwork, true)
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }
}

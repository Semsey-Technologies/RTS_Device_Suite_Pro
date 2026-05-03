package com.semseytech.rtsdevicesuitepro.net.automation

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.semseytech.rtsdevicesuitepro.net.*
import kotlinx.coroutines.flow.last

class DnsBenchmarkWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto DNS Benchmark")
        val dnsBenchmark = DnsBenchmark()
        val results = dnsBenchmark.benchmark()
        val fastest = results.minByOrNull { if (it.isSuccess) it.latency else Long.MAX_VALUE }
        
        if (fastest != null && fastest.isSuccess) {
            AutomationNotificationHelper.showNotification(
                applicationContext,
                "DNS Benchmark Complete",
                "Fastest DNS found: ${fastest.name} (${fastest.latency}ms). Switch for better speed."
            )
        }
        return Result.success()
    }
}

class DnsRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto DNS Refresh")
        val optimizer = NetworkOptimizer(applicationContext)
        // Simulate DNS refresh via socket flush and rebind which is what's available
        optimizer.optimize().last() 
        return Result.success()
    }
}

class SocketFlushWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto Socket Flush")
        val optimizer = NetworkOptimizer(applicationContext)
        // The optimizer has a private flushSockets, but we can call it if we expose it or use optimize()
        // For now let's assume optimize() handles it as per previous read
        optimizer.optimize().last()
        return Result.success()
    }
}

class NetworkRebindWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto Network Rebind")
        val reset = NetworkReset(applicationContext)
        reset.performReset()
        return Result.success()
    }
}

class WifiQualityWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto WiFi Quality Monitor")
        val infoProvider = NetworkInfoProvider(applicationContext)
        val info = infoProvider.getNetworkInfo()
        if (info.networkType == "WiFi" && info.rssi < -75) {
            AutomationNotificationHelper.showNotification(
                applicationContext,
                "WiFi Quality Alert",
                "Your WiFi signal is weak (${info.rssi} dBm). Consider moving closer."
            )
        }
        return Result.success()
    }
}

class LatencyMonitorWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto Latency Monitor")
        val tester = LatencyTester()
        val metrics = tester.testQuality()
        if (metrics.latencyAvg > 150) {
            AutomationNotificationHelper.showNotification(
                applicationContext,
                "High Latency Detected",
                "Current latency is ${metrics.latencyAvg}ms. Your connection might be slow."
            )
        }
        return Result.success()
    }
}

class CaptivePortalWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto Captive Portal Detection")
        val checker = CaptivePortalChecker()
        if (checker.isCaptive()) {
            AutomationNotificationHelper.showNotification(
                applicationContext,
                "Action Required",
                "This WiFi requires a login to access the internet."
            )
        }
        return Result.success()
    }
}

class WifiResetWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        Log.d("AutomationWorker", "Running Auto WiFi Reset")
        val reset = NetworkReset(applicationContext)
        reset.performReset()
        return Result.success()
    }
}

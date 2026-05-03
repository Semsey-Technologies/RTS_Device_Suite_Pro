package com.semseytech.rtsdevicesuitepro.net.automation

import android.content.Context
import android.util.Log
import androidx.work.*
import com.semseytech.rtsdevicesuitepro.net.*
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class NetworkAutomationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    private val repository = NetworkAutomationRepository(context)
    private val optimizer = NetworkOptimizer(context)
    private val dnsBenchmark = DnsBenchmark()
    private val latencyTester = LatencyTester()
    private val captivePortalChecker = CaptivePortalChecker()
    private val infoProvider = NetworkInfoProvider(context)

    override suspend fun doWork(): Result {
        val settings = repository.automationSettings.first()
        Log.d("NetworkAutomation", "Running automated network tasks...")

        if (settings.autoDnsBenchmark) {
            runDnsBenchmark()
        }

        if (settings.autoDnsRefresh) {
            optimizer.optimize().collect { Log.d("NetworkAutomation", "DNS Refresh: $it") }
        }

        if (settings.autoWifiQualityMonitor) {
            monitorWifiQuality()
        }

        if (settings.autoLatencyMonitor) {
            val metrics = latencyTester.testQuality()
            Log.d("NetworkAutomation", "Latency Check: ${metrics.latencyAvg}ms")
        }

        if (settings.autoCaptivePortalDetection) {
            if (captivePortalChecker.isCaptive()) {
                sendNotification("Captive Portal Detected", "Please log in to your network.")
            }
        }

        return Result.success()
    }

    private suspend fun runDnsBenchmark() {
        val results = dnsBenchmark.benchmark()
        val fastest = results.minByOrNull { if (it.isSuccess) it.latency else Long.MAX_VALUE }
        Log.d("NetworkAutomation", "Fastest DNS: ${fastest?.name}")
        // notification logic could go here
    }

    private fun monitorWifiQuality() {
        val info = infoProvider.getNetworkInfo()
        if (info.networkType == "WiFi" && info.rssi < -80) {
            sendNotification("Weak WiFi Signal", "Your connection to ${info.ssid} is unstable.")
        }
    }

    private fun sendNotification(title: String, message: String) {
        // Implementation for sending system notifications
        Log.i("NetworkAutomation", "NOTIFICATION: $title - $message")
    }

    companion object {
        private const val WORK_NAME = "NetworkAutomationWork"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NetworkAutomationWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

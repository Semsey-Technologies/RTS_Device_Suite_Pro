package com.semseytech.rtsdevicesuitepro.organizer.worker

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.FileObserver
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerDatabase
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerRepository
import com.semseytech.rtsdevicesuitepro.organizer.model.RuleTrigger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.io.File

class FolderMonitorService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var observers = mutableMapOf<String, FileObserver>()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithType()
        refreshObservers()
    }

    private fun startForegroundWithType() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                startForeground(1, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(1, createNotification())
            }
        } catch (e: Exception) {
            Log.e("FolderMonitorService", "Failed to start foreground service", e)
        }
    }

    private fun refreshObservers() {
        serviceScope.launch {
            val database = OrganizerDatabase.getDatabase(applicationContext)
            val repository = OrganizerRepository(database.organizerDao())
            val rules = repository.allRules.first()

            // Stop old observers
            observers.values.forEach { it.stopWatching() }
            observers.clear()

            // Start new observers for rules with OnFolderModified trigger
            rules.filter { it.isEnabled && it.trigger is RuleTrigger.OnFolderModified }.forEach { rule ->
                rule.sourcePaths.forEach { path ->
                    if (observers.containsKey(path)) return@forEach

                    val observer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        object : FileObserver(File(path), CREATE or MOVED_TO) {
                            override fun onEvent(event: Int, pathInFolder: String?) {
                                triggerWorker()
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        object : FileObserver(path, CREATE or MOVED_TO) {
                            override fun onEvent(event: Int, pathInFolder: String?) {
                                triggerWorker()
                            }
                        }
                    }
                    observer.startWatching()
                    observers[path] = observer
                }
            }
        }
    }

    private fun triggerWorker() {
        val workRequest = OneTimeWorkRequestBuilder<FileOrganizerWorker>()
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        
        WorkManager.getInstance(applicationContext).enqueueUniqueWork(
            "FileOrganizerWork_Observer",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundWithType()

        if (intent?.action == "REFRESH") {
            refreshObservers()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "folder_monitor",
                "Folder Monitor Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, com.semseytech.rtsdevicesuitepro.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, "folder_monitor")
            .setContentTitle("Smart Organizer Active")
            .setContentText("Monitoring folders for changes...")
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        observers.values.forEach { it.stopWatching() }
        serviceScope.cancel()
    }
}

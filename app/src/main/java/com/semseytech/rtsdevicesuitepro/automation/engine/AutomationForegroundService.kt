package com.semseytech.rtsdevicesuitepro.automation.engine

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.semseytech.rtsdevicesuitepro.MainActivity
import com.semseytech.rtsdevicesuitepro.R
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

class AutomationForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val activeTasks = ConcurrentHashMap<Int, NotificationCompat.Builder>()

    companion object {
        const val ACTION_START_TASK = "START_TASK"
        const val ACTION_UPDATE_TASK = "UPDATE_TASK"
        const val ACTION_STOP_TASK = "STOP_TASK"
        const val EXTRA_TASK_ID = "TASK_ID"
        const val EXTRA_TITLE = "TITLE"
        const val EXTRA_MESSAGE = "MESSAGE"
        const val EXTRA_PROGRESS = "PROGRESS" // -1 for indeterminate, 0-100 for determinate

        fun startTask(context: Context, title: String, message: String): Int {
            val taskId = System.currentTimeMillis().toInt()
            val intent = Intent(context, AutomationForegroundService::class.java).apply {
                action = ACTION_START_TASK
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_MESSAGE, message)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("AutomationForeground", "Failed to start service task: ${e.message}")
            }
            return taskId
        }

        fun updateTask(context: Context, taskId: Int, message: String?, progress: Int = -2) {
            val intent = Intent(context, AutomationForegroundService::class.java).apply {
                action = ACTION_UPDATE_TASK
                putExtra(EXTRA_TASK_ID, taskId)
                if (message != null) putExtra(EXTRA_MESSAGE, message)
                putExtra(EXTRA_PROGRESS, progress)
            }
            context.startService(intent)
        }

        fun stopTask(context: Context, taskId: Int) {
            val intent = Intent(context, AutomationForegroundService::class.java).apply {
                action = ACTION_STOP_TASK
                putExtra(EXTRA_TASK_ID, taskId)
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TASK -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Automation Running"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Processing..."
                if (taskId != -1) startForegroundTask(taskId, title, message)
            }
            ACTION_UPDATE_TASK -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                val message = intent.getStringExtra(EXTRA_MESSAGE)
                val progress = intent.getIntExtra(EXTRA_PROGRESS, -2)
                if (taskId != -1) updateTask(taskId, message, progress)
            }
            ACTION_STOP_TASK -> {
                val taskId = intent.getIntExtra(EXTRA_TASK_ID, -1)
                if (taskId != -1) stopTask(taskId)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundTask(id: Int, title: String, message: String) {
        val notification = createNotification(title, message)
        activeTasks[id] = notification
        
        if (activeTasks.size == 1) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(id, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    @Suppress("DEPRECATION")
                    startForeground(id, notification.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
                } else {
                    startForeground(id, notification.build())
                }
            } catch (e: Exception) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                    Log.e("AutomationForeground", "Foreground service start not allowed from background: ${e.message}")
                } else {
                    Log.e("AutomationForeground", "Failed to start foreground task: ${e.message}")
                }
            }
        } else {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, notification.build())
        }
    }

    private fun updateTask(id: Int, message: String?, progress: Int) {
        val builder = activeTasks[id] ?: return
        if (message != null) builder.setContentText(message)
        if (progress >= -1) {
            builder.setProgress(100, progress.coerceAtLeast(0), progress == -1)
        }
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(id, builder.build())
    }

    private fun stopTask(id: Int) {
        activeTasks.remove(id)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(id)
        
        if (activeTasks.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun createNotification(title: String, message: String): NotificationCompat.Builder {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val channelId = when {
            title.contains("Clean", ignoreCase = true) -> "automation_cleaner"
            title.contains("Backup", ignoreCase = true) -> "automation_backup"
            else -> "automation_general"
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            
            val channels = listOf(
                NotificationChannel("automation_cleaner", "Auto Cleaner", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel("automation_backup", "Auto Backup", NotificationManager.IMPORTANCE_LOW),
                NotificationChannel("automation_general", "General Automations", NotificationManager.IMPORTANCE_LOW)
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

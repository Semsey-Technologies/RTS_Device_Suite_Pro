package com.semseytech.rtsdevicesuitepro.automation.engine

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.semseytech.rtsdevicesuitepro.MainActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*

class AutomationService : Service() {
    private lateinit var triggerManager: TriggerManager
    private lateinit var engine: AutomationEngine
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var timeCheckJob: Job? = null

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "automation_service_channel"
        private val _refreshFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val refreshFlow = _refreshFlow.asSharedFlow()
        private var staticEngine: AutomationEngine? = null

        fun getEngine(): AutomationEngine? = staticEngine

        fun requestRefresh() {
            _refreshFlow.tryEmit(Unit)
        }
        
        fun start(context: Context) {
            Log.d("AutomationService", "Starting AutomationService")
            val intent = Intent(context, AutomationService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("AutomationService", "Failed to start service intent: ${e.message}")
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, AutomationService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("AutomationService", "onCreate")
        
        createNotificationChannel()
        startForegroundInternal()
        
        engine = AutomationEngine(this)
        staticEngine = engine
        triggerManager = TriggerManager(this, engine)
        triggerManager.start()
        startTimeCheckLoop()

        serviceScope.launch {
            refreshFlow.collect {
                if (::triggerManager.isInitialized) {
                    triggerManager.refreshMonitors()
                }
            }
        }
    }

    @SuppressLint("InlinedApi")
    private fun startForegroundInternal() {
        try {
            val notification = createPersistentNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On older versions, specialUse doesn't exist, we can use a generic type or none
                // dataSync is usually a safe bet for background processing
                @Suppress("DEPRECATION")
                startForeground(
                    NOTIFICATION_ID, 
                    notification, 
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && e is android.app.ForegroundServiceStartNotAllowedException) {
                Log.e("AutomationService", "Foreground service start not allowed from background: ${e.message}")
            } else {
                Log.e("AutomationService", "Failed to start foreground service: ${e.message}")
            }
        }
    }

    private fun startTimeCheckLoop() {
        timeCheckJob?.cancel()
        timeCheckJob = serviceScope.launch {
            while (isActive) {
                Log.d("AutomationService", "Checking time-based rules...")
                engine.onTrigger("TIME_OF_DAY")
                engine.onTrigger("TIME_RANGE")
                engine.onTrigger("DAYS_OF_WEEK")
                engine.onTrigger("DAY_OF_MONTH")
                engine.onTrigger("MONTH_OF_YEAR")
                engine.onTrigger("CRON_SCHEDULE")
                
                // Sunrise/Sunset/Golden Hour estimation if no location provider
                val calendar = Calendar.getInstance()
                val hour = calendar.get(Calendar.HOUR_OF_DAY)
                val minute = calendar.get(Calendar.MINUTE)
                
                if (hour == 6 && minute == 0) engine.onTrigger("SUNRISE")
                if (hour == 18 && minute == 0) engine.onTrigger("SUNSET")
                if ((hour == 7 && minute == 0) || (hour == 17 && minute == 0)) engine.onTrigger("GOLDEN_HOUR")

                delay(60000) // Check every minute
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("AutomationService", "onStartCommand")
        startForegroundInternal()
        return START_STICKY
    }

    override fun onDestroy() {
        if (::triggerManager.isInitialized) {
            triggerManager.stop()
        }
        staticEngine = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createPersistentNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("RTS Automation Service")
            .setContentText("Monitoring triggers in background")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Automation Background Service",
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}

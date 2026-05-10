package com.semseytech.rtsdevicesuitepro.automation.engine

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.location.Location
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase
import com.semseytech.rtsdevicesuitepro.automation.data.RuleEntity
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.semseytech.rtsdevicesuitepro.cleaner.CleanerRepository
import com.semseytech.rtsdevicesuitepro.adb.core.AdbManager
import com.semseytech.rtsdevicesuitepro.backup.engine.BackupEngine
import com.semseytech.rtsdevicesuitepro.restore.engine.RestoreEngine
import com.semseytech.rtsdevicesuitepro.backup.model.BackupDestination
import com.semseytech.rtsdevicesuitepro.backup.model.BackupDestinationType
import com.semseytech.rtsdevicesuitepro.archive.model.ArchiveFormat
import android.app.Application
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

import com.semseytech.rtsdevicesuitepro.battery.monitoring.BatteryTracker
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleType

class AutomationEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private val db = AutomationDatabase.getDatabase(context)
    private val cleanerRepository = CleanerRepository(context)
    private val adbManager = AdbManager(context)
    private val backupEngine = BackupEngine(context.applicationContext as Application)
    private val restoreEngine = RestoreEngine(context.applicationContext as Application)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var tts: TextToSpeech? = null
    private val batteryTracker = BatteryTracker.getInstance(context)
    private var activeRules: List<RuleEntity> = emptyList()
    private val activeManualJobs = ConcurrentHashMap<String, Job>()

    init {
        tts = TextToSpeech(context, this)
        refreshRules()
    }

    fun refreshRules() {
        scope.launch {
            try {
                val allRules = db.automationDao().getAllRules()
                Log.d("AutomationEngine", "Fetched ${allRules.size} rules from DB.")
                allRules.forEach { 
                    Log.d("AutomationEngine", "Rule in DB: ID=${it.id}, Name=${it.name}, Enabled=${it.isEnabled}, Trigger=${it.trigger.type}")
                }
                
                val enabledRules = allRules.filter { it.isEnabled }
                
                withContext(Dispatchers.Main) {
                    activeRules = enabledRules
                    Log.d("AutomationEngine", "Rules refreshed. Cache now has ${activeRules.size} enabled rules.")
                }
            } catch (e: Exception) {
                Log.e("AutomationEngine", "Error refreshing rules: ${e.message}", e)
            }
        }
    }

    fun runRule(rule: RuleEntity) {
        Log.d("AutomationEngine", "Manually running rule: ${rule.name}")
        val job = scope.launch {
            try {
                executeActions(rule.actions)
            } catch (e: Exception) {
                Log.e("AutomationEngine", "Error running manual rule ${rule.name}: ${e.message}")
            } finally {
                Log.d("AutomationEngine", "Manual rule ${rule.name} finished.")
                activeManualJobs.remove(rule.id)
            }
        }
        activeManualJobs[rule.id] = job
    }

    fun stopRule(ruleId: String) {
        Log.d("AutomationEngine", "Stopping rule: $ruleId")
        activeManualJobs[ruleId]?.cancel()
        activeManualJobs.remove(ruleId)
    }

    fun isRuleRunning(ruleId: String): Boolean = activeManualJobs.containsKey(ruleId)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    fun onTrigger(triggerType: String, data: Any? = null) {
        val rules = activeRules.filter { it.trigger.type == triggerType }
        Log.d("AutomationEngine", "onTrigger: $triggerType. Found ${rules.size} matching active rules (Total active: ${activeRules.size})")
        
        if (rules.isEmpty()) return

        scope.launch {
            val startTime = System.currentTimeMillis()
            rules.forEach { ruleEntity ->
                Log.d("AutomationEngine", "Evaluating rule: ${ruleEntity.name}")
                val triggerMet = evaluateTriggerSpecifics(ruleEntity.trigger, data)
                Log.d("AutomationEngine", "Trigger specifics met for ${ruleEntity.name}: $triggerMet")
                
                if (triggerMet) {
                    val conditionsMet = evaluateConditions(ruleEntity.conditions)
                    Log.d("AutomationEngine", "Conditions met for ${ruleEntity.name}: $conditionsMet")
                    
                    if (conditionsMet) {
                        Log.d("AutomationEngine", "Executing actions for rule: ${ruleEntity.name}")
                        executeActions(ruleEntity.actions)
                    }
                }
            }
            val endTime = System.currentTimeMillis()
            batteryTracker.recordActivity(ModuleType.AUTOMATION, cpuTimeMs = endTime - startTime)
        }
    }

    private fun evaluateTriggerSpecifics(trigger: Trigger, data: Any?): Boolean {
        return when (trigger) {
            is Trigger.SpecificWiFiConnected -> {
                val connectedSSID = data as? String
                trigger.ssid.isEmpty() || trigger.ssid == connectedSSID
            }
            is Trigger.WiFiSignalStrength -> {
                val rssi = data as? Int ?: -100
                if (trigger.comparison == "LESS_THAN") rssi < trigger.threshold
                else rssi > trigger.threshold
            }
            is Trigger.MobileDataTypeChanged -> {
                val currentType = data as? String
                trigger.targetType == "ANY" || trigger.targetType == currentType
            }
            is Trigger.BluetoothDeviceDisconnected -> {
                val disconnectedAddress = data as? String
                trigger.address.isEmpty() || trigger.address == disconnectedAddress
            }
            is Trigger.PingStatus -> {
                val result = data as? Pair<*, *> // (host, status)
                result?.first == trigger.host && result.second == trigger.status
            }
            is Trigger.DomainReachability -> {
                val result = data as? Pair<*, *> // (domain, status)
                result?.first == trigger.domain && result.second == trigger.status
            }
            is Trigger.NetworkSpeedThreshold -> {
                val speed = data as? Int ?: 0
                if (trigger.comparison == "LESS_THAN") speed < trigger.speedMbps
                else speed > trigger.speedMbps
            }
            is Trigger.FileCreated -> {
                val fullPath = data as? String ?: ""
                fullPath.startsWith(trigger.path)
            }
            is Trigger.FileDeleted -> {
                val fullPath = data as? String ?: ""
                fullPath.startsWith(trigger.path)
            }
            is Trigger.FileModified -> {
                val fullPath = data as? String ?: ""
                fullPath.startsWith(trigger.path)
            }
            is Trigger.FolderChanged -> {
                val changedPath = data as? String ?: ""
                changedPath == trigger.path
            }
            is Trigger.ExternalStorageMounted -> true
            is Trigger.ExternalStorageUnmounted -> true
            is Trigger.DownloadCompleted -> true
            is Trigger.BatteryLevelAbove -> {
                getCurrentBatteryLevel() > trigger.level
            }
            is Trigger.BatteryLevelBelow -> {
                getCurrentBatteryLevel() < trigger.level
            }
            is Trigger.BatteryTempAbove -> {
                val currentTemp = data as? Int ?: 0
                currentTemp > trigger.temp
            }
            is Trigger.BatteryTempBelow -> {
                val currentTemp = data as? Int ?: 0
                currentTemp < trigger.temp
            }
            is Trigger.BluetoothDeviceConnected -> {
                val connectedAddress = data as? String
                trigger.address.isEmpty() || trigger.address == connectedAddress
            }
            is Trigger.AppOpened -> {
                val openedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == openedApp
            }
            is Trigger.AppClosed -> {
                val closedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == closedApp
            }
            is Trigger.AppInstalled -> {
                val installedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == installedApp
            }
            is Trigger.AppUninstalled -> {
                val uninstalledApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == uninstalledApp
            }
            is Trigger.AppUpdated -> {
                val updatedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == updatedApp
            }
            is Trigger.AppCrashed -> {
                val crashedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == crashedApp
            }
            is Trigger.ForegroundAppChanged -> true
            is Trigger.NotificationPosted -> {
                val pkg = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == pkg
            }
            is Trigger.NotificationRemoved -> {
                val pkg = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == pkg
            }
            is Trigger.NotificationMatches -> {
                val content = data as? String ?: ""
                trigger.pattern.isEmpty() || content.contains(trigger.pattern, ignoreCase = true)
            }
            is Trigger.SystemDialogOpened -> true
            is Trigger.KeyboardStateChanged -> {
                val isOpen = data as? Boolean ?: false
                trigger.opened == isOpen
            }
            is Trigger.AccessibilityEventDetected -> {
                val event = data as? String
                trigger.eventType == event
            }
            is Trigger.ToastDetected -> {
                val message = data as? String ?: ""
                trigger.message.isEmpty() || message.contains(trigger.message, ignoreCase = true)
            }
            is Trigger.OverlayPermissionChanged -> {
                val granted = data as? Boolean ?: false
                trigger.granted == granted
            }
            is Trigger.DeviceIdle -> true
            is Trigger.DaysOfWeek -> {
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                trigger.days.contains(dayOfWeek)
            }
            is Trigger.TimeRange -> {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val currentTime = currentHour * 60 + currentMinute
                val startTime = trigger.startHour * 60 + trigger.startMin
                val endTime = trigger.endHour * 60 + trigger.endMin
                if (startTime <= endTime) currentTime in startTime..endTime
                else currentTime >= startTime || currentTime <= endTime
            }
            is Trigger.DayOfMonth -> Calendar.getInstance().get(Calendar.DAY_OF_MONTH) == trigger.day
            is Trigger.MonthOfYear -> Calendar.getInstance().get(Calendar.MONTH) + 1 == trigger.month
            is Trigger.CronSchedule -> true
            is Trigger.TimerFinished, is Trigger.StopwatchThreshold, is Trigger.CountdownReached -> {
                val name = when(trigger) {
                    is Trigger.TimerFinished -> trigger.timerName
                    is Trigger.StopwatchThreshold -> trigger.stopwatchName
                    is Trigger.CountdownReached -> trigger.countdownName
                    else -> ""
                }
                data == name
            }
            is Trigger.Sunrise, is Trigger.Sunset, is Trigger.GoldenHour -> true
            is Trigger.RecurringTrigger -> true
            is Trigger.IntervalTrigger -> true
            is Trigger.StorageSizeTrigger -> {
                val stats = StatFs(Environment.getExternalStorageDirectory().path)
                val usedBytes = stats.totalBytes - stats.availableBytes
                val usedGB = usedBytes / (1024 * 1024 * 1024)
                when (trigger.comparison) {
                    "EQUALS" -> usedGB == trigger.sizeGB.toLong()
                    "MORE_THAN" -> usedGB > trigger.sizeGB
                    "LESS_THAN" -> usedGB < trigger.sizeGB
                    else -> false
                }
            }
            is Trigger.TimeOfDay -> {
                val calendar = Calendar.getInstance()
                val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
                val currentMinute = calendar.get(Calendar.MINUTE)
                val isMatch = currentHour == trigger.hour && currentMinute == trigger.minute
                Log.d("AutomationEngine", "Evaluating Time: Schedule(${trigger.hour}:${trigger.minute}) vs Current($currentHour:$currentMinute) -> Match: $isMatch")
                isMatch
            }
            is Trigger.GeofenceEnter -> {
                val location = data as? Location ?: return false
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, trigger.latitude, trigger.longitude, results)
                results[0] <= trigger.radius
            }
            is Trigger.GeofenceExit -> {
                val location = data as? Location ?: return false
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, trigger.latitude, trigger.longitude, results)
                results[0] > trigger.radius
            }
            is Trigger.ArriveAtPlace -> {
                val location = data as? Location ?: return false
                val placeCoords = getPlaceCoordinates(trigger.place)
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, placeCoords.first, placeCoords.second, results)
                results[0] <= 100 // 100m radius for places
            }
            is Trigger.LeavePlace -> {
                val location = data as? Location ?: return false
                val placeCoords = getPlaceCoordinates(trigger.place)
                val results = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, placeCoords.first, placeCoords.second, results)
                results[0] > 100
            }
            is Trigger.GpsStateChanged -> (data as? Boolean) == trigger.enabled
            is Trigger.SpeedThreshold -> {
                val speed = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") speed > trigger.speedKmh
                else speed < trigger.speedKmh
            }
            is Trigger.ActivityDetected -> (data as? String) == trigger.activity
            is Trigger.SignificantLocationChange -> data is Location
            is Trigger.CompassHeading -> {
                val heading = data as? Int ?: return false
                val diff = Math.abs(heading - trigger.degree)
                val normalizedDiff = if (diff > 180) 360 - diff else diff
                normalizedDiff <= trigger.tolerance
            }
            is Trigger.AltitudeThreshold -> {
                val altitude = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") altitude > trigger.altitudeMeters
                else altitude < trigger.altitudeMeters
            }
            is Trigger.StepCountThreshold -> (data as? Int ?: 0) >= trigger.steps
            is Trigger.ProximityTriggered -> (data as? Boolean) == trigger.near
            is Trigger.AccelerometerPattern -> (data as? String) == trigger.pattern
            is Trigger.GyroscopePattern -> (data as? String) == trigger.pattern
            is Trigger.MagnetometerThreshold -> {
                val strength = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") strength > trigger.strength
                else strength < trigger.strength
            }
            is Trigger.BarometerThreshold -> {
                val pressure = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") pressure > trigger.pressure
                else pressure < trigger.pressure
            }
            is Trigger.TemperatureThreshold -> {
                val temp = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") temp > trigger.temp
                else temp < trigger.temp
            }
            is Trigger.HumidityThreshold -> {
                val humidity = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") humidity > trigger.humidity
                else humidity < trigger.humidity
            }
            is Trigger.HeartRateThreshold -> {
                val bpm = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") bpm > trigger.bpm
                else bpm < trigger.bpm
            }
            is Trigger.AmbientNoiseThreshold -> {
                val decibels = data as? Int ?: 0
                if (trigger.comparison == "MORE_THAN") decibels > trigger.decibels
                else decibels < trigger.decibels
            }
            is Trigger.TouchGesturePattern -> (data as? String) == trigger.pattern
            is Trigger.MusicStateChanged -> (data as? String) == trigger.state
            is Trigger.AppPlayingAudio -> {
                val openedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == openedApp
            }
            is Trigger.VolumeChanged -> {
                val result = data as? Pair<*, *>
                if (result != null && result.first == trigger.streamType) {
                    val pct = result.second as? Int ?: 0
                    when (trigger.comparison) {
                        "GREATER_THAN" -> pct > trigger.threshold
                        "LESS_THAN" -> pct < trigger.threshold
                        "EQUALS" -> pct == trigger.threshold
                        else -> false
                    }
                } else false
            }
            is Trigger.RingerModeChanged -> (data as? String) == trigger.mode
            is Trigger.AudioDeviceConnected -> {
                val deviceType = data as? String
                trigger.deviceType == "ANY" || trigger.deviceType == deviceType
            }
            is Trigger.MicrophoneActivated -> true
            is Trigger.MediaMetadataChanged -> true
            is Trigger.SmsFromContact -> {
                val sender = data as? String ?: ""
                trigger.contact.isEmpty() || sender.contains(trigger.contact)
            }
            is Trigger.EmailReceived -> {
                val account = data as? String ?: ""
                trigger.account.isEmpty() || account == trigger.account
            }
            is Trigger.MessagingAppNotification -> {
                val pkg = data as? String ?: ""
                trigger.packageName.isEmpty() || pkg == trigger.packageName
            }
            is Trigger.ContactStatusChanged -> {
                val result = data as? Pair<*, *> // (name, status)
                result?.first == trigger.contact && result.second == trigger.status
            }
            is Trigger.NotificationKeyword -> {
                val result = data as? Pair<*, *> // (package, content)
                val matchesPkg = trigger.packageName.isEmpty() || result?.first == trigger.packageName
                val matchesKeyword = trigger.keyword.isEmpty() || result?.second?.toString()?.contains(trigger.keyword, ignoreCase = true) == true
                matchesPkg && matchesKeyword
            }
            is Trigger.SmsReceived, is Trigger.MmsReceived, is Trigger.IncomingCall, 
            is Trigger.CallAnswered, is Trigger.CallEnded, is Trigger.MissedCall, 
            is Trigger.VoicemailReceived -> true
            else -> true
        }
    }

    private fun getPlaceCoordinates(place: String): Pair<Double, Double> {
        return when (place) {
            "Home" -> 37.7749 to -122.4194
            "Work" -> 37.7833 to -122.4167
            else -> 0.0 to 0.0
        }
    }

    private suspend fun evaluateConditions(conditions: List<Condition>): Boolean {
        return conditions.all { condition ->
            when (condition) {
                is Condition.IsConnectedToWiFi -> {
                    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                    val activeNetwork = connectivityManager.activeNetwork
                    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
                    capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                }
                is Condition.WiFiSSIDIs -> {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                    @Suppress("DEPRECATION")
                    val info = try { wifiManager.connectionInfo } catch (e: SecurityException) { null }
                    val ssid = info?.ssid?.replace("\"", "") ?: ""
                    ssid == condition.ssid
                }
                is Condition.BatteryLevelBetween -> {
                    val level = getCurrentBatteryLevel()
                    level in condition.min..condition.max
                }
                is Condition.IsCharging -> {
                    val batteryStatus: Intent? = withContext(Dispatchers.IO) {
                        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    }
                    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                }
                is Condition.ScreenIsOn -> {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    powerManager.isInteractive
                }
                is Condition.DeviceIsLocked -> {
                    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
                    keyguardManager.isDeviceLocked
                }
                is Condition.StorageFreeAbove -> {
                    val stats = StatFs(Environment.getExternalStorageDirectory().path)
                    val freeGB = stats.availableBytes / (1024 * 1024 * 1024)
                    freeGB > condition.gb
                }
                is Condition.Unknown -> true
            }
        }
    }

    private suspend fun executeActions(actions: List<Action>) {
        for (action in actions) {
            withContext(Dispatchers.Main) {
                when (action) {
                    is Action.RunDNSBenchmark -> Log.d("AutomationEngine", "Running DNS Benchmark")
                    is Action.RefreshDNS -> Log.d("AutomationEngine", "Refreshing DNS")
                    is Action.ToggleWiFi -> {
                        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                        @Suppress("DEPRECATION")
                        wifiManager.isWifiEnabled = !wifiManager.isWifiEnabled
                    }
                    is Action.SetVolume -> {
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val targetVolume = (maxVolume * (action.volume / 100f)).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, AudioManager.FLAG_SHOW_UI)
                    }
                    is Action.Speak -> tts?.speak(action.text, TextToSpeech.QUEUE_FLUSH, null, null)
                    is Action.Vibrate -> {
                        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                            vibratorManager.defaultVibrator
                        } else {
                            @Suppress("DEPRECATION")
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        }
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(action.durationMs.toLong(), VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(action.durationMs.toLong())
                        }
                    }
                    is Action.LaunchApp -> {
                        val intent = context.packageManager.getLaunchIntentForPackage(action.packageName)
                        intent?.let {
                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(it)
                        }
                    }
                    is Action.ShowNotification -> Toast.makeText(context, "${action.title}: ${action.message}", Toast.LENGTH_SHORT).show()
                    is Action.ShowToast -> Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
                    is Action.AutoClean -> {
                        withContext(Dispatchers.IO) {
                            val taskId = AutomationForegroundService.startTask(context, "Automated Cleanup", "Initializing...")
                            try {
                                val catList = action.categories.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                cleanerRepository.performAutoClean(catList) { message, progress ->
                                    AutomationForegroundService.updateTask(context, taskId, message, progress)
                                }
                            } finally {
                                AutomationForegroundService.stopTask(context, taskId)
                            }
                        }
                    }
                    is Action.RunBackup -> {
                        withContext(Dispatchers.IO) {
                            val taskId = AutomationForegroundService.startTask(context, "Automated Backup", "Initializing...")
                            try {
                                // Simplified automated backup - uses internal storage and ZIP by default
                                val destination = BackupDestination(BackupDestinationType.INTERNAL, "Internal Storage")
                                // We don't have selectedItems here, we would need to map categories to items
                                // For now, we might need to update BackupEngine to support categories directly
                                Log.d("AutomationEngine", "RunBackup triggered with categories: ${action.categories}")
                            } finally {
                                AutomationForegroundService.stopTask(context, taskId)
                            }
                        }
                    }
                    is Action.RunRestore -> {
                        withContext(Dispatchers.IO) {
                            val taskId = AutomationForegroundService.startTask(context, "Automated Restore", "Initializing...")
                            try {
                                Log.d("AutomationEngine", "RunRestore triggered for ${action.archivePath} with categories: ${action.categories}")
                            } finally {
                                AutomationForegroundService.stopTask(context, taskId)
                            }
                        }
                    }
                    is Action.ToggleFlashlight -> {
                        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                        try {
                            adbManager.executeSafeCommand("cmd notification set_flashlight toggle") 
                        } catch (e: Exception) {
                            Log.e("AutomationEngine", "Flashlight toggle failed", e)
                        }
                    }
                    is Action.SetBrightness -> {
                        val brightnessValue = (255 * (action.level / 100f)).toInt()
                        adbManager.executeSafeCommand("settings put system screen_brightness $brightnessValue")
                    }
                    is Action.Delay -> {
                        Log.d("AutomationEngine", "Delaying for ${action.seconds}s")
                        delay(action.seconds * 1000L)
                    }
                    is Action.RunAdbCommand -> {
                        withContext(Dispatchers.IO) {
                            adbManager.executeSafeCommand(action.command)
                        }
                    }
                    is Action.Unknown -> Log.w("AutomationEngine", "Unknown action encountered, skipping")
                }
            }
        }
    }

    private fun getCurrentBatteryLevel(): Int {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level * 100 / scale.toFloat()).toInt()
    }
}

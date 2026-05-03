package com.semseytech.rtsdevicesuitepro.automation.engine

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase
import com.semseytech.rtsdevicesuitepro.automation.models.*
import com.semseytech.rtsdevicesuitepro.cleaner.CleanerRepository
import com.semseytech.rtsdevicesuitepro.adb.core.AdbManager
import kotlinx.coroutines.*
import java.util.*

import com.semseytech.rtsdevicesuitepro.battery.monitoring.BatteryTracker
import com.semseytech.rtsdevicesuitepro.battery.data.ModuleType

class AutomationEngine(private val context: Context) : TextToSpeech.OnInitListener {
    private val db = AutomationDatabase.getDatabase(context)
    private val cleanerRepository = CleanerRepository(context)
    private val adbManager = AdbManager(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    private var tts: TextToSpeech? = null
    private val batteryTracker = BatteryTracker.getInstance(context)

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
        }
    }

    fun onTrigger(triggerType: String, data: Any? = null) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            val rules = db.automationDao().getAllRules()
            rules.filter { it.isEnabled && it.trigger.type == triggerType }.forEach { ruleEntity ->
                if (evaluateTriggerSpecifics(ruleEntity.trigger, data) && evaluateConditions(ruleEntity.conditions)) {
                    executeActions(ruleEntity.actions)
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
            is Trigger.BatteryLevelAbove -> {
                getCurrentBatteryLevel() > trigger.level
            }
            is Trigger.BatteryLevelBelow -> {
                getCurrentBatteryLevel() < trigger.level
            }
            is Trigger.BluetoothDeviceConnected -> {
                val connectedAddress = data as? String
                trigger.address.isEmpty() || trigger.address == connectedAddress
            }
            is Trigger.AppOpened -> {
                val openedApp = data as? String
                trigger.packageName.isEmpty() || trigger.packageName == openedApp
            }
            is Trigger.DeviceIdle -> {
                // Implementation for idle detection
                true 
            }
            is Trigger.DaysOfWeek -> {
                val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
                trigger.days.contains(dayOfWeek)
            }
            is Trigger.RecurringTrigger -> {
                // Logic for WEEKLY, BIWEEKLY, MONTHLY etc.
                true
            }
            is Trigger.IntervalTrigger -> {
                // Logic for every X days
                true
            }
            is Trigger.StorageSizeTrigger -> {
                val stats = android.os.StatFs(Environment.getExternalStorageDirectory().path)
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
                
                // Allow a 15-minute window for periodic checks
                val scheduledMinutes = trigger.hour * 60 + trigger.minute
                val currentMinutes = currentHour * 60 + currentMinute
                
                currentMinutes in scheduledMinutes..(scheduledMinutes + 14)
            }
            else -> true
        }
    }

    private fun evaluateConditions(conditions: List<Condition>): Boolean {
        return conditions.all { condition ->
            when (condition) {
                is Condition.IsConnectedToWiFi -> true // Simplified
                is Condition.WiFiSSIDIs -> true // Simplified
                is Condition.BatteryLevelBetween -> {
                    val level = getCurrentBatteryLevel()
                    level in condition.min..condition.max
                }
                is Condition.IsCharging -> {
                    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                        context.registerReceiver(null, ifilter)
                    }
                    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                }
                is Condition.ScreenIsOn -> true // Simplified
                is Condition.DeviceIsLocked -> false // Simplified
                is Condition.StorageFreeAbove -> true // Simplified
            }
        }
    }

    private fun executeActions(actions: List<Action>) {
        scope.launch {
            for (action in actions) {
                withContext(Dispatchers.Main) {
                    when (action) {
                        is Action.RunDNSBenchmark -> Log.d("AutomationEngine", "Running DNS Benchmark")
                        is Action.RefreshDNS -> Log.d("AutomationEngine", "Refreshing DNS")
                        is Action.ToggleWiFi -> Log.d("AutomationEngine", "Toggling WiFi")
                        is Action.SetVolume -> Log.d("AutomationEngine", "Setting volume to ${action.volume}%")
                        is Action.Speak -> tts?.speak(action.text, TextToSpeech.QUEUE_FLUSH, null, null)
                        is Action.Vibrate -> Log.d("AutomationEngine", "Vibrating for ${action.durationMs}ms")
                        is Action.LaunchApp -> Log.d("AutomationEngine", "Launching app: ${action.packageName}")
                        is Action.ShowNotification -> Toast.makeText(context, "${action.title}: ${action.message}", Toast.LENGTH_SHORT).show()
                        is Action.ShowToast -> Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
                        is Action.AutoClean -> {
                            scope.launch(Dispatchers.IO) {
                                cleanerRepository.performAutoClean(action.categories)
                            }
                        }
                        is Action.ToggleFlashlight -> Log.d("AutomationEngine", "Toggling Flashlight")
                        is Action.SetBrightness -> Log.d("AutomationEngine", "Setting brightness to ${action.level}%")
                        is Action.Delay -> {
                            Log.d("AutomationEngine", "Delaying for ${action.seconds}s")
                            delay(action.seconds * 1000L)
                        }
                        is Action.RunAdbCommand -> {
                            scope.launch(Dispatchers.IO) {
                                adbManager.executeSafeCommand(action.command)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getCurrentBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return (level * 100 / scale.toFloat()).toInt()
    }
}

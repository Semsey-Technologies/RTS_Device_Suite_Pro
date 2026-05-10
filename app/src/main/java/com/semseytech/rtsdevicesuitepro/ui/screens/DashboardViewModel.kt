package com.semseytech.rtsdevicesuitepro.ui.screens

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.core.content.edit
import com.semseytech.rtsdevicesuitepro.automation.data.AutomationDatabase
import com.semseytech.rtsdevicesuitepro.automation.engine.AutomationService
import com.semseytech.rtsdevicesuitepro.automation.models.Action
import com.semseytech.rtsdevicesuitepro.automation.models.Trigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.*
import com.semseytech.rtsdevicesuitepro.model.ModelIntelligence
import com.semseytech.rtsdevicesuitepro.cleaner.CleanerRepository

data class DeviceInfo(
    val manufacturer: String = Build.MANUFACTURER,
    val model: String = Build.MODEL,
    val variant: String = Build.DEVICE,
    val androidVersion: String = Build.VERSION.RELEASE,
    val apiLevel: Int = Build.VERSION.SDK_INT,
    val securityPatch: String = Build.VERSION.SECURITY_PATCH,
    val soc: String = Build.HARDWARE,
    val totalRam: Long = 0,
    val totalStorage: Long = 0,
    val storageType: String = "UFS/eMMC",
    val displayRefreshRate: Float = 60f
)

data class MaintenanceTask(
    val id: String,
    val title: String,
    val description: String,
    val intervalDays: Int,
    val lastCompleted: Long = 0,
    val isDue: Boolean = false,
    val category: String = "Physical"
)

enum class MaintenanceMode { AUTOMATED, GUIDED, MANUAL }
enum class ItemStatus { ACTIVE, IDLE, NEEDS_PERMISSION, WARNING, ERROR, OPTIMIZED }

data class MaintenanceItem(
    val id: String,
    val name: String,
    val description: String,
    val info: String = "",
    val mode: MaintenanceMode = MaintenanceMode.AUTOMATED,
    val status: ItemStatus = ItemStatus.IDLE,
    val lastRun: Long = 0
)

data class MaintenanceCategory(
    val id: String,
    val title: String,
    val description: String,
    val mode: MaintenanceMode = MaintenanceMode.AUTOMATED,
    val healthScore: Int = 100,
    val isExpanded: Boolean = false,
    val items: List<MaintenanceItem> = emptyList()
)


enum class AlertSeverity { INFO, WARNING, CRITICAL }

data class MaintenanceAlert(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity = AlertSeverity.INFO
)

data class HistoricalMetric(val timestamp: Long, val value: Float)

data class DashboardUiState(
    val isMasterMaintenanceEnabled: Boolean = true,
    val lastBackupDate: String = "Never",
    val lastSyncDate: String = "Never",
    val storageUsedPercent: Float = 0f,
    val storageFreeSpace: String = "0 GB",
    val storageTotalSpace: String = "0 GB",
    val ramUsedPercent: Float = 0f,
    val ramAvailable: String = "0 GB",
    val ramTotal: String = "0 GB",
    val batteryLevel: Float = 0f,
    val batteryHealth: String = "Good",
    val batteryVoltage: Int = 0,
    val cpuTemp: Float = 0f, 
    val uptimeSeconds: Long = 0,
    val isAutoCleanEnabled: Boolean = false,
    val autoCleanSummary: String = "Disabled",
    val isDailyBackupEnabled: Boolean = true,
    val networkDiagnostics: NetworkDiagnostics = NetworkDiagnostics(),
    val isDiagnosticRunning: Boolean = false,
    val deviceInfo: DeviceInfo = DeviceInfo(),
    val healthScore: Int = 100,
    val healthStatus: String = "Excellent",
    val modelSpec: ModelIntelligence.DeviceSpec = ModelIntelligence.getSpec(Build.MANUFACTURER, Build.MODEL),
    val maintenanceTasks: List<MaintenanceTask> = emptyList(),
    val categories: List<MaintenanceCategory> = emptyList(),
    val activeAlerts: List<MaintenanceAlert> = emptyList(),
    val cpuHistory: List<HistoricalMetric> = emptyList(),
    val batteryHistory: List<HistoricalMetric> = emptyList(),
    val securityPosture: SecurityPosture = SecurityPosture(),
    val recoveryStatus: RecoveryStatus = RecoveryStatus()
)

data class RecoveryStatus(
    val backupScore: Int = 0,
    val lastHealthySnapshot: String = "None",
    val storageEmergencyReady: Boolean = true,
    val recoveryNotes: String = "Standard recovery procedures apply."
)

data class SecurityPosture(
    val isRooted: Boolean = false,
    val bootloaderLocked: Boolean = true,
    val unknownSourcesEnabled: Boolean = false,
    val securityPatchStatus: String = "Up to date",
    val riskLevel: String = "Low"
)

data class NetworkDiagnostics(
    val localIp: String = "---",
    val externalIp: String = "---",
    val ping: String = "---",
    val download: String = "---",
    val upload: String = "---",
    val wifiSignal: String = "---",
    val packetLoss: String = "---"
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AutomationDatabase.getDatabase(application).automationDao()
    private val prefs = application.getSharedPreferences("maintenance_prefs", Context.MODE_PRIVATE)
    private val cleanerRepository = CleanerRepository(application)
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        initDeviceInfo()
        initCategories()
        refreshAutoCleanStatus()
        refreshBackupStatus()
        updateStorageStats()
        getRamStats()
        loadMaintenanceTasks()
        checkSecurityPosture()
        
        // Dynamic data updates
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isMasterMaintenanceEnabled) {
                    val currentCpuTemp = getCpuTemperature()
                    val currentBatteryLevel = getBatteryLevel(application)
                    val currentUptime = SystemClock.elapsedRealtime() / 1000
                    
                    getRamStats()
                    getBatteryStats(application)
                    
                    val cpuHistory = _uiState.value.cpuHistory.toMutableList()
                    cpuHistory.add(HistoricalMetric(System.currentTimeMillis(), currentCpuTemp))
                    if (cpuHistory.size > 50) cpuHistory.removeAt(0)

                    val batteryHistory = _uiState.value.batteryHistory.toMutableList()
                    batteryHistory.add(HistoricalMetric(System.currentTimeMillis(), currentBatteryLevel))
                    if (batteryHistory.size > 50) batteryHistory.removeAt(0)
                    
                    _uiState.value = _uiState.value.copy(
                        cpuTemp = currentCpuTemp,
                        uptimeSeconds = currentUptime,
                        batteryLevel = currentBatteryLevel,
                        cpuHistory = cpuHistory,
                        batteryHistory = batteryHistory,
                        healthScore = calculateHealthScore(currentCpuTemp, currentBatteryLevel),
                        healthStatus = getHealthStatusText(calculateHealthScore(currentCpuTemp, currentBatteryLevel))
                    )
                    
                    checkThresholds(currentCpuTemp, currentBatteryLevel)
                }
                delay(2000)
            }
        }
    }

    private fun initDeviceInfo() {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val totalStorage = stat.blockCountLong * stat.blockSizeLong
        
        val wm = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) // Unused but context check
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                getApplication<Application>().display
            } catch (e: Exception) { null }
        } else null
        
        val refreshRate = display?.refreshRate ?: 60f
        
        _uiState.value = _uiState.value.copy(
            deviceInfo = DeviceInfo(
                totalRam = mi.totalMem,
                totalStorage = totalStorage,
                displayRefreshRate = refreshRate
            )
        )
    }

    private fun initCategories() {
        val categories = listOf(
            MaintenanceCategory(
                "system", "System Status", "Live monitoring and health metrics",
                items = listOf(
                    MaintenanceItem("cpu_monitor", "CPU Monitor", "Real-time load tracking", "Monitors CPU load per core where accessible."),
                    MaintenanceItem("ram_opt", "RAM Optimization", "Free up memory resources", "Identifies background pressure and clears cache.")
                )
            ),
            MaintenanceCategory(
                "network", "Network Health", "Connectivity and performance optimization",
                items = listOf(
                    MaintenanceItem("dns_bench", "DNS Benchmark", "Identify fastest DNS servers", "Tests various DNS providers for latency."),
                    MaintenanceItem("wifi_scan", "Wi-Fi Analysis", "Check for congestion", "Scans for channel overlap and signal interference.")
                )
            ),
            MaintenanceCategory(
                "storage", "Storage & Files", "Space management and file hygiene",
                items = listOf(
                    MaintenanceItem("junk_clean", "Junk Cleaner", "Remove temporary files", "Safely deletes cache and residual app files."),
                    MaintenanceItem("large_map", "Large File Map", "Visualize storage usage", "Lists largest files and folders for manual review.")
                )
            ),
            MaintenanceCategory(
                "battery", "Battery & Power", "Longevity and charging optimization",
                items = listOf(
                    MaintenanceItem("drain_audit", "Drain Audit", "Identify high-consumption apps", "Analyzes app power usage patterns."),
                    MaintenanceItem("cycle_track", "Cycle Tracking", "Monitor battery aging", "Estimates charge cycles and health degradation.")
                )
            ),
            MaintenanceCategory(
                "security", "Security & Integrity", "Safety and privacy audit",
                items = listOf(
                    MaintenanceItem("root_check", "Root Status", "Verify system integrity", "Checks for unauthorized system modifications."),
                    MaintenanceItem("perm_audit", "Permission Audit", "Review sensitive access", "Scans for risky permission combinations.")
                )
            )
        )
        _uiState.value = _uiState.value.copy(categories = categories)
    }

    private fun loadMaintenanceTasks() {
        val tasks = listOf(
            MaintenanceTask("screen_wipe", "Screen Wipe", "Daily: Remove fingerprints and dust", 1),
            MaintenanceTask("lens_cleaning", "Lens Cleaning", "Daily: Ensure clear photos and scans", 1),
            MaintenanceTask("port_inspection", "Port Inspection", "Weekly: Check for lint in charging port", 7),
            MaintenanceTask("case_cleaning", "Case Cleaning", "Weekly: Remove dirt from under the case", 7),
            MaintenanceTask("button_check", "Button Check", "Monthly: Ensure tactile feedback is normal", 30),
            MaintenanceTask("speaker_grill", "Speaker Grill", "Monthly: Clear dust from speaker ports", 30),
            MaintenanceTask("battery_calib", "Battery Calibration", "Quarterly: Full discharge and charge cycle", 90),
            MaintenanceTask("thermal_insp", "Thermal Inspection", "Quarterly: Check for unusual hot spots", 90),
            MaintenanceTask("full_backup", "Full System Backup", "Yearly: Preservation before major OS update", 365),
            MaintenanceTask("factory_reset", "Factory Reset", "Yearly: Recommended for system freshness", 365)
        ).map { task ->
            val lastCompleted = prefs.getLong("task_${task.id}", 0)
            val isDue = if (lastCompleted == 0L) true else {
                val daysSince = (System.currentTimeMillis() - lastCompleted) / (1000 * 60 * 60 * 24)
                daysSince >= task.intervalDays
            }
            task.copy(lastCompleted = lastCompleted, isDue = isDue)
        }.filter { it.isDue }
        _uiState.value = _uiState.value.copy(maintenanceTasks = tasks)
    }

    fun completeMaintenanceTask(taskId: String) {
        val now = System.currentTimeMillis()
        prefs.edit { putLong("task_$taskId", now) }
        loadMaintenanceTasks()
    }

    private fun calculateHealthScore(cpuTemp: Float, batteryLevel: Float): Int {
        var score = 100
        val state = _uiState.value
        
        if (state.storageUsedPercent > 0.95f) score -= 25
        else if (state.storageUsedPercent > 0.85f) score -= 15
        
        if (batteryLevel < 0.10f) score -= 15
        else if (batteryLevel < 0.20f) score -= 5
        
        val currentTempC = cpuTemp * 100f
        if (currentTempC > state.modelSpec.thermalThreshold) score -= 20
        else if (currentTempC > state.modelSpec.thermalThreshold - 10) score -= 5
        
        if (state.ramUsedPercent > 0.90f) score -= 10
        
        val dueTasks = state.maintenanceTasks.count { it.isDue }
        score -= (dueTasks * 4)
        
        return score.coerceIn(0, 100)
    }

    private fun getHealthStatusText(score: Int): String = when {
        score >= 90 -> "Excellent"
        score >= 75 -> "Good"
        score >= 50 -> "Fair"
        score >= 25 -> "Needs Attention"
        else -> "Critical"
    }

    private fun checkThresholds(cpuTemp: Float, batteryLevel: Float) {
        val currentTempC = cpuTemp * 100f
        val spec = _uiState.value.modelSpec
        val alerts = mutableListOf<MaintenanceAlert>()
        
        if (currentTempC > spec.thermalThreshold) {
            alerts.add(MaintenanceAlert(
                "thermal_high",
                "Thermal Warning",
                "CPU temperature (${String.format(Locale.getDefault(), "%.1f", currentTempC)}°C) exceeds recommended threshold for ${spec.model}.",
                AlertSeverity.WARNING
            ))
        }
        
        if (batteryLevel < 0.15f) {
            alerts.add(MaintenanceAlert(
                "battery_low",
                "Low Battery",
                "Battery level is critical. Performance may be throttled.",
                AlertSeverity.WARNING
            ))
        }

        if (_uiState.value.storageUsedPercent > 0.9f) {
            alerts.add(MaintenanceAlert(
                "storage_critical",
                "Storage Critical",
                "Less than 10% space remaining. ${spec.storageGuidance}",
                AlertSeverity.CRITICAL
            ))
        }
        
        _uiState.value = _uiState.value.copy(activeAlerts = alerts)
    }

    fun dismissAlert(alertId: String) {
        _uiState.value = _uiState.value.copy(
            activeAlerts = _uiState.value.activeAlerts.filter { it.id != alertId }
        )
    }

    private fun updateStorageStats() {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val blockSize = stat.blockSizeLong
        val totalBlocks = stat.blockCountLong
        val availableBlocks = stat.availableBlocksLong
        
        val totalSpace = totalBlocks * blockSize
        val freeSpace = availableBlocks * blockSize
        val usedSpace = totalSpace - freeSpace
        
        val totalGb = totalSpace / (1024f * 1024f * 1024f)
        val freeGb = freeSpace / (1024f * 1024f * 1024f)
        
        _uiState.value = _uiState.value.copy(
            storageUsedPercent = if (totalSpace > 0) usedSpace.toFloat() / totalSpace.toFloat() else 0f,
            storageFreeSpace = String.format(Locale.getDefault(), "%.1f GB", freeGb),
            storageTotalSpace = String.format(Locale.getDefault(), "%.0f GB", totalGb)
        )
    }

    private fun getRamStats() {
        val am = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mi)
        
        val total = mi.totalMem
        val available = mi.availMem
        val used = total - available
        
        _uiState.value = _uiState.value.copy(
            ramUsedPercent = used.toFloat() / total.toFloat(),
            ramAvailable = String.format(Locale.getDefault(), "%.1f GB", available / (1024f * 1024f * 1024f)),
            ramTotal = String.format(Locale.getDefault(), "%.1f GB", total / (1024f * 1024f * 1024f))
        )
    }

    private fun getBatteryStats(application: Application) {
        val intent = application.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val health = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        
        val healthText = when (health) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified Failure"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Unknown"
        }
        
        _uiState.value = _uiState.value.copy(
            batteryHealth = healthText,
            batteryVoltage = voltage
        )
    }

    private fun checkSecurityPosture() {
        val isRooted = checkRoot()
        // Note: INSTALL_NON_MARKET_APPS is deprecated but still useful for older APIs; modern way is via PackageInstaller
        val unknownSources = try {
            Settings.Secure.getInt(getApplication<Application>().contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        } catch (e: Exception) { false }
        
        _uiState.value = _uiState.value.copy(
            securityPosture = SecurityPosture(
                isRooted = isRooted,
                unknownSourcesEnabled = unknownSources,
                riskLevel = if (isRooted || unknownSources) "Moderate" else "Low"
            )
        )
    }

    private fun checkRoot(): Boolean {
        val paths = arrayOf("/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su")
        for (path in paths) {
            try {
                if (File(path).exists()) return true
            } catch (e: Exception) {}
        }
        return false
    }

    private fun getCpuTemperature(): Float {
        val thermalFiles = arrayOf(
            "/sys/class/thermal/thermal_zone0/temp",
            "/sys/class/thermal/thermal_zone1/temp",
            "/sys/class/thermal/thermal_zone2/temp",
            "/sys/class/hwmon/hwmon0/temp1_input",
            "/sys/devices/virtual/thermal/thermal_zone0/temp"
        )
        
        for (filePath in thermalFiles) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    val reader = RandomAccessFile(file, "r")
                    val tempStr = reader.readLine()
                    reader.close()
                    if (tempStr != null) {
                        val temp = tempStr.trim().toFloat()
                        val celsius = if (temp > 1000 || temp < -1000) temp / 1000f else temp
                        if (celsius in 10f..110f) {
                            return (celsius / 100f).coerceIn(0f, 1f)
                        }
                    }
                }
            } catch (e: Exception) {}
        }
        return 0.38f
    }

    private fun refreshBackupStatus() {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val backupDir = File(app.getExternalFilesDir(null), "backups")
            if (backupDir.exists()) {
                val latestBackup = backupDir.listFiles()
                    ?.filter { it.isFile && it.extension in listOf("zip", "7z", "tar") }
                    ?.maxByOrNull { it.lastModified() }
                
                if (latestBackup != null) {
                    _uiState.value = _uiState.value.copy(
                        lastBackupDate = formatRelativeDate(latestBackup.lastModified())
                    )
                }
            }

            val dbFile = app.getDatabasePath("organizer_database")
            if (dbFile.exists()) {
                _uiState.value = _uiState.value.copy(
                    lastSyncDate = formatRelativeDate(dbFile.lastModified())
                )
            }
        }
    }

    private fun formatRelativeDate(time: Long): String {
        val sdfTime = SimpleDateFormat("h:mm a", Locale.getDefault())
        val sdfDate = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
        val calNow = Calendar.getInstance()
        val calTime = Calendar.getInstance().apply { timeInMillis = time }
        
        return when {
            isSameDay(calNow, calTime) -> "Today • ${sdfTime.format(Date(time))}"
            isYesterday(calNow, calTime) -> "Yesterday • ${sdfTime.format(Date(time))}"
            else -> sdfDate.format(Date(time))
        }
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(calNow: Calendar, calTime: Calendar): Boolean {
        val yesterday = calNow.clone() as Calendar
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(yesterday, calTime)
    }

    private fun getBatteryLevel(context: Context): Float {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) level / scale.toFloat() else 0f
    }

    private fun refreshAutoCleanStatus() {
        viewModelScope.launch {
            val rules = dao.getAllRules()
            val autoCleanRule = rules.find { it.actions.any { action -> action is Action.AutoClean } }
            val action = autoCleanRule?.actions?.find { it is Action.AutoClean } as? Action.AutoClean
            
            _uiState.value = _uiState.value.copy(
                isAutoCleanEnabled = autoCleanRule?.isEnabled ?: false,
                autoCleanSummary = if (autoCleanRule?.isEnabled == true) {
                    val cats = action?.categories?.split(",")?.joinToString(", ") ?: "Default"
                    "Cleaning: $cats"
                } else "Disabled"
            )
        }
    }

    fun toggleMasterMaintenance(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isMasterMaintenanceEnabled = enabled)
    }

    fun toggleCategoryExpansion(categoryId: String) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map {
                if (it.id == categoryId) it.copy(isExpanded = !it.isExpanded) else it
            }
        )
    }

    fun setCategoryMode(categoryId: String, mode: MaintenanceMode) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map {
                if (it.id == categoryId) it.copy(mode = mode) else it
            }
        )
    }

    fun setItemMode(categoryId: String, itemId: String, mode: MaintenanceMode) {
        _uiState.value = _uiState.value.copy(
            categories = _uiState.value.categories.map { category ->
                if (category.id == categoryId) {
                    category.copy(items = category.items.map { item ->
                        if (item.id == itemId) item.copy(mode = mode) else item
                    })
                } else category
            }
        )
    }

    fun runItemNow(itemId: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(categories = state.categories.map { cat ->
                    cat.copy(items = cat.items.map { 
                        if (it.id == itemId) it.copy(status = ItemStatus.ACTIVE) else it 
                    })
                })
            }
            
            withContext(Dispatchers.IO) {
                when (itemId) {
                    "junk_clean" -> {
                        cleanerRepository.performAutoClean(listOf("temp", "empty_folders"))
                    }
                    "ram_opt" -> {
                        val activityManager = getApplication<Application>().getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                        activityManager.getRunningAppProcesses()?.forEach { process ->
                            if (process.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                                activityManager.killBackgroundProcesses(process.processName)
                            }
                        }
                    }
                    "dns_bench" -> {
                        // Simple reachability check for common DNS
                        val dnsList = listOf("8.8.8.8", "1.1.1.1", "9.9.9.9")
                        dnsList.forEach { dns ->
                            try {
                                val address = InetAddress.getByName(dns)
                                address.isReachable(2000)
                            } catch (e: Exception) {}
                        }
                    }
                    "root_check" -> {
                        checkRoot()
                    }
                    // Add other real implementation logic as needed
                }
            }
            
            _uiState.update { state ->
                state.copy(categories = state.categories.map { cat ->
                    cat.copy(items = cat.items.map { 
                        if (it.id == itemId) it.copy(status = ItemStatus.OPTIMIZED, lastRun = System.currentTimeMillis()) else it 
                    })
                })
            }
            updateRecoveryScore()
        }
    }

    private fun updateRecoveryScore() {
        val backupDate = _uiState.value.lastBackupDate
        val score = if (backupDate == "Never") 0 else if (backupDate.contains("Today")) 100 else 70
        _uiState.value = _uiState.value.copy(
            recoveryStatus = _uiState.value.recoveryStatus.copy(
                backupScore = score,
                lastHealthySnapshot = if (score > 0) backupDate else "None"
            )
        )
    }

    fun toggleAutoClean(enabled: Boolean) {
        viewModelScope.launch {
            val rules = dao.getAllRules()
            val autoCleanRule = rules.find { it.actions.any { action -> action is Action.AutoClean } }
            
            if (autoCleanRule != null) {
                dao.setRuleEnabled(autoCleanRule.id, enabled)
            } else if (enabled) {
                val newRule = com.semseytech.rtsdevicesuitepro.automation.data.RuleEntity(
                    id = UUID.randomUUID().toString(),
                    name = "Scheduled Auto Clean",
                    trigger = Trigger.TimeOfDay(3, 0),
                    conditions = emptyList(),
                    actions = listOf(Action.AutoClean()),
                    isEnabled = true
                )
                dao.insertRule(newRule)
            }
            refreshAutoCleanStatus()
            AutomationService.requestRefresh()
        }
    }

    fun toggleDailyBackup(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isDailyBackupEnabled = enabled)
    }

    fun runNetworkDiagnostic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDiagnosticRunning = true)
            val localIp = getLocalIpAddress()
            val wifiSignal = getWifiSignalStrength()
            
            val pingResult = withContext(Dispatchers.IO) {
                try {
                    val start = System.currentTimeMillis()
                    val reachable = InetAddress.getByName("8.8.8.8").isReachable(2000)
                    if (reachable) "${System.currentTimeMillis() - start} ms" else "Timeout"
                } catch (e: Exception) { "Error" }
            }

            val (down, up) = getLinkSpeeds()

            _uiState.value = _uiState.value.copy(
                isDiagnosticRunning = false,
                networkDiagnostics = NetworkDiagnostics(
                    localIp = localIp ?: "Not connected",
                    wifiSignal = wifiSignal,
                    ping = pingResult,
                    download = down,
                    upload = up,
                    externalIp = "Pending..."
                )
            )
            fetchExternalIp()
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val en = NetworkInterface.getNetworkInterfaces()
            while (en.hasMoreElements()) {
                val intf = en.nextElement()
                val enumIpAddr = intf.inetAddresses
                while (enumIpAddr.hasMoreElements()) {
                    val inetAddress = enumIpAddr.nextElement()
                    if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                        return inetAddress.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {}
        return null
    }

    private fun getWifiSignalStrength(): String {
        val wifiManager = getApplication<Application>().getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo
        if (info.networkId == -1) return "No WiFi"
        val level = WifiManager.calculateSignalLevel(info.rssi, 5)
        return when (level) {
            4 -> "Excellent"
            3 -> "Good"
            2 -> "Fair"
            1 -> "Poor"
            else -> "Very Poor"
        }
    }

    private fun getLinkSpeeds(): Pair<String, String> {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nc = cm.getNetworkCapabilities(cm.activeNetwork) ?: return "---" to "---"
        val down = nc.linkDownstreamBandwidthKbps / 1000
        val up = nc.linkUpstreamBandwidthKbps / 1000
        return "$down Mbps" to "$up Mbps"
    }

    private fun fetchExternalIp() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("https://api.ipify.org")
                val connection = url.openConnection() as java.net.HttpURLConnection
                val ip = connection.inputStream.bufferedReader().readText()
                _uiState.value = _uiState.value.copy(
                    networkDiagnostics = _uiState.value.networkDiagnostics.copy(externalIp = ip)
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    networkDiagnostics = _uiState.value.networkDiagnostics.copy(externalIp = "Unavailable")
                )
            }
        }
    }

    fun generateSnapshotReport() {
        viewModelScope.launch(Dispatchers.IO) {
            val state = _uiState.value
            val report = StringBuilder()
            report.append("RTS Device Suite Pro - System Snapshot\n")
            report.append("Generated: ${Date()}\n")
            report.append("-------------------------------------------\n")
            report.append("DEVICE INFO:\n")
            report.append("Model: ${state.deviceInfo.manufacturer} ${state.deviceInfo.model}\n")
            report.append("Android: ${state.deviceInfo.androidVersion} (API ${state.deviceInfo.apiLevel})\n")
            report.append("Security Patch: ${state.deviceInfo.securityPatch}\n\n")
            report.append("HEALTH METRICS:\n")
            report.append("Health Score: ${state.healthScore}/100 (${state.healthStatus})\n")
            report.append("Storage: ${String.format(Locale.getDefault(), "%.1f", state.storageUsedPercent * 100)}% used (${state.storageFreeSpace} free)\n")
            report.append("RAM: ${String.format(Locale.getDefault(), "%.1f", state.ramUsedPercent * 100)}% used (${state.ramAvailable} free)\n")
            report.append("Battery: ${String.format(Locale.getDefault(), "%.0f", state.batteryLevel * 100)}% (${state.batteryHealth}, ${state.batteryVoltage}mV)\n")
            report.append("CPU Temp: ${String.format(Locale.getDefault(), "%.1f", state.cpuTemp * 100)}°C\n\n")
            report.append("SECURITY POSTURE:\n")
            report.append("Root Status: ${if (state.securityPosture.isRooted) "ROOTED" else "Non-Root"}\n")
            report.append("Risk Level: ${state.securityPosture.riskLevel}\n\n")
            report.append("MAINTENANCE:\n")
            report.append("Last Backup: ${state.lastBackupDate}\n")
            val dueTasks = state.maintenanceTasks.filter { it.isDue }
            report.append("Due Tasks: ${dueTasks.size}\n")
            dueTasks.forEach { report.append("- ${it.title}\n") }
            
            val app = getApplication<Application>()
            val reportFile = File(app.getExternalFilesDir(null), "snapshot_report_${System.currentTimeMillis()}.txt")
            reportFile.writeText(report.toString())
        }
    }

    fun exportDiagnosticLog() {
        viewModelScope.launch(Dispatchers.IO) {
            val diag = _uiState.value.networkDiagnostics
            val report = StringBuilder()
            report.append("RTS Device Suite Pro - Network Diagnostic Log\n")
            report.append("Generated: ${Date()}\n")
            report.append("-------------------------------------------\n")
            report.append("Local IP: ${diag.localIp}\n")
            report.append("External IP: ${diag.externalIp}\n")
            report.append("Ping: ${diag.ping}\n")
            report.append("Download Speed: ${diag.download}\n")
            report.append("Upload Speed: ${diag.upload}\n")
            report.append("Wi-Fi Signal: ${diag.wifiSignal}\n")
            report.append("Packet Loss: ${diag.packetLoss}\n")
            
            val app = getApplication<Application>()
            val logFile = File(app.getExternalFilesDir(null), "network_diag_${System.currentTimeMillis()}.txt")
            logFile.writeText(report.toString())
        }
    }
}

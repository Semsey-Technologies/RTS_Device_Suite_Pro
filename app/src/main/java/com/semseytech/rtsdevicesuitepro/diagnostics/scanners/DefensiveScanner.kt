package com.semseytech.rtsdevicesuitepro.diagnostics.scanners

import android.content.Context
import android.os.Environment
import android.os.StatFs
import android.os.BatteryManager
import android.content.Intent
import android.content.IntentFilter
import com.semseytech.rtsdevicesuitepro.diagnostics.models.DiagnosticsIssue
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueCategory
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueSeverity
import java.io.File
import java.net.InetAddress
import java.util.UUID

class DefensiveScanner(private val context: Context) {

    suspend fun scan(): List<DiagnosticsIssue> {
        // Ensure terminal environment is initialized for path checks
        com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv.init(context)

        val issues = mutableListOf<DiagnosticsIssue>()
        
        checkStorage(issues)
        checkCacheIntegrity(issues)
        checkOrphanedTempFiles(issues)
        checkThermalStatus(issues)
        checkBatteryDrain(issues)
        checkDnsLatency(issues)
        checkServiceStatus(issues)
        checkDependencies(issues)
        
        return issues
    }

    private fun checkDependencies(issues: MutableList<DiagnosticsIssue>) {
        // Check Terminal Environment Integrity
        val terminalFolders = listOf("usr/bin", "usr/lib", "usr/etc", "home")
        val missingFolders = terminalFolders.filter { !File(context.filesDir, it).exists() }
        
        if (missingFolders.isNotEmpty()) {
            issues.add(DiagnosticsIssue(
                id = "def_terminal_broken",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.HIGH,
                safeToAutoFix = true,
                description = "Terminal environment structure is corrupted or missing.",
                recommendedAction = "Initialize terminal environment.",
                autoFixAction = "init_terminal",
                technicalDetails = "Missing: ${missingFolders.joinToString()}"
            ))
        }

        // Check for Core Terminal Tools
        val coreTools = listOf("busybox", "sh")
        val missingTools = coreTools.filter { !File(context.filesDir, "usr/bin/$it").exists() }
        
        if (missingTools.isNotEmpty()) {
            issues.add(DiagnosticsIssue(
                id = "def_tools_missing",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.HIGH,
                safeToAutoFix = true,
                description = "Core terminal utilities are missing or unlinked ($missingTools).",
                recommendedAction = "Initialize core terminal tools and link system shell.",
                autoFixAction = "install_core_tools",
                technicalDetails = "Missing or unlinked in usr/bin: $missingTools"
            ))
        }

        // Check for Python in Terminal Environment
        val pythonBin = File(context.filesDir, "usr/bin/python")
        if (!pythonBin.exists()) {
            issues.add(DiagnosticsIssue(
                id = "def_python_missing",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.MEDIUM,
                safeToAutoFix = true,
                description = "Python environment is missing or incomplete.",
                recommendedAction = "Install Python packages to enable advanced automation.",
                autoFixAction = "install_python",
                technicalDetails = "Binary not found: ${pythonBin.absolutePath}"
            ))
        }

        // Check for ADB/Fastboot (simulated)
        val adbFile = File("/system/bin/adb")
        if (!adbFile.exists() && !File("/system/xbin/adb").exists()) {
            issues.add(DiagnosticsIssue(
                id = "def_adb_missing",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.INFO,
                safeToAutoFix = false,
                description = "System ADB binary not found in standard paths.",
                recommendedAction = "Use the built-in ADB setup for remote management.",
                technicalDetails = "Local ADB execution may be limited."
            ))
        }
    }

    private fun checkStorage(issues: MutableList<DiagnosticsIssue>) {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val availableBytes = stat.availableBytes
        val totalBytes = stat.totalBytes
        val freePercent = (availableBytes.toDouble() / totalBytes.toDouble()) * 100

        if (freePercent < 5) {
            issues.add(DiagnosticsIssue(
                id = "def_storage_critical",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.HIGH,
                safeToAutoFix = true,
                description = "Extremely low storage space ($freePercent% free).",
                recommendedAction = "Run Junk Cleaner to free up space.",
                autoFixAction = "run_cleaner",
                technicalDetails = "Available: ${availableBytes / (1024 * 1024)} MB, Total: ${totalBytes / (1024 * 1024)} MB"
            ))
        } else if (freePercent < 15) {
            issues.add(DiagnosticsIssue(
                id = "def_storage_low",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.MEDIUM,
                safeToAutoFix = true,
                description = "Low storage space ($freePercent% free).",
                recommendedAction = "Consider cleaning up old files.",
                autoFixAction = "run_cleaner",
                technicalDetails = "Available: ${availableBytes / (1024 * 1024)} MB"
            ))
        }
    }

    private fun checkCacheIntegrity(issues: MutableList<DiagnosticsIssue>) {
        val cacheDir = context.cacheDir
        if (cacheDir.exists() && cacheDir.isDirectory) {
            val files = cacheDir.listFiles() ?: emptyArray()
            if (files.size > 1000) {
                issues.add(DiagnosticsIssue(
                    id = "def_cache_bloat",
                    category = IssueCategory.DEFENSIVE,
                    severity = IssueSeverity.LOW,
                    safeToAutoFix = true,
                    description = "Cache directory contains a large number of files (${files.size}).",
                    recommendedAction = "Clear application cache.",
                    autoFixAction = "clear_cache",
                    technicalDetails = "Path: ${cacheDir.absolutePath}, Count: ${files.size}"
                ))
            }
        }
    }

    private fun checkOrphanedTempFiles(issues: MutableList<DiagnosticsIssue>) {
        val externalCache = context.externalCacheDir
        if (externalCache != null && externalCache.exists()) {
            val tempFiles = externalCache.listFiles { file -> file.extension == "tmp" || file.name.contains("temp") } ?: emptyArray()
            if (tempFiles.isNotEmpty()) {
                issues.add(DiagnosticsIssue(
                    id = "def_temp_files",
                    category = IssueCategory.DEFENSIVE,
                    severity = IssueSeverity.LOW,
                    safeToAutoFix = true,
                    description = "Found ${tempFiles.size} orphaned temporary files.",
                    recommendedAction = "Remove temporary files.",
                    autoFixAction = "remove_temp",
                    technicalDetails = "Total size: ${tempFiles.sumOf { it.length() } / 1024} KB"
                ))
            }
        }
    }

    private fun checkThermalStatus(issues: MutableList<DiagnosticsIssue>) {
        // Simplified thermal check using common sysfs paths if readable, else info
        val thermalFile = File("/sys/class/thermal/thermal_zone0/temp")
        if (thermalFile.exists()) {
            try {
                val temp = thermalFile.readText().trim().toFloat() / 1000f
                if (temp > 45) {
                    issues.add(DiagnosticsIssue(
                        id = "def_thermal_high",
                        category = IssueCategory.DEFENSIVE,
                        severity = IssueSeverity.MEDIUM,
                        safeToAutoFix = false,
                        description = "High device temperature detected (${temp}°C).",
                        recommendedAction = "Close background apps and avoid heavy usage.",
                        technicalDetails = "Thermal Zone 0: ${temp}°C. Throttling may occur."
                    ))
                }
            } catch (e: Exception) {}
        }
    }

    private fun checkBatteryDrain(issues: MutableList<DiagnosticsIssue>) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        // This is a placeholder for actual drain analysis which requires historical data
        // For now, we flag if battery is low and not charging
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()

        if (batteryPct < 20 && !isCharging) {
            issues.add(DiagnosticsIssue(
                id = "def_battery_low",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.MEDIUM,
                safeToAutoFix = false,
                description = "Battery level is low ($batteryPct%).",
                recommendedAction = "Connect to a charger or enable power saving.",
                technicalDetails = "Current Level: $batteryPct%, Status: Discharging"
            ))
        }
    }

    private fun checkDnsLatency(issues: MutableList<DiagnosticsIssue>) {
        try {
            val start = System.currentTimeMillis()
            InetAddress.getByName("google.com")
            val latency = System.currentTimeMillis() - start
            
            if (latency > 300) {
                issues.add(DiagnosticsIssue(
                    id = "def_dns_slow",
                    category = IssueCategory.DEFENSIVE,
                    severity = IssueSeverity.LOW,
                    safeToAutoFix = true,
                    description = "High DNS latency detected ($latency ms).",
                    recommendedAction = "Switch to a faster DNS provider.",
                    autoFixAction = "optimize_dns",
                    technicalDetails = "Primary resolution time: $latency ms"
                ))
            }
        } catch (e: Exception) {}
    }

    private fun checkServiceStatus(issues: MutableList<DiagnosticsIssue>) {
        // Check if automation service is running (simulated check)
        val prefs = context.getSharedPreferences("maintenance_prefs", Context.MODE_PRIVATE)
        val selfHealingEnabled = prefs.getBoolean("self_healing_enabled", true)
        
        if (!selfHealingEnabled) {
            issues.add(DiagnosticsIssue(
                id = "def_self_healing_disabled",
                category = IssueCategory.DEFENSIVE,
                severity = IssueSeverity.MEDIUM,
                safeToAutoFix = true,
                description = "Self-Healing engine is currently disabled.",
                recommendedAction = "Enable Self-Healing for automatic maintenance.",
                autoFixAction = "enable_self_healing",
                technicalDetails = "Self-healing allows the system to fix minor issues automatically."
            ))
        }
    }
}

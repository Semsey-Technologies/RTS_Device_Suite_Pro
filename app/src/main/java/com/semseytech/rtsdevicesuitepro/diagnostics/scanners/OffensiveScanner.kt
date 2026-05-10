package com.semseytech.rtsdevicesuitepro.diagnostics.scanners

import android.content.Context
import android.provider.Settings
import com.semseytech.rtsdevicesuitepro.diagnostics.models.DiagnosticsIssue
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueCategory
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueSeverity
import java.io.File
import java.net.NetworkInterface

class OffensiveScanner(private val context: Context) {

    fun scan(): List<DiagnosticsIssue> {
        val issues = mutableListOf<DiagnosticsIssue>()
        
        checkUnknownSources(issues)
        checkAdbEnabled(issues)
        checkSuspiciousBinaries(issues)
        checkOpenPorts(issues)
        checkModifiedConfigs(issues)
        checkRootAccess(issues)
        checkLargeProcessBurst(issues)
        
        return issues
    }

    private fun checkUnknownSources(issues: MutableList<DiagnosticsIssue>) {
        val isEnabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        if (isEnabled) {
            issues.add(DiagnosticsIssue(
                id = "off_unknown_sources",
                category = IssueCategory.OFFENSIVE,
                severity = IssueSeverity.MEDIUM,
                safeToAutoFix = true,
                description = "Installation from unknown sources is enabled.",
                recommendedAction = "Disable unknown sources to prevent malicious APK installs.",
                autoFixAction = "disable_unknown_sources",
                technicalDetails = "Setting: INSTALL_NON_MARKET_APPS = 1"
            ))
        }
    }

    private fun checkAdbEnabled(issues: MutableList<DiagnosticsIssue>) {
        val isAdbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        if (isAdbEnabled) {
            issues.add(DiagnosticsIssue(
                id = "off_adb_enabled",
                category = IssueCategory.OFFENSIVE,
                severity = IssueSeverity.LOW,
                safeToAutoFix = false,
                description = "USB Debugging (ADB) is enabled.",
                recommendedAction = "Disable ADB when not in use for development.",
                technicalDetails = "Setting: ADB_ENABLED = 1"
            ))
        }
    }

    private fun checkSuspiciousBinaries(issues: MutableList<DiagnosticsIssue>) {
        val downloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val paths = listOf(
            File("/data/local/tmp"), // May not be accessible, but worth checking if exists
            downloadDir
        )
        for (dir in paths) {
            if (dir.exists() && dir.isDirectory) {
                val suspiciousFiles = dir.listFiles { file -> 
                    file.isFile && (file.extension == "sh" || file.extension == "bin" || file.canExecute()) 
                } ?: emptyArray()
                
                if (suspiciousFiles.isNotEmpty()) {
                    issues.add(DiagnosticsIssue(
                        id = "off_suspicious_files_${dir.absolutePath.hashCode()}",
                        category = IssueCategory.OFFENSIVE,
                        severity = IssueSeverity.HIGH,
                        safeToAutoFix = true,
                        description = "Detected suspicious executable files in ${dir.name}.",
                        recommendedAction = "Review and remove unknown scripts or binaries.",
                        autoFixAction = "clean_suspicious_files:${dir.absolutePath}",
                        technicalDetails = "Found: ${suspiciousFiles.joinToString { it.name }}"
                    ))
                }
            }
        }
    }

    private fun checkOpenPorts(issues: MutableList<DiagnosticsIssue>) {
        try {
            val adbWifi = Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
            if (adbWifi) {
                issues.add(DiagnosticsIssue(
                    id = "off_adb_wifi",
                    category = IssueCategory.OFFENSIVE,
                    severity = IssueSeverity.HIGH,
                    safeToAutoFix = true,
                    description = "ADB over WiFi is enabled.",
                    recommendedAction = "Disable wireless debugging unless specifically needed.",
                    autoFixAction = "disable_adb_wifi",
                    technicalDetails = "Wireless ADB allows remote command execution without a cable."
                ))
            }
        } catch (e: Exception) {}
    }

    private fun checkModifiedConfigs(issues: MutableList<DiagnosticsIssue>) {
        val prefsFile = File(context.filesDir.parent, "shared_prefs/maintenance_prefs.xml")
        if (prefsFile.exists()) {
            if (prefsFile.length() > 50 * 1024) {
                issues.add(DiagnosticsIssue(
                    id = "off_config_integrity",
                    category = IssueCategory.OFFENSIVE,
                    severity = IssueSeverity.MEDIUM,
                    safeToAutoFix = true,
                    description = "System configuration file is unusually large.",
                    recommendedAction = "Reset configuration to defaults.",
                    autoFixAction = "reset_config",
                    technicalDetails = "File: ${prefsFile.name}, Size: ${prefsFile.length()} bytes"
                ))
            }
        }
    }

    private fun checkRootAccess(issues: MutableList<DiagnosticsIssue>) {
        val rootPaths = arrayOf(
            "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", 
            "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su", 
            "/system/bin/failsafe/su", "/data/local/su"
        )
        val found = rootPaths.any { File(it).exists() }
        if (found) {
            issues.add(DiagnosticsIssue(
                id = "off_root_detected",
                category = IssueCategory.OFFENSIVE,
                severity = IssueSeverity.MEDIUM,
                safeToAutoFix = false,
                description = "Device appears to be rooted.",
                recommendedAction = "Be cautious as root access bypasses standard security models.",
                technicalDetails = "Found 'su' binary or Superuser app."
            ))
        }
    }

    private fun checkLargeProcessBurst(issues: MutableList<DiagnosticsIssue>) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningProcesses = am.runningAppProcesses ?: emptyList()
        
        if (runningProcesses.size > 150) {
            issues.add(DiagnosticsIssue(
                id = "off_process_burst",
                category = IssueCategory.OFFENSIVE,
                severity = IssueSeverity.HIGH,
                safeToAutoFix = true,
                description = "Abnormal number of running processes detected (${runningProcesses.size}).",
                recommendedAction = "Restart device and review recently installed apps.",
                autoFixAction = "kill_bg_processes",
                technicalDetails = "Process count: ${runningProcesses.size}"
            ))
        }
    }
}

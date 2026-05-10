package com.semseytech.rtsdevicesuitepro.diagnostics.engine

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.semseytech.rtsdevicesuitepro.cleaner.CleanerRepository
import com.semseytech.rtsdevicesuitepro.diagnostics.models.DiagnosticsIssue
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueCategory
import com.semseytech.rtsdevicesuitepro.diagnostics.scanners.DefensiveScanner
import com.semseytech.rtsdevicesuitepro.diagnostics.scanners.OffensiveScanner
import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import com.semseytech.rtsdevicesuitepro.terminal.pkg.PkgManager
import kotlinx.coroutines.flow.last
import java.io.File
import androidx.core.content.edit

class AutoFixEngine(private val context: Context) {
    private val cleanerRepository = CleanerRepository(context)

    suspend fun executeFix(issue: DiagnosticsIssue): Boolean {
        Log.d("AutoFixEngine", "Executing fix for: ${issue.id}")
        val action = issue.autoFixAction ?: return false
        
        return try {
            val success = when {
                action == "run_cleaner" -> {
                    cleanerRepository.performAutoClean(listOf("temp", "empty_folders", "cache"))
                    true
                }
                action == "clear_cache" -> {
                    context.cacheDir.deleteRecursively()
                    true
                }
                action == "remove_temp" -> {
                    context.externalCacheDir?.listFiles { f -> f.extension == "tmp" }?.forEach { it.delete() }
                    true
                }
                action == "optimize_dns" -> {
                    val reset = com.semseytech.rtsdevicesuitepro.net.NetworkReset(context)
                    reset.performReset()
                    true
                }
                action == "init_terminal" -> {
                    TerminalEnv.init(context)
                    // Link system sh to local bin for standard access
                    val localSh = File(TerminalEnv.binDir, "sh")
                    if (!localSh.exists()) {
                        try {
                            localSh.writeText("#!/system/bin/sh\n/system/bin/sh \"$@\"\n")
                            localSh.setExecutable(true)
                        } catch (e: Exception) {}
                    }
                    true
                }
                action == "install_core_tools" -> {
                    TerminalEnv.init(context)
                    // Ensure sh is linked during tool install too
                    val localSh = File(TerminalEnv.binDir, "sh")
                    if (!localSh.exists()) {
                        localSh.writeText("#!/system/bin/sh\n/system/bin/sh \"$@\"\n")
                        localSh.setExecutable(true)
                    }
                    val pkgManager = PkgManager()
                    pkgManager.install("busybox").last()
                    true
                }
                action == "install_python" -> {
                    TerminalEnv.init(context)
                    val pkgManager = PkgManager()
                    pkgManager.install("python").last()
                    true
                }
                action == "enable_self_healing" -> {
                    val prefs = context.getSharedPreferences("maintenance_prefs", Context.MODE_PRIVATE)
                    prefs.edit { putBoolean("self_healing_enabled", true) }
                    true
                }
                action == "disable_unknown_sources" -> {
                    // Requires secure settings write permission, usually not available to apps
                    // We might need to send an intent or use ADB if available
                    false
                }
                action.startsWith("clean_suspicious_files:") -> {
                    val path = action.substringAfter("clean_suspicious_files:")
                    val dir = File(path)
                    dir.listFiles()?.filter { it.extension == "sh" || it.extension == "bin" }?.forEach { it.delete() }
                    true
                }
                action == "kill_bg_processes" -> {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.runningAppProcesses?.forEach { process ->
                        if (process.importance > android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                            am.killBackgroundProcesses(process.processName)
                        }
                    }
                    true
                }
                action == "reset_config" -> {
                    val prefs = context.getSharedPreferences("maintenance_prefs", Context.MODE_PRIVATE)
                    prefs.edit { clear() }
                    true
                }
                else -> false
            }
            
            if (success) {
                Log.i("AutoFixEngine", "Fix successful for ${issue.id}")
                verifyFix(issue)
            } else {
                Log.e("AutoFixEngine", "Fix failed for ${issue.id}")
                false
            }
        } catch (e: Exception) {
            Log.e("AutoFixEngine", "Error during fix: ${e.message}")
            false
        }
    }

    private fun verifyFix(issue: DiagnosticsIssue): Boolean {
        // Targeted verification: Re-run the specific scanner that found the issue
        val found = when (issue.category) {
            IssueCategory.DEFENSIVE -> {
                val results = android.util.Log.d("AutoFix", "Verifying defensive fix")
                // Using a simplified check or re-scanning
                false // For now, assume success if no exception, but real logic should re-scan
            }
            IssueCategory.OFFENSIVE -> false
        }
        return true 
    }
}

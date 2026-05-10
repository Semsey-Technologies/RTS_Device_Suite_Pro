package com.semseytech.rtsdevicesuitepro.diagnostics.engine

import com.semseytech.rtsdevicesuitepro.diagnostics.models.DiagnosticsIssue
import com.semseytech.rtsdevicesuitepro.diagnostics.models.IssueSeverity

class RecommendationEngine {

    fun getExplanation(issue: DiagnosticsIssue): String {
        return when (issue.id) {
            "def_storage_critical" -> "Your device has less than 5% storage left. This can cause apps to crash and system updates to fail."
            "def_storage_low" -> "Low storage can slow down your device. It is recommended to keep at least 15% free space."
            "def_cache_bloat" -> "Applications store temporary files that can accumulate over time. Clearing them can recover space."
            "off_unknown_sources" -> "Enabling this allows installing apps from outside the Play Store, which significantly increases the risk of malware."
            "off_adb_wifi" -> "Remote debugging is active. An attacker on the same network could potentially control your device."
            "def_self_healing_disabled" -> "Self-Healing is a proactive system that fixes common issues without user intervention."
            else -> issue.description
        }
    }

    fun getRiskAssessment(issue: DiagnosticsIssue): String {
        return when (issue.severity) {
            IssueSeverity.CRITICAL -> "Extremely high risk. System failure or data loss is imminent."
            IssueSeverity.HIGH -> "High risk. Security compromise or severe performance degradation is likely."
            IssueSeverity.MEDIUM -> "Moderate risk. Noticeable impact on system stability or security posture."
            IssueSeverity.LOW -> "Minor risk. Slight impact on performance or secondary security settings."
            IssueSeverity.INFO -> "No immediate risk. General information about system state."
        }
    }

    fun getAutoFixImpact(issue: DiagnosticsIssue): String {
        if (!issue.safeToAutoFix) return "Auto-fix is not available or requires manual intervention."
        
        return when (issue.autoFixAction) {
            "run_cleaner" -> "Will remove temporary files and empty folders. No user data will be deleted."
            "clear_cache" -> "Will clear temporary app data. This is safe and only affects performance temporarily."
            "optimize_dns" -> "Will update your connection to use a faster, more reliable DNS server."
            "disable_unknown_sources" -> "Will prevent installation of apps from untrusted sources."
            "enable_self_healing" -> "Will enable background workers to handle system maintenance."
            else -> "Will attempt to resolve the issue using the recommended action."
        }
    }
}

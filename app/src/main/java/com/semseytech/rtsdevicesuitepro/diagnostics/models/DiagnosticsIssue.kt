package com.semseytech.rtsdevicesuitepro.diagnostics.models

data class DiagnosticsIssue(
    val id: String,
    val category: IssueCategory,
    val severity: IssueSeverity,
    val safeToAutoFix: Boolean,
    val description: String,
    val recommendedAction: String,
    val autoFixAction: String? = null,
    val technicalDetails: String
)

enum class IssueCategory {
    OFFENSIVE,
    DEFENSIVE
}

enum class IssueSeverity {
    INFO,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

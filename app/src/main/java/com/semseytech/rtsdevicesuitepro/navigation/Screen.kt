package com.semseytech.rtsdevicesuitepro.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Backup : Screen("backup", "Backup & Restore")
    object Recovery : Screen("recovery", "Recovery Suite")
    object Cleaner : Screen("cleaner", "Cleaner & Maintenance")
    object Network : Screen("network", "Network & Optimization")
    object Archive : Screen("archive", "Archive Engine")
    object PreReset : Screen("prereset", "Pre-Reset Guide")
    object ThemeEngine : Screen("theme_engine", "Theme Engine")
    object ViewBackups : Screen("view_backups", "View Backups")
    object Tools : Screen("tools", "Tools")
    object Automation : Screen("automation", "Automation Engine")
    object Config : Screen("config", "System Configuration")
    object LogExporter : Screen("log_exporter", "Log Exporter")
    object ResourceMonitor : Screen("resource_monitor", "Resource Monitor")
    object SmartOrganizer : Screen("smart_organizer", "Smart Organizer")
    object StorageAnalyzer : Screen("storage_analyzer", "Storage Analyzer")
    object CategoryViewer : Screen("category_viewer/{category}", "Category Viewer") {
        fun createRoute(category: String) = "category_viewer/$category"
    }
}

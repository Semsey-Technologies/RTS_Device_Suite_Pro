package com.semseytech.rtsdevicesuitepro.navigation

sealed class Screen(val route: String, val title: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object Backup : Screen("backup", "Backup Suite")
    object Restore : Screen("restore", "Restore Module")
    object Recovery : Screen("recovery", "Recovery Suite")
    object Cleaner : Screen("cleaner", "Cleaner & Maintenance")
    object Network : Screen("network", "Network & Optimization")
    object Archive : Screen("archive", "Archive Engine")
    object PreReset : Screen("prereset", "Pre-Reset Guide")
    object ThemeEngine : Screen("theme_engine", "Theme Engine")
    object ViewBackups : Screen("view_backups", "View Backups")
    object Tools : Screen("tools", "Tools")
    object Automation : Screen("automation", "Automation Engine")
    object FlowEditor : Screen("flow_editor?flowId={flowId}", "Flow Editor") {
        fun createRoute(flowId: String? = null) = if (flowId != null) "flow_editor?flowId=$flowId" else "flow_editor"
    }
    object AutomationControls : Screen("automation_controls", "Automation Controls")
    object Config : Screen("config?tab={tab}", "System Configuration") {
        fun createRoute(tab: Int = 0) = "config?tab=$tab"
    }
    object LogExporter : Screen("log_exporter", "Log Exporter")
    object ResourceMonitor : Screen("resource_monitor", "Resource Monitor")
    object SmartOrganizer : Screen("smart_organizer", "Smart Organizer")
    object StorageAnalyzer : Screen("storage_analyzer", "Storage Analyzer")
    object FileExplorer : Screen("file_explorer", "File Explorer")
    object FileList : Screen("file_list/{path}", "Files") {
        fun createRoute(path: String) = "file_list/${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object TextEditor : Screen("text_editor?path={path}", "Editor") {
        fun createRoute(path: String) = "text_editor?path=${java.net.URLEncoder.encode(path, "UTF-8")}"
    }
    object Terminal : Screen("terminal", "Command Terminal")
    object WipeSuite : Screen("wipe_suite", "Secure Wipe Suite")
    object BatteryEstimation : Screen("battery_estimation", "Battery Usage Estimator")
    object HelpAndPermissions : Screen("help_permissions", "Help & Permissions")
    object AdbSetup : Screen("adb_setup", "ADB Setup")
    object AdbConsole : Screen("adb_console", "ADB Console")
    object CategoryViewer : Screen("category_viewer/{category}", "Category Viewer") {
        fun createRoute(category: String) = "category_viewer/$category"
    }
    object Diagnostics : Screen("diagnostics", "Diagnostics Suite")
}

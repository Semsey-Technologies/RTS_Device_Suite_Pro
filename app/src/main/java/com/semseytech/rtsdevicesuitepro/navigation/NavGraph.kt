package com.semseytech.rtsdevicesuitepro.navigation

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.semseytech.rtsdevicesuitepro.ui.screens.DashboardScreen
import com.semseytech.rtsdevicesuitepro.ui.screens.PlaceholderScreen
import com.semseytech.rtsdevicesuitepro.ui.screens.ThemeEngineScreen
import com.semseytech.rtsdevicesuitepro.ui.screens.ConfigScreen
import com.semseytech.rtsdevicesuitepro.ui.screens.ToolsScreen
import com.semseytech.rtsdevicesuitepro.ui.screens.ResourceMonitorScreen
import com.semseytech.rtsdevicesuitepro.ui.help.HelpAndPermissionsScreen
import com.semseytech.rtsdevicesuitepro.viewer.ViewBackupsScreen
import com.semseytech.rtsdevicesuitepro.organizer.ui.SmartOrganizerScreen
import com.semseytech.rtsdevicesuitepro.organizer.ui.OrganizerViewModel
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerRepository
import com.semseytech.rtsdevicesuitepro.organizer.data.OrganizerDatabase
import com.semseytech.rtsdevicesuitepro.cleaner.CleanerScreen
import com.semseytech.rtsdevicesuitepro.storage.analyzer.StorageAnalyzerScreen
import com.semseytech.rtsdevicesuitepro.storage.analyzer.StorageAnalyzerViewModel
import com.semseytech.rtsdevicesuitepro.storage.analyzer.StorageAnalyzerRepository
import com.semseytech.rtsdevicesuitepro.storage.analyzer.CategoryViewerScreen
import com.semseytech.rtsdevicesuitepro.storage.analyzer.FileCategory
import com.semseytech.rtsdevicesuitepro.archive.ui.ArchiveScreen
import com.semseytech.rtsdevicesuitepro.archive.logic.ArchiveViewModel
import com.semseytech.rtsdevicesuitepro.backup.ui.BackupScreen
import com.semseytech.rtsdevicesuitepro.backup.BackupViewModel
import com.semseytech.rtsdevicesuitepro.restore.ui.RestoreScreen
import com.semseytech.rtsdevicesuitepro.restore.RestoreViewModel
import com.semseytech.rtsdevicesuitepro.battery.ui.BatteryScreen
import com.semseytech.rtsdevicesuitepro.battery.ui.BatteryViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.navArgument

@Composable
fun SetupNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {
        composable(route = Screen.Dashboard.route) {
            DashboardScreen(onNavigate = { route ->
                navController.navigate(route)
            })
        }
        composable(route = Screen.Backup.route) {
            BackupScreen(onNavigateToRestore = { navController.navigate(Screen.Restore.route) })
        }
        composable(route = Screen.Restore.route) {
            RestoreScreen(
                onBack = { navController.popBackStack() },
                onViewResults = { navController.navigate(Screen.ViewBackups.route) }
            )
        }
        composable(route = Screen.Recovery.route) {
            val context = LocalContext.current
            val repository = remember { com.semseytech.rtsdevicesuitepro.recovery.RecoveryRepository(context) }
            val viewModel: com.semseytech.rtsdevicesuitepro.recovery.RecoveryViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return com.semseytech.rtsdevicesuitepro.recovery.RecoveryViewModel(
                            context.applicationContext as android.app.Application,
                            repository
                        ) as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.recovery.RecoveryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Cleaner.route) {
            CleanerScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Network.route) {
            val context = LocalContext.current
            val viewModel: com.semseytech.rtsdevicesuitepro.net.NetOptimizerViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return com.semseytech.rtsdevicesuitepro.net.NetOptimizerViewModel(context.applicationContext as android.app.Application) as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.net.NetOptimizerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onNavigateToAutomation = { navController.navigate(Screen.Automation.route) }
            )
        }
        composable(route = Screen.Archive.route) {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.PreReset.route) {
            com.semseytech.rtsdevicesuitepro.prereset.ui.PreResetGuideScreen(
                onNavigate = { route -> navController.navigate(route) }
            )
        }
        composable(route = Screen.ThemeEngine.route) {
            ThemeEngineScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.ViewBackups.route) {
            ViewBackupsScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Screen.Config.route,
            arguments = listOf(navArgument("tab") { 
                type = NavType.IntType
                defaultValue = 0
            })
        ) { backStackEntry ->
            val tabIndex = backStackEntry.arguments?.getInt("tab") ?: 0
            ConfigScreen(
                initialTab = tabIndex,
                onBack = { navController.popBackStack() },
                onNavigateToThemes = { navController.navigate(Screen.ThemeEngine.route) }
            )
        }
        composable(route = Screen.LogExporter.route) {
            com.semseytech.rtsdevicesuitepro.tools.logs.LogExporterScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Tools.route) {
            ToolsScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(route = Screen.ResourceMonitor.route) {
            ResourceMonitorScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.SmartOrganizer.route) {
            val context = LocalContext.current
            val viewModel: OrganizerViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val database = OrganizerDatabase.getDatabase(context)
                        val repository = OrganizerRepository(database.organizerDao())
                        return OrganizerViewModel(context.applicationContext as android.app.Application, repository) as T
                    }
                }
            )
            SmartOrganizerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Automation.route) {
            val context = LocalContext.current
            val viewModel: com.semseytech.rtsdevicesuitepro.automation.ui.AutomationViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        val vm = com.semseytech.rtsdevicesuitepro.automation.ui.AutomationViewModel(context.applicationContext as Application)
                        // Try to get engine from service if available
                        com.semseytech.rtsdevicesuitepro.automation.engine.AutomationService.getEngine()?.let { 
                            vm.setEngine(it) 
                        }
                        return vm as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.automation.ui.AutomationScreen(
                onBackClick = { navController.popBackStack() },
                onNavigate = { route -> navController.navigate(route) },
                viewModel = viewModel
            )
        }
        composable(
            route = Screen.FlowEditor.route,
            arguments = listOf(navArgument("flowId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val flowId = backStackEntry.arguments?.getString("flowId")
            val viewModel: com.semseytech.rtsdevicesuitepro.automation.flow.FlowEditorViewModel = viewModel()
            
            androidx.compose.runtime.LaunchedEffect(flowId) {
                flowId?.let { viewModel.loadFlow(it) }
            }

            com.semseytech.rtsdevicesuitepro.automation.flow.ui.FlowEditorScreen(
                onBackClick = { navController.popBackStack() },
                viewModel = viewModel
            )
        }
        composable(route = Screen.AutomationControls.route) {
            val context = LocalContext.current
            val viewModel: com.semseytech.rtsdevicesuitepro.net.automation.NetworkAutomationViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return com.semseytech.rtsdevicesuitepro.net.automation.NetworkAutomationViewModel(context.applicationContext as android.app.Application) as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.net.automation.AutomationControlsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Terminal.route) {
            com.semseytech.rtsdevicesuitepro.terminal.ui.TerminalScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.WipeSuite.route) {
            com.semseytech.rtsdevicesuitepro.wipe.ui.WipeSuiteScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(route = Screen.BatteryEstimation.route) {
            val context = LocalContext.current
            val viewModel: BatteryViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return BatteryViewModel(context.applicationContext as Application) as T
                    }
                }
            )
            BatteryScreen(viewModel = viewModel)
        }
        composable(route = Screen.HelpAndPermissions.route) {
            HelpAndPermissionsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.AdbSetup.route) {
            com.semseytech.rtsdevicesuitepro.adb.ui.AdbSetupScreen(
                onNavigateToConsole = { navController.navigate(Screen.AdbConsole.route) }
            )
        }
        composable(route = Screen.AdbConsole.route) {
            com.semseytech.rtsdevicesuitepro.adb.ui.AdbConsoleScreen()
        }
        composable(route = Screen.FileExplorer.route) {
            val context = LocalContext.current
            val viewModel: com.semseytech.rtsdevicesuitepro.filemanager.FileExplorerViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return com.semseytech.rtsdevicesuitepro.filemanager.FileExplorerViewModel(context.applicationContext as android.app.Application) as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.filemanager.FileExplorerScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.FileList.route,
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", "UTF-8")
            val context = LocalContext.current
            val viewModel: com.semseytech.rtsdevicesuitepro.filemanager.FileExplorerViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return com.semseytech.rtsdevicesuitepro.filemanager.FileExplorerViewModel(context.applicationContext as android.app.Application) as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.filemanager.FileListScreen(
                path = path,
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.TextEditor.route,
            arguments = listOf(navArgument("path") { type = NavType.StringType })
        ) { backStackEntry ->
            val path = java.net.URLDecoder.decode(backStackEntry.arguments?.getString("path") ?: "", "UTF-8")
            val viewModel: com.semseytech.rtsdevicesuitepro.editor.TextEditorViewModel = viewModel()
            com.semseytech.rtsdevicesuitepro.editor.TextEditorScreen(
                path = path,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.StorageAnalyzer.route) { backStackEntry ->
            val context = LocalContext.current
            val mainActivityEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Dashboard.route)
            }
            val viewModel: StorageAnalyzerViewModel = viewModel(
                viewModelStoreOwner = mainActivityEntry,
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StorageAnalyzerViewModel(context.applicationContext as Application, StorageAnalyzerRepository(context.applicationContext)) as T
                    }
                }
            )
            StorageAnalyzerScreen(
                viewModel = viewModel,
                onNavigateToCategory = { category ->
                    navController.navigate(Screen.CategoryViewer.createRoute(category.name))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.CategoryViewer.route,
            arguments = listOf(navArgument("category") { type = NavType.StringType })
        ) { backStackEntry ->
            val categoryName = backStackEntry.arguments?.getString("category") ?: ""
            val category = try {
                FileCategory.valueOf(categoryName)
            } catch (e: Exception) {
                FileCategory.OTHERS
            }
            
            val context = LocalContext.current
            val storageEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Dashboard.route)
            }
            
            val viewModel: StorageAnalyzerViewModel = viewModel(
                viewModelStoreOwner = storageEntry,
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return StorageAnalyzerViewModel(context.applicationContext as Application, StorageAnalyzerRepository(context.applicationContext)) as T
                    }
                }
            )
            CategoryViewerScreen(
                category = category,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(route = Screen.Diagnostics.route) {
            val context = LocalContext.current
            val viewModel: com.semseytech.rtsdevicesuitepro.diagnostics.DiagnosticsViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return com.semseytech.rtsdevicesuitepro.diagnostics.DiagnosticsViewModel(context.applicationContext as Application) as T
                    }
                }
            )
            com.semseytech.rtsdevicesuitepro.diagnostics.ui.DiagnosticsScreen(viewModel = viewModel)
        }
    }
}

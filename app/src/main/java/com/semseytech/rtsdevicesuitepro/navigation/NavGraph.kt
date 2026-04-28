package com.semseytech.rtsdevicesuitepro.navigation

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
            BackupScreen()
        }
        composable(route = Screen.Recovery.route) {
            RestoreScreen()
        }
        composable(route = Screen.Cleaner.route) {
            CleanerScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Network.route) {
            PlaceholderScreen(title = Screen.Network.title)
        }
        composable(route = Screen.Archive.route) {
            ArchiveScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.PreReset.route) {
            PlaceholderScreen(title = Screen.PreReset.title)
        }
        composable(route = Screen.ThemeEngine.route) {
            ThemeEngineScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.ViewBackups.route) {
            ViewBackupsScreen(onBack = { navController.popBackStack() })
        }
        composable(route = Screen.Config.route) {
            ConfigScreen(onBack = { navController.popBackStack() })
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
            PlaceholderScreen(title = Screen.Automation.title)
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
                        return StorageAnalyzerViewModel(StorageAnalyzerRepository(context.applicationContext)) as T
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
                        return StorageAnalyzerViewModel(StorageAnalyzerRepository(context.applicationContext)) as T
                    }
                }
            )
            CategoryViewerScreen(
                category = category,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

package com.semseytech.rtsdevicesuitepro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.navigation.SetupNavGraph
import com.semseytech.rtsdevicesuitepro.ui.components.BottomNavBar
import com.semseytech.rtsdevicesuitepro.ui.components.DashboardHeader
import com.semseytech.rtsdevicesuitepro.ui.theme.RTSDeviceSuiteProTheme
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionExplanationDialog
import com.semseytech.rtsdevicesuitepro.ui.permissions.PermissionPrefs

import com.semseytech.rtsdevicesuitepro.automation.engine.AutomationEngine
import com.semseytech.rtsdevicesuitepro.automation.engine.TriggerManager

class MainActivity : ComponentActivity() {
    private lateinit var triggerManager: TriggerManager
    private lateinit var permissionPrefs: PermissionPrefs
    private var permissionState = mutableStateOf(false)
    private var showExplanation by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionState.value = permissions.values.all { it }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        permissionPrefs = PermissionPrefs(this)
        val engine = AutomationEngine(this)
        triggerManager = TriggerManager(this, engine)
        triggerManager.start()

        enableEdgeToEdge()
        
        checkAndRequestPermissions()

        setContent {
            RTSDeviceSuiteProTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                if (showExplanation) {
                    PermissionExplanationDialog(
                        onConfirm = {
                            showExplanation = false
                            permissionPrefs.markGlobalExplanationShown()
                            launchPermissionRequest()
                            checkManageExternalStorage()
                        },
                        onDismiss = {
                            showExplanation = false
                        },
                        onNavigateToHelp = {
                            showExplanation = false
                            navController.navigate(Screen.HelpAndPermissions.route)
                        }
                    )
                }

                Scaffold(
                    topBar = {
                        DashboardHeader(onNavigate = { route ->
                            navController.navigate(route)
                        })
                    },
                    bottomBar = {
                        BottomNavBar(
                            currentRoute = currentRoute,
                            onNavigate = { route ->
                                if (currentRoute == route) return@BottomNavBar
                                
                                if (route == Screen.Dashboard.route) {
                                    // Directly pop back to dashboard if it's already in the stack
                                    val popped = navController.popBackStack(Screen.Dashboard.route, inclusive = false)
                                    if (!popped) {
                                        navController.navigate(Screen.Dashboard.route)
                                    }
                                } else {
                                    navController.navigate(route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        SetupNavGraph(navController = navController)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::triggerManager.isInitialized) {
            triggerManager.stop()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = getRequiredPermissions()

        val allStandardGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        val allSpecialGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else true

        if ((!allStandardGranted || !allSpecialGranted) && !permissionPrefs.isGlobalExplanationShown()) {
            showExplanation = true
        } else {
            permissionState.value = true
            if (!allStandardGranted || !allSpecialGranted) {
                // If we already showed explanation once, just launch request
                launchPermissionRequest()
            }
        }
    }

    private fun launchPermissionRequest() {
        requestPermissionLauncher.launch(getRequiredPermissions().toTypedArray())
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.READ_SMS)
        permissions.add(Manifest.permission.READ_CONTACTS)
        permissions.add(Manifest.permission.WRITE_CONTACTS)
        permissions.add(Manifest.permission.READ_CALL_LOG)
        permissions.add(Manifest.permission.WRITE_CALL_LOG)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        permissions.add(Manifest.permission.READ_PHONE_STATE)
        permissions.add(Manifest.permission.GET_ACCOUNTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        return permissions
    }

    private fun checkManageExternalStorage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${packageName}")
                startActivity(intent)
            }
        }
    }
}

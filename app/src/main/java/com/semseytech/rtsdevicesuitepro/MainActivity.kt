package com.semseytech.rtsdevicesuitepro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.semseytech.rtsdevicesuitepro.navigation.Screen
import com.semseytech.rtsdevicesuitepro.navigation.SetupNavGraph
import com.semseytech.rtsdevicesuitepro.ui.components.BottomNavBar
import com.semseytech.rtsdevicesuitepro.ui.components.DashboardHeader
import com.semseytech.rtsdevicesuitepro.ui.theme.RTSDeviceSuiteProTheme

import com.semseytech.rtsdevicesuitepro.automation.engine.AutomationService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        setContent {
            RTSDeviceSuiteProTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

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

    override fun onStart() {
        super.onStart()
        AutomationService.start(this)
    }
}

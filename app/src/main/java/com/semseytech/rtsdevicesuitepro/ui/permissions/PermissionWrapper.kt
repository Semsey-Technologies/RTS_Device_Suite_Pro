package com.semseytech.rtsdevicesuitepro.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@Composable
fun PermissionGate(
    permissions: List<String>,
    onPermissionGranted: () -> Unit,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PermissionPrefs(context) }
    
    var checkTrigger by remember { mutableLongStateOf(0L) }
    
    val hasAllPermissions = remember(permissions, checkTrigger) {
        permissions.all { PermissionUtils.isPermissionGranted(context, it) }
    }
    
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        checkTrigger = System.currentTimeMillis()
        if (results.values.all { it }) {
            onPermissionGranted()
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkTrigger = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (hasAllPermissions) {
        content()
    } else {
        LaunchedEffect(permissions) {
            val unshown = permissions.filter { !prefs.isExplanationShown(it) }
            if (unshown.isNotEmpty()) {
                showRationale = true
            } else {
                PermissionUtils.requestPermissions(context, permissions, launcher)
            }
        }

        if (showRationale) {
            PermissionRationaleDialog(
                permissions = permissions,
                onConfirm = {
                    showRationale = false
                    permissions.forEach { prefs.markExplanationShown(it) }
                    PermissionUtils.requestPermissions(context, permissions, launcher)
                },
                onDismiss = onBack,
                onNavigateToHelp = {
                    showRationale = false
                    onBack()
                }
            )
        }
    }
}

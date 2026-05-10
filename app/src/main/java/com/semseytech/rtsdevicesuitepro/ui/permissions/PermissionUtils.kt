package com.semseytech.rtsdevicesuitepro.ui.permissions

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.Settings
import androidx.core.content.ContextCompat

object PermissionUtils {
    fun isPermissionGranted(context: Context, permission: String): Boolean {
        return when (permission) {
            "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Environment.isExternalStorageManager()
                } else {
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
            }
            "android.permission.PACKAGE_USAGE_STATS" -> {
                val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
                val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
                } else {
                    appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
                }
                mode == AppOpsManager.MODE_ALLOWED
            }
            else -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestPermissions(context: Context, permissions: List<String>, launcher: androidx.activity.result.ActivityResultLauncher<Array<String>>) {
        val specialPermissions = listOf("android.permission.MANAGE_EXTERNAL_STORAGE", "android.permission.PACKAGE_USAGE_STATS")
        val standardPermissions = permissions.filter { it !in specialPermissions }
        val neededSpecial = permissions.filter { it in specialPermissions }

        if (standardPermissions.isNotEmpty()) {
            launcher.launch(standardPermissions.toTypedArray())
        }

        neededSpecial.forEach { permission ->
            when (permission) {
                "android.permission.MANAGE_EXTERNAL_STORAGE" -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:${context.packageName}")
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
                "android.permission.PACKAGE_USAGE_STATS" -> {
                    if (!isPermissionGranted(context, permission)) {
                        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            }
        }
    }
}

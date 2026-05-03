package com.semseytech.rtsdevicesuitepro.adb.core

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AdbBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val adbManager = AdbManager(context)
            CoroutineScope(Dispatchers.IO).launch {
                if (adbManager.isEnabled.first()) {
                    AdbBackgroundService.start(context)
                }
            }
        }
    }
}

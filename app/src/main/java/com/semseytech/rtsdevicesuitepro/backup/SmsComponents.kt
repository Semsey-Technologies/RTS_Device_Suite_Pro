package com.semseytech.rtsdevicesuitepro.backup

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.activity.ComponentActivity

/**
 * Dummy components required by Android to allow the app to be set as the Default SMS App.
 * These are necessary for comprehensive SMS backup/restore permissions.
 */

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // No-op: Just satisfying system requirements for Default SMS app status
    }
}

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        // No-op
    }
}

class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}

class ComposeSmsActivity : ComponentActivity()

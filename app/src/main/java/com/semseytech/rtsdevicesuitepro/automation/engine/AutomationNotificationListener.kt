package com.semseytech.rtsdevicesuitepro.automation.engine

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class AutomationNotificationListener : NotificationListenerService() {

    private var engine: AutomationEngine? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d("AutomationNotification", "Listener Connected")
        engine = AutomationEngine(applicationContext)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val engine = engine ?: return
        val packageName = sbn.packageName
        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""
        val content = "$title $text"

        engine.onTrigger("NOTIFICATION_POSTED", packageName)
        engine.onTrigger("NOTIFICATION_MATCHES", content)
        engine.onTrigger("NOTIFICATION_KEYWORD", Pair(packageName, content))

        // Check for messaging apps (simplified list)
        val messagingApps = listOf("com.whatsapp", "com.facebook.orca", "org.telegram.messenger", "com.google.android.apps.messaging")
        if (messagingApps.contains(packageName) || sbn.notification.category == "msg") {
            engine.onTrigger("MESSAGING_APP_NOTIFICATION", packageName)
        }

        // Check for email apps
        if (packageName.contains("mail") || sbn.notification.category == "email") {
            engine.onTrigger("EMAIL_RECEIVED", title) // Often title is the account or sender
        }

        // Check for contact status (very simplified, usually depends on specific app notification content)
        if (content.contains("online", ignoreCase = true)) {
            engine.onTrigger("CONTACT_STATUS_CHANGED", Pair(title, "ONLINE"))
        } else if (content.contains("offline", ignoreCase = true)) {
            engine.onTrigger("CONTACT_STATUS_CHANGED", Pair(title, "OFFLINE"))
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        val engine = engine ?: return
        engine.onTrigger("NOTIFICATION_REMOVED", sbn.packageName)
    }
}

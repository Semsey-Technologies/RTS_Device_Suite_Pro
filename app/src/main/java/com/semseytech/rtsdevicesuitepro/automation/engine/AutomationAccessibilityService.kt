package com.semseytech.rtsdevicesuitepro.automation.engine

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class AutomationAccessibilityService : AccessibilityService() {

    private var engine: AutomationEngine? = null
    private var lastPackageName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AutomationAccessibility", "Service Connected")
        engine = AutomationEngine(applicationContext)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val engine = engine ?: return
        
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                val packageName = event.packageName?.toString()
                if (packageName != null && packageName != lastPackageName) {
                    if (lastPackageName != null) {
                        engine.onTrigger("APP_CLOSED", lastPackageName)
                    }
                    engine.onTrigger("APP_OPENED", packageName)
                    engine.onTrigger("FOREGROUND_APP_CHANGED", packageName)
                    lastPackageName = packageName
                }
                
                // Detect system dialogs
                if (packageName == "com.android.systemui" || packageName == "android") {
                    engine.onTrigger("SYSTEM_DIALOG_OPENED")
                }
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                engine.onTrigger("ACCESSIBILITY_EVENT", "VIEW_CLICKED")
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                engine.onTrigger("ACCESSIBILITY_EVENT", "VIEW_FOCUSED")
            }
            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                // Toasts often come through here
                val text = event.text.joinToString(" ")
                if (text.isNotBlank()) {
                    engine.onTrigger("TOAST_DETECTED", text)
                }
            }
            AccessibilityEvent.TYPE_WINDOWS_CHANGED -> {
                // Can be used for keyboard detection in some cases, but unreliable
            }
        }
    }

    override fun onInterrupt() {
        Log.d("AutomationAccessibility", "Service Interrupted")
    }
}

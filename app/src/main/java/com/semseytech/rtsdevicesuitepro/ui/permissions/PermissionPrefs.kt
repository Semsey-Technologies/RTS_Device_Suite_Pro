package com.semseytech.rtsdevicesuitepro.ui.permissions

import android.content.Context

class PermissionPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)

    fun isExplanationShown(permission: String): Boolean {
        return prefs.getBoolean("shown_$permission", false)
    }

    fun markExplanationShown(permission: String) {
        prefs.edit().putBoolean("shown_$permission", true).apply()
    }
    
    fun isGlobalExplanationShown(): Boolean {
        return prefs.getBoolean("global_explanation_shown", false)
    }
    
    fun markGlobalExplanationShown() {
        prefs.edit().putBoolean("global_explanation_shown", true).apply()
    }
}

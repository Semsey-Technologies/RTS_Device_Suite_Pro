package com.semseytech.rtsdevicesuitepro.adb.core

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "adb_settings")

/**
 * High-level manager for ADB functionality.
 */
class AdbManager(private val context: Context) {
    private val keyManager = AdbKeyManager(context)
    private var activeConnection: AdbConnection? = null

    companion object {
        private val PREF_ENABLED = booleanPreferencesKey("adb_enabled")
        private val PREF_PORT = intPreferencesKey("adb_port")
        private val PREF_HOST = stringPreferencesKey("adb_host")
        
        val SAFE_COMMANDS = listOf(
            "pm clear %s --cache-only",
            "pm grant %s %s",
            "settings put global %s %s",
            "am force-stop %s",
            "dumpsys battery",
            "logcat -d",
            "cmd package compile -m speed %s"
        )
    }

    val isEnabled: Flow<Boolean> = context.dataStore.data.map { it[PREF_ENABLED] ?: false }
    val lastPort: Flow<Int> = context.dataStore.data.map { it[PREF_PORT] ?: 5555 }

    suspend fun setEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PREF_ENABLED] = enabled }
        if (enabled) {
            AdbBackgroundService.start(context)
        } else {
            disconnect()
        }
    }

    suspend fun pair(port: Int, code: String, host: String? = null): Boolean {
        disconnect()
        val targetHost = if (!host.isNullOrBlank()) host else NetworkUtils.getLocalIpAddress()
        val connection = AdbConnection(targetHost, port, keyManager)
        return connection.pair(code)
    }

    suspend fun connect(host: String? = null, port: Int): Boolean {
        disconnect()
        val targetHost = host ?: NetworkUtils.getLocalIpAddress()
        val connection = AdbConnection(targetHost, port, keyManager)
        return if (connection.connect()) {
            activeConnection = connection
            context.dataStore.edit {
                it[PREF_HOST] = targetHost
                it[PREF_PORT] = port
            }
            true
        } else {
            false
        }
    }

    fun disconnect() {
        activeConnection?.close()
        activeConnection = null
    }

    suspend fun executeSafeCommand(command: String): String {
        if (!isCommandSafe(command)) {
            return "Command rejected: Unsafe or unauthorized."
        }
        return activeConnection?.executeCommand(command) ?: "Not connected"
    }

    private fun isCommandSafe(command: String): Boolean {
        // Simple validation: check if it starts with one of the allowed prefixes
        return SAFE_COMMANDS.any { command.startsWith(it.substringBefore("%")) }
    }
    
    fun getStatus(): String = if (activeConnection != null) "Connected" else "Disconnected"
}

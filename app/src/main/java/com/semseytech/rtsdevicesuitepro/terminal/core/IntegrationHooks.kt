package com.semseytech.rtsdevicesuitepro.terminal.core

import android.content.Context
import android.content.Intent
import com.semseytech.rtsdevicesuitepro.MainActivity
import java.io.File

object IntegrationHooks {
    
    /**
     * Opens the terminal at a specific directory.
     */
    fun openTerminalAt(context: Context, directory: File) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "open_terminal")
            putExtra("path", directory.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Executes a script file in the terminal.
     */
    fun runScript(context: Context, scriptFile: File) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "run_script")
            putExtra("script_path", scriptFile.absolutePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Notifies the system about terminal activity.
     */
    fun onTerminalActivity(message: String) {
        // Integration with a notification system could go here
        android.util.Log.d("TerminalIntegration", "Activity: $message")
    }
}

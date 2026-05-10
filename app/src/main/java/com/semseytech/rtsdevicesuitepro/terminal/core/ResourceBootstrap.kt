package com.semseytech.rtsdevicesuitepro.terminal.core

import android.content.Context
import com.semseytech.rtsdevicesuitepro.organizer.worker.ExtractionUtils
import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ResourceBootstrap {
    
    suspend fun bootstrap(context: Context, onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        onProgress("Initializing environment...")
        TerminalEnv.init(context)
        
        // 1. Extract Python Runtime
        if (!File(TerminalEnv.pythonDir, "bin/python3.11").exists()) {
            onProgress("Extracting Python 3.11 runtime...")
            val pythonZip = File(TerminalEnv.tmpDir, "python.zip")
            copyAssetToFile(context, "python/python.zip", pythonZip)
            ExtractionUtils.extract(pythonZip, TerminalEnv.pythonDir)
            pythonZip.delete()
            
            // Set permissions
            File(TerminalEnv.pythonDir, "bin/python3.11").setExecutable(true)
            File(TerminalEnv.pythonDir, "bin/python").setExecutable(true)
        }

        // 2. Extract Base System Tools
        if (!File(TerminalEnv.binDir, "sh").exists()) {
            onProgress("Setting up system binaries...")
            val toolsZip = File(TerminalEnv.tmpDir, "tools.zip")
            copyAssetToFile(context, "system/tools.zip", toolsZip)
            ExtractionUtils.extract(toolsZip, TerminalEnv.usrDir)
            toolsZip.delete()
            
            // Make all binaries in /usr/bin executable
            TerminalEnv.binDir.listFiles()?.forEach { it.setExecutable(true) }
        }

        onProgress("Ready.")
    }

    private fun copyAssetToFile(context: Context, assetPath: String, targetFile: File) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}

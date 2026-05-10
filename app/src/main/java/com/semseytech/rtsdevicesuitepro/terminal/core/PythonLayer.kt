package com.semseytech.rtsdevicesuitepro.terminal.core

import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PythonLayer(private val env: ShellEnvironment) {
    private val pip = PipManager()
    private val pythonBinary = File(TerminalEnv.pythonDir, "bin/python3.11")

    suspend fun execute(args: List<String>, stdin: String? = null): String = withContext(Dispatchers.IO) {
        if (!pythonBinary.exists()) {
            return@withContext "python: error: runtime not initialized. Please wait for bootstrap."
        }

        if (args.isNotEmpty() && args[0] == "pip") {
            return@withContext pip.execute(args.drop(1))
        }
        
        if (args.isEmpty()) {
            return@withContext "Python 3.11.4 REPL (Type exit() to quit)\n>>> "
        }

        return@withContext try {
            val pb = ProcessBuilder(listOf(pythonBinary.absolutePath) + args)
            pb.directory(env.currentWorkingDirectory)
            val processEnv = pb.environment()
            processEnv.putAll(env.getAllVariables())
            
            processEnv["PYTHONHOME"] = TerminalEnv.pythonDir.absolutePath
            processEnv["PYTHONPATH"] = "${TerminalEnv.pythonLibDir.absolutePath}:${TerminalEnv.sitePackagesDir.absolutePath}"
            processEnv["PATH"] = "${env.getVariable("PATH")}:${File(TerminalEnv.pythonDir, "bin").absolutePath}"
            
            val process = pb.start()
            
            if (stdin != null) {
                process.outputStream.bufferedWriter().use { it.write(stdin) }
            }
            
            val output = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            process.waitFor()
            
            if (error.isNotEmpty()) "$output\n$error".trim() else output.trim()
        } catch (e: Exception) {
            "python: execution failed: ${e.message}"
        }
    }
}

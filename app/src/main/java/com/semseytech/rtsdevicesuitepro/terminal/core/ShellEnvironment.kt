package com.semseytech.rtsdevicesuitepro.terminal.core

import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import java.io.File

class ShellEnvironment {
    private val variables = mutableMapOf<String, String>()
    private val aliases = mutableMapOf<String, String>()
    var currentWorkingDirectory: File = TerminalEnv.homeDir

    init {
        variables["HOME"] = TerminalEnv.homeDir.absolutePath
        variables["PATH"] = "${TerminalEnv.binDir.absolutePath}:${File(TerminalEnv.pythonDir, "bin").absolutePath}:/system/bin:/system/xbin"
        variables["USER"] = "rts_user"
        variables["SHELL"] = "/bin/sh"
        variables["TERM"] = "xterm-256color"
        variables["PWD"] = currentWorkingDirectory.absolutePath
        variables["PYTHONHOME"] = TerminalEnv.pythonDir.absolutePath
        variables["PYTHONPATH"] = "${TerminalEnv.pythonLibDir.absolutePath}:${TerminalEnv.sitePackagesDir.absolutePath}"
    }

    fun getVariable(name: String): String = variables[name] ?: ""
    
    fun setVariable(name: String, value: String) {
        variables[name] = value
        if (name == "PWD") {
            val dir = File(value)
            if (dir.exists() && dir.isDirectory) {
                currentWorkingDirectory = dir
            }
        }
    }

    fun getAllVariables(): Map<String, String> = variables.toMap()

    fun setAlias(name: String, command: String) {
        aliases[name] = command
    }

    fun getAlias(name: String): String? = aliases[name]

    fun resolvePath(path: String): File {
        return when {
            path == "~" -> TerminalEnv.homeDir
            path.startsWith("~/") -> File(TerminalEnv.homeDir, path.substring(2))
            path.startsWith("/") -> File(path)
            else -> File(currentWorkingDirectory, path)
        }
    }
}

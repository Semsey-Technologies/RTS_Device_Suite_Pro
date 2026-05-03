package com.semseytech.rtsdevicesuitepro.terminal

import android.content.Context
import java.io.File

object TerminalEnv {
    lateinit var filesDir: File
    lateinit var homeDir: File
    lateinit var usrDir: File
    lateinit var binDir: File
    lateinit var libDir: File
    lateinit var etcDir: File
    lateinit var tmpDir: File

    fun init(context: Context) {
        filesDir = context.filesDir
        val root = File(filesDir, "usr")
        
        homeDir = File(filesDir, "home")
        usrDir = root
        binDir = File(root, "bin")
        libDir = File(root, "lib")
        etcDir = File(root, "etc")
        tmpDir = File(root, "tmp")

        arrayOf(homeDir, usrDir, binDir, libDir, etcDir, tmpDir).forEach {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getEnvironment(context: Context): Map<String, String> {
        val env = mutableMapOf<String, String>()
        env["HOME"] = homeDir.absolutePath
        env["PATH"] = "${binDir.absolutePath}:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = libDir.absolutePath
        env["TMPDIR"] = tmpDir.absolutePath
        env["TERM"] = "xterm-256color"
        return env
    }
}

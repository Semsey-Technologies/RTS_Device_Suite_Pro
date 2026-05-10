package com.semseytech.rtsdevicesuitepro.terminal

import android.content.Context
import java.io.File

object TerminalEnv {
    private lateinit var context: Context
    lateinit var filesDir: File
    lateinit var homeDir: File
    lateinit var usrDir: File
    lateinit var binDir: File
    lateinit var libDir: File
    lateinit var etcDir: File
    lateinit var tmpDir: File

    lateinit var pythonDir: File
    lateinit var pythonLibDir: File
    lateinit var sitePackagesDir: File
    lateinit var packagesDir: File

    fun init(context: Context) {
        this.context = context
        filesDir = context.filesDir
        val root = File(filesDir, "usr")
        
        homeDir = File(filesDir, "home")
        usrDir = root
        binDir = File(root, "bin")
        libDir = File(root, "lib")
        etcDir = File(root, "etc")
        tmpDir = File(root, "tmp")

        pythonDir = File(filesDir, "python")
        pythonLibDir = File(pythonDir, "lib/python3.11")
        sitePackagesDir = File(pythonLibDir, "site-packages")
        packagesDir = File(filesDir, "packages")

        arrayOf(homeDir, usrDir, binDir, libDir, etcDir, tmpDir, pythonDir, pythonLibDir, sitePackagesDir, packagesDir).forEach {
            if (!it.exists()) it.mkdirs()
        }
    }

    fun getAssetManager() = context.assets

    fun getEnvironment(context: Context): Map<String, String> {
        val env = mutableMapOf<String, String>()
        env["HOME"] = homeDir.absolutePath
        env["PATH"] = "${binDir.absolutePath}:${File(pythonDir, "bin").absolutePath}:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = "${libDir.absolutePath}:${File(pythonDir, "lib").absolutePath}"
        env["TMPDIR"] = tmpDir.absolutePath
        env["TERM"] = "xterm-256color"
        env["PYTHONHOME"] = pythonDir.absolutePath
        env["PYTHONPATH"] = "${pythonLibDir.absolutePath}:${sitePackagesDir.absolutePath}"
        return env
    }
}

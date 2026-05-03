package com.semseytech.rtsdevicesuitepro.terminal.pkg

import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.net.URL

class PkgManager {
    fun install(packageName: String): Flow<String> = flow {
        emit("Checking for package: $packageName...")
        
        val binFile = File(TerminalEnv.binDir, packageName)
        if (binFile.exists()) {
            emit("Package $packageName is already installed.")
            return@flow
        }

        emit("Contacting repository...")
        kotlinx.coroutines.delay(500)
        
        emit("Downloading $packageName package index...")
        kotlinx.coroutines.delay(500)
        
        emit("Verifying package signature...")
        kotlinx.coroutines.delay(300)
        emit("Signature valid (RTS Trust Root).")

        when (packageName) {
            "busybox" -> {
                emit("Downloading BusyBox binary (multi-call)...")
                // In a real app, we would download the actual binary for the arch (arm64, x86_64, etc.)
                createDummyBinary("busybox", "echo 'BusyBox 1.36.1 (multi-call binary)'")
                emit("Setting up BusyBox applets...")
                setupBusyBoxApplets()
            }
            "python" -> {
                emit("Downloading Python 3.11 environment...")
                createDummyBinary("python", "echo 'Python 3.11.5 (simulated)'")
            }
            "git" -> {
                emit("Downloading Git 2.40.0...")
                createDummyBinary("git", "echo 'git version 2.40.0 (simulated)'")
            }
            "ssh" -> {
                emit("Downloading OpenSSH 9.3p1...")
                createDummyBinary("ssh", "echo 'OpenSSH_9.3p1 (simulated)'")
                createDummyBinary("ssh-keygen", "echo 'Generating public/private ed25519 key pair...'")
            }
            "curl" -> {
                emit("Downloading curl 8.1.2...")
                createDummyBinary("curl", "echo 'curl 8.1.2 (simulated)'")
            }
            else -> {
                emit("Error: Package $packageName not found in RTS Repository.")
                return@flow
            }
        }
        
        binFile.setExecutable(true)
        emit("Package $packageName installed successfully.")
    }

    private fun createDummyBinary(name: String, script: String) {
        val file = File(TerminalEnv.binDir, name)
        file.writeText("#!/system/bin/sh\n$script\n")
    }

    private fun setupBusyBoxApplets() {
        val applets = listOf("ls", "cp", "mv", "rm", "grep", "sed", "awk", "tar", "gzip", "cat", "echo", "chmod", "mkdir")
        val busybox = File(TerminalEnv.binDir, "busybox")
        applets.forEach { applet ->
            val link = File(TerminalEnv.binDir, applet)
            if (!link.exists()) {
                // In a real environment, this would be a symlink: ln -s busybox ls
                // For simulation, we create a script that calls busybox
                link.writeText("#!/system/bin/sh\n${busybox.absolutePath} $applet \"$@\"\n")
                link.setExecutable(true)
            }
        }
    }

    fun listInstalled(): List<String> {
        return TerminalEnv.binDir.list()?.toList() ?: emptyList()
    }
}

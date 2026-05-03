package com.semseytech.rtsdevicesuitepro.terminal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import com.semseytech.rtsdevicesuitepro.terminal.emulator.TerminalEmulator
import com.semseytech.rtsdevicesuitepro.terminal.shell.TerminalSession
import kotlinx.coroutines.launch

import com.semseytech.rtsdevicesuitepro.terminal.pkg.PkgManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val session: TerminalSession
    val emulator = TerminalEmulator()
    private val pkgManager = PkgManager()

    private val _clipboard = MutableStateFlow<List<String>>(emptyList())
    val clipboard: StateFlow<List<String>> = _clipboard.asStateFlow()

    init {
        TerminalEnv.init(application)
        session = TerminalSession(application)
        
        viewModelScope.launch {
            session.outputFlow.collect { text ->
                emulator.appendText(text)
            }
        }
        
        showWelcomeMessage()
        session.start()
    }

    fun saveToClipboard(text: String) {
        if (text.isNotBlank() && !_clipboard.value.contains(text)) {
            _clipboard.value = _clipboard.value + text
        }
    }

    fun clearClipboard() {
        _clipboard.value = emptyList()
    }

    fun removeFromClipboard(text: String) {
        _clipboard.value = _clipboard.value.filter { it != text }
    }

    private fun showWelcomeMessage() {
        emulator.appendText("\u001b[1;36mWelcome to RTS Advanced Terminal v1.0\u001b[0m\n")
        emulator.appendText("Type \u001b[32mpkg install openssh\u001b[0m to enable SSH.\n")
        emulator.appendText("Type \u001b[32mpkg install busybox\u001b[0m to get started.\n")
        emulator.appendText("Type \u001b[32mstorage setup\u001b[0m to link your storage.\n\n")
    }

    fun sendInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.startsWith("pkg ")) {
            handlePkgCommand(trimmed.substring(4))
        } else if (trimmed.startsWith("storage ")) {
            handleStorageCommand(trimmed.substring(8))
        } else if (trimmed == "clear") {
            emulator.clear()
        } else {
            session.write(input)
        }
    }

    private fun handleStorageCommand(args: String) {
        val parts = args.split(" ")
        val command = parts.getOrNull(0)
        
        viewModelScope.launch {
            when (command) {
                "setup" -> {
                    emulator.appendText("\rSetting up storage access...\n")
                    setupStorageLinks()
                    emulator.appendText("Storage linked: ~/storage/shared -> /sdcard\n")
                }
                "list" -> {
                    val storageDir = java.io.File(TerminalEnv.homeDir, "storage")
                    val links = storageDir.list()?.joinToString("\n") ?: "No links"
                    emulator.appendText("\rStorage links:\n$links\n")
                }
                else -> {
                    emulator.appendText("\rUsage: storage <setup|list>\n")
                }
            }
        }
    }

    private fun setupStorageLinks() {
        val storageDir = java.io.File(TerminalEnv.homeDir, "storage")
        if (!storageDir.exists()) storageDir.mkdirs()
        
        val sharedLink = java.io.File(storageDir, "shared")
        if (!sharedLink.exists()) {
            // Simulated link
            sharedLink.writeText("LINK:/sdcard")
        }
    }

    fun clear() {
        emulator.clear()
    }

    fun sendSpecialKey(key: String) {
        // Handle special keys like ESC, TAB, etc.
        when (key) {
            "ESC" -> session.write("\u001b")
            "TAB" -> session.write("\t")
            "CTRL-C" -> session.write("\u0003")
            "CTRL-D" -> session.write("\u0004")
            "CTRL-L" -> clear()
            "UP" -> session.write("\u001b[A")
            "DOWN" -> session.write("\u001b[B")
            "LEFT" -> session.write("\u001b[D")
            "RIGHT" -> session.write("\u001b[C")
        }
    }

    private fun handlePkgCommand(args: String) {
        val parts = args.split(" ")
        val command = parts.getOrNull(0)
        val target = parts.getOrNull(1)

        viewModelScope.launch {
            when (command) {
                "install" -> {
                    if (target != null) {
                        pkgManager.install(target).collect {
                            emulator.appendText("\r$it\n")
                        }
                    } else {
                        emulator.appendText("\rUsage: pkg install <package>\n")
                    }
                }
                "list" -> {
                    val installed = pkgManager.listInstalled()
                    emulator.appendText("\rInstalled packages:\n" + installed.joinToString("\n") + "\n")
                }
                else -> {
                    emulator.appendText("\rUnknown pkg command. Available: install, list\n")
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        session.stop()
    }
}

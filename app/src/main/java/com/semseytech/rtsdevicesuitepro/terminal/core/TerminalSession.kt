package com.semseytech.rtsdevicesuitepro.terminal.core

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class TerminalSession(val id: Int = 0) {
    val env = ShellEnvironment()
    private val engine = CommandEngine(env)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _outputFlow = MutableSharedFlow<String>()
    val outputFlow = _outputFlow.asSharedFlow()

    private var history = mutableListOf<String>()
    private var historyIndex = -1

    fun start() {
        scope.launch {
            _outputFlow.emit("\u001b[1;34mRTS Terminal Session $id started.\u001b[0m\n")
            updatePrompt()
        }
    }

    private suspend fun updatePrompt() {
        val pwd = env.currentWorkingDirectory.absolutePath.replace(env.getVariable("HOME"), "~")
        _outputFlow.emit("\n\u001b[1;32mrts_user@android\u001b[0m:\u001b[1;34m$pwd\u001b[0m$ ")
    }

    fun write(input: String) {
        scope.launch {
            if (input.endsWith("\n")) {
                val cmd = input.trim()
                if (cmd.isNotEmpty()) {
                    history.add(cmd)
                    historyIndex = history.size
                    
                    val result = engine.executeLine(cmd)
                    if (result == "EXIT_SESSION") {
                        _outputFlow.emit("\n[Session $id terminated]\n")
                    } else {
                        if (result.isNotEmpty()) {
                            _outputFlow.emit("\n$result")
                        }
                    }
                }
                updatePrompt()
            } else {
                // Handling partial input or control characters
                _outputFlow.emit(input)
            }
        }
    }

    fun complete(line: String): String {
        return engine.complete(line)
    }

    fun getHistoryUp(): String? {
        if (historyIndex > 0) {
            historyIndex--
            return history[historyIndex]
        }
        return null
    }

    fun getHistoryDown(): String? {
        if (historyIndex < history.size - 1) {
            historyIndex++
            return history[historyIndex]
        }
        historyIndex = history.size
        return ""
    }

    fun stop() {
        scope.cancel()
    }
}

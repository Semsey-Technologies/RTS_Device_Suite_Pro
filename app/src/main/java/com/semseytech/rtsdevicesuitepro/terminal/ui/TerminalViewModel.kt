package com.semseytech.rtsdevicesuitepro.terminal.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import com.semseytech.rtsdevicesuitepro.terminal.core.TerminalSession
import com.semseytech.rtsdevicesuitepro.terminal.core.ProcessManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val _activeSessionIndex = MutableStateFlow(0)
    val activeSessionIndex: StateFlow<Int> = _activeSessionIndex.asStateFlow()

    val processManager = ProcessManager()
    private val sessionIdCounter = AtomicInteger(1)

    private val _clipboard = MutableStateFlow<List<String>>(emptyList())
    val clipboard: StateFlow<List<String>> = _clipboard.asStateFlow()

    init {
        TerminalEnv.init(application)
        addNewSession()
    }

    fun addNewSession() {
        val session = TerminalSession(sessionIdCounter.getAndIncrement())
        _sessions.value = _sessions.value + session
        _activeSessionIndex.value = _sessions.value.lastIndex
        session.start()
    }

    fun removeSession(index: Int) {
        val list = _sessions.value.toMutableList()
        if (list.size > 1) {
            val session = list.removeAt(index)
            session.stop()
            _sessions.value = list
            if (_activeSessionIndex.value >= list.size) {
                _activeSessionIndex.value = list.size - 1
            }
        }
    }

    fun setActiveSession(index: Int) {
        if (index in _sessions.value.indices) {
            _activeSessionIndex.value = index
        }
    }

    fun sendInput(input: String) {
        val activeSession = _sessions.value.getOrNull(_activeSessionIndex.value)
        activeSession?.write(input)
    }

    fun sendSpecialKey(key: String, currentInput: String = ""): String {
        val activeSession = _sessions.value.getOrNull(_activeSessionIndex.value) ?: return currentInput
        when (key) {
            "ESC" -> activeSession.write("\u001b")
            "TAB" -> {
                val completed = activeSession.complete(currentInput)
                return completed
            }
            "CTRL-C" -> activeSession.write("\u0003")
            "CTRL-D" -> activeSession.write("\u0004")
            "UP" -> activeSession.getHistoryUp()?.let { return it }
            "DOWN" -> activeSession.getHistoryDown()?.let { return it }
        }
        return currentInput
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

    override fun onCleared() {
        super.onCleared()
        _sessions.value.forEach { it.stop() }
        processManager.stopAll()
    }
}

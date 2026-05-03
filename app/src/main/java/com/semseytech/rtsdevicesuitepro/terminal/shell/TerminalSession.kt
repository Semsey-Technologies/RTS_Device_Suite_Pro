package com.semseytech.rtsdevicesuitepro.terminal.shell

import com.semseytech.rtsdevicesuitepro.terminal.TerminalEnv
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets

class TerminalSession(
    private val context: android.content.Context,
    private val command: String = "/system/bin/sh",
    private val env: Map<String, String> = emptyMap()
) {
    private var process: Process? = null
    private var outputStream: OutputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _outputFlow = MutableSharedFlow<String>()
    val outputFlow = _outputFlow.asSharedFlow()

    fun start() {
        scope.launch {
            try {
                val pb = ProcessBuilder(command)
                pb.directory(TerminalEnv.homeDir)
                val fullEnv = pb.environment()
                fullEnv.putAll(TerminalEnv.getEnvironment(context))
                fullEnv.putAll(env)
                
                process = pb.start()
                outputStream = process?.outputStream

                launch {
                    readStream(process?.inputStream)
                }
                launch {
                    readStream(process?.errorStream)
                }

                process?.waitFor()
                _outputFlow.emit("\n[Process exited]")
            } catch (e: Exception) {
                _outputFlow.emit("\n[Error: ${e.message}]")
            }
        }
    }

    private suspend fun readStream(inputStream: InputStream?) {
        try {
            inputStream?.use { stream ->
                val buffer = ByteArray(1024)
                var bytesRead: Int
                while (stream.read(buffer).also { bytesRead = it } != -1) {
                    val text = String(buffer, 0, bytesRead, StandardCharsets.UTF_8)
                    _outputFlow.emit(text)
                }
            }
        } catch (e: java.io.IOException) {
            // Expected when process is destroyed or stream is closed
            android.util.Log.d("TerminalSession", "Stream closed: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("TerminalSession", "Error reading stream", e)
        }
    }

    fun write(text: String) {
        scope.launch {
            outputStream?.write(text.toByteArray(StandardCharsets.UTF_8))
            outputStream?.flush()
        }
    }

    fun stop() {
        process?.destroy()
        scope.cancel()
    }
}

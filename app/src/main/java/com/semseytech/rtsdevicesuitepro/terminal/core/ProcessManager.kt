package com.semseytech.rtsdevicesuitepro.terminal.core

import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

data class RTSProcess(
    val pid: Int,
    val name: String,
    val startTime: Long,
    val job: Job
)

class ProcessManager {
    private val processes = ConcurrentHashMap<Int, RTSProcess>()
    private val pidCounter = AtomicInteger(1000)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startBackgroundTask(name: String, task: suspend () -> Unit): Int {
        val pid = pidCounter.incrementAndGet()
        val job = scope.launch {
            try {
                task()
            } finally {
                processes.remove(pid)
            }
        }
        processes[pid] = RTSProcess(pid, name, System.currentTimeMillis(), job)
        return pid
    }

    fun listProcesses(): List<RTSProcess> = processes.values.toList()

    fun killProcess(pid: Int): Boolean {
        val proc = processes.remove(pid)
        return if (proc != null) {
            proc.job.cancel()
            true
        } else {
            false
        }
    }

    fun stopAll() {
        scope.cancel()
        processes.clear()
    }
}

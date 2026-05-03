package com.semseytech.rtsdevicesuitepro.automation.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.semseytech.rtsdevicesuitepro.automation.engine.AutomationEngine

class PeriodicAutomationWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val engine = AutomationEngine(applicationContext)
        engine.onTrigger("TIME_OF_DAY") // Check time-based rules
        return Result.success()
    }
}

package com.neytron.sshcommander.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neytron.sshcommander.widget.ServerWidgetProvider

class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ServerWidgetProvider.triggerUpdate(applicationContext)
        return Result.success()
    }
}

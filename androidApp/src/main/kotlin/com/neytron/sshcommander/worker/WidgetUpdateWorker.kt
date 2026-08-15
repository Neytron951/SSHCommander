package com.neytron.sshcommander.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neytron.sshcommander.widget.ServerWidgetProvider

/**
 * Periodic worker that refreshes the widget data. Android-only.
 */
class WidgetUpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        ServerWidgetProvider.triggerUpdate(applicationContext)
        return Result.success()
    }
}

package com.neytron.sshcommander

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.neytron.sshcommander.data.JsonServerRepository
import com.neytron.sshcommander.ui.AndroidAppContext
import com.neytron.sshcommander.worker.WidgetUpdateWorker
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * App-level initialization for Android-only features:
 * periodic widget refresh + Yandex Mobile Ads SDK.
 */
class SshCommanderApplication : Application() {
    /** Shared server persistence (also used by widgets and the settings screens). */
    val serverRepository: JsonServerRepository by lazy {
        JsonServerRepository(File(filesDir, "data"))
    }

    override fun onCreate() {
        super.onCreate()
        AndroidAppContext.appContext = applicationContext
        setupWidgetUpdateWorker()

        // Инициализация Yandex Mobile Ads SDK согласно инструкции
        AdsSdk.initialize(this)
    }

    private fun setupWidgetUpdateWorker() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "WidgetUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}

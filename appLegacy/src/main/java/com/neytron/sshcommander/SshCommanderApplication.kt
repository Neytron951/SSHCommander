package com.neytron.sshcommander

import android.app.Application
import androidx.work.*
import com.neytron.sshcommander.worker.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

class SshCommanderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
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

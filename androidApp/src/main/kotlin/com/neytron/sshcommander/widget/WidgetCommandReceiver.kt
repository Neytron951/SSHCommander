package com.neytron.sshcommander.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.neytron.sshcommander.MainActivity
import com.neytron.sshcommander.worker.SshWorker

/**
 * Routes widget button clicks to background SSH commands or to the app itself.
 * Android-only.
 */
class WidgetCommandReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val serverId = intent.getIntExtra("serverId", -1)
        val actionType = intent.getStringExtra("action_type") ?: "OPEN"

        if (serverId == -1) return

        when (actionType) {
            "RUN" -> {
                val command = intent.getStringExtra("command")
                if (!command.isNullOrEmpty()) {
                    executeCommand(context, serverId, command)
                }
            }
            "REBOOT" -> {
                executeCommand(context, serverId, "sudo reboot")
            }
            "OPEN" -> {
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    putExtra("serverId", serverId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(mainIntent)
            }
        }
    }

    private fun executeCommand(context: Context, serverId: Int, command: String) {
        val workRequest = OneTimeWorkRequestBuilder<SshWorker>()
            .setInputData(Data.Builder()
                .putInt("serverId", serverId)
                .putString("command", command)
                .build())
            .build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }
}

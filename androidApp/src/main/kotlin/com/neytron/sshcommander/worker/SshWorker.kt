package com.neytron.sshcommander.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.JsonServerRepository
import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Properties

/**
 * Executes an SSH command in the background (triggered from the widget) and
 * reports the result via a notification. Android-only.
 */
class SshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val serverId = inputData.getInt("serverId", -1)
        val command = inputData.getString("command") ?: return Result.failure()

        if (serverId == -1) return Result.failure()

        val repository = JsonServerRepository(File(applicationContext.filesDir, "data"))
        val server = repository.getServers().firstOrNull { it.id == serverId } ?: return Result.failure()
        val password = repository.getPassword(serverId) ?: ""

        // Stage 1: Notification indicating work has started
        showNotification(server.name, "Executing: $command", isOngoing = true)

        return try {
            val result = performSshCommand(server, password, command)

            val cleanOutput = result.trim().take(200).let { if (it.length >= 200) "$it..." else it }
            val notificationText = if (cleanOutput.isNotEmpty()) cleanOutput else "Command finished successfully."

            // Stage 2: Success Notification
            showNotification(server.name, notificationText, isOngoing = false)
            Result.success()
        } catch (e: Exception) {
            // Stage 2: Error Notification
            showNotification(server.name, "Failed: ${e.localizedMessage}", isOngoing = false)
            Result.failure()
        }
    }

    private fun showNotification(serverName: String, message: String, isOngoing: Boolean) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Change ID to force refresh channel importance on some devices
        val channelId = "ssh_ops_channel_v2"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SSH Operations", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Shows status of SSH commands executed from widgets"
                enableLights(true)
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.terminal_24dp)
            .setContentTitle(serverName)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(isOngoing)
            .setAutoCancel(!isOngoing)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setSilent(false)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        // Use a unique ID based on server but fixed for the operation to update the same notification
        manager.notify(serverName.hashCode(), notification)
    }

    private fun performSshCommand(
        server: com.neytron.sshcommander.data.Server,
        password: String,
        command: String
    ): String {
        val jsch = JSch()
        val session: Session = jsch.getSession(server.username, server.host, server.port)
        session.setPassword(password)
        val config = Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        session.connect(10000)

        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        val outputStream = ByteArrayOutputStream()
        val errorStream = ByteArrayOutputStream()
        channel.setOutputStream(outputStream)
        channel.setErrStream(errorStream)
        channel.connect()

        while (!channel.isClosed) {
            Thread.sleep(150)
        }

        val res = outputStream.toString().trim()
        val err = errorStream.toString().trim()

        channel.disconnect()
        session.disconnect()

        return when {
            res.isNotEmpty() -> res
            err.isNotEmpty() -> "Error: $err"
            else -> ""
        }
    }
}

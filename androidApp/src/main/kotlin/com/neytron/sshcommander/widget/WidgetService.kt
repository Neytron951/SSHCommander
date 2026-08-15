package com.neytron.sshcommander.widget

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.JsonServerRepository
import com.neytron.sshcommander.data.NetworkUtils
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.SettingsManager
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * RemoteViewsService backing the large widget's server list.
 * Android-only — the desktop build has no widgets.
 */
class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return ServerRemoteViewsFactory(applicationContext)
    }
}

class ServerRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var servers: List<Server> = emptyList()
    private var textColor = "#FFFFFF"
    private var accentColor = "#03DAC6"
    private var itemBgColor = "#1A1C1E"

    override fun onCreate() {}

    override fun onDataSetChanged() {
        runBlocking {
            val repo = JsonServerRepository(File(context.filesDir, "data"))
            val settings = SettingsManager(context)
            servers = repo.getServers().filter { it.showInWidget }

            textColor = settings.widgetTextColor.first()
            accentColor = settings.widgetAccentColor.first()
            itemBgColor = settings.widgetItemBgColor.first()
        }
    }

    override fun onDestroy() {}
    override fun getCount(): Int = servers.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= servers.size) return RemoteViews(context.packageName, R.layout.server_widget_item)

        val server = servers[position]
        val views = RemoteViews(context.packageName, R.layout.server_widget_item)

        // 1. Identity & Custom Colors
        views.setTextViewText(R.id.server_name, server.name)
        try {
            views.setTextColor(R.id.server_name, Color.parseColor(textColor))
            // Apply background color to the background ImageView
            views.setInt(R.id.item_background_image, "setColorFilter", Color.parseColor(itemBgColor))
        } catch (e: Exception) {}

        // 2. Health Monitoring (TCP check on the SSH port — ICMP is unreliable on Android)
        val isOnline = NetworkUtils.isPortOpen(server.host, server.port, timeoutMs = 1500)

        val statusText = if (isOnline) context.getString(R.string.online_caps) else context.getString(R.string.offline_caps)
        val statusColor = if (isOnline) accentColor else "#CF6679"

        views.setTextViewText(R.id.server_status_text, statusText)
        try {
            views.setTextColor(R.id.server_status_text, Color.parseColor(statusColor))
            views.setInt(R.id.status_dot, "setColorFilter", Color.parseColor(statusColor))

            // Apply accent color tinting to both icons
            val tint = Color.parseColor(accentColor)
            views.setInt(R.id.btn_run_icon, "setColorFilter", tint)
            views.setInt(R.id.btn_reboot_icon, "setColorFilter", tint)
        } catch (e: Exception) {}

        // 3. Command Action mapping
        val rawCommand = server.widgetCommand ?: "uptime"
        val mappedCommand = when (rawCommand) {
            "Status", "Uptime" -> "uptime"
            "Reboot" -> "sudo reboot"
            "Free -m" -> "free -m"
            else -> rawCommand // User's actual custom command
        }

        val runIntent = Intent().apply {
            putExtra("action_type", "RUN")
            putExtra("serverId", server.id)
            putExtra("command", mappedCommand)
        }
        views.setOnClickFillInIntent(R.id.btn_run_command, runIntent)

        // 4. Reboot Action
        val rebootIntent = Intent().apply {
            putExtra("action_type", "REBOOT")
            putExtra("serverId", server.id)
        }
        views.setOnClickFillInIntent(R.id.btn_reboot, rebootIntent)

        // 5. Open App
        val openIntent = Intent().apply {
            putExtra("action_type", "OPEN")
            putExtra("serverId", server.id)
        }
        views.setOnClickFillInIntent(R.id.info_container, openIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = if (position < servers.size) servers[position].id.toLong() else position.toLong()
    override fun hasStableIds(): Boolean = true
}

package com.neytron.sshcommander.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.RemoteViews
import com.neytron.sshcommander.MainActivity
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.JsonServerRepository
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.ui.WidgetIconUtils
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Home-screen widget. Android-only — the desktop build has no widgets.
 * Reads servers through the same shared [JsonServerRepository] the app uses.
 */
class ServerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        updateAppWidget(context, appWidgetManager, appWidgetId)
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

            val isSmall = minWidth < 180
            val layoutRes = if (isSmall) R.layout.widget_small_layout else R.layout.widget_large_layout
            val views = RemoteViews(context.packageName, layoutRes)

            if (isSmall) {
                setupSmallWidget(context, views)
            } else {
                setupLargeWidget(context, views, appWidgetId)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setupSmallWidget(context: Context, views: RemoteViews) {
            runBlocking {
                val repo = JsonServerRepository(File(context.filesDir, "data"))
                val server = repo.getServers().filter { it.showInWidget }.firstOrNull()
                val settings = SettingsManager(context)
                val bgColor = settings.termBgColor.first().ifEmpty { "#2C2C2C" }
                val textColor = settings.termTextColor.first().ifEmpty { "#FFFFFF" }

                if (server != null) {
                    views.setTextViewText(R.id.widget_server_name, server.name)
                    views.setImageViewResource(R.id.widget_server_icon, WidgetIconUtils.getIconResource(server.iconName))

                    try {
                        // Use a light black if it's pure black
                        val displayBg = if (bgColor == "#000000") "#2C2C2C" else bgColor
                        views.setInt(R.id.widget_background, "setBackgroundColor", Color.parseColor(displayBg))
                        views.setTextColor(R.id.widget_server_name, Color.parseColor(textColor))
                    } catch (e: Exception) {}

                    // Action: Run Command
                    val runIntent = Intent(context, WidgetCommandReceiver::class.java).apply {
                        action = "com.neytron.sshcommander.ACTION_WIDGET_CONTROL"
                        putExtra("action", "RUN")
                        putExtra("serverId", server.id)
                        putExtra("command", server.widgetCommand ?: "uptime")
                    }
                    val runPendingIntent = PendingIntent.getBroadcast(
                        context, server.id + 100, runIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_btn_status, runPendingIntent)

                    // Action: Open App
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        putExtra("serverId", server.id)
                    }
                    val openPendingIntent = PendingIntent.getActivity(
                        context, server.id + 200, openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_background, openPendingIntent)
                }
            }
        }

        private fun setupLargeWidget(context: Context, views: RemoteViews, appWidgetId: Int) {
            val intent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_server_list, intent)
            views.setEmptyView(R.id.widget_server_list, android.R.id.empty)

            // Collection items use a PendingIntent template
            // We route all clicks through WidgetCommandReceiver to distinguish actions
            val templateIntent = Intent(context, WidgetCommandReceiver::class.java).apply {
                action = "com.neytron.sshcommander.ACTION_WIDGET_CONTROL"
            }
            val templatePendingIntent = PendingIntent.getBroadcast(
                context, 0, templateIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_server_list, templatePendingIntent)
        }

        fun triggerUpdate(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, ServerWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            // Refresh data
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.widget_server_list)

            // Refresh layouts
            val updateIntent = Intent(context, ServerWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        }
    }
}

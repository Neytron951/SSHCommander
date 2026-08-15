package com.neytron.sshcommander.ui

import com.neytron.sshcommander.R

/**
 * Widget-only icon helpers. The Compose `ImageVector` mapping lives in the
 * shared UI layer; RemoteViews need a drawable resource id instead.
 */
object WidgetIconUtils {
    fun getIconResource(iconName: String?): Int {
        return when (iconName) {
            "Dev", "Terminal" -> R.drawable.terminal_24dp
            else -> R.drawable.terminal_24dp // Fallback to terminal icon for now
        }
    }
}

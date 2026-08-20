package com.neytron.sshcommander.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.neytron.sshcommander.R

object IconUtils {
    data class IconOption(val name: String, val icon: ImageVector, val labelRes: Int)

    val availableIcons = listOf(
        IconOption("SportsEsports", Icons.Filled.SportsEsports, R.string.icon_gaming),
        IconOption("Web", Icons.Filled.Web, R.string.icon_web),
        IconOption("Database", Icons.Filled.Storage, R.string.icon_database),
        IconOption("Cloud", Icons.Filled.Cloud, R.string.icon_cloud),
        IconOption("NAS", Icons.Filled.Dns, R.string.icon_nas),
        IconOption("VPN", Icons.Filled.VpnLock, R.string.icon_vpn),
        IconOption("Dev", Icons.Filled.Terminal, R.string.icon_dev),
        IconOption("Media", Icons.Filled.PermMedia, R.string.icon_media),
        IconOption("Default", Icons.Filled.Computer, R.string.icon_default)
    )

    fun getIcon(iconName: String?): ImageVector {
        return availableIcons.find { it.name == iconName }?.icon ?: Icons.Filled.Dns
    }

    // Map icon names to drawable resources for Widgets
    fun getIconResource(iconName: String?): Int {
        return when (iconName) {
            "Dev", "Terminal" -> R.drawable.terminal_24dp
            else -> R.drawable.terminal_24dp // Fallback to terminal icon for now
        }
    }
}

package com.neytron.sshcommander.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnLock
import androidx.compose.material.icons.filled.Web
import androidx.compose.ui.graphics.vector.ImageVector

object IconUtils {
    data class IconOption(val name: String, val icon: ImageVector, val label: String)

    val availableIcons = listOf(
        IconOption("SportsEsports", Icons.Filled.SportsEsports, AppStrings.iconGaming),
        IconOption("Web", Icons.Filled.Web, AppStrings.iconWeb),
        IconOption("Database", Icons.Filled.Storage, AppStrings.iconDatabase),
        IconOption("Cloud", Icons.Filled.Cloud, AppStrings.iconCloud),
        IconOption("NAS", Icons.Filled.Dns, AppStrings.iconNas),
        IconOption("VPN", Icons.Filled.VpnLock, AppStrings.iconVpn),
        IconOption("Dev", Icons.Filled.Terminal, AppStrings.iconDev),
        IconOption("Media", Icons.Filled.PermMedia, AppStrings.iconMedia),
        IconOption("Default", Icons.Filled.Computer, AppStrings.iconDefault)
    )

    fun getIcon(iconName: String?): ImageVector {
        return availableIcons.find { it.name == iconName }?.icon ?: Icons.Filled.Dns
    }
}

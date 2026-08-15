package com.neytron.sshcommander.data

/**
 * Pure data model for a server connection target (no Room annotations here —
 * platform-specific persistence keeps its own entities).
 */
data class Server(
    val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val passwordKey: String = "",
    val privateKeyPath: String? = null,
    val passphraseKey: String? = null,
    val hostKey: String? = null,
    val hostKeyType: String? = null,
    val iconName: String = "Default",
    val showInWidget: Boolean = false,
    val widgetCommand: String? = null,
    val sftpStartPath: String? = null,
    val lastSftpPath: String? = null,
    val folderId: Int? = null
)

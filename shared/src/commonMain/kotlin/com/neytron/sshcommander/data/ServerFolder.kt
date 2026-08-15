package com.neytron.sshcommander.data

/**
 * A grouping folder for servers. Servers reference a folder by [Server.folderId];
 * servers without a folder belong to the implicit "Unfiled" group.
 */
data class ServerFolder(
    val id: Int = 0,
    val name: String
)

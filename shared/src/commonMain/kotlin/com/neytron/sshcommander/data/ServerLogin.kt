package com.neytron.sshcommander.data

data class ServerLogin(
    val id: Int = 0,
    val serverId: Int,
    val label: String,
    val username: String,
    val passwordKey: String = "",
    val privateKeyPath: String? = null,
    val passphraseKey: String? = null,
    val sftpStartPath: String? = null,
    val lastSftpPath: String? = null,
    val isDefault: Boolean = false,
    val sshKeyId: Int? = null
)

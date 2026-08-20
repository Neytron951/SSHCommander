package com.neytron.sshcommander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "server_logins")
data class ServerLogin(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: Int,
    val label: String,
    val username: String,
    val passwordKey: String = "",
    val privateKeyPath: String? = null,
    val passphraseKey: String? = null,
    val sftpStartPath: String? = null,
    val lastSftpPath: String? = null,
    val isDefault: Boolean = false
)

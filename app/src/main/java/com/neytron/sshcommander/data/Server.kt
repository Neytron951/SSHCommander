package com.neytron.sshcommander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class Server(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val passwordKey: String = "", // Fixed: Matches NOT NULL in existing DB
    val privateKeyPath: String? = null,
    val passphraseKey: String? = null,
    val hostKey: String? = null,
    val hostKeyType: String? = null, // Added to stabilize algorithm selection
    val iconName: String = "Default",
    val showInWidget: Boolean = false,
    val widgetCommand: String? = null,
    val sftpStartPath: String? = null,
    val lastSftpPath: String? = null
)

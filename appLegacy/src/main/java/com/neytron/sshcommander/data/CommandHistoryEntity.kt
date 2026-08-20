package com.neytron.sshcommander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val serverId: Int,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)

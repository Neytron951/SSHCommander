package com.neytron.sshcommander.data

data class CommandHistoryEntity(
    val id: Int = 0,
    val serverId: Int,
    val command: String,
    val output: String,
    val timestamp: Long = System.currentTimeMillis()
)

package com.neytron.sshcommander.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_commands")
data class CustomCommand(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val command: String, // Store multiple commands separated by newlines
    val iconName: String,
    val colorHex: String,
    val orderIndex: Int,
    val isDangerous: Boolean = false
)

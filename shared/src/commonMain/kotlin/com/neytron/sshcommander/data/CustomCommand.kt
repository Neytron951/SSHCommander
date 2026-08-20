package com.neytron.sshcommander.data

data class CustomCommand(
    val id: Int = 0,
    val name: String,
    val command: String,
    val iconName: String,
    val colorHex: String,
    val orderIndex: Int,
    val isDangerous: Boolean = false,
    val categoryName: String? = null,
    val variables: List<String>? = null
)

package com.neytron.sshcommander.data

import kotlinx.serialization.Serializable

@Serializable
data class MarketScript(
    val id: String,
    val name: String,
    val description: String,
    val command: String,
    val author: String,
    val category: String,
    val compatibleOs: List<String> = emptyList(),
    val isDangerous: Boolean = false,
    val githubUrl: String? = null
) {
    /**
     * Parses the command string to find all unique variables in {{VAR_NAME}} format.
     */
    fun extractVariables(): List<String> {
        val regex = Regex("\\{\\{(.*?)\\}\\}")
        return regex.findAll(command)
            .map { it.groupValues[1].trim() }
            .distinct()
            .toList()
    }
    
    /**
     * Replaces placeholders in the command with provided values.
     */
    fun buildFinalCommand(values: Map<String, String>): String {
        var finalCmd = command
        values.forEach { (key, value) ->
            finalCmd = finalCmd.replace("{{$key}}", value)
        }
        return finalCmd
    }
}

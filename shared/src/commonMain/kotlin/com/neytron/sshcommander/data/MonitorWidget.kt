package com.neytron.sshcommander.data

import kotlinx.serialization.Serializable

@Serializable
enum class WidgetType {
    PERCENTAGE, // Shows progress bar (0-100)
    BYTES,      // Formats as GB/MB/KB
    TEXT        // Raw text output
}

@Serializable
data class MonitorWidget(
    val id: String,
    val title: String,
    val command: String,
    val type: WidgetType = WidgetType.TEXT,
    val isWide: Boolean = false,
    val colorHex: String? = null
) {
    companion object {
        fun createDefault(): List<MonitorWidget> = listOf(
            MonitorWidget(
                id = "cpu",
                title = "CPU Load",
                command = "top -bn1 | grep 'Cpu(s)' | awk '{print 100 - \$8}'",
                type = WidgetType.PERCENTAGE,
                colorHex = "#4CAF50"
            ),
            MonitorWidget(
                id = "ram",
                title = "RAM Usage",
                command = "free -b | grep Mem | awk '{print \$3}'",
                type = WidgetType.BYTES,
                colorHex = "#2196F3"
            ),
            MonitorWidget(
                id = "disk",
                title = "Disk Space (/)",
                command = "df -B1 / | tail -n 1 | awk '{print \$3}'",
                type = WidgetType.BYTES,
                colorHex = "#FF9800"
            ),
            MonitorWidget(
                id = "uptime",
                title = "Uptime",
                command = "uptime -p",
                type = WidgetType.TEXT,
                isWide = true
            )
        )
    }
}

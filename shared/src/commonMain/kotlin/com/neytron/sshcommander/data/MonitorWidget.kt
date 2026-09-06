package com.neytron.sshcommander.data

import kotlinx.serialization.Serializable

@Serializable
enum class WidgetType {
    PERCENTAGE, // Shows progress bar (0-100)
    BYTES,      // Formats as GB/MB/KB
    TEXT,       // Raw text output
    CHART       // Shows a small line chart (needs numeric output)
}

@Serializable
data class MonitorWidget(
    val id: String,
    val title: String,
    val command: String,
    val type: WidgetType = WidgetType.TEXT,
    val x: Int = 0,      // Grid X (0..11)
    val y: Int = 0,      // Grid Y
    val w: Int = 3,      // Width in columns (1..12)
    val h: Int = 2,      // Height in grid units
    val fontSize: Float = 16f, // Font size for value
    val colorHex: String? = null,
    val textAlign: String = "center", // "left", "center", "right"
    val textVerticalAlign: String = "center" // "top", "center", "bottom"
) {
    companion object {
        data class Preset(val title: String, val command: String, val type: WidgetType, val w: Int = 3, val h: Int = 2, val fontSize: Float = 16f, val color: String? = null, val textAlign: String = "center", val textVerticalAlign: String = "center")

        val PRESETS = listOf(
            Preset("CPU History", "top -bn1 | grep 'Cpu(s)' | awk '{print 100 - \$8}'", WidgetType.CHART, 6, 2, 14f, "#4CAF50"),
            Preset("RAM History", "free | grep Mem | awk '{print \$3/\$2 * 100.0}'", WidgetType.CHART, 6, 2, 14f, "#2196F3"),
            Preset("Network Rx", "grep -E 'eth0|enp|ens|wlan' /proc/net/dev | head -n 1 | awk -F'[: ]+' '{print (\$2==\"\" ? \$3 : \$2)}'", WidgetType.CHART, 6, 2, 12f, "#FFEB3B"),
            Preset("CPU Load", "top -bn1 | grep 'Cpu(s)' | awk '{print 100 - \$8}'", WidgetType.PERCENTAGE, 3, 2, 20f, "#4CAF50"),
            Preset("RAM Usage", "free -b | grep Mem | awk '{print \$3}'", WidgetType.BYTES, 3, 2, 16f, "#2196F3"),
            Preset("Disk % (/)", "df / | tail -n 1 | awk '{print \$5}' | sed 's/%//'", WidgetType.PERCENTAGE, 3, 2, 18f, "#FF9800"),
            Preset("CPU Temp", "cat /sys/class/thermal/thermal_zone*/temp | head -n 1 | awk '{print \$1/1000\"°C\"}'", WidgetType.TEXT, 3, 1, 16f, "#FF5722"),
            Preset("Swap Usage", "free -b | grep Swap | awk '{print \$3}'", WidgetType.BYTES, 3, 1, 14f, "#9C27B0"),
            Preset("Uptime", "uptime -p", WidgetType.TEXT, 6, 1, 14f),
            Preset("Hostname", "hostname", WidgetType.TEXT, 3, 1, 14f, "#00BCD4"),
            Preset("Kernel", "uname -r", WidgetType.TEXT, 3, 1, 12f, "#607D8B"),
            Preset("Logged Users", "who | wc -l", WidgetType.TEXT, 3, 1, 18f, "#E91E63"),
            Preset("SSH Connections", "netstat -ntu | grep :22 | grep ESTABLISHED | wc -l", WidgetType.TEXT, 3, 1, 18f, "#FF9800"),
            Preset("Docker Count", "docker ps -q | wc -l 2>/dev/null || echo 'N/A'", WidgetType.TEXT, 3, 1, 18f, "#2196F3"),
            Preset("Load Avg", "uptime | awk -F'load average:' '{print \$2}' | sed 's/,//g'", WidgetType.TEXT, 6, 1, 14f, "#FF9800"),
            Preset("Mem Available", "free -b | grep Mem | awk '{print \$7}'", WidgetType.BYTES, 3, 1, 16f, "#4CAF50"),
            Preset("Top RAM Process", "ps -eo pmem,comm --sort=-pmem | head -n 2 | tail -n 1 | awk '{print \$2 \" (\" \$1 \"%)\"}'", WidgetType.TEXT, 6, 1, 13f),
            Preset("Public IP", "curl -s https://api.ipify.org", WidgetType.TEXT, 3, 1, 14f, "#FFC107")
        )

        fun createDefault(): List<MonitorWidget> = listOf(
            MonitorWidget("cpu", "CPU Load", "top -bn1 | grep 'Cpu(s)' | awk '{print 100 - \$8}'", WidgetType.PERCENTAGE, 0, 0, 3, 2, 20f, "#4CAF50"),
            MonitorWidget("ram", "RAM Usage", "free -b | grep Mem | awk '{print \$3}'", WidgetType.BYTES, 3, 0, 3, 2, 16f, "#2196F3"),
            MonitorWidget("disk", "Disk Space (/)", "df -B1 / | tail -n 1 | awk '{print \$3}'", WidgetType.BYTES, 6, 0, 3, 2, 16f, "#FF9800"),
            MonitorWidget("uptime", "Uptime", "uptime -p", WidgetType.TEXT, 0, 2, 6, 1, 14f)
        )
    }
}

package com.neytron.sshcommander.data

/**
 * Snapshot of server system metrics.
 */
data class ServerStats(
    val cpuLoad: Float = 0f,
    val ramUsed: Long = 0,
    val ramTotal: Long = 0,
    val diskUsed: Long = 0,
    val diskTotal: Long = 0,
    val uptime: String = "",
    val rawLogs: String = ""
) {
    val ramPercentage: Float get() = if (ramTotal > 0) ramUsed.toFloat() / ramTotal else 0f
    val diskPercentage: Float get() = if (diskTotal > 0) diskUsed.toFloat() / diskTotal else 0f
}

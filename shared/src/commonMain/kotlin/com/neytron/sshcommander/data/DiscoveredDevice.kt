package com.neytron.sshcommander.data

data class DiscoveredDevice(
    val name: String,
    val host: String,
    val port: Int,
    var pairingPort: Int? = null
)

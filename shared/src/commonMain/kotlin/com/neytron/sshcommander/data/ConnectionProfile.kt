package com.neytron.sshcommander.data

/**
 * Pure authentication profile for an SSH connection attempt.
 */
data class ConnectionProfile(
    val username: String,
    val password: String = "",
    val privateKeyPath: String? = null,
    val privateKeyContent: String? = null,
    val publicKeyContent: String? = null,
    val passphrase: String? = null
)

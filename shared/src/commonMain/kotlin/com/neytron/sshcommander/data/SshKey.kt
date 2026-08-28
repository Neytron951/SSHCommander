package com.neytron.sshcommander.data

/**
 * Represents an SSH key pair managed by the app.
 */
data class SshKey(
    val id: Int = 0,
    val name: String,
    val type: String = "RSA", // RSA, Ed25519
    val privateKeyPath: String? = null,
    val privateKeyContent: String? = null,
    val publicKeyContent: String? = null,
    val passphraseKey: String? = null,
    val createdAt: Long = 0
)

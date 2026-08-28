package com.neytron.sshcommander.data

expect object SshKeyUtils {
    /**
     * Generates a new SSH key pair.
     * @param type "RSA" or "DSA"
     * @param bits 2048, 3072, 4096 for RSA
     * @param passphrase Optional passphrase to encrypt the private key
     * @return Pair of (privateKeyContent, publicKeyContent)
     */
    fun generateKeyPair(type: String = "RSA", bits: Int = 2048, passphrase: String? = null): Pair<String, String>
}

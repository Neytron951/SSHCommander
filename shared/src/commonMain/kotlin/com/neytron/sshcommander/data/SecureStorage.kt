package com.neytron.sshcommander.data

/**
 * Platform-specific storage for sensitive values (passwords, passphrases).
 *
 * Android: EncryptedSharedPreferences / Keystore.
 * Desktop: Windows DPAPI (per-user encrypted blob) or a plain fallback.
 */
interface SecureStorage {
    /** Stores a value under the given key. Returns true on success. */
    suspend fun put(key: String, value: String): Boolean

    /** Returns the stored value, or null if absent. */
    suspend fun get(key: String): String?

    /** Removes a stored value. */
    suspend fun remove(key: String)
}

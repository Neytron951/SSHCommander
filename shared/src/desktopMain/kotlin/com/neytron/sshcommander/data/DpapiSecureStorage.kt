package com.neytron.sshcommander.data

import com.sun.jna.platform.win32.Crypt32Util
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Base64

/**
 * Windows DPAPI-backed [SecureStorage].
 *
 * Values are encrypted with CryptProtectData (current user scope) and stored
 * in a single file. Because the blob is tied to the Windows user account, it
 * cannot be decrypted by other users or on another machine.
 */
class DpapiSecureStorage(private val file: File) : SecureStorage {

    init {
        file.parentFile?.mkdirs()
    }

    override suspend fun put(key: String, value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val encrypted = Crypt32Util.cryptProtectData(value.toByteArray(Charsets.UTF_8))
            val entries = readEntries().toMutableMap()
            entries[key] = Base64.getEncoder().encodeToString(encrypted)
            writeEntries(entries)
            true
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun get(key: String): String? = withContext(Dispatchers.IO) {
        try {
            val encoded = readEntries()[key] ?: return@withContext null
            val decrypted = Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(encoded))
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        val entries = readEntries().toMutableMap()
        if (entries.remove(key) != null) writeEntries(entries)
    }

    // ---- internal helpers ----

    @Synchronized
    private fun readEntries(): Map<String, String> {
        if (!file.exists()) return emptyMap()
        return try {
            val lines = file.readText().lineSequence().filter { it.isNotBlank() }
            val map = mutableMapOf<String, String>()
            for (line in lines) {
                val idx = line.indexOf('=')
                if (idx > 0) map[line.substring(0, idx)] = line.substring(idx + 1)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @Synchronized
    private fun writeEntries(entries: Map<String, String>) {
        val sb = StringBuilder()
        for ((k, v) in entries) {
            sb.append(k).append('=').append(v).append('\n')
        }
        file.writeText(sb.toString())
    }
}

package com.neytron.sshcommander.data

import android.content.Context
import com.jcraft.jsch.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

data class ConnectionProfile(
    val username: String,
    val password: String = "",
    val privateKeyPath: String? = null,
    val passphrase: String? = null
)

class SshConnectionManager(private val context: Context) {
    private val jsch = JSch()
    private val repository = ServerRepository(context)

    companion object {
        // Global cache to share sessions between Terminal and SFTP ViewModels.
        // Keyed by "serverId:username" so different logins on the same server
        // can have their own active session.
        private val activeSessions = mutableMapOf<String, Session>()
        private val sessionMutex = Mutex()

        /**
         * Disconnects every session of the given server (all logins).
         */
        suspend fun closeSession(serverId: Int) = sessionMutex.withLock {
            val keys = activeSessions.keys.filter { it.startsWith("$serverId:") }
            keys.forEach { key ->
                activeSessions[key]?.disconnect()
                activeSessions.remove(key)
            }
        }
    }

    private fun sessionKey(server: Server, profile: ConnectionProfile) = "${server.id}:${profile.username}"

    /**
     * Retrieves an existing connected session or establishes a new one.
     * This ensures SFTP and Terminal share the same underlying connection
     * when they use the same login.
     */
    suspend fun getOrCreateSession(server: Server, profile: ConnectionProfile): Session = sessionMutex.withLock {
        val key = sessionKey(server, profile)
        val existing = activeSessions[key]
        if (existing != null && existing.isConnected) {
            return@withLock existing
        }

        return@withLock withContext(Dispatchers.IO) {
            val session = jsch.getSession(profile.username, server.host, server.port)

            // Setup Authentication
            if (!profile.privateKeyPath.isNullOrEmpty()) {
                val keyFile = File(profile.privateKeyPath)
                if (keyFile.exists()) {
                    jsch.addIdentity(profile.privateKeyPath, profile.passphrase)
                }
            } else {
                session.setPassword(profile.password)
            }

            val config = Properties()
            
            // STABILITY FIX: Pin the algorithm once captured to prevent "Identification changed" errors.
            if (!server.hostKeyType.isNullOrEmpty()) {
                config["server_host_key"] = server.hostKeyType
            }
            
            // Use SHA-256 for consistent fingerprinting
            config["fingerprint_hash"] = "sha256"

            // Manual verification against DB to avoid known_hosts file issues on Android.
            config["StrictHostKeyChecking"] = "no"

            session.setConfig(config)
            
            // RELIABILITY: Keep-alive settings to maintain the connection when app is minimized
            session.setServerAliveInterval(15000) // 15 seconds
            session.setServerAliveCountMax(3)
            session.timeout = 20000 // 20s connect timeout
            
            try {
                session.connect()
            } catch (e: JSchException) {
                throw Exception("SSH Connection failed: ${e.message}")
            }
            
            val hostKey = session.hostKey
            val currentFingerprint = hostKey.getFingerPrint(jsch)
            val currentType = hostKey.type

            if (server.hostKey == null || server.hostKeyType == null) {
                // First connect: capture and store signature
                repository.updateServer(server.copy(hostKey = currentFingerprint, hostKeyType = currentType), null)
            } else {
                // Subsequent: strict manual verification
                if (server.hostKey != currentFingerprint) {
                    session.disconnect()
                    throw Exception("SECURITY WARNING: Remote host identification has changed! Potential MitM attack. If you trust this server, edit settings to clear the host key.")
                }
            }

            activeSessions[key] = session
            session
        }
    }
}

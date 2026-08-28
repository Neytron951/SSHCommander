package com.neytron.sshcommander.data

import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import com.jcraft.jsch.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

/**
 * Callback for persisting captured host-key fingerprints.
 */
fun interface HostKeyStore {
    suspend fun storeHostKey(serverId: Int, hostKey: String, hostKeyType: String)
}

/**
 * Logger for debugging SSH connections.
 */
class JSchLogger : Logger {
    override fun isEnabled(level: Int): Boolean = true
    override fun log(level: Int, message: String) {
        println("JSCH: $message")
    }
}

class SshConnectionManager(
    private val hostKeyStore: HostKeyStore? = null
) {
    companion object {
        private val activeSessions = mutableMapOf<String, Session>()
        private val sessionMutex = Mutex()
        
        init {
            // Register BouncyCastle for modern crypto support
            try {
                val bcProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
                if (java.security.Security.getProvider("BC") == null) {
                    val bcProvider = bcProviderClass.getConstructor().newInstance() as java.security.Provider
                    java.security.Security.addProvider(bcProvider)
                    println("JSCH: BouncyCastle registered in SshConnectionManager.")
                }
            } catch (e: Exception) {
                println("JSCH: [DEBUG] BouncyCastle registration skipped: ${e.message}")
            }
            
            JSch.setLogger(JSchLogger())
            println("JSCH: RUNTIME VERSION: ${JSch.VERSION}")
        }

        suspend fun closeSession(serverId: Int) = sessionMutex.withLock {
            val keys = activeSessions.keys.filter { it.startsWith("$serverId:") }
            keys.forEach { key ->
                activeSessions[key]?.disconnect()
                activeSessions.remove(key)
            }
        }

        suspend fun getActiveSession(serverId: Int): Session? = sessionMutex.withLock {
            // Find any connected session for this server
            activeSessions.entries
                .filter { it.key.startsWith("$serverId:") }
                .map { it.value }
                .firstOrNull { it.isConnected }
        }
    }

    private fun sessionKey(server: Server, profile: ConnectionProfile) = "${server.id}:${profile.username}"

    suspend fun getOrCreateSession(server: Server, profile: ConnectionProfile): Session = sessionMutex.withLock {
        val key = sessionKey(server, profile)
        val existing = activeSessions[key]
        if (existing != null && existing.isConnected) {
            return@withLock existing
        }

        return@withLock withContext(Dispatchers.IO) {
            val jsch = JSch()
            val session = jsch.getSession(profile.username, server.host, server.port)

            // Setup Authentication
            if (!profile.privateKeyContent.isNullOrEmpty()) {
                val identityName = "managed-key-${server.id}-${profile.username}"
                val pub = profile.publicKeyContent?.toByteArray()
                jsch.addIdentity(identityName, profile.privateKeyContent.toByteArray(), pub, profile.passphrase?.toByteArray())
            } else if (!profile.privateKeyPath.isNullOrEmpty()) {
                val keyFile = File(profile.privateKeyPath)
                if (keyFile.exists()) {
                    jsch.addIdentity(profile.privateKeyPath, profile.passphrase)
                }
            } else {
                session.setPassword(profile.password)
            }

            val config = Properties()
            config["StrictHostKeyChecking"] = "no"
            
            // Modern SSH defaults for mwiede/jsch 0.2.x
            config["PreferredAuthentications"] = "publickey,password,keyboard-interactive"
            
            // Enable modern RSA signatures (SHA-256/512) and Ed25519
            config["server_host_key"] = "ssh-ed25519,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521,rsa-sha2-512,rsa-sha2-256,ssh-rsa"
            config["PubkeyAcceptedAlgorithms"] = "ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa"

            session.setConfig(config)
            session.setServerAliveInterval(15000)
            session.setServerAliveCountMax(3)
            session.timeout = 20000

            try {
                session.connect()
            } catch (e: JSchException) {
                // If publickey failed, JSch will try password if available
                throw Exception("SSH Connection failed: ${e.message}")
            }

            val hostKey = session.hostKey
            // mwiede version uses modern fingerprinting by default
            val currentFingerprint = hostKey.getFingerPrint(jsch)
            val currentType = hostKey.type

            if (server.hostKey == null || server.hostKeyType == null) {
                hostKeyStore?.storeHostKey(server.id, currentFingerprint, currentType)
            } else {
                if (server.hostKey != currentFingerprint) {
                    session.disconnect()
                    throw Exception("SECURITY WARNING: Host key mismatch!")
                }
            }

            activeSessions[key] = session
            session
        }
    }
}

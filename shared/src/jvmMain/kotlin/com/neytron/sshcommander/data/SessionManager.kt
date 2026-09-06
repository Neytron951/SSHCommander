package com.neytron.sshcommander.data

import com.neytron.sshcommander.terminal.TerminalController
import com.neytron.sshcommander.terminal.TerminalSession
import com.neytron.sshcommander.sftp.SftpController
import com.neytron.sshcommander.sftp.SftpSession

/**
 * Singleton that maintains live terminal and SFTP controllers for open sessions.
 * This ensures sessions survive navigation (especially on Android) until 
 * explicitly closed by the user.
 */
object SessionManager {
    private val sessions = mutableMapOf<Int, SessionBundle>()

    data class SessionBundle(
        val sessionId: Int,
        val serverId: Int,
        var terminal: TerminalController? = null,
        var sftp: SftpController? = null,
        var lastLoginId: Int? = null
    )

    fun getOrCreateBundle(
        sessionId: Int, 
        server: Server, 
        profile: ConnectionProfile,
        settings: AppSettings,
        hostKeyStore: HostKeyStore? = null
    ): SessionBundle {
        val existing = sessions[sessionId]
        if (existing != null) return existing
        
        val bundle = SessionBundle(sessionId, server.id)
        bundle.terminal = TerminalSession(server, profile, settings, hostKeyStore)
        bundle.sftp = SftpSession(server, profile, hostKeyStore)
        sessions[sessionId] = bundle
        
        bundle.terminal?.connect()
        bundle.sftp?.connect()
        
        return bundle
    }

    fun getBundle(sessionId: Int): SessionBundle? = sessions[sessionId]

    fun closeSession(sessionId: Int) {
        sessions.remove(sessionId)?.let {
            it.terminal?.close()
            it.sftp?.close()
        }
    }

    fun getAllActiveSessionIds(): List<Int> = sessions.keys.toList()
    
    fun clearAll() {
        sessions.values.forEach {
            it.terminal?.close()
            it.sftp?.close()
        }
        sessions.clear()
    }
}

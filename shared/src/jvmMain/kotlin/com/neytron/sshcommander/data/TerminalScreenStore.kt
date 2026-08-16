package com.neytron.sshcommander.data

/**
 * Keeps one [TerminalScreen] buffer per open session so a session "survives"
 * navigating away and back (and switching servers). The underlying SSH
 * connection itself stays alive in [SshConnectionManager]'s shared pool.
 *
 * Sessions are keyed by a unique [session id][createSession] (not by server id)
 * so several sessions can be open to the same server at once. Buffers are only
 * dropped when a session is explicitly closed or the app exits.
 */
object TerminalScreenStore {
    private data class Entry(val serverId: Int, val screen: TerminalScreen)

    private val sessions = mutableMapOf<Int, Entry>()
    private var nextSessionId = 1

    /** Creates a fresh session for [serverId] and returns its unique id. */
    fun createSession(serverId: Int): Int {
        val id = nextSessionId++
        sessions[id] = Entry(serverId, TerminalScreen())
        return id
    }

    /** Buffer for an existing session, or null. */
    fun get(sessionId: Int): TerminalScreen? = sessions[sessionId]?.screen

    /** Id of the server a session is connected to, or null if it doesn't exist. */
    fun serverOf(sessionId: Int): Int? = sessions[sessionId]?.serverId

    /** Existing open session for a server (if any). */
    fun findForServer(serverId: Int): Int? =
        sessions.entries.firstOrNull { it.value.serverId == serverId }?.key

    fun save(sessionId: Int, screen: TerminalScreen) {
        sessions[sessionId]?.let { sessions[sessionId] = Entry(it.serverId, screen) }
    }

    fun remove(sessionId: Int) {
        sessions.remove(sessionId)
    }

    /** All open sessions as a map of sessionId -> serverId. */
    fun openSessions(): Map<Int, Int> = sessions.mapValues { it.value.serverId }

    /** Whether any open session still targets the given server. */
    fun hasSessionForServer(serverId: Int): Boolean =
        sessions.values.any { it.serverId == serverId }
}

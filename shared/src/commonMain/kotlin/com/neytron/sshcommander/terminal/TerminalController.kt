package com.neytron.sshcommander.terminal

import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.TerminalScreen
import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over an interactive SSH shell session so that the shared UI can
 * drive a terminal without depending on the concrete JSch implementation.
 */
interface TerminalController {
    val terminalScreen: TerminalScreen
    val terminalRevision: StateFlow<Int>
    val isLoading: StateFlow<Boolean>
    val isConnected: StateFlow<Boolean>
    val error: StateFlow<String?>

    fun connect()
    fun sendInput(input: String)
    fun sendCtrlC()
    fun sendEscape()
    fun sendBackspace()
    fun sendEnter()
    fun sendArrowUp()
    fun sendArrowDown()
    fun sendArrowRight()
    fun sendArrowLeft()
    fun sendCtrlKey(letter: Char)
    /** Runs a command line: sends the text then Enter (newline) if not full-screen. */
    fun executeCommand(command: String)
    fun clearTerminal()
    fun disconnect()
    fun close()
}

/** Factory supplied by each platform entry point. */
fun interface TerminalSessionFactory {
    fun create(server: Server, profile: ConnectionProfile): TerminalController
}

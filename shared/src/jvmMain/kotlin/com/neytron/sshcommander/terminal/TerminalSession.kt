package com.neytron.sshcommander.terminal

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.Session
import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.HostKeyStore
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.SshConnectionManager
import com.neytron.sshcommander.data.TerminalDimensions
import com.neytron.sshcommander.data.TerminalScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.InputStream
import java.io.OutputStream

/**
 * Cross-platform interactive SSH shell session backed by JSch.
 * Drives a [TerminalScreen] emulator from the server's output stream and
 * forwards user keystrokes back over the channel.
 *
 * Unlike the Android ViewModel this holds no Context/Room dependencies so it
 * runs identically on Windows and Android.
 */
class TerminalSession(
    private val server: Server,
    private val profile: ConnectionProfile,
    private val hostKeyStore: HostKeyStore? = null,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : TerminalController {
    override val terminalScreen = TerminalScreen()
    private val _terminalRevision = MutableStateFlow(0)
    override val terminalRevision: StateFlow<Int> = _terminalRevision

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected

    private val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    private val connectionManager = SshConnectionManager(hostKeyStore)

    private var currentSession: Session? = null
    private var currentChannel: ChannelShell? = null
    private var channelOutputStream: OutputStream? = null
    private var shellJob: Job? = null

    private var utf8Leftover = ByteArray(0)
    private var notConnectedNotified = false

    // Serializes writes to the channel output stream. sendInput() is invoked
    // from keystroke handlers AND command buttons, each launching its own IO
    // coroutine; concurrent writes to JSch's stream can corrupt data or crash.
    private val writeLock = Mutex()

    override fun connect() {
        shellJob?.cancel()
        _error.value = null
        notConnectedNotified = false
        shellJob = scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            terminalScreen.feed("Connecting to ${server.host}:${server.port} as ${profile.username}...\r\n")
            _terminalRevision.value++
            try {
                val session = connectionManager.getOrCreateSession(server, profile)
                currentSession = session
                if (currentChannel?.isConnected == true) currentChannel?.disconnect()

                val channel = session.openChannel("shell") as ChannelShell
                channel.setPty(true)
                // The PTY term string is what OpenSSH uses to set TERM for the
                // remote shell. "xterm" alone made many servers start the shell as
                // "dumb"/dash, which disables readline — arrow keys then echo
                // "^[[A" garbage instead of navigating. xterm-256color keeps
                // readline/history working for non-root logins too.
                channel.setPtyType("xterm-256color")
                // Provide real pixel dimensions as well — some shells also use the
                // pty size to decide on line-wrapping/readline behavior.
                channel.setPtySize(TerminalDimensions.COLS, 30, 640, 400)
                // Belt-and-braces: forwarded for servers that honor sshd SetEnv.
                channel.setEnv("TERM", "xterm-256color")

                val inputStream: InputStream = channel.inputStream
                channelOutputStream = channel.outputStream

                channel.connect()
                currentChannel = channel
                _isLoading.value = false
                _isConnected.value = true

                val buffer = ByteArray(4096)
                while (currentCoroutineContext().isActive && channel.isConnected) {
                    val len = inputStream.read(buffer)
                    if (len > 0) {
                        val data = decodeUtf8(buffer.copyOfRange(0, len))
                        if (data.isNotEmpty()) {
                            terminalScreen.feed(data)
                            _terminalRevision.value++
                        }
                    } else if (len == -1) break
                }
                _isConnected.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _isConnected.value = false
                val message = e.message ?: "Connection failed"
                _error.value = message
                terminalScreen.feed("\r\n\u001b[31mERROR: $message\u001b[0m\r\n")
                _terminalRevision.value++
            }
        }
    }

    override fun sendInput(input: String) {
        scope.launch(Dispatchers.IO) {
            writeLock.withLock {
                val out = channelOutputStream
                if (out == null) {
                    // Not connected: tell the user once so keystrokes don't silently
                    // vanish (e.g. they typed before a session was established).
                    if (!_isLoading.value && !notConnectedNotified) {
                        notConnectedNotified = true
                        terminalScreen.feed("\r\n\u001b[33mNot connected — add your server or check credentials.\u001b[0m\r\n")
                        _terminalRevision.value++
                    }
                    return@launch
                }
                try {
                    out.write(input.toByteArray(Charsets.UTF_8))
                    out.flush()
                } catch (e: Exception) {
                    // Channel closed — ignore.
                }
            }
        }
    }

    override fun sendCtrlC() = sendInput("\u0003")
    override fun sendEscape() = sendInput("\u001b")
    override fun sendBackspace() = sendInput("\u007f")
    override fun sendEnter() = sendInput("\r")
    override fun sendArrowUp() = sendInput("\u001b[A")
    override fun sendArrowDown() = sendInput("\u001b[B")
    override fun sendArrowRight() = sendInput("\u001b[C")
    override fun sendArrowLeft() = sendInput("\u001b[D")

    override fun sendCtrlKey(letter: Char) {
        val ctrl = letter.lowercaseChar() - 'a' + 1
        if (ctrl in 1..26) sendInput(ctrl.toChar().toString())
    }

    override fun executeCommand(command: String) {
        // In full-screen apps (nano/vim/htop) a trailing newline would be sent
        // raw (Ctrl+J → "justify" in nano). In the plain shell we need Enter.
        //
        // CRITICAL: command + Enter must be sent in ONE write. Two separate
        // sendInput() calls each launch their own IO coroutine, so the Enter
        // can arrive before the command text (they run concurrently) and the
        // shell would execute an empty line instead of the command.
        if (terminalScreen.isFullScreen) {
            sendInput(command)
        } else {
            sendInput("$command\r")
        }
    }

    override fun clearTerminal() {
        terminalScreen.clear()
        _terminalRevision.value++
    }

    override fun disconnect() {
        shellJob?.cancel()
        currentChannel?.disconnect()
        currentChannel = null
        channelOutputStream = null
        _isConnected.value = false
        _isLoading.value = false
    }

    override fun close() {
        disconnect()
        scope.cancel()
    }

    private fun decodeUtf8(chunk: ByteArray): String {
        val combined = utf8Leftover + chunk
        for (drop in 0..3) {
            val len = combined.size - drop
            if (len < 0) break
            try {
                val text = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(java.nio.charset.CodingErrorAction.REPORT)
                    .onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(combined, 0, len))
                utf8Leftover = combined.copyOfRange(len, combined.size)
                return text.toString()
            } catch (e: java.nio.charset.CharacterCodingException) {
                // Incomplete/oversized tail — drop one more byte and retry.
            }
        }
        val fallback = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
            .decode(java.nio.ByteBuffer.wrap(combined))
        utf8Leftover = ByteArray(0)
        return fallback.toString()
    }
}

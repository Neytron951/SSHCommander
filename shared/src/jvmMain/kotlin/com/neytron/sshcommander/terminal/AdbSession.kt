package com.neytron.sshcommander.terminal

import com.flyfishxu.kadb.Kadb
import com.flyfishxu.kadb.shell.AdbShellPacket
import com.neytron.sshcommander.data.MonitorWidget
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.ServerStats
import com.neytron.sshcommander.data.TerminalScreen
import com.neytron.sshcommander.data.WidgetType
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdbSession(
    private val server: Server,
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

    private val _sysStats = MutableStateFlow(ServerStats())
    override val sysStats: StateFlow<ServerStats> = _sysStats.asStateFlow()

    private val _lastCommand = MutableStateFlow<String?>(null)
    override val lastCommand: StateFlow<String?> = _lastCommand

    override val monitorWidgets = MutableStateFlow<List<MonitorWidget>>(emptyList()).asStateFlow()
    override val widgetResults = MutableStateFlow<Map<String, String>>(emptyMap()).asStateFlow()
    override val widgetHistory = MutableStateFlow<Map<String, List<Float>>>(emptyMap()).asStateFlow()
    override val widgetLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap()).asStateFlow()

    private var kadb: Kadb? = null
    private var shellJob: Job? = null
    private var activeShellStream: com.flyfishxu.kadb.shell.AdbPtyShellSession? = null
    private val inputChannel = Channel<String>(Channel.UNLIMITED)

    override fun connect() {
        shellJob?.cancel()
        _error.value = null
        _isLoading.value = true
        
        shellJob = scope.launch(Dispatchers.IO) {
            try {
                terminalScreen.feed("Connecting to ADB ${server.host}:${server.port} (Direct via Kadb)...\r\n")
                _terminalRevision.value++

                // 1. Create Kadb session
                val device = Kadb.create(server.host, server.port)
                kadb = device
                
                println("[ADB] Opening interactive shell with PTY via Kadb...")
                // Kadb's openPtyShellSession uses shell,v2,pty by default
                val ptySession = device.openPtyShellSession(term = "xterm-256color")
                activeShellStream = ptySession
                
                println("[ADB] Interactive shell opened")
                _isConnected.value = true
                _isLoading.value = false
                
                // Sync initial window size
                val cols = terminalScreen.width
                val rows = terminalScreen.height
                if (cols > 0 && rows > 0) {
                    println("[ADB] Syncing initial terminal size: $cols x $rows")
                    ptySession.resize(rows, cols)
                }

                // Reading loop
                val readJob = launch {
                    try {
                        println("[ADB] Starting read loop")
                        while (isActive) {
                            val packet = ptySession.read()
                            when (packet) {
                                is AdbShellPacket.StdOut -> {
                                    val text = String(packet.payload)
                                    terminalScreen.feed(text)
                                    _terminalRevision.value++
                                }
                                is AdbShellPacket.StdError -> {
                                    val text = String(packet.payload)
                                    terminalScreen.feed("\u001b[31m$text\u001b[0m")
                                    _terminalRevision.value++
                                }
                                is AdbShellPacket.Exit -> {
                                    val exitCode = packet.payload[0].toInt()
                                    terminalScreen.feed("\r\nSession closed with exit code: $exitCode\r\n")
                                    _terminalRevision.value++
                                    break
                                }
                            }
                        }
                    } catch (e: Exception) {
                        if (isActive) {
                            println("[ADB] Read error: ${e.message}")
                            _error.value = "Read error: ${e.message}"
                        }
                    }
                }

                // Writing loop
                val writeJob = launch {
                    try {
                        for (input in inputChannel) {
                            ptySession.write(input)
                        }
                    } catch (e: Exception) {
                        println("[ADB] Write error: ${e.message}")
                        _error.value = "Write error: ${e.message}"
                    }
                }

                joinAll(readJob, writeJob)
            } catch (e: Exception) {
                _isLoading.value = false
                _isConnected.value = false
                val originalMsg = e.message ?: "ADB Connection failed"
                println("[ADB] Connection error: $originalMsg")
                
                val msg = when {
                    originalMsg.contains("Connection refused") -> "Connection refused. Make sure Wireless Debugging is ON and you use the correct port."
                    originalMsg.contains("Authentication required") || originalMsg.contains("unauthorized") -> "Authentication failed. Please pair your device first."
                    else -> originalMsg
                }
                
                _error.value = msg
                terminalScreen.feed("\r\n\u001b[31mERROR: $msg\u001b[0m\r\n")
                _terminalRevision.value++
            } finally {
                kadb?.close()
                _isConnected.value = false
            }
        }
    }

    override fun sendInput(input: String) {
        scope.launch {
            inputChannel.send(input)
        }
    }

    override fun executeCommand(command: String) {
        _lastCommand.value = command
        sendInput("$command\n")
    }

    override fun addWidget(title: String, command: String, type: WidgetType, x: Int, y: Int, w: Int, h: Int, fontSize: Float, colorHex: String?, textAlign: String, textVerticalAlign: String) {}
    override fun deleteWidget(id: String) {}
    override fun updateWidget(updatedWidget: MonitorWidget) {}
    override fun updateSize(cols: Int, rows: Int) {
        terminalScreen.resize(rows, cols)
        scope.launch(Dispatchers.IO) {
            try {
                activeShellStream?.resize(rows, cols)
            } catch (e: Exception) {}
        }
    }
    override fun clearTerminal() { terminalScreen.clear(); _terminalRevision.value++ }
    override fun disconnect() { 
        shellJob?.cancel()
        kadb?.close()
        _isConnected.value = false 
    }
    override fun close() { disconnect(); scope.cancel() }
    
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
}

package com.neytron.sshcommander.terminal

import dadb.Dadb
import dadb.AdbShellPacket
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

    private var device: Dadb? = null
    private var shellJob: Job? = null
    private var activeShellStream: dadb.AdbShellStream? = null
    private val inputChannel = Channel<String>(Channel.UNLIMITED)

    override fun connect() {
        shellJob?.cancel()
        _error.value = null
        _isLoading.value = true
        
        shellJob = scope.launch(Dispatchers.IO) {
            try {
                terminalScreen.feed("Connecting to ADB ${server.host}:${server.port} via local server...\r\n")
                _terminalRevision.value++

                val adb = AdbBinaryManager.findAdb() ?: throw Exception("ADB binary not found")
                
                // 1. Force connect via system ADB
                terminalScreen.feed("Ensuring connection...\r\n")
                val connectProcess = ProcessBuilder(adb.absolutePath, "connect", "${server.host}:${server.port}")
                    .redirectErrorStream(true)
                    .start()
                val connectOutput = connectProcess.inputStream.bufferedReader().readText()
                connectProcess.waitFor()
                
                if (connectOutput.contains("failed") || connectOutput.contains("cannot")) {
                    terminalScreen.feed("\u001b[33mWarning: System ADB says: ${connectOutput.trim()}\u001b[0m\r\n")
                }

                // 2. Create Dadb session via local ADB server
                val serial = "${server.host}:${server.port}"
                println("[ADB] Creating Dadb session for $serial")
                val adbDevice = dadb.adbserver.AdbServer.createDadb(
                    adbServerHost = "localhost",
                    deviceQuery = "host:transport:$serial",
                    connectTimeout = 10000
                )
                device = adbDevice
                
                println("[ADB] Opening interactive shell with PTY...")
                // Use TERM=xterm-256color to improve terminal behavior and support colors
                adbDevice.open("shell,v2,pty:export TERM=xterm-256color; exec sh").use { stream ->
                    val shellStream = dadb.AdbShellStream(stream)
                    activeShellStream = shellStream
                    
                    println("[ADB] Interactive shell opened")
                    _isConnected.value = true
                    _isLoading.value = false
                    
                    // Immediately sync window size
                    val cols = terminalScreen.width
                    val rows = terminalScreen.height
                    if (cols > 0 && rows > 0) {
                        println("[ADB] Syncing initial terminal size: ${cols}x${rows}")
                        val payload = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                            putShort(rows.toShort())
                            putShort(cols.toShort())
                            putShort(0)
                            putShort(0)
                        }.array()
                        shellStream.write(4, payload)
                    }

                    // Trigger prompt with a simple CR
                    shellStream.write("\r")
                    println("[ADB] Initial newline sent")

                    // Reading loop
                    val readJob = launch {
                        try {
                            println("[ADB] Starting read loop")
                            while (isActive) {
                                val packet = shellStream.read()
                                when (packet) {
                                    is AdbShellPacket.StdOut -> {
                                        val text = String(packet.payload, Charsets.UTF_8)
                                        println("[ADB] StdOut: '$text'")
                                        terminalScreen.feed(text)
                                        _terminalRevision.value++
                                    }
                                    is AdbShellPacket.StdError -> {
                                        val text = String(packet.payload, Charsets.UTF_8)
                                        println("[ADB] StdError: '$text'")
                                        terminalScreen.feed("\u001b[31m$text\u001b[0m")
                                        _terminalRevision.value++
                                    }
                                    is AdbShellPacket.Exit -> {
                                        println("[ADB] Shell Exit: ${packet.payload[0]}")
                                        terminalScreen.feed("\r\nSession closed with exit code: ${packet.payload[0]}\r\n")
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
                                println("[ADB] Writing: '$input'")
                                shellStream.write(input)
                            }
                        } catch (e: Exception) {
                            println("[ADB] Write error: ${e.message}")
                            _error.value = "Write error: ${e.message}"
                        }
                    }

                    joinAll(readJob, writeJob)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                _isConnected.value = false
                val originalMsg = e.message ?: "ADB Connection failed"
                println("[ADB] Connection error: $originalMsg")
                
                // Provide a more helpful message for common failures
                val msg = when {
                    originalMsg.contains("1000000") -> {
                        "Connection timed out. Make sure you are using the 'Connection Port' (e.g. 192.168.0.72:38475) and NOT the 'Pairing Port'."
                    }
                    originalMsg.contains("Device rejected authentication") || originalMsg.contains("unauthorized") -> {
                        "Authentication failed. Please pair your device first using the 'Pair' button."
                    }
                    else -> originalMsg
                }
                
                _error.value = msg
                terminalScreen.feed("\r\n\u001b[31mERROR: $msg\u001b[0m\r\n")
                _terminalRevision.value++
            } finally {
                device?.close()
                _isConnected.value = false
            }
        }
    }

    override fun sendInput(input: String) {
        println("[ADB] sendInput: '$input'")
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
        // Send window size change to ADB shell v2
        scope.launch(Dispatchers.IO) {
            try {
                val payload = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN).apply {
                    putShort(rows.toShort())
                    putShort(cols.toShort())
                    putShort(0) // xpixels
                    putShort(0) // ypixels
                }.array()
                activeShellStream?.write(4, payload) // 4 = ID_WINDOW_SIZE
            } catch (e: Exception) {
                // Ignore if stream is closed
            }
        }
    }
    override fun clearTerminal() { terminalScreen.clear(); _terminalRevision.value++ }
    override fun disconnect() { 
        shellJob?.cancel()
        device?.close()
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

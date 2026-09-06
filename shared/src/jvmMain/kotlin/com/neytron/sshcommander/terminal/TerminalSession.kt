package com.neytron.sshcommander.terminal

import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.Session
import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.HostKeyStore
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.ServerStats
import com.neytron.sshcommander.data.MonitorWidget
import com.neytron.sshcommander.data.WidgetType
import com.neytron.sshcommander.data.SshConnectionManager
import com.neytron.sshcommander.data.TerminalDimensions
import com.neytron.sshcommander.data.TerminalScreen
import com.neytron.sshcommander.data.AppSettings
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

/**
 * Cross-platform interactive SSH shell session backed by JSch.
 * Drives a [TerminalScreen] emulator from the server's output stream and
 * forwards user keystrokes back over the channel.
 */
class TerminalSession(
    private val server: Server,
    private val profile: ConnectionProfile,
    private val settings: AppSettings,
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

    private val _lastCommand = MutableStateFlow<String?>(null)
    override val lastCommand: StateFlow<String?> = _lastCommand

    private val _sysStats = MutableStateFlow(ServerStats())
    override val sysStats: StateFlow<ServerStats> = _sysStats.asStateFlow()

    private val _widgetResults = MutableStateFlow<Map<String, String>>(emptyMap())
    override val widgetResults: StateFlow<Map<String, String>> = _widgetResults.asStateFlow()

    private val _widgetHistory = MutableStateFlow<Map<String, List<Float>>>(emptyMap())
    override val widgetHistory: StateFlow<Map<String, List<Float>>> = _widgetHistory.asStateFlow()

    private val _widgetLoading = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    override val widgetLoading: StateFlow<Map<String, Boolean>> = _widgetLoading.asStateFlow()

    private val lastRawValues = mutableMapOf<String, Float>()

    private val _monitorWidgets = MutableStateFlow<List<MonitorWidget>>(emptyList())
    override val monitorWidgets: StateFlow<List<MonitorWidget>> = _monitorWidgets.asStateFlow()

    private val connectionManager = SshConnectionManager(hostKeyStore)

    private var currentSession: Session? = null
    private var currentChannel: ChannelShell? = null
    private var channelOutputStream: OutputStream? = null
    private var shellJob: Job? = null
    private var statsJob: Job? = null

    private var utf8Leftover = ByteArray(0)
    private var notConnectedNotified = false

    private val writeLock = Mutex()

    init {
        scope.launch {
            // Load initial widgets from settings and stay in sync
            settings.monitorWidgets.collect { json ->
                if (json.isNotBlank()) {
                    try {
                        val list = Json.decodeFromString<List<MonitorWidget>>(json)
                        if (list != _monitorWidgets.value) {
                            _monitorWidgets.value = list
                        }
                    } catch (e: Exception) {}
                }
            }
        }
        
        scope.launch {
            // Observe local changes and persist to settings
            _monitorWidgets.drop(1).collect { list ->
                val json = Json.encodeToString(list)
                val currentStored = settings.monitorWidgets.first()
                if (json != currentStored) {
                    settings.setMonitorWidgets(json)
                }
            }
        }
    }

    fun updateWidgets(widgets: List<MonitorWidget>) {
        _monitorWidgets.value = widgets
    }

    override fun connect() {
        shellJob?.cancel()
        _error.value = null
        notConnectedNotified = false
        shellJob = scope.launch(Dispatchers.IO) {
            _isLoading.value = true
            startStatsPolling()
            
            var reconnectAttempt = 0
            val maxAttempts = 5
            
            while (isActive) {
                try {
                    reconnectAttempt = 0
                    terminalScreen.feed("Connecting to ${server.host}:${server.port} as ${profile.username}...\r\n")
                    _terminalRevision.value++
                    
                    val session = connectionManager.getOrCreateSession(server, profile)
                    currentSession = session
                    if (currentChannel?.isConnected == true) currentChannel?.disconnect()

                    val channel = session.openChannel("shell") as ChannelShell
                    channel.setPty(true)
                    channel.setPtyType("xterm-256color")
                    channel.setPtySize(TerminalDimensions.COLS, 30, 640, 400)
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
                    if (!isActive) break
                    
                    // If we reached here, connection was lost but scope is still active
                    throw Exception("Connection lost")
                    
                } catch (e: Exception) {
                    _isLoading.value = false
                    _isConnected.value = false
                    val message = e.message ?: "Connection failed"
                    _error.value = message
                    
                    if (message != "Connection lost") {
                        terminalScreen.feed("\r\n\u001b[31mERROR: $message\u001b[0m\r\n")
                        _terminalRevision.value++
                    }
                    
                    reconnectAttempt++
                    if (reconnectAttempt > maxAttempts) break
                    
                    val delayMs = (2000L * (1L shl (reconnectAttempt - 1))).coerceAtMost(30_000L)
                    terminalScreen.feed("\r\nRetrying connection ($reconnectAttempt/$maxAttempts) in ${delayMs/1000}s...\r\n")
                    _terminalRevision.value++
                    delay(delayMs)
                }
            }
        }
    }

    override fun sendInput(input: String) {
        scope.launch(Dispatchers.IO) {
            writeLock.withLock {
                val out = channelOutputStream
                if (out == null) {
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
                } catch (e: Exception) {}
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
        _lastCommand.value = command
        if (terminalScreen.isFullScreen) {
            sendInput(command)
        } else {
            sendInput("$command\r")
        }
    }

    override fun updateSize(cols: Int, rows: Int) {
        terminalScreen.resize(rows, cols)
        val channel = currentChannel ?: return
        if (channel.isConnected) {
            try {
                // wp/hp (pixels) are less critical than cols/rows for layout
                channel.setPtySize(cols, rows, cols * 8, rows * 16)
            } catch (e: Exception) {}
        }
    }

    override fun clearTerminal() {
        terminalScreen.clear()
        _terminalRevision.value++
    }

    override fun addWidget(title: String, command: String, type: WidgetType, x: Int, y: Int, w: Int, h: Int, fontSize: Float, colorHex: String?, textAlign: String, textVerticalAlign: String) {
        val current = _monitorWidgets.value.toMutableList()
        current.add(MonitorWidget(java.util.UUID.randomUUID().toString(), title, command, type, x, y, w, h, fontSize, colorHex, textAlign, textVerticalAlign))
        _monitorWidgets.value = current
    }

    override fun deleteWidget(id: String) {
        _monitorWidgets.value = _monitorWidgets.value.filter { it.id != id }
    }

    override fun updateWidget(updatedWidget: MonitorWidget) {
        _monitorWidgets.value = _monitorWidgets.value.map { if (it.id == updatedWidget.id) updatedWidget else it }
    }

    override fun disconnect() {
        shellJob?.cancel()
        statsJob?.cancel()
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

    private fun startStatsPolling() {
        statsJob?.cancel()
        statsJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                if (_isConnected.value) {
                    try {
                        val currentWidgets = _monitorWidgets.value
                        val results = mutableMapOf<String, String>()
                        val history = _widgetHistory.value.toMutableMap()
                        val loading = mutableMapOf<String, Boolean>()
                        
                        currentWidgets.forEach { widget ->
                            loading[widget.id] = true
                            _widgetLoading.value = loading.toMap()
                            
                            val output = executeSingleCommand(widget.command)
                            loading[widget.id] = false
                            _widgetLoading.value = loading.toMap()

                            if (output != null) {
                                val trimmed = output.trim()
                                results[widget.id] = trimmed
                                
                                // Update history if numeric
                                val cleanValue = trimmed.replace(',', '.').filter { it.isDigit() || it == '.' }
                                cleanValue.toFloatOrNull()?.let { newValue ->
                                    var displayValue = newValue
                                    
                                    // Calculate rate for values that only increase (like network bytes)
                                    val lastValue = lastRawValues[widget.id]
                                    if (lastValue != null && widget.title.contains("Net", ignoreCase = true)) {
                                        val delta = if (newValue >= lastValue) newValue - lastValue else 0f
                                        displayValue = delta / 10f // rate per second (10s interval)
                                        
                                        // Store formatted rate for the text display
                                        results[widget.id] = if (displayValue > 1024) 
                                            "%.1f kB/s".format(displayValue / 1024f) 
                                            else "${displayValue.toLong()} B/s"
                                    } else if (widget.title.contains("Net", ignoreCase = true)) {
                                        // First run for network: don't show raw bytes
                                        results[widget.id] = "Calculating..."
                                    }
                                    
                                    lastRawValues[widget.id] = newValue

                                    val list = (history[widget.id] ?: emptyList()).toMutableList()
                                    list.add(displayValue)
                                    if (list.size > 20) list.removeAt(0)
                                    history[widget.id] = list
                                }
                            }
                        }
                        _widgetResults.value = results
                        _widgetHistory.value = history

                        val basicOutput = executeSingleCommand("top -bn1 | grep 'Cpu(s)'; free -b; df -B1 / | tail -n 1; uptime -p; journalctl -n 10 --no-pager 2>/dev/null || tail -n 10 /var/log/syslog")
                        if (basicOutput != null) {
                            _sysStats.value = parseStats(basicOutput)
                        }
                    } catch (e: Exception) {}
                }
                delay(10_000)
            }
        }
    }

    private suspend fun executeSingleCommand(cmd: String): String? {
        val session = currentSession ?: return null
        if (!session.isConnected) return null
        return withContext(Dispatchers.IO) {
            try {
                val channel = session.openChannel("exec") as com.jcraft.jsch.ChannelExec
                channel.setCommand(cmd)
                val input = channel.inputStream
                channel.connect()
                val output = input.bufferedReader().readText()
                channel.disconnect()
                output
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun parseStats(output: String): ServerStats {
        var cpu = 0f
        var ramUsed = 0L
        var ramTotal = 0L
        var diskUsed = 0L
        var diskTotal = 0L
        var uptime = ""
        val logs = mutableListOf<String>()

        val lines = output.lines()
        lines.forEach { line ->
            when {
                line.contains("Cpu(s)") -> {
                    val match = Regex("(\\d+[.,]\\d+)\\s+id").find(line)
                    match?.let {
                        val idle = it.groupValues[1].replace(',', '.').toFloatOrNull() ?: 100f
                        cpu = 100f - idle
                    }
                }
                line.startsWith("Mem:") -> {
                    val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (parts.size >= 3) {
                        ramTotal = parts[1].toLongOrNull() ?: 0L
                        ramUsed = parts[2].toLongOrNull() ?: 0L
                    }
                }
                line.startsWith("/") -> {
                    val parts = line.split(Regex("\\s+")).filter { it.isNotBlank() }
                    if (parts.size >= 4) {
                        diskTotal = parts[1].toLongOrNull() ?: 0L
                        diskUsed = parts[2].toLongOrNull() ?: 0L
                    }
                }
                line.startsWith("up ") -> {
                    uptime = line.removePrefix("up ")
                }
                else -> {
                    if (line.isNotBlank() && !line.startsWith("total")) {
                        logs.add(line)
                    }
                }
            }
        }

        return ServerStats(
            cpuLoad = cpu,
            ramUsed = ramUsed,
            ramTotal = ramTotal,
            diskUsed = diskUsed,
            diskTotal = diskTotal,
            uptime = uptime,
            rawLogs = logs.takeLast(10).joinToString("\n")
        )
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
            } catch (e: java.nio.charset.CharacterCodingException) {}
        }
        val fallback = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
            .decode(java.nio.ByteBuffer.wrap(combined))
        utf8Leftover = ByteArray(0)
        return fallback.toString()
    }
}

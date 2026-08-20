package com.neytron.sshcommander.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.Session
import com.neytron.sshcommander.data.*
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream

class SshViewModel(
    private val repository: ServerRepository,
    private val settings: AppSettings
) : ViewModel(), TerminalController {
    private val connectionManager = SshConnectionManager(hostKeyStore = RepositoryHostKeyStore(repository))

    var currentServer by mutableStateOf<Server?>(null)
    var sessionId by mutableIntStateOf(-1)
    
    private var _terminalScreen = TerminalScreen()
    override val terminalScreen: TerminalScreen get() = _terminalScreen
    
    private val _terminalRevision = MutableStateFlow(0)
    override val terminalRevision: StateFlow<Int> = _terminalRevision.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _sshError = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _sshError.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    var selectedLogin by mutableStateOf<ServerLogin?>(null)
    val logins = mutableStateListOf<ServerLogin>()

    var autoReconnectEnabled by mutableStateOf(true)

    val history = mutableStateListOf<CommandHistoryEntity>()
    val customCommands = mutableStateListOf<CustomCommand>()

    private var currentSession: Session? = null
    private var currentChannel: ChannelShell? = null
    private var channelOutputStream: OutputStream? = null
    private var shellJob: Job? = null

    private var historyIndex by mutableIntStateOf(-1)
    private var temporaryInput = ""
    private var isNavigatingHistory = false
    private var navigationHistoryList: List<String> = emptyList()

    companion object {
        private const val MAX_RECONNECT_ATTEMPTS = 5
    }

    val termBgColor: StateFlow<String> = settings.termBgColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000")
    val termTextColor: StateFlow<String> = settings.termTextColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#00FF00")
    val termFontSizePx: StateFlow<Float> = settings.termFontSizePx.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14f)

    init {
        viewModelScope.launch {
            settings.autoReconnect.collect { autoReconnectEnabled = it }
        }
    }

    fun setServer(server: Server, sessionId: Int) {
        if (currentServer?.id != server.id || this.sessionId != sessionId) {
            // Save the buffer of the session we're leaving
            currentServer?.let { if (this.sessionId > 0) TerminalScreenStore.save(this.sessionId, _terminalScreen) }
            stopExecution()
            currentServer = server
            this.sessionId = sessionId
            _terminalScreen = TerminalScreenStore.get(sessionId) ?: TerminalScreen()
            _terminalRevision.value++
            selectedLogin = null
            loadHistory(server.id)
            loadCustomCommands()
            // Load logins first, pick the default one, then connect once.
            loadLogins(server.id) { defaultLogin ->
                selectedLogin = defaultLogin
                connect()
            }
        } else {
            loadCustomCommands()
        }
    }

    fun selectLogin(login: ServerLogin?) {
        if (selectedLogin?.id == login?.id) return
        val server = currentServer ?: return
        selectedLogin = login
        _terminalScreen.clear()
        _terminalRevision.value++
        stopExecution()
        viewModelScope.launch {
            SshConnectionManager.closeSession(server.id)
            connectToShell(server)
        }
    }

    private var loginsJob: Job? = null

    private fun loadLogins(serverId: Int, onLoaded: (ServerLogin?) -> Unit = {}) {
        loginsJob?.cancel()
        loginsJob = viewModelScope.launch {
            val list = repository.getLoginsForServer(serverId).first()
            logins.clear()
            logins.addAll(list)
            onLoaded(list.firstOrNull { it.isDefault })
            // Keep the list fresh when returning from the "Manage Logins" screen.
            repository.getLoginsForServer(serverId).collect { updated ->
                logins.clear()
                logins.addAll(updated)
                // If the selected login was removed, fall back to the default or null
                val current = selectedLogin
                if (current != null && updated.none { it.id == current.id }) {
                    selectedLogin = updated.firstOrNull { it.isDefault }
                }
            }
        }
    }

    private fun loadHistory(serverId: Int) {
        viewModelScope.launch {
            repository.getHistoryForServer(serverId).collect { historyList ->
                val cleanedHistory = historyList.distinctBy { it.command }.take(100)
                if (!isNavigatingHistory) {
                    history.clear()
                    history.addAll(cleanedHistory)
                }
            }
        }
    }

    private fun loadCustomCommands() {
        viewModelScope.launch {
            repository.getAllCustomCommands().collect {
                customCommands.clear()
                customCommands.addAll(it)
            }
        }
    }

    fun onInputChanged(newText: String) {
        if (!isNavigatingHistory) {
            historyIndex = -1
            temporaryInput = newText
        }
    }

    fun navigateHistory(up: Boolean, currentText: String): String {
        if (historyIndex == -1) {
            temporaryInput = currentText
            navigationHistoryList = history.map { it.command }
        }
        if (navigationHistoryList.isEmpty()) return currentText

        isNavigatingHistory = true
        if (up) {
            if (historyIndex < navigationHistoryList.size - 1) historyIndex++
        } else {
            if (historyIndex >= 0) historyIndex--
        }

        return if (historyIndex == -1) {
            isNavigatingHistory = false
            temporaryInput
        } else {
            navigationHistoryList[historyIndex]
        }
    }

    /**
     * Ensures we are connected before performing any operation.
     */
    private suspend fun ensureConnected(): Boolean {
        val server = currentServer ?: return false
        if (currentChannel?.isConnected == true) return true

        return try {
            connectToShell(server)
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun connect() {
        val server = currentServer ?: return
        connectToShell(server)
    }

    private fun connectToShell(server: Server) {
        shellJob?.cancel()
        _sshError.value = null
        shellJob = viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            var reconnectAttempt = 0
            var lastError: Exception? = null
            try {
                while (isActive) {
                    try {
                        reconnectAttempt = 0
                        lastError = null
                        if (!openChannelAndRead(server)) break
                    } catch (e: Exception) {
                        lastError = e
                    }
                    if (!isActive) break
                    if (!autoReconnectEnabled) {
                        lastError?.let { withContext(Dispatchers.Main) { handleSshError(it) } }
                        break
                    }
                    reconnectAttempt++
                    if (reconnectAttempt > MAX_RECONNECT_ATTEMPTS) {
                        lastError?.let { withContext(Dispatchers.Main) { handleSshError(it) } }
                        break
                    }
                    val delayMs = reconnectDelay(reconnectAttempt)
                    _isLoading.value = true
                    withContext(Dispatchers.Main) {
                        _terminalScreen.feed(String.format(AppStrings.reconnectingMsg, delayMs / 1000) + "\n")
                        _terminalRevision.value++
                    }
                    delay(delayMs)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun reconnectDelay(attempt: Int): Long {
        return (2000L * (1L shl (attempt - 1))).coerceAtMost(30_000L)
    }

    private suspend fun openChannelAndRead(server: Server): Boolean {
        val profile = repository.buildConnectionProfile(server, selectedLogin)
        val session = connectionManager.getOrCreateSession(server, profile)
        currentSession = session

        if (currentChannel?.isConnected == true) {
            currentChannel?.disconnect()
        }

        val channel = session.openChannel("shell") as ChannelShell
        channel.setPty(true)
        channel.setPtyType("xterm")
        channel.setPtySize(TerminalDimensions.COLS, 30, 0, 0)

        val inputStream: InputStream = channel.inputStream
        channelOutputStream = channel.outputStream

        channel.connect()
        currentChannel = channel
        _isConnected.value = true
        _isLoading.value = false

        val buffer = ByteArray(4096)
        while (currentCoroutineContext().isActive && channel.isConnected) {
            val len = inputStream.read(buffer)
            if (len > 0) {
                val data = decodeUtf8(buffer.copyOfRange(0, len))
                if (data.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _terminalScreen.feed(data)
                        _terminalRevision.value++
                    }
                }
            } else if (len == -1) break
        }
        return currentCoroutineContext().isActive
    }

    private var utf8Leftover = ByteArray(0)

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
            }
        }
        val fallback = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
            .decode(java.nio.ByteBuffer.wrap(combined))
        utf8Leftover = ByteArray(0)
        return fallback.toString()
    }

    private fun handleSshError(e: Exception) {
        val errorObj = when {
            e.message?.contains("timeout") == true -> SshError.ConnectionTimeout
            e.message?.contains("Auth fail") == true -> SshError.AuthenticationFailed
            e.message?.contains("identification") == true -> SshError.HostKeyMismatch
            else -> SshError.Unknown(e.localizedMessage)
        }
        val errorMessage = when (errorObj) {
            is SshError.ConnectionTimeout -> AppStrings.errTimeout
            is SshError.AuthenticationFailed -> AppStrings.errAuthFailed
            is SshError.HostUnreachable -> AppStrings.errHostUnreachable
            is SshError.HostKeyMismatch -> AppStrings.errHostKeyMismatch
            is SshError.Unknown -> errorObj.message ?: AppStrings.errUnknown
        }
        _sshError.value = errorMessage
        _terminalScreen.feed(String.format(AppStrings.sshErrorTemplate, errorMessage))
        _terminalRevision.value++
    }

    override fun sendInput(input: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            try {
                channelOutputStream?.write(input.toByteArray(Charsets.UTF_8))
                channelOutputStream?.flush()
            } catch (e: Exception) {}
        }
    }

    override fun executeCommand(command: String) {
        viewModelScope.launch(Dispatchers.IO) {
            if (!ensureConnected()) return@launch
            sendInput(if (_terminalScreen.isFullScreen) command else "$command\n")
            currentServer?.let {
                repository.insertHistory(CommandHistoryEntity(
                    serverId = it.id,
                    command = command,
                    output = ""
                ))
            }
        }
    }

    override fun sendCtrlC() { sendInput("\u0003") }
    override fun sendEscape() { sendInput("\u001b") }
    override fun sendBackspace() { sendInput("\u007f") }
    override fun sendEnter() { sendInput("\r") }
    override fun sendArrowUp() { sendInput("\u001b[A") }
    override fun sendArrowDown() { sendInput("\u001b[B") }
    override fun sendArrowRight() { sendInput("\u001b[C") }
    override fun sendArrowLeft() { sendInput("\u001b[D") }

    override fun sendCtrlKey(letter: Char) {
        val ctrl = letter.lowercaseChar() - 'a' + 1
        if (ctrl in 1..26) sendInput(ctrl.toChar().toString())
    }

    override fun clearTerminal() {
        _terminalScreen.clear()
        _terminalRevision.value++
    }

    override fun disconnect() {
        stopExecution()
    }

    override fun close() {
        stopExecution()
    }

    fun stopExecution() {
        shellJob?.cancel()
        currentChannel?.disconnect()
        currentChannel = null
        channelOutputStream = null
        _isLoading.value = false
        _isConnected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        if (sessionId > 0) TerminalScreenStore.save(sessionId, _terminalScreen)
        stopExecution()
    }

    fun closeSession(sessionId: Int) {
        val serverId = TerminalScreenStore.serverOf(sessionId)
        TerminalScreenStore.remove(sessionId)
        if (this.sessionId == sessionId) {
            stopExecution()
            currentServer = null
            this.sessionId = -1
            _terminalScreen = TerminalScreen()
        }
        if (serverId != null && !TerminalScreenStore.hasSessionForServer(serverId)) {
            viewModelScope.launch {
                SshConnectionManager.closeSession(serverId)
            }
        }
    }

    fun updateTerminalFontSize(newSize: Float) {
        viewModelScope.launch {
            settings.setTermFontSizePx(newSize.coerceIn(8f, 30f))
        }
    }
}

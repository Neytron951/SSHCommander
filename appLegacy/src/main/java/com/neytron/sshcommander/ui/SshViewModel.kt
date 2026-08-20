package com.neytron.sshcommander.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.*
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.Session
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.InputStream
import java.io.OutputStream

import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SshViewModel(application: Application) : AndroidViewModel(application), TerminalController {
    private val repository = ServerRepository(application)
    private val dao = AppDatabase.getDatabase(application).serverDao()
    private val settingsManager = SettingsManager(application)
    private val connectionManager = SshConnectionManager(application)
    
    var currentServer by mutableStateOf<Server?>(null)
    override val terminalScreen = TerminalScreen()
    
    private val _terminalRevision = MutableStateFlow(0)
    override val terminalRevision: StateFlow<Int> = _terminalRevision.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _sshError = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _sshError.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    var sshErrorObj by mutableStateOf<SshError?>(null)
    
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

    val termBgColor: StateFlow<String> = settingsManager.termBgColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000")
    val termTextColor: StateFlow<String> = settingsManager.termTextColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#00FF00")
    val termFontSizePx: StateFlow<Float> = settingsManager.termFontSizePx.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14f)

    init {
        viewModelScope.launch {
            settingsManager.autoReconnect.collect { autoReconnectEnabled = it }
        }
    }

    fun setServer(server: Server) {
        if (currentServer?.id != server.id) {
            stopExecution()
            currentServer = server
            terminalScreen.clear()
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

    override fun connect() {
        val server = currentServer ?: return
        connectToShell(server)
    }

    override fun disconnect() {
        stopExecution()
    }

    override fun close() {
        stopExecution()
    }

    fun selectLogin(login: ServerLogin?) {
        if (selectedLogin?.id == login?.id) return
        val server = currentServer ?: return
        selectedLogin = login
        terminalScreen.clear()
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
            val list = dao.getLoginsForServer(serverId).first()
            logins.clear()
            logins.addAll(list)
            onLoaded(list.firstOrNull { it.isDefault })
            // Keep the list fresh when returning from the "Manage Logins" screen.
            dao.getLoginsForServer(serverId).collect { updated ->
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
            dao.getHistoryForServer(serverId).collect { historyList ->
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
            dao.getAllCustomCommands().collect {
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
     * This fixes the "session stops working" issue after minimizing the app.
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

    private fun connectToShell(server: Server) {
        shellJob?.cancel()
        _sshError.value = null
        sshErrorObj = null
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
                        terminalScreen.feed(
                            getApplication<Application>()
                                .getString(R.string.reconnecting_msg, delayMs / 1000) + "\n"
                        )
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
        // Exponential backoff: 2s, 4s, 8s, 16s, 30s (capped)
        return (2000L * (1L shl (attempt - 1))).coerceAtMost(30_000L)
    }

    /**
     * Opens the shell channel and pumps its output until the channel closes.
     * Returns true if the connection dropped unexpectedly (should reconnect),
     * false when the coroutine was cancelled (user left the screen).
     */
    private suspend fun openChannelAndRead(server: Server): Boolean {
        // Build auth profile: selected login (if any) or the server's main credentials
        val profile = repository.buildConnectionProfile(server, selectedLogin)
        // SHARED SESSION: Use the shared pool to avoid conflicts with SFTP
        val session = connectionManager.getOrCreateSession(server, profile)
        currentSession = session

        if (currentChannel?.isConnected == true) {
            currentChannel?.disconnect()
        }

        val channel = session.openChannel("shell") as ChannelShell
        channel.setPty(true)
        channel.setPtyType("xterm")
        // Advertise the same size as the emulator buffer so full-screen apps
        // (nano/vim) lay out against the grid we actually render.
        channel.setPtySize(TerminalDimensions.COLS, 30, 0, 0)
        
        val inputStream: InputStream = channel.inputStream
        channelOutputStream = channel.outputStream
        
        channel.connect()
        currentChannel = channel
        _isConnected.value = true
        // Connected — stop the loading indicator. It's turned back on when a
        // reconnect attempt begins and stays on during the backoff delay.
        _isLoading.value = false

        val buffer = ByteArray(4096)
        while (currentCoroutineContext().isActive && channel.isConnected) {
            val len = inputStream.read(buffer)
            if (len > 0) {
                val data = decodeUtf8(buffer.copyOfRange(0, len))
                if (data.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        terminalScreen.feed(data)
                        _terminalRevision.value++
                    }
                }
            } else if (len == -1) break
        }
        // If the coroutine is still active, the channel must have closed on its own.
        return currentCoroutineContext().isActive
    }

    // Holds the tail of a multi-byte UTF-8 sequence that was split across reads.
    private var utf8Leftover = ByteArray(0)

    /**
     * Decodes UTF-8 bytes, keeping an incomplete trailing multi-byte sequence
     * in [utf8Leftover] until the rest of it arrives in a later read.
     */
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
        // Pathological: give up on the leftover and flush with replacement.
        val fallback = Charsets.UTF_8.newDecoder()
            .onMalformedInput(java.nio.charset.CodingErrorAction.REPLACE)
            .decode(java.nio.ByteBuffer.wrap(combined))
        utf8Leftover = ByteArray(0)
        return fallback.toString()
    }

    private fun handleSshError(e: Exception) {
        val error = when {
            e.message?.contains("timeout") == true -> SshError.ConnectionTimeout
            e.message?.contains("Auth fail") == true -> SshError.AuthenticationFailed
            e.message?.contains("identification") == true -> SshError.HostKeyMismatch
            else -> SshError.Unknown(e.localizedMessage)
        }
        sshErrorObj = error
        _sshError.value = error.getMessage(getApplication())
        terminalScreen.feed(getApplication<Application>().getString(R.string.ssh_error_template, _sshError.value))
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
            // In full-screen apps (nano/vim/htop) the input field inserts text
            // raw — a trailing LF is Ctrl+J, which nano maps to "justify" and
            // violently reformats the screen. In the plain shell we do need the
            // Enter to run the command.
            sendInput(if (terminalScreen.isFullScreen) command else "$command\n")
            currentServer?.let {
                dao.insertHistory(CommandHistoryEntity(
                    serverId = it.id, 
                    command = command, 
                    output = ""
                ))
            }
        }
    }

    override fun sendCtrlC() {
        sendInput("\u0003")
    }

    override fun sendEscape() {
        sendInput("\u001b")
    }

    override fun sendBackspace() {
        sendInput("\u007f")
    }

    override fun sendEnter() {
        // Real terminal Enter = CR. LF (Ctrl+J) triggers "justify" in nano.
        sendInput("\r")
    }

    override fun sendArrowUp() { sendInput("\u001b[A") }
    override fun sendArrowDown() { sendInput("\u001b[B") }
    override fun sendArrowRight() { sendInput("\u001b[C") }
    override fun sendArrowLeft() { sendInput("\u001b[D") }

    /** Sends Ctrl+[letter] — e.g. Ctrl+X exits nano. */
    override fun sendCtrlKey(letter: Char) {
        val ctrl = letter.lowercaseChar() - 'a' + 1
        if (ctrl in 1..26) sendInput(ctrl.toChar().toString())
    }

    override fun clearTerminal() {
        terminalScreen.clear()
        _terminalRevision.value++
    }

    fun stopExecution() {
        shellJob?.cancel()
        // PERSISTENCE FIX: Only close the channel, not the shared session.
        // This ensures SFTP doesn't get disconnected when you leave the terminal.
        currentChannel?.disconnect()
        currentChannel = null
        channelOutputStream = null
        _isLoading.value = false
        _isConnected.value = false
    }

    override fun onCleared() {
        super.onCleared()
        stopExecution()
    }

    fun updateTerminalFontSize(newSize: Float) {
        viewModelScope.launch {
            settingsManager.setTermFontSizePx(newSize.coerceIn(8f, 30f))
        }
    }
}

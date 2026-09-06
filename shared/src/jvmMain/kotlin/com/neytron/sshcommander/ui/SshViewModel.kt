package com.neytron.sshcommander.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.*
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SshViewModel(
    private val repository: ServerRepository,
    private val settings: AppSettings
) : ViewModel(), TerminalController {
    var currentServer by mutableStateOf<Server?>(null)
    var sessionId by mutableIntStateOf(-1)

    private var activeController by mutableStateOf<TerminalController?>(null)

    override val terminalScreen: TerminalScreen get() = activeController?.terminalScreen ?: TerminalScreen()
    override val terminalRevision: StateFlow<Int> get() = activeController?.terminalRevision ?: MutableStateFlow(0)
    override val isLoading: StateFlow<Boolean> get() = activeController?.isLoading ?: MutableStateFlow(false)
    override val error: StateFlow<String?> get() = activeController?.error ?: MutableStateFlow(null)
    override val isConnected: StateFlow<Boolean> get() = activeController?.isConnected ?: MutableStateFlow(false)
    override val lastCommand: StateFlow<String?> get() = activeController?.lastCommand ?: MutableStateFlow(null)
    override val widgetResults: StateFlow<Map<String, String>> get() = activeController?.widgetResults ?: MutableStateFlow(emptyMap())
    override val widgetHistory: StateFlow<Map<String, List<Float>>> get() = activeController?.widgetHistory ?: MutableStateFlow(emptyMap())
    override val widgetLoading: StateFlow<Map<String, Boolean>> get() = activeController?.widgetLoading ?: MutableStateFlow(emptyMap())
    override val sysStats: StateFlow<ServerStats> get() = activeController?.sysStats ?: MutableStateFlow(ServerStats())

    // THE ONLY LIST OF WIDGETS
    private val _monitorWidgets = MutableStateFlow<List<MonitorWidget>>(emptyList())
    override val monitorWidgets: StateFlow<List<MonitorWidget>> = _monitorWidgets.asStateFlow()

    init {
        viewModelScope.launch {
            // Observe settings and update local state flow
            settings.monitorWidgets.collect { json ->
                if (json.isBlank()) {
                    if (_monitorWidgets.value.isEmpty()) {
                        _monitorWidgets.value = MonitorWidget.createDefault()
                    }
                } else {
                    try {
                        val list = Json.decodeFromString<List<MonitorWidget>>(json)
                        if (list != _monitorWidgets.value) {
                            _monitorWidgets.value = list
                        }
                    } catch (e: Exception) {}
                }
            }
        }
    }

    var selectedLogin by mutableStateOf<ServerLogin?>(null)
    val logins = mutableStateListOf<ServerLogin>()
    val history = mutableStateListOf<CommandHistoryEntity>()
    val customCommands = mutableStateListOf<CustomCommand>()

    private var historyIndex by mutableIntStateOf(-1)
    private var temporaryInput = ""
    private var isNavigatingHistory = false
    private var navigationHistoryList: List<String> = emptyList()

    val termBgColor: StateFlow<String> = settings.termBgColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000")
    val termTextColor: StateFlow<String> = settings.termTextColor.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "#00FF00")
    val termFontSizePx: StateFlow<Float> = settings.termFontSizePx.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 14f)

    fun setServer(server: Server, sessionId: Int) {
        this.sessionId = sessionId
        this.currentServer = server
        viewModelScope.launch {
            val list = repository.getLoginsForServer(server.id).first()
            logins.clear()
            logins.addAll(list)
            val bundle = SessionManager.getBundle(sessionId)
            if (bundle != null) {
                activeController = bundle.terminal
                selectedLogin = list.firstOrNull { it.id == bundle.lastLoginId }
            } else {
                val defaultLogin = list.firstOrNull { it.isDefault }
                selectedLogin = defaultLogin
                val profile = repository.buildConnectionProfile(server, defaultLogin)
                val newBundle = SessionManager.getOrCreateBundle(sessionId, server, profile, settings, RepositoryHostKeyStore(repository))
                activeController = newBundle.terminal
                newBundle.lastLoginId = defaultLogin?.id
            }
        }
        loadHistory(server.id); loadCustomCommands(); startSyncingLogins(server.id)
    }

    private fun startSyncingLogins(serverId: Int) {
        viewModelScope.launch { repository.getLoginsForServer(serverId).collect { logins.clear(); logins.addAll(it) } }
    }

    fun selectLogin(login: ServerLogin?) {
        val server = currentServer ?: return
        val sid = sessionId
        if (sid < 0 || selectedLogin?.id == login?.id) return
        selectedLogin = login
        viewModelScope.launch {
            val profile = repository.buildConnectionProfile(server, login)
            SessionManager.closeSession(sid)
            val newBundle = SessionManager.getOrCreateBundle(sid, server, profile, settings, RepositoryHostKeyStore(repository))
            activeController = newBundle.terminal
            newBundle.lastLoginId = login?.id
        }
    }

    private fun loadHistory(serverId: Int) {
        viewModelScope.launch { repository.getHistoryForServer(serverId).collect { if (!isNavigatingHistory) { history.clear(); history.addAll(it.distinctBy { h -> h.command }.take(100)) } } }
    }

    private fun loadCustomCommands() {
        viewModelScope.launch { repository.getAllCustomCommands().collect { customCommands.clear(); customCommands.addAll(it) } }
    }

    override fun connect() { activeController?.connect() }
    override fun disconnect() { activeController?.disconnect() }
    override fun sendInput(input: String) { activeController?.sendInput(input) }
    override fun executeCommand(command: String) { 
        activeController?.executeCommand(command) 
        currentServer?.let { viewModelScope.launch { repository.insertHistory(CommandHistoryEntity(serverId = it.id, command = command, output = "")) } }
    }
    override fun updateSize(cols: Int, rows: Int) { activeController?.updateSize(cols, rows) }
    override fun sendCtrlC() { activeController?.sendCtrlC() }
    override fun sendEscape() { activeController?.sendEscape() }
    override fun sendBackspace() { activeController?.sendBackspace() }
    override fun sendEnter() { activeController?.sendEnter() }
    override fun sendArrowUp() { activeController?.sendArrowUp() }
    override fun sendArrowDown() { activeController?.sendArrowDown() }
    override fun sendArrowRight() { activeController?.sendArrowRight() }
    override fun sendArrowLeft() { activeController?.sendArrowLeft() }
    override fun sendCtrlKey(letter: Char) { activeController?.sendCtrlKey(letter) }
    override fun clearTerminal() { activeController?.clearTerminal() }
    override fun close() {}

    fun closeSession(sessionId: Int) { SessionManager.closeSession(sessionId); TerminalScreenStore.remove(sessionId) }
    fun onInputChanged(newText: String) { if (!isNavigatingHistory) { historyIndex = -1; temporaryInput = newText } }
    fun navigateHistory(up: Boolean, currentText: String): String {
        if (historyIndex == -1) { temporaryInput = currentText; navigationHistoryList = history.map { it.command } }
        if (navigationHistoryList.isEmpty()) return currentText
        isNavigatingHistory = true
        if (up) { if (historyIndex < navigationHistoryList.size - 1) historyIndex++ } else { if (historyIndex >= 0) historyIndex-- }
        return if (historyIndex == -1) { isNavigatingHistory = false; temporaryInput } else { navigationHistoryList[historyIndex] }
    }
    fun updateTerminalFontSize(newSize: Float) { viewModelScope.launch { settings.setTermFontSizePx(newSize.coerceIn(8f, 30f)) } }

    override fun addWidget(title: String, command: String, type: WidgetType, x: Int, y: Int, w: Int, h: Int, fontSize: Float, colorHex: String?, textAlign: String, textVerticalAlign: String) {
        val newList = _monitorWidgets.value + MonitorWidget(java.util.UUID.randomUUID().toString(), title, command, type, x, y, w, h, fontSize, colorHex, textAlign, textVerticalAlign)
        viewModelScope.launch {
            settings.setMonitorWidgets(Json.encodeToString(newList))
        }
    }
    override fun deleteWidget(id: String) {
        val newList = _monitorWidgets.value.filter { it.id != id }
        viewModelScope.launch {
            settings.setMonitorWidgets(Json.encodeToString(newList))
        }
    }
    override fun updateWidget(updatedWidget: MonitorWidget) {
        val newList = _monitorWidgets.value.map { if (it.id == updatedWidget.id) updatedWidget else it }
        viewModelScope.launch {
            settings.setMonitorWidgets(Json.encodeToString(newList))
        }
    }
}

package com.neytron.sshcommander

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GroupWork
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import com.neytron.sshcommander.ui.AboutContent
import com.neytron.sshcommander.ui.CloudSyncSection
import com.neytron.sshcommander.ui.LocalAppDeps
import com.neytron.sshcommander.ui.SshKeyManagerScreen
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.ConnectionProfile
import com.neytron.sshcommander.data.CustomCommand
import com.neytron.sshcommander.data.DataBackupManager
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.ServerFolder
import com.neytron.sshcommander.data.ServerLogin
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.data.SshKey
import com.neytron.sshcommander.data.Workspace
import com.neytron.sshcommander.data.WorkspaceItem
import com.neytron.sshcommander.data.WorkspaceItemType
import com.neytron.sshcommander.sftp.SftpController
import com.neytron.sshcommander.sftp.SftpSessionFactory
import com.neytron.sshcommander.terminal.TerminalController
import com.neytron.sshcommander.terminal.TerminalSessionFactory
import com.neytron.sshcommander.ui.AppStrings
import com.neytron.sshcommander.ui.IconUtils
import com.neytron.sshcommander.ui.OnboardingGate
import com.neytron.sshcommander.ui.desktopTourSteps
import com.neytron.sshcommander.ui.PlatformInputStream
import com.neytron.sshcommander.ui.PrivacyUtils
import com.neytron.sshcommander.ui.resizeHoverCursor
import com.neytron.sshcommander.ui.SftpView
import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Warning
import com.neytron.sshcommander.ui.platformOpenUrl
import com.neytron.sshcommander.ui.ScriptMarketScreen
import com.neytron.sshcommander.ui.TerminalTheme
import com.neytron.sshcommander.ui.TerminalThemes
import com.neytron.sshcommander.ui.TerminalView
import com.neytron.sshcommander.ui.MonitoringDashboard
import com.neytron.sshcommander.ui.getSystemFontFamily
import com.neytron.sshcommander.ui.platformToast
import com.neytron.sshcommander.ui.rememberSavePicker
import com.neytron.sshcommander.ui.rememberUploadPicker
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import kotlinx.coroutines.launch

/** Root composable shared by Android and Windows. */
@Composable
fun App(
    terminalSessionFactory: TerminalSessionFactory? = null,
    sftpSessionFactory: SftpSessionFactory? = null,
    serverRepository: ServerRepository? = null,
    settings: AppSettings? = null,
    appVersion: String = "",
    initialServerId: Int? = null,
    backupManager: DataBackupManager? = null
) {
    // Keep the runtime string catalog in sync with the persisted language.
    val language by (settings?.language?.collectAsState(initial = "en")
        ?: remember { mutableStateOf("en") })
    LaunchedEffect(language) { AppStrings.language = language }

    val themeMode by (settings?.themeMode?.collectAsState(initial = "system")
        ?: remember { mutableStateOf("system") })
    val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    SSHCommanderTheme(darkTheme = darkTheme) {
        SSHCommanderLayout(
            terminalSessionFactory, sftpSessionFactory, serverRepository,
            settings, appVersion, initialServerId, backupManager
        )
    }
}

/**
 * Adaptive layout:
 * - Wide window (desktop): master-detail — servers on the left, terminal/SFTP on the right.
 * - Narrow window (phone): single-column stack (servers list fills the screen).
 */
@Composable
fun SSHCommanderLayout(
    terminalSessionFactory: TerminalSessionFactory?,
    sftpSessionFactory: SftpSessionFactory? = null,
    serverRepository: ServerRepository? = null,
    settings: AppSettings? = null,
    appVersion: String = "",
    initialServerId: Int? = null,
    backupManager: DataBackupManager? = null
) {
    var selectedPane by remember { mutableStateOf(PaneType.Terminal) }
    val servers = remember { mutableStateListOf<Server>() }
    val passwords = remember { mutableStateMapOf<Int, String>() }
    val folders = remember { mutableStateListOf<ServerFolder>() }
    var showConnectDialog by remember { mutableStateOf(false) }
    var editingServer by remember { mutableStateOf<Server?>(null) }
    var showLoginsFor by remember { mutableStateOf<Server?>(null) }
    var serverToDelete by remember { mutableStateOf<Server?>(null) }
    var folderToDelete by remember { mutableStateOf<ServerFolder?>(null) }
    var folderNameDialog by remember { mutableStateOf<FolderNameDialogState?>(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSshKeyManager by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showManageCommandsDialog by remember { mutableStateOf(false) }
    var showSaveWorkspaceDialog by remember { mutableStateOf(false) }
    var showScriptMarket by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<CustomCommand?>(null) }
    var showAddCommandDialog by remember { mutableStateOf(false) }
    val workspaces = remember { mutableStateListOf<Workspace>() }
    val sshKeys = remember { mutableStateListOf<SshKey>() }
    var dataLoaded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // --- Open sessions (tabs) ---------------------------------------------
    // Each tab owns a live terminal + SFTP connection. Sessions are NOT closed
    // when you switch servers or tabs — they persist until the tab is closed
    // (×) or the app exits.
    val sessions = remember { mutableStateListOf<SessionTab>() }
    var activeSessionId by remember { mutableIntStateOf(-1) }
    var nextSessionId by remember { mutableIntStateOf(1) }

    // Logins/passwords for the currently active server (login switcher).
    val logins = remember { mutableStateListOf<ServerLogin>() }
    val loginPasswords = remember { mutableStateMapOf<Int, String>() }

    // The currently visible session and its server.
    val activeSession = sessions.firstOrNull { it.id == activeSessionId }
    val selectedServerId = activeSession?.serverId ?: 0
    val selectedServer = servers.firstOrNull { it.id == selectedServerId }
        ?: Server(id = 0, name = "No server", host = "", port = 22, username = "")
    // null login = the server's own (main) login, as in the phone app.
    val selectedLoginId = activeSession?.loginId

    // Build and start the connection for a session. Closing only happens when
    // the tab is closed, so other sessions keep running in the background.
    fun connectSession(tab: SessionTab, loginId: Int?) {
        val server = servers.firstOrNull { it.id == tab.serverId } ?: return
        if (server.host.isEmpty()) return
        tab.loginId = loginId
        val login = if (loginId != null) logins.firstOrNull { it.id == loginId } else null
        val username = login?.username ?: server.username
        val password = if (login != null) (loginPasswords[login.id] ?: "") else (passwords[server.id] ?: "")
        val profile = ConnectionProfile(username, password)
        tab.terminal?.close()
        tab.terminal = if (server.host.isEmpty()) null else terminalSessionFactory?.create(server, profile)
        tab.sftp?.close()
        tab.sftp = sftpSessionFactory?.create(server, profile)
        tab.terminal?.connect()
        tab.sftp?.connect()
    }

    // Open (or activate) a session for a server. Clicking a server in the list
    // brings its existing session back instead of starting a new connection.
    fun openSession(server: Server) {
        if (server.host.isEmpty()) return
        val existing = sessions.firstOrNull { it.serverId == server.id }
        if (existing != null) {
            activeSessionId = existing.id
            return
        }
        val tab = SessionTab(id = nextSessionId++, serverId = server.id, server = server, loginId = null)
        sessions.add(tab)
        activeSessionId = tab.id
    }

    // "+" button: open another session for the currently active server.
    fun addSession() {
        val server = selectedServer
        if (server.host.isEmpty()) return
        val tab = SessionTab(id = nextSessionId++, serverId = server.id, server = server, loginId = null)
        sessions.add(tab)
        activeSessionId = tab.id
    }

    // Close a tab: terminate its connection and remove it.
    fun closeSession(tabId: Int) {
        val idx = sessions.indexOfFirst { it.id == tabId }
        if (idx < 0) return
        val tab = sessions.removeAt(idx)
        tab.terminal?.close()
        tab.sftp?.close()
        if (activeSessionId == tabId) {
            activeSessionId = sessions.lastOrNull()?.id ?: -1
        }
    }

    fun openWorkspace(workspace: Workspace) {
        // Close all UNPINNED sessions first to avoid clutter.
        val unpinned = sessions.filter { !it.isPinned }
        unpinned.forEach {
            it.terminal?.close()
            it.sftp?.close()
        }
        sessions.removeAll(unpinned)

        var firstNewTabId = -1
        workspace.items.forEach { item ->
            val server = servers.firstOrNull { it.id == item.serverId } ?: return@forEach
            val tab = SessionTab(
                id = nextSessionId++,
                serverId = server.id,
                server = server,
                loginId = item.loginId,
                isPinned = item.isPinned,
                tabColorHex = item.tabColorHex
            )
            sessions.add(tab)
            if (firstNewTabId == -1) firstNewTabId = tab.id
        }
        
        // Reset active session to the first tab of the opened workspace
        if (firstNewTabId != -1) {
            activeSessionId = firstNewTabId
        }
    }

    fun saveWorkspace(name: String, colorHex: String?) {
        val items = sessions.map { tab ->
            WorkspaceItem(
                serverId = tab.serverId,
                loginId = tab.loginId,
                type = WorkspaceItemType.TERMINAL, // Defaulting to terminal for now
                isPinned = tab.isPinned,
                tabColorHex = tab.tabColorHex,
                initialPath = tab.sftp?.currentPath?.value
            )
        }
        scope.launch {
            serverRepository?.insertWorkspace(Workspace(name = name, colorHex = colorHex, items = items))
        }
    }

    // When the app window closes, tear down every remaining connection.
    DisposableEffect(Unit) {
        onDispose {
            sessions.forEach {
                it.terminal?.close()
                it.sftp?.close()
            }
        }
    }

    // Folder list loaded from the repository (for grouping + the server dialog).
    LaunchedEffect(serverRepository) {
        serverRepository?.allFolders?.collect {
            folders.clear()
            folders.addAll(it)
        }
    }

    // Custom commands loaded from the repository (shared with the phone UI).
    val customCommands = remember { mutableStateListOf<CustomCommand>() }
    LaunchedEffect(serverRepository) {
        serverRepository?.getAllCustomCommands()?.collect {
            customCommands.clear()
            customCommands.addAll(it)
        }
    }

    // Workspaces loaded from the repository
    LaunchedEffect(serverRepository) {
        serverRepository?.allWorkspaces?.collect { list ->
            workspaces.clear()
            workspaces.addAll(list)
        }
    }

    // SSH Keys loaded from the repository
    LaunchedEffect(serverRepository) {
        serverRepository?.allSshKeys?.collect { list ->
            sshKeys.clear()
            sshKeys.addAll(list)
        }
    }

    // Terminal styling from settings so the desktop console matches the phone.
    val termBgHex by (settings?.termBgColor?.collectAsState(initial = "#000000")
        ?: remember { mutableStateOf("#000000") })
    val termTextHex by (settings?.termTextColor?.collectAsState(initial = "#00FF00")
        ?: remember { mutableStateOf("#00FF00") })
    val termFontSizePx by (settings?.termFontSizePx?.collectAsState(initial = 14f)
        ?: remember { mutableStateOf(14f) })
    val termFontFamily by (settings?.termFontFamily?.collectAsState(initial = "JetBrains Mono")
        ?: remember { mutableStateOf("JetBrains Mono") })
    val rebootConfirmMode by (settings?.rebootConfirm?.collectAsState(initial = "always")
        ?: remember { mutableStateOf("always") })
    val privacyMode by (settings?.privacyMode?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) })

    // Desktop layout settings (menu-bar toggles + pane widths).
    val showServerListSetting by (settings?.showServerList?.collectAsState(initial = true)
        ?: remember { mutableStateOf(true) })
    val showCommandPanelSetting by (settings?.showCommandPanel?.collectAsState(initial = true)
        ?: remember { mutableStateOf(true) })
    val showTopBarSetting by (settings?.showTopBar?.collectAsState(initial = true)
        ?: remember { mutableStateOf(true) })
    val savedServerPaneWidth by (settings?.serverPaneWidthPx?.collectAsState(initial = 280)
        ?: remember { mutableStateOf(280) })
    val savedCommandPaneWidth by (settings?.commandPaneWidthPx?.collectAsState(initial = 190)
        ?: remember { mutableStateOf(190) })

    // Live (draggable) pane widths, synced from persisted settings.
    var serverPaneWidth by remember { mutableIntStateOf(savedServerPaneWidth) }
    var commandPaneWidth by remember { mutableIntStateOf(savedCommandPaneWidth) }
    LaunchedEffect(savedServerPaneWidth) { serverPaneWidth = savedServerPaneWidth }
    LaunchedEffect(savedCommandPaneWidth) { commandPaneWidth = savedCommandPaneWidth }

    // Logins for the active server (login switcher). The loaded flag is reset
    // synchronously when the server changes so the connect effect waits for it.
    val loginsLoaded = remember(selectedServerId, serverRepository) { mutableStateOf(false) }
    LaunchedEffect(serverRepository, selectedServerId) {
        logins.clear()
        val repo = serverRepository
        val target = if (repo != null) servers.firstOrNull { it.id == selectedServerId } else null
        if (repo != null && target != null) {
            repo.getLoginsForServer(target.id).collect { list ->
                logins.clear()
                logins.addAll(list)
                list.forEach { l ->
                    if (l.id !in loginPasswords) {
                        loginPasswords[l.id] = repo.getLoginPassword(l.id) ?: ""
                    }
                }
                loginsLoaded.value = true
            }
        } else {
            loginsLoaded.value = true
        }
    }

    // Load persisted servers once the repository is available, then open a
    // session for the first/initial server.
    LaunchedEffect(serverRepository) {
        val repo = serverRepository
        if (repo != null) {
            val stored = repo.getServers()
            servers.clear()
            servers.addAll(stored)
            // Preload passwords so switching servers works without re-entering.
            stored.forEach { s ->
                val pw = repo.getPassword(s.id)
                if (pw != null) passwords[s.id] = pw
            }
            if (servers.isNotEmpty()) {
                val firstId = initialServerId?.takeIf { id -> servers.any { it.id == id } } ?: servers.first().id
                openSession(servers.first { it.id == firstId })
            }
        }
        dataLoaded = true
    }

    // Connect a newly opened session once its logins are loaded. Already-open
    // sessions are left untouched (this is what keeps them alive).
    LaunchedEffect(activeSessionId, selectedServerId, loginsLoaded.value) {
        val tab = sessions.firstOrNull { it.id == activeSessionId } ?: return@LaunchedEffect
        if (tab.terminal != null || selectedServerId == 0) return@LaunchedEffect
        if (!loginsLoaded.value && serverRepository != null) return@LaunchedEffect
        val loginId = tab.loginId
            ?: logins.firstOrNull { it.isDefault }?.id
            ?: logins.firstOrNull()?.id
        connectSession(tab, loginId)
    }

    // --- Import / export (File menu) ------------------------------------
    val savePicker = rememberSavePicker { target ->
        if (target != null) {
            scope.launch {
                try {
                    val backup = backupManager ?: return@launch
                    val json = backup.exportJson()
                    target.openOutput()?.let { out ->
                        val bytes = json.toByteArray()
                        out.write(bytes, 0, bytes.size)
                        out.close()
                    }
                    platformToast(AppStrings.exportSuccess)
                } catch (e: Exception) {
                    platformToast(String.format(AppStrings.errorPrefix, e.message ?: ""))
                }
            }
        }
    }
    val uploadPicker = rememberUploadPicker { files ->
        files.firstOrNull()?.let { f ->
            scope.launch {
                try {
                    val backup = backupManager ?: return@launch
                    val text = f.openInput()?.let { readAllText(it) } ?: ""
                    backup.importJson(text)
                    // Refresh the in-memory server list from the repository.
                    val repo = serverRepository
                    if (repo != null) {
                        val stored = repo.getServers()
                        servers.clear()
                        servers.addAll(stored)
                        stored.forEach { s ->
                            if (s.id !in passwords) {
                                passwords[s.id] = repo.getPassword(s.id) ?: ""
                            }
                        }
                    }
                    platformToast(AppStrings.importSuccess)
                } catch (e: Exception) {
                    platformToast(String.format(AppStrings.errorPrefix, e.message ?: ""))
                }
            }
        }
    }
    val onExport = { savePicker("sshcommander_backup.json") }
    val onImport = { uploadPicker() }

    // A theme background on the root keeps every gap between rounded
    // surfaces (tab bar margins, card padding, etc.) themed instead of
    // showing the raw white window background in dark mode.
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val wide = this.maxWidth >= 720.dp

        if (wide) {
            Column(Modifier.fillMaxSize()) {
                // Menu bar: File (Import/Export JSON) + View (element toggles).
                DesktopMenuBar(
                    showServerList = showServerListSetting,
                    showCommandPanel = showCommandPanelSetting,
                    showTopBar = showTopBarSetting,
                    onToggleServerList = { scope.launch { settings?.setShowServerList(it) } },
                    onToggleCommandPanel = { scope.launch { settings?.setShowCommandPanel(it) } },
                    onToggleTopBar = { scope.launch { settings?.setShowTopBar(it) } },
                    onExport = onExport,
                    onImport = onImport,
                    onOpenSettings = { showSettingsDialog = true },
                    onOpenAbout = { showAboutDialog = true },
                    onSaveWorkspace = { showSaveWorkspaceDialog = true }
                )
                if (sessions.isNotEmpty()) {
                    SessionTabBar(
                        sessions = sessions,
                        activeSessionId = activeSessionId,
                        onSelect = { activeSessionId = it },
                        onClose = { closeSession(it) },
                        onPin = { id ->
                            sessions.firstOrNull { it.id == id }?.let { it.isPinned = !it.isPinned }
                        },
                        onColorChange = { id, color ->
                            sessions.firstOrNull { it.id == id }?.let { it.tabColorHex = color }
                        },
                        onAdd = { addSession() }
                    )
                }
                Row(Modifier.fillMaxSize()) {
                    // Left: server list (optional + resizable)
                    if (showServerListSetting) {
                        ServerListPane(
                            servers = servers,
                            folders = folders,
                            workspaces = workspaces,
                            selectedId = selectedServerId,
                            onSelect = { serverId ->
                                servers.firstOrNull { it.id == serverId }?.let { openSession(it) }
                            },
                            onOpenWorkspace = { openWorkspace(it) },
                            onDeleteWorkspace = { id ->
                                scope.launch { serverRepository?.deleteWorkspace(id) }
                            },
                            onAddServer = { editingServer = null; showConnectDialog = true },
                            onEditServer = { editingServer = it; showConnectDialog = true },
                            onDeleteServer = { serverToDelete = it },
                            onManageLogins = { showLoginsFor = it },
                            onAddFolder = { folderNameDialog = FolderNameDialogState.Add },
                            onRenameFolder = { folder -> folderNameDialog = FolderNameDialogState.Rename(folder) },
                            onDeleteFolder = { folderToDelete = it },
                            onOpenMarket = { showScriptMarket = true },
                            privacyMode = privacyMode,
                            modifier = Modifier
                                .width(serverPaneWidth.dp)
                                .fillMaxHeight()
                        )
                        ResizableDivider(
                            onDrag = { delta ->
                                serverPaneWidth = (serverPaneWidth + delta).toInt().coerceIn(160, 600)
                            },
                            onDragEnd = { scope.launch { settings?.setServerPaneWidthPx(serverPaneWidth) } }
                        )
                    }
                    // Right: interaction pane
                    InteractionPane(
                        server = selectedServer,
                        pane = selectedPane,
                        onPaneChange = { selectedPane = it },
                        terminalSession = activeSession?.terminal,
                        sftpController = activeSession?.sftp,
                        customCommands = customCommands,
                        logins = logins,
                        activeLoginId = selectedLoginId,
                        onSelectLogin = { loginId ->
                            // Reconnect the active session with the new login
                            // (keeps the tab alive).
                            sessions.firstOrNull { it.id == activeSessionId }?.let { connectSession(it, loginId) }
                        },
                        onManageLogins = { showLoginsFor = selectedServer.takeIf { it.host.isNotEmpty() } },
                        showTopBar = showTopBarSetting,
                        showCommandPanel = showCommandPanelSetting,
                        commandPaneWidth = commandPaneWidth,
                        onCommandPaneResize = { delta ->
                            // The divider sits on the LEFT edge of the command
                            // panel (terminal | divider | panel), so dragging it
                            // left must widen the panel: invert the drag delta.
                            commandPaneWidth = (commandPaneWidth - delta).toInt().coerceIn(120, 420)
                        },
                        onCommandPaneResizeEnd = { scope.launch { settings?.setCommandPaneWidthPx(commandPaneWidth) } },
                        termBgHex = termBgHex,
                        termTextHex = termTextHex,
                        termFontSizePx = termFontSizePx,
                        termFontFamily = termFontFamily,
                        onFontSizeChange = { newSize ->
                            scope.launch { settings?.setTermFontSizePx(newSize) }
                        },
                        rebootConfirmMode = rebootConfirmMode,
                        privacyMode = privacyMode,
                        onAddCommand = { showAddCommandDialog = true },
                        onEditCommand = { commandToEdit = it },
                        onManageCommands = { showManageCommandsDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        } else {
            // Narrow: just the server list for now (phone flow is a later phase).
            ServerListPane(
                servers = servers,
                folders = folders,
                selectedId = selectedServerId,
                onSelect = { serverId ->
                    servers.firstOrNull { it.id == serverId }?.let { openSession(it) }
                },
                onAddServer = { editingServer = null; showConnectDialog = true },
                onEditServer = { editingServer = it; showConnectDialog = true },
                onDeleteServer = { serverToDelete = it },
                onManageLogins = { showLoginsFor = it },
                onAddFolder = { folderNameDialog = FolderNameDialogState.Add },
                onRenameFolder = { folder -> folderNameDialog = FolderNameDialogState.Rename(folder) },
                onDeleteFolder = { folderToDelete = it },
                privacyMode = privacyMode,
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // --- Dialogs ---------------------------------------------------------

    if (showConnectDialog) {
        val editing = editingServer
        val editingPassword = editing?.let { passwords[it.id] ?: "" } ?: ""
        ServerDialog(
            server = editing,
            initialPassword = editingPassword,
            folders = folders,
            sshKeys = sshKeys,
            onSave = { newServer, password ->
                val repo = serverRepository
                scope.launch {
                    if (editing != null) {
                        // Update existing server in place.
                        repo?.updateServer(newServer.copy(id = editing.id), password)
                        val idx = servers.indexOfFirst { it.id == editing.id }
                        if (idx >= 0) servers[idx] = newServer.copy(id = editing.id)
                        passwords[editing.id] = password
                        sessions.forEach { if (it.serverId == editing.id) it.server = newServer.copy(id = editing.id) }
                    } else if (repo != null) {
                        // Persist through the repository so the id is assigned by
                        // the storage layer and servers survive app restarts.
                        val savedId = repo.insertServer(newServer, password)
                        servers.add(newServer.copy(id = savedId))
                        passwords[savedId] = password
                        openSession(newServer.copy(id = savedId))
                    } else {
                        // No repository (Android placeholder): in-memory only.
                        val newId = (servers.maxOfOrNull { it.id } ?: 0) + 1
                        passwords[newId] = password
                        servers.add(newServer.copy(id = newId))
                        openSession(newServer.copy(id = newId))
                    }
                    showConnectDialog = false
                    editingServer = null
                }
            },
            onManageLogins = editing?.takeIf { it.host.isNotEmpty() }?.let { {
                showConnectDialog = false
                editingServer = null
                showLoginsFor = it
            } },
            onDismiss = {
                showConnectDialog = false
                editingServer = null
            }
        )
    }

    showLoginsFor?.let { target ->
        ManageLoginsDialog(
            server = target,
            repository = serverRepository,
            onDismiss = { showLoginsFor = null }
        )
    }

    serverToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { serverToDelete = null },
            title = { Text(AppStrings.deleteServer, fontWeight = FontWeight.Bold) },
            text = { Text(String.format(deleteServerConfirmMsg(), target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val repo = serverRepository
                    scope.launch {
                        repo?.deleteServer(target.id)
                        servers.removeAll { it.id == target.id }
                        passwords.remove(target.id)
                        // Close any sessions that pointed at the deleted server.
                        sessions.filter { it.serverId == target.id }.forEach {
                            it.terminal?.close()
                            it.sftp?.close()
                        }
                        sessions.removeAll { it.serverId == target.id }
                        if (activeSessionId !in sessions.map { it.id }) {
                            activeSessionId = sessions.lastOrNull()?.id ?: -1
                        }
                    }
                    serverToDelete = null
                }) { Text(AppStrings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { serverToDelete = null }) { Text(AppStrings.cancel) }
            }
        )
    }

    folderToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(AppStrings.deleteServer, fontWeight = FontWeight.Bold) },
            text = { Text(String.format(AppStrings.deleteFolderConfirm, target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    val repo = serverRepository
                    scope.launch {
                        repo?.deleteFolder(target.id)
                        // Servers in the folder moved to unfiled; refresh local list.
                        val stored = repo?.getServers()
                        if (stored != null) {
                            servers.clear()
                            servers.addAll(stored)
                        }
                    }
                    folderToDelete = null
                }) { Text(AppStrings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) { Text(AppStrings.cancel) }
            }
        )
    }

    folderNameDialog?.let { state ->
        FolderNameDialog(
            initialName = (state as? FolderNameDialogState.Rename)?.folder?.name ?: "",
            title = if (state is FolderNameDialogState.Rename) AppStrings.rename else AppStrings.newFolder,
            onConfirm = { name ->
                val repo = serverRepository
                scope.launch {
                    when (state) {
                        is FolderNameDialogState.Add -> repo?.insertFolder(name)
                        is FolderNameDialogState.Rename -> repo?.updateFolder(state.folder.copy(name = name))
                    }
                }
                folderNameDialog = null
            },
            onDismiss = { folderNameDialog = null }
        )
    }

    if (showSettingsDialog) {
        DesktopSettingsDialog(
            settings = settings,
            backupManager = backupManager,
            onSshKeysClick = { showSshKeyManager = true },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showSshKeyManager) {
        Dialog(onDismissRequest = { showSshKeyManager = false }) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxSize(0.85f)
            ) {
                SshKeyManagerScreen(onNavigateBack = { showSshKeyManager = false })
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(
            appVersion = appVersion,
            settings = settings,
            onDismiss = { showAboutDialog = false }
        )
    }

    if (showManageCommandsDialog) {
        ManageCommandsDialog(
            repository = serverRepository,
            onDismiss = { showManageCommandsDialog = false }
        )
    }

    if (showAddCommandDialog) {
        val primary = MaterialTheme.colorScheme.primary
        val initialColor = String.format(
            "#%06X",
            ((primary.red * 255).toInt() shl 16) or
                ((primary.green * 255).toInt() shl 8) or
                (primary.blue * 255).toInt()
        )
        AddCommandDialogDesktop(
            initialColorHex = initialColor,
            onDismiss = { showAddCommandDialog = false },
            onConfirm = { name, cmd, cat, isDangerous, colorHex ->
                scope.launch {
                    serverRepository?.insertCustomCommand(
                        CustomCommand(
                            name = name,
                            command = cmd,
                            categoryName = cat.ifBlank { null },
                            iconName = "default",
                            colorHex = colorHex,
                            orderIndex = customCommands.size,
                            isDangerous = isDangerous
                        )
                    )
                }
                showAddCommandDialog = false
            }
        )
    }

    commandToEdit?.let { editing ->
        AddCommandDialogDesktop(
            initialName = editing.name,
            initialCommand = editing.command,
            initialCategory = editing.categoryName ?: "",
            initialIsDangerous = editing.isDangerous,
            initialColorHex = editing.colorHex,
            onDismiss = { commandToEdit = null },
            onConfirm = { name, cmd, cat, isDangerous, colorHex ->
                scope.launch {
                    serverRepository?.updateCustomCommand(
                        editing.copy(
                            name = name,
                            command = cmd,
                            categoryName = cat.ifBlank { null },
                            isDangerous = isDangerous,
                            colorHex = colorHex
                        )
                    )
                }
                commandToEdit = null
            }
        )
    }

    if (showSaveWorkspaceDialog) {
        SaveWorkspaceDialog(
            onSave = { name, color ->
                saveWorkspace(name, color)
                showSaveWorkspaceDialog = false
            },
            onDismiss = { showSaveWorkspaceDialog = false }
        )
    }

    if (showScriptMarket) {
        Dialog(onDismissRequest = { showScriptMarket = false }) {
            Surface(
                modifier = Modifier.fillMaxSize(0.95f),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                ScriptMarketScreen(
                    onNavigateBack = { showScriptMarket = false },
                    onExecuteScript = { cmdStr ->
                        activeSession?.terminal?.executeCommand(cmdStr)
                        showScriptMarket = false
                    }
                )
            }
        }
    }

    // First-run guide: welcome → language → tab tour (or JSON import).
    OnboardingGate(
        settings = settings,
        tourSteps = desktopTourSteps(),
        onImportJson = onImport
    )
}

private fun deleteServerConfirmMsg(): String =
    if (AppStrings.language == "ru") "Удалить сервер \"%1\$s\"? Все логины будут удалены."
    else "Delete server \"%1\$s\"? All its logins will be deleted."

@Composable
private fun SaveWorkspaceDialog(
    onSave: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var colorHex by remember { mutableStateOf<String?>(null) }
    val colors = listOf("#F44336", "#4CAF50", "#2196F3", "#FFEB3B", "#9C27B0")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save Workspace", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Workspace Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Theme Color", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(parseHexColor(hex))
                                .border(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name.trim(), colorHex) }, enabled = name.isNotBlank()) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}

enum class PaneType { Terminal, Sftp, Split, Dashboard }

@Composable
private fun ScriptMarketSideContent(onOpenMarket: () -> Unit, isUltraNarrow: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ShoppingCart,
            contentDescription = null,
            modifier = Modifier.size(if (isUltraNarrow) 32.dp else 48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )
        if (!isUltraNarrow) {
            Spacer(Modifier.height(16.dp))
            Text(
                "ScriptMarket",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Browse community scripts",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onOpenMarket,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Market")
            }
            
            Spacer(Modifier.height(8.dp))
            
            OutlinedButton(
                onClick = { platformOpenUrl("https://github.com/Neytron951/SSHC_Scripts") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Cloud, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Contribute")
            }
        } else {
            IconButton(onClick = onOpenMarket) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { platformOpenUrl("https://github.com/Neytron951/SSHC_Scripts") }) {
                Icon(Icons.Default.Cloud, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private enum class SidePaneType { Servers, Workspaces, Market }

/**
 * One open terminal/SFTP session. Holds the live controllers so the connection
 * survives tab switches and is only closed when the tab (×) or the app exits.
 */
private class SessionTab(
    val id: Int,
    val serverId: Int,
    server: Server,
    loginId: Int?,
    terminal: TerminalController? = null,
    sftp: SftpController? = null,
    isPinned: Boolean = false,
    tabColorHex: String? = null
) {
    var server by mutableStateOf(server)
    var loginId by mutableStateOf(loginId)
    var terminal by mutableStateOf(terminal)
    var sftp by mutableStateOf(sftp)
    var isPinned by mutableStateOf(isPinned)
    var tabColorHex by mutableStateOf(tabColorHex)
}

@Composable
private fun SessionTabBar(
    sessions: List<SessionTab>,
    activeSessionId: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onPin: (Int) -> Unit,
    onColorChange: (Int, String?) -> Unit,
    onAdd: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp)
            ) {
                val sortedSessions = sessions.sortedWith(compareByDescending<SessionTab> { it.isPinned }.thenBy { it.id })

                sortedSessions.forEachIndexed { index, tab ->
                    val selected = tab.id == activeSessionId
                    var showContextMenu by remember { mutableStateOf(false) }
                    val tabColor = tab.tabColorHex?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary

                    Box(
                        modifier = Modifier
                            .widthIn(min = 120.dp, max = 240.dp)
                            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                else Color.Transparent
                            )
                            .clickable { onSelect(tab.id) }
                            .pointerInput(tab.id) {
                                detectTapGestures(
                                    onLongPress = { showContextMenu = true },
                                    onTap = { onSelect(tab.id) }
                                )
                            }
                    ) {
                        Column {
                            // Top Indicator Line
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .background(if (selected) tabColor else Color.Transparent)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .height(40.dp)
                                    .padding(start = 12.dp, end = 4.dp)
                            ) {
                                if (tab.isPinned) {
                                    Icon(
                                        Icons.Default.PushPin,
                                        contentDescription = null,
                                        modifier = Modifier.size(12.dp).padding(end = 4.dp),
                                        tint = tabColor
                                    )
                                }
                                Text(
                                    tab.server.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                IconButton(
                                    onClick = { onClose(tab.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = AppStrings.closeSession,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (selected) 0.8f else 0.4f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        DropdownMenu(expanded = showContextMenu, onDismissRequest = { showContextMenu = false }) {
                            DropdownMenuItem(
                                text = { Text(if (tab.isPinned) "Unpin" else "Pin") },
                                leadingIcon = { Icon(if (tab.isPinned) Icons.Outlined.PushPin else Icons.Default.PushPin, null) },
                                onClick = { onPin(tab.id); showContextMenu = false }
                            )
                            HorizontalDivider()
                            Text("Tab Color", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                            val colors = listOf("#F44336", "#4CAF50", "#2196F3", "#FFEB3B", "#9C27B0")
                            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                                colors.forEach { hex ->
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(parseHexColor(hex))
                                            .clickable { onColorChange(tab.id, hex); showContextMenu = false }
                                            .padding(4.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable { onColorChange(tab.id, null); showContextMenu = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                    
                    if (!selected && index < sortedSessions.size - 1 && sortedSessions[index+1].id != activeSessionId) {
                        VerticalDivider(
                            modifier = Modifier.height(20.dp).align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .padding(end = 12.dp, bottom = 4.dp)
                    .size(32.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = AppStrings.addSession,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }
}

@Composable
private fun ServerListPane(
    servers: List<Server>,
    folders: List<ServerFolder> = emptyList(),
    workspaces: List<Workspace> = emptyList(),
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onOpenWorkspace: (Workspace) -> Unit = {},
    onDeleteWorkspace: (Int) -> Unit = {},
    onAddServer: () -> Unit,
    onEditServer: (Server) -> Unit = {},
    onDeleteServer: (Server) -> Unit = {},
    onManageLogins: (Server) -> Unit = {},
    onAddFolder: () -> Unit = {},
    onRenameFolder: (ServerFolder) -> Unit = {},
    onDeleteFolder: (ServerFolder) -> Unit = {},
    onOpenMarket: () -> Unit = {},
    privacyMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    var sidePaneType by remember { mutableStateOf(SidePaneType.Servers) }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        BoxWithConstraints {
            val paneWidth = this.maxWidth
            val isNarrow = paneWidth < 180.dp
            val isUltraNarrow = paneWidth < 100.dp

            Column {
                // Tab switcher for Servers / Workspaces
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                ) {
                    SidePaneTab(
                        label = if (isNarrow) null else (if (AppStrings.language == "ru") "Серверы" else "Servers"),
                        icon = Icons.Default.Dns,
                        selected = sidePaneType == SidePaneType.Servers,
                        onClick = { sidePaneType = SidePaneType.Servers },
                        modifier = Modifier.weight(1f)
                    )
                    SidePaneTab(
                        label = if (isNarrow) null else (if (AppStrings.language == "ru") "Пространства" else "Workspaces"),
                        icon = Icons.Default.GroupWork,
                        selected = sidePaneType == SidePaneType.Workspaces,
                        onClick = { sidePaneType = SidePaneType.Workspaces },
                        modifier = Modifier.weight(1f)
                    )
                    SidePaneTab(
                        label = if (isNarrow) null else (if (AppStrings.language == "ru") "Маркет" else "Market"),
                        icon = Icons.Default.ShoppingCart,
                        selected = sidePaneType == SidePaneType.Market,
                        onClick = { sidePaneType = SidePaneType.Market },
                        modifier = Modifier.weight(1f)
                    )
                }

                if (sidePaneType == SidePaneType.Servers) {
                    ServerListContent(
                        servers = servers,
                        folders = folders,
                        selectedId = selectedId,
                        onSelect = onSelect,
                        onAddServer = onAddServer,
                        onEditServer = onEditServer,
                        onDeleteServer = onDeleteServer,
                        onManageLogins = onManageLogins,
                        onAddFolder = onAddFolder,
                        onRenameFolder = onRenameFolder,
                        onDeleteFolder = onDeleteFolder,
                        privacyMode = privacyMode,
                        isUltraNarrow = isUltraNarrow
                    )
                } else if (sidePaneType == SidePaneType.Workspaces) {
                    WorkspaceListContent(
                        workspaces = workspaces,
                        onOpen = onOpenWorkspace,
                        onDelete = onDeleteWorkspace,
                        isUltraNarrow = isUltraNarrow
                    )
                } else {
                    ScriptMarketSideContent(
                        onOpenMarket = onOpenMarket,
                        isUltraNarrow = isUltraNarrow
                    )
                }
            }
        }
    }
}

@Composable
private fun SidePaneTab(
    label: String?, 
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean, 
    onClick: () -> Unit, 
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (label == null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ServerListContent(
    servers: List<Server>,
    folders: List<ServerFolder>,
    selectedId: Int,
    onSelect: (Int) -> Unit,
    onAddServer: () -> Unit,
    onEditServer: (Server) -> Unit,
    onDeleteServer: (Server) -> Unit,
    onManageLogins: (Server) -> Unit,
    onAddFolder: () -> Unit,
    onRenameFolder: (ServerFolder) -> Unit,
    onDeleteFolder: (ServerFolder) -> Unit,
    privacyMode: Boolean,
    isUltraNarrow: Boolean = false
) {
    Column {
            // Folders start expanded; toggles are remembered per folder id.
            val expandedFolders = remember { mutableStateMapOf<Int, Boolean>() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                if (!isUltraNarrow) {
                    Text(
                        text = AppStrings.servers,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                
                // Add folder button (folder icon).
                IconButton(onClick = onAddFolder, modifier = Modifier.size(if (isUltraNarrow) 24.dp else 40.dp)) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = AppStrings.newFolder,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(if (isUltraNarrow) 16.dp else 24.dp)
                    )
                }
                IconButton(onClick = onAddServer, modifier = Modifier.size(if (isUltraNarrow) 24.dp else 40.dp)) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = AppStrings.addServer,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(if (isUltraNarrow) 16.dp else 24.dp)
                    )
                }
            }
            if (servers.isEmpty()) {
                // Empty state: guide the user to add their first server.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет серверов.\nНажмите +, чтобы добавить.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val unfiled = servers.filter { it.folderId == null }
                LazyColumn(Modifier.fillMaxSize()) {
                    // Unfiled servers first (no folder header).
                    items(unfiled, key = { it.id }) { server ->
                        ServerRow(
                            server = server,
                            selected = server.id == selectedId,
                            onClick = { onSelect(server.id) },
                            onEdit = { onEditServer(server) },
                            onDelete = { onDeleteServer(server) },
                            onManageLogins = { onManageLogins(server) },
                            privacyMode = privacyMode
                        )
                    }
                    // Then one collapsible group per folder.
                    folders.forEach { folder ->
                        val inFolder = servers.filter { it.folderId == folder.id }
                        val expanded = expandedFolders[folder.id] ?: true
                        item(key = "folder-${folder.id}") {
                            FolderHeader(
                                folder = folder,
                                count = inFolder.size,
                                expanded = expanded,
                                onToggle = { expandedFolders[folder.id] = !expanded },
                                onRename = { onRenameFolder(folder) },
                                onDelete = { onDeleteFolder(folder) },
                                isUltraNarrow = isUltraNarrow
                            )
                        }
                        if (expanded) {
                            items(inFolder, key = { it.id }) { server ->
                                ServerRow(
                                    server = server,
                                    selected = server.id == selectedId,
                                    onClick = { onSelect(server.id) },
                                    onEdit = { onEditServer(server) },
                                    onDelete = { onDeleteServer(server) },
                                    onManageLogins = { onManageLogins(server) },
                                    privacyMode = privacyMode
                                )
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun WorkspaceListContent(
    workspaces: List<Workspace>,
    onOpen: (Workspace) -> Unit,
    onDelete: (Int) -> Unit,
    isUltraNarrow: Boolean = false
) {
    Column {
        if (!isUltraNarrow) {
            Text(
                text = if (AppStrings.language == "ru") "Рабочие пространства" else "Workspaces",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (workspaces.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (AppStrings.language == "ru") "Нет сохраненных пространств" else "No saved workspaces",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(10.dp)) {
                items(workspaces, key = { it.id }) { ws ->
                    Card(
                        onClick = { onOpen(ws) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = ws.colorHex?.let { BorderStroke(2.dp, parseHexColor(it)) }
                    ) {
                        BoxWithConstraints {
                            val width = this.maxWidth
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(Icons.Default.GroupWork, null, tint = ws.colorHex?.let { parseHexColor(it) } ?: MaterialTheme.colorScheme.primary)
                                
                                if (width > 80.dp) {
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(ws.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (width > 160.dp) {
                                            Text("${ws.items.size} tabs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }

                                if (width > 120.dp) {
                                    IconButton(onClick = { onDelete(ws.id) }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Collapsible folder group header with rename/delete actions. */
@Composable
private fun FolderHeader(
    folder: ServerFolder,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isUltraNarrow: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 8.dp).size(16.dp)
            )

            if (!isUltraNarrow) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (count > 0) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                IconButton(onClick = onRename, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = AppStrings.rename, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = AppStrings.delete, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            } else {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ServerRow(
    server: Server,
    selected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onManageLogins: () -> Unit = {},
    privacyMode: Boolean = false
) {
    val containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
                         else MaterialTheme.colorScheme.surfaceVariant
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        BoxWithConstraints {
            val width = this.maxWidth
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
            ) {
                Icon(
                    imageVector = IconUtils.getIcon(server.iconName),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = if (width > 80.dp) 12.dp else 0.dp)
                )
                
                if (width > 80.dp) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = server.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (width > 180.dp) {
                            Text(
                                text = if (privacyMode) {
                                    "${server.username}@${PrivacyUtils.maskHost(server.host)}"
                                } else {
                                    "${server.username}@${server.host}:${server.port}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Row actions (edit / delete / logins) — visible on selection.
                // Hide buttons if extremely narrow to avoid overlapping
                if (selected && width > 130.dp) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = AppStrings.editServer, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onManageLogins, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Policy, contentDescription = AppStrings.identities, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = AppStrings.deleteServer, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractionPane(
    server: Server,
    pane: PaneType,
    onPaneChange: (PaneType) -> Unit,
    terminalSession: TerminalController?,
    sftpController: SftpController? = null,
    customCommands: List<CustomCommand> = emptyList(),
    logins: List<ServerLogin> = emptyList(),
    activeLoginId: Int? = null,
    onSelectLogin: (Int?) -> Unit = {},
    onManageLogins: () -> Unit = {},
    showTopBar: Boolean = true,
    showCommandPanel: Boolean = true,
    commandPaneWidth: Int = 190,
    onCommandPaneResize: (Float) -> Unit = {},
    onCommandPaneResizeEnd: () -> Unit = {},
    termBgHex: String = "#000000",
    termTextHex: String = "#00FF00",
    termFontSizePx: Float = 14f,
    termFontFamily: String = "JetBrains Mono",
    onFontSizeChange: (Float) -> Unit = {},
    rebootConfirmMode: String = "always",
    privacyMode: Boolean = false,
    onAddCommand: () -> Unit = {},
    onEditCommand: (CustomCommand) -> Unit = {},
    onManageCommands: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Shared so the terminal can be re-focused after clicking command buttons
    // (a Button click steals keyboard focus and keystrokes would stop
    // reaching the terminal otherwise).
    val terminalFocusRequester = remember { FocusRequester() }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            // Top bar: server info + login switch + pane switcher + app actions
            if (showTopBar) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            if (privacyMode) "${activeUsername(server, activeLoginId, logins)}@${PrivacyUtils.maskHost(server.host)}"
                            else "${activeUsername(server, activeLoginId, logins)}@${server.host}:${server.port}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Login selector (only if the server has extra logins).
                    if (logins.isNotEmpty()) {
                        LoginSwitcher(
                            logins = logins,
                            serverUsername = server.username,
                            activeLoginId = activeLoginId,
                            onSelect = onSelectLogin,
                            onManageLogins = onManageLogins
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    PaneSwitcher(pane, onPaneChange)
                }
                HorizontalDivider()
            }
            // Content area
            when (pane) {
                PaneType.Terminal -> if (terminalSession != null) {
                    Row(Modifier.fillMaxSize()) {
                        TerminalView(
                            terminalScreen = terminalSession.terminalScreen,
                            terminalRevision = terminalSession.terminalRevision,
                            isLoading = terminalSession.isLoading,
                            controller = terminalSession,
                            bgColor = parseHexColor(termBgHex),
                            textColor = parseHexColor(termTextHex),
                            fontSizeSp = termFontSizePx,
                            fontFamily = termFontFamily,
                            onFontSizeChange = onFontSizeChange,
                            focusRequester = terminalFocusRequester,
                            modifier = Modifier.weight(1f)
                        )
                        if (showCommandPanel) {
                            ResizableDivider(
                                onDrag = onCommandPaneResize,
                                onDragEnd = onCommandPaneResizeEnd,
                                orientation = ResizeOrientation.Horizontal
                            )
                            CommandPanel(
                                customCommands = customCommands,
                                rebootConfirmMode = rebootConfirmMode,
                                onExecute = {
                                    terminalSession.executeCommand(it)
                                    terminalFocusRequester.requestFocus()
                                },
                                onAddCommand = onAddCommand,
                                onEditCommand = onEditCommand,
                                onManageCommands = onManageCommands,
                                modifier = Modifier.width(commandPaneWidth.dp).fillMaxHeight()
                            )
                        }
                    }
                } else {
                    TerminalPreview(server)
                }
                PaneType.Split -> {
                    // Terminal on the left, SFTP on the right, resizable divider
                    // between them (starts 50/50). Quick-commands panel stays
                    // available on the far right, like in the Terminal pane.
                    val terminalWeight = remember(terminalSession) { mutableFloatStateOf(0.5f) }
                    var splitWidthPx by remember(terminalSession) { mutableIntStateOf(0) }
                    Row(
                        Modifier
                            .fillMaxSize()
                            .onSizeChanged { splitWidthPx = it.width }
                    ) {
                        if (terminalSession != null) {
                            TerminalView(
                                terminalScreen = terminalSession.terminalScreen,
                                terminalRevision = terminalSession.terminalRevision,
                                isLoading = terminalSession.isLoading,
                                controller = terminalSession,
                                bgColor = parseHexColor(termBgHex),
                                textColor = parseHexColor(termTextHex),
                                fontSizeSp = termFontSizePx,
                                fontFamily = termFontFamily,
                                onFontSizeChange = onFontSizeChange,
                                focusRequester = terminalFocusRequester,
                                modifier = Modifier.weight(terminalWeight.floatValue)
                            )
                        } else {
                            TerminalPreview(server, Modifier.weight(terminalWeight.floatValue))
                        }
                        ResizableDivider(
                            onDrag = { delta ->
                                if (splitWidthPx > 0) {
                                    terminalWeight.floatValue =
                                        (terminalWeight.floatValue + delta / splitWidthPx)
                                            .coerceIn(0.15f, 0.85f)
                                }
                            },
                            onDragEnd = {},
                            orientation = ResizeOrientation.Horizontal
                        )
                        SftpView(
                            sftpController,
                            modifier = Modifier.weight(1f - terminalWeight.floatValue)
                        )
                        if (showCommandPanel) {
                            ResizableDivider(
                                onDrag = onCommandPaneResize,
                                onDragEnd = onCommandPaneResizeEnd,
                                orientation = ResizeOrientation.Horizontal
                            )
                            CommandPanel(
                                customCommands = customCommands,
                                rebootConfirmMode = rebootConfirmMode,
                                onExecute = {
                                    terminalSession?.executeCommand(it)
                                    terminalFocusRequester.requestFocus()
                                },
                                onAddCommand = onAddCommand,
                                onEditCommand = onEditCommand,
                                onManageCommands = onManageCommands,
                                modifier = Modifier.width(commandPaneWidth.dp).fillMaxHeight()
                            )
                        }
                    }
                }
                PaneType.Sftp -> SftpView(sftpController)
                PaneType.Dashboard -> MonitoringDashboard(terminalSession)
            }
        }
    }
}

/** Converts a "#RRGGBB" / "#AARRGGBB" hex string to a Compose [Color]. */
internal fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val argb = when (cleaned.length) {
            6 -> (0xFF000000L or cleaned.toLong(16)).toInt()
            8 -> cleaned.toLong(16).toInt()
            else -> 0xFF000000.toInt()
        }
        Color(argb)
    } catch (e: Exception) {
        Color.Black
    }
}

/** Reads the whole [PlatformInputStream] into a UTF-8 string and closes it. */
private fun readAllText(input: PlatformInputStream): String {
    val buffer = java.io.ByteArrayOutputStream()
    val chunk = ByteArray(8192)
    while (true) {
        val n = input.read(chunk, 0, chunk.size)
        if (n <= 0) break
        buffer.write(chunk, 0, n)
    }
    input.close()
    return String(buffer.toByteArray(), Charsets.UTF_8)
}

/**
 * Quick-command rail shown to the right of the terminal: base commands plus
 * the user's custom commands (matching the phone's quick-commands row, but
 * arranged as a vertical list as requested for the desktop layout).
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun CommandPanel(
    customCommands: List<CustomCommand>,
    rebootConfirmMode: String,
    onExecute: (String) -> Unit,
    onAddCommand: () -> Unit = {},
    onEditCommand: (CustomCommand) -> Unit = {},
    onManageCommands: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var templateToFill by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categories = customCommands.mapNotNull { it.categoryName }.distinct().sorted()
    val filteredCommands = customCommands.filter { cmd ->
        (selectedCategory == null || cmd.categoryName == selectedCategory) &&
        (searchQuery.isEmpty() || cmd.name.contains(searchQuery, ignoreCase = true) || cmd.command.contains(searchQuery, ignoreCase = true))
    }

    fun handleExecute(rawCmd: String) {
        if (rawCmd.contains("{{") && rawCmd.contains("}}")) {
            templateToFill = rawCmd
        } else {
            onExecute(rawCmd)
        }
    }

    fun run(cmd: CustomCommand) {
        if (cmd.isDangerous && rebootConfirmMode != "never") {
            pendingCommand = cmd.command
        } else {
            handleExecute(cmd.command)
        }
    }

    Column(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Toolbar with Add and Manage actions
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                AppStrings.commands,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onAddCommand, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onManageCommands, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Settings, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
        }

        // Search and Categories
        Column(Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search scripts...", fontSize = 12.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(8.dp),
                trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp)) } }
            )
            
            if (categories.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    CategoryChip("All", selectedCategory == null) { selectedCategory = null }
                    categories.forEach { cat ->
                        CategoryChip(cat, selectedCategory == cat) { selectedCategory = cat }
                    }
                }
            }
        }
        
        HorizontalDivider()
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filteredCommands, key = { it.id }) { cmd ->
                val color = parseHexColor(cmd.colorHex).let { c ->
                    if (c == Color.Black) MaterialTheme.colorScheme.primary else c
                }
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = color.copy(alpha = 0.9f),
                    contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .combinedClickable(
                                onClick = { run(cmd) },
                                onLongClick = { onEditCommand(cmd) }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            cmd.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (cmd.isDangerous) {
                            Icon(Icons.Default.Warning, null, modifier = Modifier.size(14.dp).alpha(0.7f))
                        }
                    }
                }
            }

            if (filteredCommands.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No commands found", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    pendingCommand?.let { command ->
        AlertDialog(
            onDismissRequest = { pendingCommand = null },
            title = { Text(AppStrings.confirmExecution) },
            text = { Text(String.format(AppStrings.executeConfirmMsg, command)) },
            confirmButton = {
                TextButton(onClick = {
                    handleExecute(command)
                    pendingCommand = null
                }) { Text(AppStrings.execute) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCommand = null }) { Text(AppStrings.cancel) }
            }
        )
    }

    templateToFill?.let { template ->
        FillTemplateDialog(
            template = template,
            onConfirm = { filled ->
                onExecute(filled)
                templateToFill = null
            },
            onDismiss = { templateToFill = null }
        )
    }
}

@Composable
private fun CategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun FillTemplateDialog(
    template: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val vars = remember(template) {
        val regex = Regex("\\{\\{(.*?)\\}\\}")
        regex.findAll(template).map { it.groupValues[1] }.distinct().toList()
    }
    val values = remember { mutableStateMapOf<String, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fill Variables") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                vars.forEach { v ->
                    OutlinedTextField(
                        value = values[v] ?: "",
                        onValueChange = { values[v] = it },
                        label = { Text(v) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                var filled = template
                vars.forEach { v ->
                    filled = filled.replace("{{$v}}", values[v] ?: "")
                }
                onConfirm(filled)
            }) { Text("Run") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun PaneSwitcher(selected: PaneType, onSelect: (PaneType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PaneTab("Terminal", selected == PaneType.Terminal) { onSelect(PaneType.Terminal) }
        PaneTab("SFTP", selected == PaneType.Sftp) { onSelect(PaneType.Sftp) }
        PaneTab("Monitoring", selected == PaneType.Dashboard) { onSelect(PaneType.Dashboard) }
        PaneTab(
            if (AppStrings.language == "ru") "Терминал + SFTP" else "Terminal + SFTP",
            selected == PaneType.Split
        ) { onSelect(PaneType.Split) }
    }
}

@Composable
private fun PaneTab(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = bg
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/**
 * Fallback view shown when no real terminal session is available
 * (e.g. no server selected yet).
 */
@Composable
private fun TerminalPreview(server: Server, modifier: Modifier = Modifier) {
    val consoleBg = Color(0xFF0D1117)
    val consoleFg = Color(0xFFC9D1D9)
    val green = Color(0xFF3FB950)

    Box(
        modifier
            .fillMaxSize()
            .background(consoleBg)
            .padding(16.dp)
    ) {
        Column {
            if (server.host.isNotEmpty()) {
                Text(
                    text = "ssh ${server.username}@${server.host}",
                    color = green,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
                Text(
                    text = "SSH Commander desktop — терминал в разработке",
                    color = consoleFg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            } else {
                Text(
                    text = "Добавьте сервер слева, чтобы подключиться.",
                    color = consoleFg,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
/** Username to display, honoring the active login selection. */
private fun activeUsername(server: Server, activeLoginId: Int?, logins: List<ServerLogin>): String {
    val login = logins.firstOrNull { it.id == activeLoginId }
    return login?.username ?: server.username
}

/** Small dropdown to switch between a server's logins (like the phone's). */
@Composable
private fun LoginSwitcher(
    logins: List<ServerLogin>,
    serverUsername: String,
    activeLoginId: Int?,
    onSelect: (Int?) -> Unit,
    onManageLogins: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val active = logins.firstOrNull { it.id == activeLoginId }
    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (active != null) active.label
                else String.format(AppStrings.mainLoginLabel, serverUsername),
                maxLines = 1
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            // Main (server) login
            DropdownMenuItem(
                text = { Text(String.format(AppStrings.mainLoginLabel, serverUsername)) },
                onClick = { onSelect(null); expanded = false },
                leadingIcon = {
                    if (activeLoginId == null) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
            logins.forEach { login ->
                DropdownMenuItem(
                    text = { Text(login.label) },
                    onClick = { onSelect(login.id); expanded = false },
                    leadingIcon = {
                        if (login.id == activeLoginId) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(AppStrings.manageLogins) },
                onClick = { expanded = false; onManageLogins() }
            )
        }
    }
}

/** Which way a [ResizableDivider] drags the sibling pane. */
private enum class ResizeOrientation { Horizontal, Vertical }

/**
 * A thin draggable strip used to resize adjacent panes. Drags report deltas
 * so the caller can clamp and persist the new width/height.
 */
@Composable
private fun ResizableDivider(
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    orientation: ResizeOrientation = ResizeOrientation.Horizontal,
    modifier: Modifier = Modifier
) {
    val isHorizontal = orientation == ResizeOrientation.Horizontal
    val base = if (isHorizontal) {
        // Vertical strip that splits a Row horizontally. Needs fillMaxHeight()
        // (and a generous width for a comfortable grab target), otherwise it
        // collapses to zero height and there is nothing to drag.
        Modifier.width(10.dp).fillMaxHeight()
    } else {
        Modifier.fillMaxWidth().height(10.dp)
    }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = modifier
            .then(base)
            .resizeHoverCursor()
            .hoverable(interactionSource)
            .background(
                if (hovered) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else MaterialTheme.colorScheme.outlineVariant
            )
            .pointerInput(orientation) {
                if (isHorizontal) {
                    detectHorizontalDragGestures(
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                } else {
                    detectVerticalDragGestures(
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Visible grab line in the middle of the strip.
        Box(
            modifier = Modifier
                .then(
                    if (isHorizontal) Modifier.width(2.dp).fillMaxHeight(0.5f)
                    else Modifier.height(2.dp).fillMaxWidth(0.5f)
                )
                .background(
                    if (hovered) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline
                )
        )
    }
}

/**
 * Top menu bar: File (Import/Export JSON) + View (element visibility toggles).
 * Mirrors the desktop-native menu conventions while staying in Compose.
 */
@Composable
private fun DesktopMenuBar(
    showServerList: Boolean,
    showCommandPanel: Boolean,
    showTopBar: Boolean,
    onToggleServerList: (Boolean) -> Unit,
    onToggleCommandPanel: (Boolean) -> Unit,
    onToggleTopBar: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onSaveWorkspace: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fileExpanded by remember { mutableStateOf(false) }
    var viewExpanded by remember { mutableStateOf(false) }
    val fileMenuLabel = if (AppStrings.language == "ru") "Файл" else "File"
    val viewMenuLabel = if (AppStrings.language == "ru") "Вид" else "View"

    Surface(modifier = modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // File menu — icon-only button (keeps the desktop style consistent with
            // the Settings gear and About "i" icons).
            Box {
                IconButton(onClick = { fileExpanded = true; viewExpanded = false }) {
                    Icon(Icons.Default.InsertDriveFile, contentDescription = fileMenuLabel)
                }
                DropdownMenu(expanded = fileExpanded, onDismissRequest = { fileExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.exportData) },
                        onClick = { fileExpanded = false; onExport() }
                    )
                    DropdownMenuItem(
                        text = { Text(AppStrings.importData) },
                        onClick = { fileExpanded = false; onImport() }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Save Workspace") },
                        leadingIcon = { Icon(Icons.Default.Save, null) },
                        onClick = { fileExpanded = false; onSaveWorkspace() }
                    )
                }
            }
            // View menu — icon-only button.
            Box {
                IconButton(onClick = { viewExpanded = true; fileExpanded = false }) {
                    Icon(Icons.Default.Visibility, contentDescription = viewMenuLabel)
                }
                DropdownMenu(expanded = viewExpanded, onDismissRequest = { viewExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(if (AppStrings.language == "ru") "Список серверов" else "Server list") },
                        leadingIcon = {
                            Checkbox(
                                checked = showServerList,
                                onCheckedChange = null
                            )
                        },
                        onClick = { onToggleServerList(!showServerList) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (AppStrings.language == "ru") "Панель команд" else "Command panel") },
                        leadingIcon = {
                            Checkbox(
                                checked = showCommandPanel,
                                onCheckedChange = null
                            )
                        },
                        onClick = { onToggleCommandPanel(!showCommandPanel) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (AppStrings.language == "ru") "Верхняя панель" else "Top bar") },
                        leadingIcon = {
                            Checkbox(
                                checked = showTopBar,
                                onCheckedChange = null
                            )
                        },
                        onClick = { onToggleTopBar(!showTopBar) }
                    )
                }
            }
            // Settings & About live on the same row as File/View.
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = AppStrings.settings)
            }
            IconButton(onClick = onOpenAbout) {
                Icon(Icons.Default.Info, contentDescription = AppStrings.aboutApp)
            }
        }
    }
}

@Composable
private fun ServerDialog(
    server: Server?,
    initialPassword: String,
    folders: List<ServerFolder> = emptyList(),
    sshKeys: List<SshKey> = emptyList(),
    onSave: (Server, String) -> Unit,
    onManageLogins: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isEdit = server != null
    var name by remember { mutableStateOf(server?.name ?: "") }
    var host by remember { mutableStateOf(server?.host ?: "") }
    var port by remember { mutableStateOf((server?.port ?: 22).toString()) }
    var username by remember { mutableStateOf(server?.username ?: "") }
    var password by remember { mutableStateOf(initialPassword) }
    var sftpStartPath by remember { mutableStateOf(server?.sftpStartPath ?: "") }
    var iconName by remember { mutableStateOf(server?.iconName ?: "Default") }
    var folderId by remember { mutableStateOf(server?.folderId) }
    var sshKeyId by remember { mutableStateOf(server?.sshKeyId) }

    fun submit() {
        val trimmedHost = host.trim()
        val trimmedName = name.trim().ifEmpty { trimmedHost }
        val trimmedUser = username.trim()
        if (trimmedHost.isEmpty() || trimmedUser.isEmpty()) return
        onSave(
            Server(
                id = server?.id ?: 0,
                name = trimmedName,
                host = trimmedHost,
                port = port.toIntOrNull() ?: 22,
                username = trimmedUser,
                iconName = iconName,
                sftpStartPath = sftpStartPath.trim().ifEmpty { null },
                folderId = folderId,
                sshKeyId = sshKeyId
            ),
            password
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) AppStrings.editServer else AppStrings.addServer, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.serverName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text(AppStrings.hostIp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(AppStrings.port) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text(AppStrings.username) },
                        singleLine = true,
                        modifier = Modifier.weight(2f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(AppStrings.identities, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    if (isEdit && onManageLogins != null) {
                        TextButton(onClick = onManageLogins) {
                            Icon(Icons.Default.Settings, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(AppStrings.edit)
                        }
                    }
                }

                if (isEdit && onManageLogins != null) {
                    Card(
                        onClick = onManageLogins,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Policy, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Manage Identities & Keys", fontWeight = FontWeight.Bold)
                                Text("Add users, generate keys or auto-provision", style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Default.ChevronRight, null)
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(AppStrings.password) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        val cb = androidx.compose.ui.platform.LocalClipboardManager.current
                        IconButton(onClick = { cb.getText()?.text?.let { password = it } }) {
                            Icon(Icons.Default.ContentPaste, "Paste")
                        }
                    }
                )
                OutlinedTextField(
                    value = sftpStartPath,
                    onValueChange = { sftpStartPath = it },
                    label = { Text(AppStrings.sftpStartPath) },
                    placeholder = { Text("/") },
                    supportingText = { Text(AppStrings.sftpStartPathHint2) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Folder selector (server grouping).
                FolderDropdown(
                    folders = folders,
                    selectedId = folderId,
                    onSelect = { folderId = it }
                )

                // SSH Key selector (legacy, keeping for compatibility in this dialog)
                if (sshKeys.isNotEmpty()) {
                    Text(AppStrings.sshKeys, style = MaterialTheme.typography.titleSmall)
                    var expandedKeyMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedCard(
                            onClick = { expandedKeyMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text = sshKeys.firstOrNull { it.id == sshKeyId }?.name ?: "No key selected",
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, null)
                            }
                        }
                        DropdownMenu(expanded = expandedKeyMenu, onDismissRequest = { expandedKeyMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("No key") },
                                onClick = { sshKeyId = null; expandedKeyMenu = false }
                            )
                            sshKeys.forEach { key ->
                                DropdownMenuItem(
                                    text = { Text(key.name) },
                                    onClick = { sshKeyId = key.id; expandedKeyMenu = false }
                                )
                            }
                        }
                    }
                }

                Text(AppStrings.chooseIcon, style = MaterialTheme.typography.titleSmall)
                // Icon picker grid (mirrors the mobile AddEditServerScreen).
                Box(modifier = Modifier.height(120.dp)) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(72.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItems(IconUtils.availableIcons) { option ->
                            val isSelected = iconName == option.name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .border(
                                        width = if (isSelected) 2.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { iconName = option.name }
                                    .padding(6.dp)
                            ) {
                                Icon(
                                    option.icon,
                                    contentDescription = option.label,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = ::submit) {
                Text(if (isEdit) AppStrings.save else AppStrings.addServer)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}

/** Which action a [FolderNameDialog] performs on confirm. */
private sealed class FolderNameDialogState {
    data object Add : FolderNameDialogState()
    data class Rename(val folder: ServerFolder) : FolderNameDialogState()
}

/** Small text-input dialog for creating/renaming a folder. */
@Composable
private fun FolderNameDialog(
    initialName: String,
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(AppStrings.folderNamePlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text(if (initialName.isEmpty()) AppStrings.create else AppStrings.rename)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}

/** Dropdown to pick a folder (or "No folder") when editing a server. */
@Composable
private fun FolderDropdown(
    folders: List<ServerFolder>,
    selectedId: Int?,
    onSelect: (Int?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = folders.firstOrNull { it.id == selectedId }?.name ?: AppStrings.noFolder
    Column {
        Text(AppStrings.folders, style = MaterialTheme.typography.titleSmall)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(selectedName, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text(AppStrings.noFolder) },
                    onClick = { onSelect(null); expanded = false },
                    leadingIcon = {
                        if (selectedId == null) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
                folders.forEach { folder ->
                    DropdownMenuItem(
                        text = { Text(folder.name, maxLines = 1) },
                        onClick = { onSelect(folder.id); expanded = false },
                        leadingIcon = {
                            if (folder.id == selectedId) {
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Desktop dialog to manage a server's logins (add/edit/delete/set default).
 * Mirrors the phone ManageLoginsScreen.
 */
@Composable
private fun ManageLoginsDialog(
    server: Server,
    repository: ServerRepository?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val logins = remember { mutableStateListOf<ServerLogin>() }
    val sshKeys = remember { mutableStateListOf<SshKey>() }
    var editing by remember { mutableStateOf<ServerLogin?>(null) }
    var adding by remember { mutableStateOf(false) }
    var loginToDelete by remember { mutableStateOf<ServerLogin?>(null) }
    var editingPassword by remember { mutableStateOf<String?>(null) }
    var isProvisioning by remember { mutableStateOf(false) }

    LaunchedEffect(repository, server.id) {
        logins.clear()
        repository?.getLoginsForServer(server.id)?.collect {
            logins.clear()
            logins.addAll(it)
        }
    }

    LaunchedEffect(repository) {
        repository?.allSshKeys?.collect {
            sshKeys.clear()
            sshKeys.addAll(it)
        }
    }

    LaunchedEffect(editing) {
        editingPassword = editing?.let { repository?.getLoginPassword(it.id) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.identities, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    String.format(AppStrings.mainLoginLabel, server.username),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (logins.isEmpty()) {
                    Text(
                        AppStrings.noLogins,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(logins, key = { it.id }) { login ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = if (login.sshKeyId != null) Icons.Default.VpnKey else Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        login.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        login.username,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (login.isDefault) {
                                    Icon(Icons.Default.Star, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp).padding(end = 8.dp))
                                } else {
                                    TextButton(onClick = {
                                        scope.launch {
                                            repository?.updateLogin(login.copy(isDefault = true), null)
                                            logins.filter { it.id != login.id && it.isDefault }.forEach { other ->
                                                repository?.updateLogin(other.copy(isDefault = false), null)
                                            }
                                        }
                                    }) {
                                        Text(if (AppStrings.language == "ru") "По умолч." else "Default", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                IconButton(onClick = { editing = login }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp))
                                }
                                IconButton(onClick = { loginToDelete = login }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(onClick = { adding = true }, enabled = !isProvisioning) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(AppStrings.addLogin)
                }
                Spacer(Modifier.width(12.dp))
                TextButton(onClick = onDismiss) { Text(AppStrings.dismiss) }
            }
        }
    )

    if (adding || editing != null) {
        val target = editing
        IdentityEditDialog(
            initialLabel = target?.label ?: "",
            initialUsername = target?.username ?: "",
            initialPassword = editingPassword ?: "",
            initialSftpPath = target?.sftpStartPath ?: "",
            initialSshKeyId = target?.sshKeyId,
            sshKeys = sshKeys,
            serverId = server.id,
            isProvisioning = isProvisioning,
            onDismiss = { adding = false; editing = null },
            onConfirm = { label, username, password, sftpPath, sshKeyId, autoProvision, genType, genBits, genPass ->
                scope.launch {
                    if (autoProvision) {
                        isProvisioning = true
                        try {
                            // Generate key with user-defined settings
                            val (priv, pub) = com.neytron.sshcommander.data.SshKeyUtils.generateKeyPair(genType, genBits, genPass)
                            val newKeyId = repository?.insertSshKey(SshKey(
                                name = "Key for $username", 
                                type = genType, 
                                privateKeyContent = priv, 
                                publicKeyContent = pub,
                                passphraseKey = genPass
                            ))
                            
                            val result = repository?.provisionUser(server.id, username, pub, password)
                            
                            if (result?.isSuccess == true) {
                                repository?.insertLogin(
                                    ServerLogin(
                                        serverId = server.id, label = label, username = username,
                                        sftpStartPath = sftpPath.ifBlank { null }, sshKeyId = newKeyId,
                                        isDefault = logins.isEmpty()
                                    ),
                                    password
                                )
                                platformToast(AppStrings.provisionSuccess)
                                adding = false
                                editing = null
                            } else {
                                platformToast("Provisioning failed: ${result?.exceptionOrNull()?.message}")
                            }
                        } catch (e: Exception) {
                            platformToast("Error: ${e.message}")
                        } finally {
                            isProvisioning = false
                        }
                    } else {
                        if (target != null) {
                            repository?.updateLogin(
                                target.copy(label = label, username = username, sftpStartPath = sftpPath.ifBlank { null }, sshKeyId = sshKeyId),
                                password
                            )
                        } else {
                            repository?.insertLogin(
                                ServerLogin(serverId = server.id, label = label, username = username, sftpStartPath = sftpPath.ifBlank { null }, isDefault = logins.isEmpty(), sshKeyId = sshKeyId),
                                password
                            )
                        }
                        adding = false
                        editing = null
                    }
                }
            }
        )
    }

    loginToDelete?.let { login ->
        AlertDialog(
            onDismissRequest = { loginToDelete = null },
            title = { Text(AppStrings.deleteLoginTitle) },
            text = { Text(String.format(AppStrings.deleteLoginMsg, login.label)) },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { repository?.deleteLogin(login) }
                    loginToDelete = null
                }) { Text(AppStrings.delete, color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { loginToDelete = null }) { Text(AppStrings.cancel) }
            }
        )
    }
}

/** Single identity editor (Unified Auth Master). */
@Composable
private fun IdentityEditDialog(
    initialLabel: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    initialSftpPath: String = "",
    initialSshKeyId: Int? = null,
    sshKeys: List<SshKey> = emptyList(),
    serverId: Int,
    isProvisioning: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (label: String, username: String, password: String, sftpPath: String, sshKeyId: Int?, autoProvision: Boolean, keyType: String, keyBits: Int, keyPass: String?) -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var sftpPath by remember { mutableStateOf(initialSftpPath) }
    var sshKeyId by remember { mutableStateOf(initialSshKeyId) }
    var authMethod by remember { mutableStateOf(if (initialSshKeyId != null) 1 else 0) } // 0: Password, 1: Key
    var autoProvision by remember { mutableStateOf(false) }
    
    // Key generation settings
    var genBits by remember { mutableIntStateOf(4096) }
    var genPassphrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLabel.isEmpty()) AppStrings.addLogin else AppStrings.edit, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(AppStrings.loginLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(AppStrings.username) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text(AppStrings.authMethod, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = authMethod == 0, onClick = { authMethod = 0; autoProvision = false })
                    Text(AppStrings.usePassword, modifier = Modifier.clickable { authMethod = 0; autoProvision = false })
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = authMethod == 1, onClick = { authMethod = 1 })
                    Text(AppStrings.useSshKey, modifier = Modifier.clickable { authMethod = 1 })
                }

                if (authMethod == 0) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(AppStrings.password) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Key selection / generation
                    if (initialLabel.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = autoProvision, onCheckedChange = { autoProvision = it })
                                    Text(AppStrings.autoProvisionDesc, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    AppStrings.provisioningWarning,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                                
                                if (autoProvision) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Text("New Key Settings:", style = MaterialTheme.typography.labelMedium)
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("RSA Bits: ", style = MaterialTheme.typography.bodySmall)
                                        RadioButton(selected = genBits == 2048, onClick = { genBits = 2048 })
                                        Text("2048", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(8.dp))
                                        RadioButton(selected = genBits == 4096, onClick = { genBits = 4096 })
                                        Text("4096", style = MaterialTheme.typography.bodySmall)
                                    }

                                    OutlinedTextField(
                                        value = genPassphrase,
                                        onValueChange = { genPassphrase = it },
                                        label = { Text(AppStrings.passphrase) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                                    )

                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Initial System Password (for useradd)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!autoProvision) {
                        Text(AppStrings.selectExistingKey, style = MaterialTheme.typography.labelMedium)
                        var expandedKeyMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedCard(onClick = { expandedKeyMenu = true }, modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Text(sshKeys.firstOrNull { it.id == sshKeyId }?.name ?: "No key selected", modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowDropDown, null)
                                }
                            }
                            DropdownMenu(expanded = expandedKeyMenu, onDismissRequest = { expandedKeyMenu = false }) {
                                DropdownMenuItem(text = { Text("No key") }, onClick = { sshKeyId = null; expandedKeyMenu = false })
                                sshKeys.forEach { key ->
                                    DropdownMenuItem(text = { Text(key.name) }, onClick = { sshKeyId = key.id; expandedKeyMenu = false })
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sftpPath,
                    onValueChange = { sftpPath = it },
                    label = { Text(AppStrings.sftpStartPath) },
                    placeholder = { Text("/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        label.trim(), 
                        username.trim(), 
                        password, 
                        sftpPath.trim(), 
                        if (authMethod == 0) null else sshKeyId, 
                        autoProvision,
                        "RSA", 
                        genBits, 
                        genPassphrase.takeIf { it.isNotBlank() }
                    )
                },
                enabled = label.isNotBlank() && username.isNotBlank() && !isProvisioning
            ) {
                if (isProvisioning) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Processing...")
                } else {
                    Text(if (autoProvision) "Provision & Save" else AppStrings.save)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProvisioning) { Text(AppStrings.cancel) }
        }
    )
}

/**
 * Desktop settings dialog. Mirrors the phone SettingsScreen but adapted to a
 * dialog: theme, language, terminal colors, privacy mode and auto-reconnect.
 */
@Composable
private fun DesktopSettingsDialog(
    settings: AppSettings?,
    backupManager: DataBackupManager? = null,
    onSshKeysClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val themeMode by (settings?.themeMode?.collectAsState(initial = "system")
        ?: remember { mutableStateOf("system") })
    val language by (settings?.language?.collectAsState(initial = "en")
        ?: remember { mutableStateOf("en") })
    val termBgHex by (settings?.termBgColor?.collectAsState(initial = "#000000")
        ?: remember { mutableStateOf("#000000") })
    val termTextHex by (settings?.termTextColor?.collectAsState(initial = "#00FF00")
        ?: remember { mutableStateOf("#00FF00") })
    val termThemeId by (settings?.termThemeId?.collectAsState(initial = "tokyo_night")
        ?: remember { mutableStateOf("tokyo_night") })
    val termFontFamily by (settings?.termFontFamily?.collectAsState(initial = "JetBrains Mono")
        ?: remember { mutableStateOf("JetBrains Mono") })
    
    var themeToPreview by remember { mutableStateOf<TerminalTheme?>(null) }

    val privacyMode by (settings?.privacyMode?.collectAsState(initial = false)
        ?: remember { mutableStateOf(false) })
    val autoReconnect by (settings?.autoReconnect?.collectAsState(initial = true)
        ?: remember { mutableStateOf(true) })

    // Keep identical to the phone SettingsScreen presets for color parity.
    val presetBgColors = listOf("#000000", "#1A1A1B", "#2D2D2D", "#FFFFFF", "#F5F5F5", "#002B36", "#073642")
    val presetTextColors = listOf("#00FF00", "#008000", "#FFFFFF", "#000000", "#FFD700", "#FFA500", "#FF0000", "#268BD2")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.settings, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Theme
                SettingsBlockTitle(AppStrings.theme)
                RadioButtonOption(AppStrings.themeLight, themeMode == "light") {
                    scope.launch { settings?.setThemeMode("light") }
                }
                RadioButtonOption(AppStrings.themeDark, themeMode == "dark") {
                    scope.launch { settings?.setThemeMode("dark") }
                }
                RadioButtonOption(AppStrings.themeSystem, themeMode == "system") {
                    scope.launch { settings?.setThemeMode("system") }
                }

                // Language
                SettingsBlockTitle(AppStrings.languageLabel)
                RadioButtonOption(AppStrings.langEn, language == "en") {
                    scope.launch { settings?.setLanguage("en"); AppStrings.language = "en" }
                }
                RadioButtonOption(AppStrings.langRu, language == "ru") {
                    scope.launch { settings?.setLanguage("ru"); AppStrings.language = "ru" }
                }

                Text(if (AppStrings.language == "ru") "Тема терминала" else "Terminal Theme", style = MaterialTheme.typography.labelMedium)
                DesktopThemePresetSelector(
                    selectedThemeId = termThemeId,
                    onThemeSelected = { themeId ->
                        val theme = TerminalThemes.presets.find { it.id == themeId }
                        if (theme != null && themeId != "custom") {
                            themeToPreview = theme
                        } else {
                            scope.launch { settings?.setTermThemeId(themeId) }
                        }
                    }
                )

                if (termThemeId == "custom") {
                    Text(AppStrings.backgroundColor, style = MaterialTheme.typography.labelSmall)
                    ColorChipRow(presetBgColors, termBgHex) {
                        scope.launch { settings?.setTermBgColor(it) }
                    }
                    Text(AppStrings.textColor, style = MaterialTheme.typography.labelSmall)
                    ColorChipRow(presetTextColors, termTextHex) {
                        scope.launch { settings?.setTermTextColor(it) }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(if (AppStrings.language == "ru") "Шрифт терминала" else "Terminal Font", style = MaterialTheme.typography.labelMedium)
                DesktopFontSelector(
                    selectedFont = termFontFamily,
                    onFontSelected = { scope.launch { settings?.setTermFontFamily(it) } }
                )

                // Privacy mode
                SettingsBlockTitle(AppStrings.privacyMode)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(AppStrings.privacyMode, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            AppStrings.privacyModeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = privacyMode,
                        onCheckedChange = { scope.launch { settings?.setPrivacyMode(it) } }
                    )
                }

                // Auto-reconnect
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(AppStrings.autoReconnect, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            AppStrings.autoReconnectDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { scope.launch { settings?.setAutoReconnect(it) } }
                    )
                }

                HorizontalDivider()

                // SSH Keys Management
                SettingsBlockTitle(AppStrings.manageKeys)
                OutlinedButton(
                    onClick = onSshKeysClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.sshKeys)
                }

                // Cloud Sync
                CloudSyncSection(backupManager = backupManager)

                HorizontalDivider()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.dismiss) }
        }
    )

    // Desktop Theme Preview Dialog
    themeToPreview?.let { theme ->
        DesktopThemePreviewDialog(
            theme = theme,
            fontFamily = termFontFamily,
            onDismiss = { themeToPreview = null },
            onApply = {
                scope.launch {
                    settings?.setTermThemeId(theme.id)
                    settings?.setTermBgColor(theme.backgroundColor)
                    settings?.setTermTextColor(theme.textColor)
                }
                themeToPreview = null
            }
        )
    }
}

@Composable
private fun DesktopThemePreviewDialog(
    theme: TerminalTheme,
    fontFamily: String,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        kotlinx.coroutines.delay(50)
        visible = true 
    }

    val currentFontFamily = getSystemFontFamily(fontFamily)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Terminal, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Text(theme.name, fontWeight = FontWeight.ExtraBold)
            }
        },
        text = {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + expandVertically(tween(600)),
                exit = fadeOut(tween(300))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Large Rich terminal preview box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(parseHexColor(theme.backgroundColor))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "neytron@commander:~$ ls -la /var/log",
                                color = parseHexColor(theme.textColor).copy(alpha = 0.6f),
                                fontSize = 13.sp,
                                fontFamily = currentFontFamily
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "drwxr-xr-x 2 root root 4096 Aug 29 auth.log",
                                color = parseHexColor(theme.textColor),
                                fontSize = 13.sp,
                                fontFamily = currentFontFamily
                            )
                            Text(
                                "-rw-r----- 1 root adm  1285 Aug 29 syslog",
                                color = parseHexColor(theme.textColor),
                                fontSize = 13.sp,
                                fontFamily = currentFontFamily
                            )
                            Spacer(Modifier.height(6.dp))
                            Row {
                                Text(
                                    "neytron@commander:~$ ",
                                    color = parseHexColor(theme.textColor).copy(alpha = 0.6f),
                                    fontSize = 13.sp,
                                    fontFamily = currentFontFamily
                                )
                                // Animated cursor
                                var cursorVisible by remember { mutableStateOf(true) }
                                LaunchedEffect(Unit) {
                                    while(true) {
                                        kotlinx.coroutines.delay(500)
                                        cursorVisible = !cursorVisible
                                    }
                                }
                                if (cursorVisible) {
                                    Box(Modifier.width(8.dp).height(18.dp).background(parseHexColor(theme.textColor)))
                                }
                            }
                        }
                    }

                    Text(
                        theme.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(if (AppStrings.language == "ru") "Применить тему" else "Apply Theme", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.cancel, color = MaterialTheme.colorScheme.error)
            }
        }
    )
}

@Composable
private fun DesktopThemePresetSelector(
    selectedThemeId: String,
    onThemeSelected: (String) -> Unit
) {
    Box(modifier = Modifier.height(350.dp).fillMaxWidth().padding(vertical = 8.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 90.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            gridItems(TerminalThemes.presets) { theme ->
                val isSelected = selectedThemeId == theme.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onThemeSelected(theme.id) }
                        .padding(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(35.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(parseHexColor(theme.backgroundColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("abc", color = parseHexColor(theme.textColor), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        theme.name, 
                        style = MaterialTheme.typography.labelSmall, 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
private fun DesktopFontSelector(
    selectedFont: String,
    onFontSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedFont,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.exposedDropdownSize()
        ) {
            TerminalThemes.modernFonts.forEach { font ->
                DropdownMenuItem(
                    text = { Text(font, fontFamily = getSystemFontFamily(font)) },
                    onClick = {
                        onFontSelected(font)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsBlockTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun RadioButtonOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun ColorChipRow(colors: List<String>, selectedColor: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEach { colorHex ->
            val isSelected = colorHex.equals(selectedColor, ignoreCase = true)
            Surface(
                onClick = { onSelect(colorHex) },
                color = parseHexColor(colorHex),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.size(32.dp)
            ) {}
        }
    }
}

/** Advanced About dialog with sections and ad settings. */
@Composable
private fun AboutDialog(
    appVersion: String,
    settings: AppSettings?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val adsEnabled by (settings?.adsEnabled?.collectAsState(initial = true) ?: remember { mutableStateOf(true) })
    val language by (settings?.language?.collectAsState(initial = AppStrings.language) ?: remember { mutableStateOf("en") })
    val content = remember(language) { AboutContent.forLanguage(language) }

    var showAboutText by remember { mutableStateOf(false) }
    var showLicense by remember { mutableStateOf(false) }
    var showAdConfirm1 by remember { mutableStateOf(false) }
    var showAdConfirm2 by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text(AppStrings.appName, fontWeight = FontWeight.Bold)
                Text(
                    String.format(AppStrings.aboutVersion, appVersion.ifBlank { "1.6" }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider()
                
                // Sections
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(AppStrings.aboutApp) },
                            leadingContent = { Icon(Icons.Default.Info, null) },
                            modifier = Modifier.clickable { showAboutText = true }
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                        ListItem(
                            headlineContent = { Text(AppStrings.license) },
                            leadingContent = { Icon(Icons.Default.Policy, null) },
                            modifier = Modifier.clickable { showLicense = true }
                        )
                    }
                }

                // Ad Toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    ListItem(
                        headlineContent = { Text(AppStrings.disableAds) },
                        supportingContent = { Text(AppStrings.disableAdsDesc) },
                        trailingContent = {
                            Switch(
                                checked = !adsEnabled,
                                onCheckedChange = { checked ->
                                    if (checked) showAdConfirm1 = true
                                    else scope.launch { settings?.setAdsEnabled(true) }
                                }
                            )
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.dismiss) }
        }
    )

    // Sub-dialogs
    if (showAboutText) {
        AlertDialog(
            onDismissRequest = { showAboutText = false },
            title = { Text(AppStrings.aboutApp) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    content.description.forEach { paragraph ->
                        Text(paragraph, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAboutText = false }) { Text("OK") } }
        )
    }

    if (showLicense) {
        AlertDialog(
            onDismissRequest = { showLicense = false },
            title = { Text(AppStrings.license) },
            text = {
                Text(
                    "SSH Commander is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.",
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = { TextButton(onClick = { showLicense = false }) { Text("OK") } }
        )
    }

    if (showAdConfirm1) {
        AlertDialog(
            onDismissRequest = { showAdConfirm1 = false },
            title = { Text(AppStrings.disableAdsConfirmTitle) },
            text = { Text(AppStrings.disableAdsConfirmMsg) },
            confirmButton = {
                Button(onClick = {
                    showAdConfirm1 = false
                    showAdConfirm2 = true
                }) { Text(AppStrings.yes) }
            },
            dismissButton = { TextButton(onClick = { showAdConfirm1 = false }) { Text(AppStrings.no) } }
        )
    }

    if (showAdConfirm2) {
        AlertDialog(
            onDismissRequest = { showAdConfirm2 = false },
            title = { Text(AppStrings.disableAdsConfirmFinalTitle) },
            text = { Text(AppStrings.disableAdsConfirmFinalMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { settings?.setAdsEnabled(false) }
                        showAdConfirm2 = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(AppStrings.disableAds) }
            },
            dismissButton = { TextButton(onClick = { showAdConfirm2 = false }) { Text(AppStrings.cancel) } }
        )
    }
}

/**
 * Desktop dialog to add / edit / delete / reorder the user's custom quick
 * commands. Mirrors the phone ManageCommandsScreen functionality so both
 * platforms share the same command repository.
 */
@Composable
private fun ManageCommandsDialog(
    repository: ServerRepository?,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val commands = remember { mutableStateListOf<CustomCommand>() }
    var showAddDialog by remember { mutableStateOf(false) }
    var commandToEdit by remember { mutableStateOf<CustomCommand?>(null) }

    LaunchedEffect(repository) {
        repository?.getAllCustomCommands()?.collect {
            commands.clear()
            commands.addAll(it)
        }
    }

    fun moveUp(index: Int) {
        if (index > 0 && repository != null) {
            val current = commands[index]
            val previous = commands[index - 1]
            scope.launch {
                repository.updateCustomCommand(current.copy(orderIndex = previous.orderIndex))
                repository.updateCustomCommand(previous.copy(orderIndex = current.orderIndex))
            }
        }
    }

    fun moveDown(index: Int) {
        if (index < commands.size - 1 && repository != null) {
            val current = commands[index]
            val next = commands[index + 1]
            scope.launch {
                repository.updateCustomCommand(current.copy(orderIndex = next.orderIndex))
                repository.updateCustomCommand(next.copy(orderIndex = current.orderIndex))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.manageCommands, fontWeight = FontWeight.Bold) },
        text = {
            if (commands.isEmpty()) {
                Text(
                    if (AppStrings.language == "ru") "Нет своих команд" else "No custom commands yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    itemsIndexed(commands) { index, command ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { commandToEdit = command }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(parseHexColor(command.colorHex))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(command.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    command.command,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            IconButton(
                                onClick = { moveUp(index) },
                                enabled = index > 0
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = null) }
                            IconButton(
                                onClick = { moveDown(index) },
                                enabled = index < commands.size - 1
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = null) }
                            IconButton(onClick = {
                                scope.launch { repository?.deleteCustomCommand(command) }
                            }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(AppStrings.newCommand)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text(AppStrings.dismiss) }
            }
        }
    )

    if (showAddDialog) {
        val primary = MaterialTheme.colorScheme.primary
        val initialColor = String.format(
            "#%06X",
            ((primary.red * 255).toInt() shl 16) or
                ((primary.green * 255).toInt() shl 8) or
                (primary.blue * 255).toInt()
        )
        AddCommandDialogDesktop(
            initialColorHex = initialColor,
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cmd, cat, isDangerous, colorHex ->
                scope.launch {
                    repository?.insertCustomCommand(
                        CustomCommand(
                            name = name,
                            command = cmd,
                            categoryName = cat.ifBlank { null },
                            iconName = "default",
                            colorHex = colorHex,
                            orderIndex = commands.size,
                            isDangerous = isDangerous
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }

    commandToEdit?.let { editing ->
        AddCommandDialogDesktop(
            initialName = editing.name,
            initialCommand = editing.command,
            initialCategory = editing.categoryName ?: "",
            initialIsDangerous = editing.isDangerous,
            initialColorHex = editing.colorHex,
            onDismiss = { commandToEdit = null },
            onConfirm = { name, cmd, cat, isDangerous, colorHex ->
                scope.launch {
                    repository?.updateCustomCommand(
                        editing.copy(
                            name = name,
                            command = cmd,
                            categoryName = cat.ifBlank { null },
                            isDangerous = isDangerous,
                            colorHex = colorHex
                        )
                    )
                }
                commandToEdit = null
            }
        )
    }
}

/** Add/edit dialog for a single custom command (desktop flavour). */
@Composable
private fun AddCommandDialogDesktop(
    initialName: String = "",
    initialCommand: String = "",
    initialCategory: String = "",
    initialIsDangerous: Boolean = false,
    initialColorHex: String = "#2196F3",
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var cmd by remember { mutableStateOf(initialCommand) }
    var category by remember { mutableStateOf(initialCategory) }
    var isDangerous by remember { mutableStateOf(initialIsDangerous) }
    var colorHex by remember { mutableStateOf(initialColorHex) }

    val presetColors = listOf(
        "#F44336", "#E91E63", "#9C27B0", "#673AB7",
        "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
        "#009688", "#4CAF50", "#8BC34A", "#CDDC39",
        "#FFEB3B", "#FFC107", "#FF9800", "#FF5722",
        "#795548", "#9E9E9E", "#607D8B", "#000000"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialName.isEmpty()) AppStrings.newCommand else AppStrings.edit, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.serverName) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cmd,
                    onValueChange = { cmd = it },
                    label = { Text(AppStrings.commands) },
                    placeholder = { Text(AppStrings.cmdPlaceholderExample) },
                    modifier = Modifier.fillMaxWidth().height(110.dp)
                )

                Text(
                    if (AppStrings.language == "ru") "Цвет кнопки" else "Button Color",
                    style = MaterialTheme.typography.labelLarge
                )
                ColorChipRow(presetColors, colorHex) { colorHex = it }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isDangerous, onCheckedChange = { isDangerous = it })
                    Text(AppStrings.requiresBio)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, cmd, category, isDangerous, colorHex) }) {
                Text(AppStrings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}

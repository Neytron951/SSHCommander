package com.neytron.sshcommander.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.TerminalScreenStore
import com.neytron.sshcommander.ui.MonitoringDashboard
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerControlScreen(
    serverId: Int,
    sessionId: Int,
    onNavigateBack: () -> Unit,
    onManageCommands: () -> Unit,
    onManageLogins: () -> Unit,
    onNavigateToSftp: () -> Unit,
    onSwitchSession: (Int) -> Unit = {},
    onAddSession: (Int) -> Unit = {},
    onCloseSession: (Int) -> Unit = {}
) {
    val deps = LocalAppDeps.current
    val viewModel: SshViewModel = viewModel { SshViewModel(deps.repository, deps.settings) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Terminal, 1: Monitoring

    // In landscape the vertical space is tight: the quick-command row is collapsed
    // by default (can be toggled via the top bar button), and the bottom panel
    // is rendered more compact.
    val isLandscape = isLandscapeLayout()
    var quickCommandsVisible by remember { mutableStateOf(!isLandscape) }

    var customCommandInput by remember { mutableStateOf("") }
    var templateToFill by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var isPendingDangerous by remember { mutableStateOf(false) }

    var showExitConfirmation by remember { mutableStateOf(false) }
    var showSaveWorkspaceDialog by remember { mutableStateOf(false) }

    // Soft keyboard support: tapping the terminal focuses a hidden text field
    // and opens the device keyboard; typed characters stream straight to SSH.
    val terminalFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var terminalInputBuffer by remember { mutableStateOf("") }

    val fontFamilyStr by deps.settings.fontFamily.collectAsState(initial = "monospace")
    val rebootConfirmMode by deps.settings.rebootConfirm.collectAsState(initial = "always")

    val termBgColor by viewModel.termBgColor.collectAsState()
    val termTextColor by viewModel.termTextColor.collectAsState()
    val termFontSizePx by viewModel.termFontSizePx.collectAsState()

    val consoleFontFamily = when(fontFamilyStr) {
        "monospace" -> FontFamily.Monospace
        "sans_serif" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        else -> FontFamily.Default
    }

    // List of useful base commands
    val baseCommands = listOf(
        "ls -la" to AppStrings.cmdList,
        "top" to AppStrings.cmdTop,
        "df -h" to AppStrings.cmdDisk,
        "free -m" to AppStrings.cmdRam,
        "uptime" to AppStrings.cmdUptime,
        "ps aux" to AppStrings.cmdProcesses,
        "dmesg" to AppStrings.cmdLogs
    )

    fun handleExecute(rawCmd: String) {
        if (rawCmd.contains("{{") && rawCmd.contains("}}")) {
            templateToFill = rawCmd
        } else {
            viewModel.executeCommand(rawCmd)
        }
    }

    LaunchedEffect(serverId, sessionId) {
        val server = deps.repository.getServerById(serverId)
        if (server != null) {
            viewModel.setServer(server, sessionId)
        }
    }

    // All servers, used for the session tab labels and the "+" picker.
    val allServers = remember { mutableStateListOf<Server>() }
    LaunchedEffect(Unit) {
        allServers.clear()
        allServers.addAll(deps.repository.getServers())
    }

    // Open sessions (from the shared store) + current, so the tab row stays in
    // sync when a session is closed on this screen. Sessions are keyed by a
    // unique session id so the same server can appear in several tabs.
    var sessionsVersion by remember { mutableIntStateOf(0) }
    val currentServerId = viewModel.currentServer?.id ?: -1
    val openSessions = remember(sessionsVersion, currentServerId) {
        val map = TerminalScreenStore.openSessions().toMutableMap()
        if (sessionId > 0 && !map.containsKey(sessionId)) map[sessionId] = currentServerId
        map
    }

    // UI Optimization for Terminal
    val density = LocalDensity.current
    val isKeyboardVisible = WindowInsets.ime.getBottom(density) > 0
    val hideTopUI = (isLandscape || isKeyboardVisible) && selectedTab == 0

    // Handle back press to show confirmation
    PlatformBackHandler(enabled = true) {
        showExitConfirmation = true
    }

    Scaffold(
        topBar = {
            if (!hideTopUI) {
                TopAppBar(
                    title = { /* Название сервера удалено по просьбе пользователя */ },
                    navigationIcon = {
                        IconButton(onClick = { showExitConfirmation = true }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStrings.back)
                        }
                    },
                    actions = {
                        // Login switcher
                        var loginMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            TextButton(onClick = { loginMenuExpanded = true }) {
                                Text(
                                    viewModel.selectedLogin?.label ?: viewModel.currentServer?.username ?: "",
                                    maxLines = 1,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = AppStrings.selectLogin,
                                    modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = loginMenuExpanded, 
                                onDismissRequest = { loginMenuExpanded = false },
                                modifier = Modifier.background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                            ) {
                                Text(
                                    AppStrings.selectLogin,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Bold
                                )
                                HorizontalDivider(modifier = Modifier.padding(bottom = 4.dp))
                                
                                val isMainSelected = viewModel.selectedLogin == null
                                DropdownMenuItem(
                                    text = { Text(String.format(AppStrings.mainLoginLabel, viewModel.currentServer?.username ?: ""), fontWeight = if (isMainSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = { 
                                        Icon(
                                            Icons.Default.Person, 
                                            null, 
                                            tint = if (isMainSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        ) 
                                    },
                                    onClick = {
                                        viewModel.selectLogin(null)
                                        loginMenuExpanded = false
                                    },
                                    colors = if (isMainSelected) MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.primary) else MenuDefaults.itemColors()
                                )
                                
                                viewModel.logins.forEach { login ->
                                    val isSelected = login.id == viewModel.selectedLogin?.id
                                    DropdownMenuItem(
                                        text = { Text(login.label.ifBlank { login.username }, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        leadingIcon = { 
                                            Icon(
                                                Icons.Default.Person, 
                                                null, 
                                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                            ) 
                                        },
                                        trailingIcon = {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        onClick = {
                                            viewModel.selectLogin(login)
                                            loginMenuExpanded = false
                                        },
                                        colors = if (isSelected) MenuDefaults.itemColors(textColor = MaterialTheme.colorScheme.primary) else MenuDefaults.itemColors()
                                    )
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                
                                DropdownMenuItem(
                                    text = { Text(AppStrings.manageLogins) },
                                    leadingIcon = { Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        loginMenuExpanded = false
                                        onManageLogins()
                                    }
                                )
                            }
                        }
                        IconButton(onClick = { quickCommandsVisible = !quickCommandsVisible }) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = AppStrings.quickCommands,
                                tint = if (quickCommandsVisible) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = onNavigateToSftp) {
                            Icon(Icons.Default.Folder, contentDescription = AppStrings.sftpExplorer)
                        }
                        IconButton(onClick = onManageCommands) {
                            Icon(Icons.Default.Build, contentDescription = AppStrings.manageCommands)
                        }
                        IconButton(onClick = { showSaveWorkspaceDialog = true }) {
                            Icon(Icons.Default.Save, contentDescription = "Save Workspace")
                        }
                    }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
            if (!hideTopUI) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text(
                            "Terminal",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text(
                            "Monitoring",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (selectedTab == 1) {
                MonitoringDashboard(viewModel)
            } else {
                if (!hideTopUI) {
                    // Session tabs: one tab per open session, "+" to add, "×" to close.
                    SessionTabRow(
                        openSessions = openSessions,
                        allServers = allServers,
                        currentSessionId = sessionId,
                        onSelect = { sid ->
                            if (sid != sessionId) onSwitchSession(sid)
                        },
                        onClose = { sid ->
                            viewModel.closeSession(sid)
                            if (sid == sessionId) onCloseSession(sid) else sessionsVersion++
                        },
                        onAddServer = { id ->
                            onAddSession(id)
                        }
                    )
                }
                
                // Quick & Base Commands Row (Only show if not collapsed by keyboard)
                if (quickCommandsVisible && !isKeyboardVisible) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(baseCommands) { (cmd, label) ->
                            OutlinedButton(
                                onClick = { handleExecute(cmd) },
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }
                        items(viewModel.customCommands) { cmd ->
                            Button(
                                onClick = {
                                    if (cmd.isDangerous) {
                                        if (rebootConfirmMode == "never") handleExecute(cmd.command)
                                        else { pendingCommand = cmd.command; isPendingDangerous = true; showConfirmDialog = true }
                                    } else {
                                        handleExecute(cmd.command)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorFromHex(cmd.colorHex, MaterialTheme.colorScheme.primary)
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(cmd.name, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                val terminalRevision by viewModel.terminalRevision.collectAsState()
                val parsedOutput = remember(terminalRevision, termTextColor) {
                    viewModel.terminalScreen.render(
                        colorFromHex(termTextColor, Color(0xFF00FF00))
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    // Terminal View
                    val scrollState = rememberScrollState()
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(colorFromHex(termBgColor, Color.Black))
                            // Tap the console to open the device keyboard.
                            .pointerInput(Unit) {
                                detectTapGestures {
                                    terminalFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, _, zoom, _ ->
                                    if (zoom != 1f) {
                                        viewModel.updateTerminalFontSize(termFontSizePx * zoom)
                                    }
                                }
                            }
                    ) {
                        SelectionContainer {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = parsedOutput,
                                    fontFamily = consoleFontFamily,
                                    fontSize = termFontSizePx.sp,
                                    modifier = Modifier.fillMaxWidth(),
                                    lineHeight = (termFontSizePx * 1.2f).sp,
                                    color = colorFromHex(termTextColor, Color.Unspecified)
                                )
                            }
                        }

                        BasicTextField(
                            value = terminalInputBuffer,
                            onValueChange = { newValue ->
                                val old = terminalInputBuffer
                                terminalInputBuffer = newValue
                                if (newValue.length > old.length) {
                                    val addedText = newValue.substring(old.length)
                                    // Send batch instead of char-by-char for performance and reliability
                                    if (addedText.length > 1) {
                                        viewModel.sendInput(addedText.replace("\n", "\r"))
                                    } else {
                                        val ch = addedText[0]
                                        if (ch == '\n') viewModel.sendEnter() else viewModel.sendInput(ch.toString())
                                    }
                                } else if (newValue.length < old.length) {
                                    repeat(old.length - newValue.length) { viewModel.sendBackspace() }
                                }
                            },
                            modifier = Modifier
                                .size(1.dp)
                                .alpha(0f)
                                .focusRequester(terminalFocusRequester)
                                .onPreviewKeyEvent { event ->
                                    if (event.type == KeyEventType.KeyDown) {
                                        when {
                                            event.key == Key.Enter -> { viewModel.sendEnter(); true }
                                            event.key == Key.Backspace -> { viewModel.sendBackspace(); true }
                                            event.key == Key.Tab -> { viewModel.sendInput("\t"); true }
                                            event.key == Key.Escape -> { viewModel.sendEscape(); true }
                                            event.key == Key.DirectionUp -> { viewModel.sendArrowUp(); true }
                                            event.key == Key.DirectionDown -> { viewModel.sendArrowDown(); true }
                                            event.key == Key.DirectionLeft -> { viewModel.sendArrowLeft(); true }
                                            event.key == Key.DirectionRight -> { viewModel.sendArrowRight(); true }
                                            else -> false
                                        }
                                    } else false
                                },
                            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
                            cursorBrush = SolidColor(Color.Transparent),
                            singleLine = false,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                        )

                        LaunchedEffect(terminalRevision) {
                            if (!viewModel.terminalScreen.isFullScreen) {
                                scrollState.scrollTo(scrollState.maxValue)
                            }
                        }

                        val loading by viewModel.isLoading.collectAsState()
                        if (loading) {
                            LinearProgressIndicator(
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                color = colorFromHex(termTextColor, Color.Green),
                                trackColor = Color.Transparent
                            )
                        }

                        // Floating Back Button when header is hidden
                        if (hideTopUI) {
                            IconButton(
                                onClick = { showExitConfirmation = true },
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = AppStrings.back,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Landscape Sidebar
                    if (isLandscape) {
                        TerminalSidebar(viewModel)
                    }
                }

                // Bottom Controls (Only in Portrait or non-landscape)
                if (!isLandscape) {
                    val isCompact = isKeyboardVisible
                    if (isCompact) {
                        // Merged Row for Keyboard Focus
                        CompactControlRow(viewModel)
                    } else {
                        // Standard Two-Row Layout
                        Column {
                            // Console Utilities Toolbar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row {
                                    IconButton(
                                        onClick = {
                                            customCommandInput = viewModel.navigateHistory(true, customCommandInput)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, AppStrings.historyUp)
                                    }
                                    IconButton(
                                        onClick = {
                                            customCommandInput = viewModel.navigateHistory(false, customCommandInput)
                                        },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, AppStrings.historyDown)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    TextButton(
                                        onClick = { viewModel.sendInput("\t") },
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(AppStrings.tabKey, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                                    TextButton(
                                        onClick = { viewModel.sendCtrlC() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(AppStrings.ctrlCKey, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp))
                                    TextButton(
                                        onClick = { viewModel.clearTerminal() },
                                        contentPadding = PaddingValues(horizontal = 12.dp)
                                    ) {
                                        Text(AppStrings.clearKey, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }

                            // Control Keys Bar
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                item { ControlKeyButton(AppStrings.escKey, { viewModel.sendEscape() }, false) }
                                item {
                                    HoldableKeyButton(
                                        onClick = { viewModel.sendBackspace() },
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text("⌫", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                                item { ControlKeyButton(AppStrings.enterKey, { viewModel.sendEnter() }, false) }
                                item {
                                    HoldableKeyButton(
                                        onClick = { viewModel.sendArrowLeft() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
                                    }
                                }
                                item {
                                    HoldableKeyButton(
                                        onClick = { viewModel.sendArrowUp() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                                    }
                                }
                                item {
                                    HoldableKeyButton(
                                        onClick = { viewModel.sendArrowDown() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    }
                                }
                                item {
                                    HoldableKeyButton(
                                        onClick = { viewModel.sendArrowRight() },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
                                    }
                                }
                                item { VerticalDivider(modifier = Modifier.height(24.dp)) }
                                item { ControlKeyButton("Ctrl+X", { viewModel.sendCtrlKey('x') }, false) }
                                item { ControlKeyButton("Ctrl+O", { viewModel.sendCtrlKey('o') }, false) }
                                item { ControlKeyButton("Ctrl+G", { viewModel.sendCtrlKey('g') }, false) }
                                item { ControlKeyButton("Ctrl+W", { viewModel.sendCtrlKey('w') }, false) }
                                item { ControlKeyButton("Ctrl+K", { viewModel.sendCtrlKey('k') }, false) }
                                item { ControlKeyButton("Ctrl+U", { viewModel.sendCtrlKey('u') }, false) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(AppStrings.confirmExecution) },
            text = { Text(String.format(AppStrings.executeConfirmMsg, pendingCommand ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    val cmdToRun = pendingCommand ?: return@TextButton
                    val biometric = deps.biometric
                    if (isPendingDangerous && biometric != null && biometric.canAuthenticate()) {
                        biometric.showPrompt(
                            title = AppStrings.confirmExecution,
                            subtitle = String.format(AppStrings.executeConfirmMsg, cmdToRun),
                            negativeButtonText = AppStrings.cancel,
                            onSuccess = { handleExecute(cmdToRun) },
                            onError = {}
                        )
                    } else {
                        handleExecute(cmdToRun)
                    }
                }) {
                    Text(AppStrings.execute, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(AppStrings.cancel)
                }
            }
        )
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(AppStrings.confirmExit) },
            text = { Text(AppStrings.exitSshSessionMsg) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    onNavigateBack()
                }) {
                    Text(AppStrings.exit, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(AppStrings.cancel)
                }
            }
        )
    }

    templateToFill?.let { template ->
        FillTemplateDialog(
            template = template,
            onConfirm = { filled ->
                viewModel.executeCommand(filled)
                templateToFill = null
            },
            onDismiss = { templateToFill = null }
        )
    }

    if (showSaveWorkspaceDialog) {
        SaveWorkspaceDialog(
            onSave = { name, color ->
                scope.launch {
                    val openSessions = TerminalScreenStore.openSessions()
                    val items = openSessions.map { (sid, serverId) ->
                        val bundle = com.neytron.sshcommander.data.SessionManager.getBundle(sid)
                        com.neytron.sshcommander.data.WorkspaceItem(
                            serverId = serverId,
                            loginId = bundle?.lastLoginId,
                            type = com.neytron.sshcommander.data.WorkspaceItemType.TERMINAL,
                            initialPath = bundle?.sftp?.currentPath?.value,
                            lastCommand = bundle?.terminal?.lastCommand?.value
                        )
                    }
                    deps.repository.insertWorkspace(com.neytron.sshcommander.data.Workspace(name = name, colorHex = color, items = items))
                    showSaveWorkspaceDialog = false
                    platformToast("Workspace saved!")
                }
            },
            onDismiss = { showSaveWorkspaceDialog = false }
        )
    }
}

@Composable
fun SaveWorkspaceDialog(
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
                                .clip(androidx.compose.foundation.shape.CircleShape)
                                .background(colorFromHex(hex, Color.Gray))
                                .border(if (selected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, androidx.compose.foundation.shape.CircleShape)
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

@Composable
fun FillTemplateDialog(
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
private fun ControlKeyButton(
    label: String,
    onClick: () -> Unit,
    isLandscape: Boolean
) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = if (isLandscape) 4.dp else 10.dp, vertical = 0.dp)
    ) {
        Text(
            label,
            fontWeight = FontWeight.Bold,
            fontSize = if (isLandscape) 11.sp else 13.sp
        )
    }
}

@Composable
private fun HoldableKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    repeatDelayMs: Long = 400L,
    repeatIntervalMs: Long = 60L,
    content: @Composable () -> Unit
) {
    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    currentOnClick()
                    var repeatJob: Job? = null
                    try {
                        repeatJob = scope.launch {
                            delay(repeatDelayMs)
                            while (true) {
                                currentOnClick()
                                delay(repeatIntervalMs)
                            }
                        }
                        waitForUpOrCancellation()
                    } finally {
                        repeatJob?.cancel()
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

private fun colorFromHex(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            6 -> (0xFF000000L or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return fallback
        }
        Color(argb)
    } catch (e: Exception) {
        fallback
    }
}

@Composable
private fun SessionTabRow(
    openSessions: Map<Int, Int>,
    allServers: List<Server>,
    currentSessionId: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onAddServer: (Int) -> Unit,
    isCompact: Boolean = false
) {
    var addMenuExpanded by remember { mutableStateOf(false) }
    
    val backgroundColor = if (isCompact) Color.Transparent else MaterialTheme.colorScheme.surface
    val tabHeight = if (isCompact) 42.dp else 38.dp

    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = if (isCompact) 0.dp else 8.dp)
    ) {
        openSessions.entries.sortedBy { it.key }.forEach { (sid, serverId) ->
            val name = allServers.firstOrNull { it.id == serverId }?.name ?: "#$serverId"
            val selected = sid == currentSessionId
            
            Box(
                modifier = Modifier
                    .width(if (isCompact) 110.dp else 130.dp)
                    .height(tabHeight)
                    .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                    .background(
                        if (selected) {
                            if (isCompact) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        } else Color.Transparent
                    )
                    .clickable { onSelect(sid) }
            ) {
                Column {
                    if (!isCompact) {
                        // Tiny top indicator
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        )
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = if (isCompact) 8.dp else 10.dp, end = 2.dp)
                    ) {
                        Text(
                            name,
                            style = if (isCompact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onClose(sid) },
                            modifier = Modifier.size(if (isCompact) 20.dp else 24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = AppStrings.closeSession,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (selected) 1f else 0.4f),
                                modifier = Modifier.size(if (isCompact) 12.dp else 14.dp)
                            )
                        }
                    }
                }
            }
            
            if (!selected) {
                VerticalDivider(
                    modifier = Modifier.height(16.dp).align(Alignment.CenterVertically).padding(horizontal = 2.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }

        // Plus button
        Box {
            IconButton(
                onClick = { addMenuExpanded = true },
                modifier = Modifier.size(tabHeight).padding(bottom = if (isCompact) 0.dp else 2.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = AppStrings.addSession,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(if (isCompact) 16.dp else 18.dp)
                )
            }
            DropdownMenu(expanded = addMenuExpanded, onDismissRequest = { addMenuExpanded = false }) {
                allServers.forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.name) },
                        onClick = {
                            addMenuExpanded = false
                            onAddServer(s.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TerminalSidebar(viewModel: SshViewModel) {
    Column(
        modifier = Modifier
            .width(54.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Essential terminal keys in vertical layout
        ControlKeyButton(AppStrings.escKey, { viewModel.sendEscape() }, true)
        HoldableKeyButton(onClick = { viewModel.sendBackspace() }, modifier = Modifier.size(40.dp)) {
            Text("⌫", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        ControlKeyButton(AppStrings.tabKey, { viewModel.sendInput("\t") }, true)
        ControlKeyButton(AppStrings.enterKey, { viewModel.sendEnter() }, true)
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
        
        HoldableKeyButton(onClick = { viewModel.sendArrowUp() }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.KeyboardArrowUp, null)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            HoldableKeyButton(onClick = { viewModel.sendArrowLeft() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
            HoldableKeyButton(onClick = { viewModel.sendArrowRight() }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.KeyboardArrowRight, null)
            }
        }
        HoldableKeyButton(onClick = { viewModel.sendArrowDown() }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
        
        HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))

        ControlKeyButton("Ctrl+C", { viewModel.sendCtrlC() }, true)
        ControlKeyButton("Ctrl+X", { viewModel.sendCtrlKey('x') }, true)
        ControlKeyButton("Ctrl+O", { viewModel.sendCtrlKey('o') }, true)
        ControlKeyButton("Ctrl+W", { viewModel.sendCtrlKey('w') }, true)
    }
}

@Composable
private fun CompactControlRow(viewModel: SshViewModel) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        contentPadding = PaddingValues(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        item { ControlKeyButton(AppStrings.escKey, { viewModel.sendEscape() }, true) }
        item { ControlKeyButton(AppStrings.tabKey, { viewModel.sendInput("\t") }, true) }
        item { ControlKeyButton("Ctrl+C", { viewModel.sendCtrlC() }, true) }
        item { VerticalDivider(modifier = Modifier.height(20.dp)) }
        item {
            HoldableKeyButton(onClick = { viewModel.sendArrowLeft() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowLeft, null, modifier = Modifier.size(20.dp))
            }
        }
        item {
            HoldableKeyButton(onClick = { viewModel.sendArrowUp() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowUp, null, modifier = Modifier.size(20.dp))
            }
        }
        item {
            HoldableKeyButton(onClick = { viewModel.sendArrowDown() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowDown, null, modifier = Modifier.size(20.dp))
            }
        }
        item {
            HoldableKeyButton(onClick = { viewModel.sendArrowRight() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.KeyboardArrowRight, null, modifier = Modifier.size(20.dp))
            }
        }
        item { VerticalDivider(modifier = Modifier.height(20.dp)) }
        item { ControlKeyButton("Ctrl+X", { viewModel.sendCtrlKey('x') }, true) }
        item { ControlKeyButton("Ctrl+O", { viewModel.sendCtrlKey('o') }, true) }
        item { ControlKeyButton("Ctrl+G", { viewModel.sendCtrlKey('g') }, true) }
        item { ControlKeyButton("Ctrl+W", { viewModel.sendCtrlKey('w') }, true) }
        item { ControlKeyButton("⌫", { viewModel.sendBackspace() }, true) }
        item { ControlKeyButton("ENTER", { viewModel.sendEnter() }, true) }
    }
}

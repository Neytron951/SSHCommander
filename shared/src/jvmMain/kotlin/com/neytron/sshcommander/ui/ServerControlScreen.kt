package com.neytron.sshcommander.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.Server
import com.neytron.sshcommander.data.TerminalScreenStore
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

    // In landscape the vertical space is tight: the quick-command row is collapsed
    // by default (can be toggled via the top bar button), and the bottom panel
    // is rendered more compact.
    val isLandscape = isLandscapeLayout()
    var quickCommandsVisible by remember { mutableStateOf(!isLandscape) }

    var customCommandInput by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var isPendingDangerous by remember { mutableStateOf(false) }

    var showExitConfirmation by remember { mutableStateOf(false) }

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
    val privacyMode by deps.settings.privacyMode.collectAsState(initial = false)

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

    // Handle back press to show confirmation
    PlatformBackHandler(enabled = true) {
        showExitConfirmation = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.currentServer?.name ?: AppStrings.loading, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (privacyMode) PrivacyUtils.maskHost(viewModel.currentServer?.host ?: "")
                            else viewModel.currentServer?.host ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
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
                                style = MaterialTheme.typography.labelMedium
                            )
                            Icon(
                                Icons.Default.KeyboardArrowDown,
                                contentDescription = AppStrings.selectLogin,
                                modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                            )
                        }
                        DropdownMenu(expanded = loginMenuExpanded, onDismissRequest = { loginMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(String.format(AppStrings.mainLoginLabel, viewModel.currentServer?.username ?: "")) },
                                onClick = {
                                    viewModel.selectLogin(null)
                                    loginMenuExpanded = false
                                }
                            )
                            viewModel.logins.forEach { login ->
                                DropdownMenuItem(
                                    text = { Text(login.label.ifBlank { login.username }) },
                                    trailingIcon = {
                                        if (login.id == viewModel.selectedLogin?.id) {
                                            Icon(Icons.Default.Apps, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    },
                                    onClick = {
                                        viewModel.selectLogin(login)
                                        loginMenuExpanded = false
                                    }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(AppStrings.manageLogins) },
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
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .imePadding()
        ) {
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
                    // "+" always starts a brand-new session, even if one already
                    // exists for the chosen server.
                    onAddSession(id)
                }
            )
            // Quick & Base Commands Row (collapsed by default in landscape,
            // can be toggled from the top bar)
            if (quickCommandsVisible) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    // Base Commands
                    items(baseCommands) { (cmd, label) ->
                        OutlinedButton(
                            onClick = { viewModel.executeCommand(cmd) },
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Vertical Divider in LazyRow
                    item { VerticalDivider(modifier = Modifier.height(24.dp).padding(horizontal = 4.dp)) }

                    // Custom User Commands
                    items(viewModel.customCommands) { cmd ->
                        Button(
                            onClick = {
                                if (cmd.isDangerous) {
                                    if (rebootConfirmMode == "never") viewModel.executeCommand(cmd.command)
                                    else { pendingCommand = cmd.command; isPendingDangerous = true; showConfirmDialog = true }
                                } else {
                                    viewModel.executeCommand(cmd.command)
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

            // Terminal View
            val scrollState = rememberScrollState()

            // Render the emulator screen buffer. terminalRevision bumps on
            // every feed so the Compose state read here recomposes correctly.
            val terminalRevision = viewModel.terminalRevision
            val parsedOutput = remember(terminalRevision, termTextColor) {
                viewModel.terminalScreen.render(
                    colorFromHex(termTextColor, Color(0xFF00FF00))
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                            // Ensure the Text composable itself doesn't force white
                            color = colorFromHex(termTextColor, Color.Unspecified)
                        )
                    }
                }

                // Hidden text field that captures soft/hardware keyboard input.
                // It is invisible (1dp, alpha 0) but holds the IME connection.
                // Characters are forwarded straight into the SSH channel.
                BasicTextField(
                    value = terminalInputBuffer,
                    onValueChange = { newValue ->
                        val old = terminalInputBuffer
                        terminalInputBuffer = newValue
                        if (newValue.length > old.length) {
                            // Appended characters (IME commits them here).
                            newValue.substring(old.length).forEach { ch ->
                                if (ch == '\n') {
                                    viewModel.sendEnter()
                                } else {
                                    viewModel.sendInput(ch.toString())
                                }
                            }
                        } else if (newValue.length < old.length) {
                            // Character(s) removed by soft-keyboard backspace.
                            repeat(old.length - newValue.length) { viewModel.sendBackspace() }
                        }
                    },
                    modifier = Modifier
                        .size(1.dp)
                        .alpha(0f)
                        .focusRequester(terminalFocusRequester)
                        .onPreviewKeyEvent { event ->
                            // Hardware keyboards: map special keys directly.
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

                LaunchedEffect(viewModel.terminalRevision) {
                    // Full-screen apps (nano/vim/htop) redraw the whole screen
                    // on each keystroke; jumping to the bottom fights their
                    // display. Only auto-scroll plain shell output.
                    if (!viewModel.terminalScreen.isFullScreen) {
                        scrollState.scrollTo(scrollState.maxValue)
                    }
                }

                if (viewModel.isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = colorFromHex(termTextColor, Color.Green),
                        trackColor = Color.Transparent
                    )
                }
            }

            // Console Utilities Toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = if (isLandscape) 2.dp else 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row {
                    IconButton(
                        onClick = {
                            customCommandInput = viewModel.navigateHistory(true, customCommandInput)
                        },
                        modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, AppStrings.historyUp, modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                    IconButton(
                        onClick = {
                            customCommandInput = viewModel.navigateHistory(false, customCommandInput)
                        },
                        modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, AppStrings.historyDown, modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = { viewModel.sendInput("\t") },
                        contentPadding = PaddingValues(horizontal = if (isLandscape) 6.dp else 12.dp)
                    ) {
                        Text(AppStrings.tabKey, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp)
                    }
                    VerticalDivider(modifier = Modifier.height(if (isLandscape) 20.dp else 24.dp).padding(horizontal = 4.dp))
                    TextButton(
                        onClick = { viewModel.sendCtrlC() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = if (isLandscape) 6.dp else 12.dp)
                    ) {
                        Text(AppStrings.ctrlCKey, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp)
                    }
                    VerticalDivider(modifier = Modifier.height(if (isLandscape) 20.dp else 24.dp).padding(horizontal = 4.dp))
                    TextButton(
                        onClick = { viewModel.clearTerminal() },
                        contentPadding = PaddingValues(horizontal = if (isLandscape) 6.dp else 12.dp)
                    ) {
                        Text(AppStrings.clearKey, fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp)
                    }
                }
            }

            // Control Keys Bar — for interactive programs (nano/vim/htop).
            // Scrollable horizontally so it works in both orientations.
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 2.dp else 4.dp),
                contentPadding = PaddingValues(horizontal = if (isLandscape) 4.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item { ControlKeyButton(AppStrings.escKey, { viewModel.sendEscape() }, isLandscape) }
                item {
                    HoldableKeyButton(
                        onClick = { viewModel.sendBackspace() },
                        modifier = Modifier.height(if (isLandscape) 32.dp else 40.dp)
                    ) {
                        Text("⌫", fontWeight = FontWeight.Bold, fontSize = if (isLandscape) 11.sp else 13.sp)
                    }
                }
                item { ControlKeyButton(AppStrings.enterKey, { viewModel.sendEnter() }, isLandscape) }
                item {
                    HoldableKeyButton(
                        onClick = { viewModel.sendArrowLeft() },
                        modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                }
                item {
                    HoldableKeyButton(
                        onClick = { viewModel.sendArrowUp() },
                        modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null, modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                }
                item {
                    HoldableKeyButton(
                        onClick = { viewModel.sendArrowDown() },
                        modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                }
                item {
                    HoldableKeyButton(
                        onClick = { viewModel.sendArrowRight() },
                        modifier = Modifier.size(if (isLandscape) 32.dp else 40.dp)
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                }
                item { VerticalDivider(modifier = Modifier.height(if (isLandscape) 18.dp else 24.dp)) }
                item { ControlKeyButton("Ctrl+X", { viewModel.sendCtrlKey('x') }, isLandscape) }
                item { ControlKeyButton("Ctrl+O", { viewModel.sendCtrlKey('o') }, isLandscape) }
                item { ControlKeyButton("Ctrl+G", { viewModel.sendCtrlKey('g') }, isLandscape) }
                item { ControlKeyButton("Ctrl+W", { viewModel.sendCtrlKey('w') }, isLandscape) }
                item { ControlKeyButton("Ctrl+K", { viewModel.sendCtrlKey('k') }, isLandscape) }
                item { ControlKeyButton("Ctrl+U", { viewModel.sendCtrlKey('u') }, isLandscape) }
                item { ControlKeyButton("Ctrl+A", { viewModel.sendCtrlKey('a') }, isLandscape) }
                item { ControlKeyButton("Ctrl+E", { viewModel.sendCtrlKey('e') }, isLandscape) }
                item { ControlKeyButton("Ctrl+D", { viewModel.sendCtrlKey('d') }, isLandscape) }
                item { ControlKeyButton("Ctrl+Z", { viewModel.sendCtrlKey('z') }, isLandscape) }
            }

            // Input Section
            // Removed: the Command... text field and Send button. Keyboard input
            // is captured directly by the hidden field inside the terminal view,
            // so the device keyboard feeds characters straight into the shell.
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
                            onSuccess = { viewModel.executeCommand(cmdToRun) },
                            onError = {}
                        )
                    } else {
                        viewModel.executeCommand(cmdToRun)
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

/**
 * A button that fires [onClick] on tap, and keeps firing it on a timer while
 * the finger is held down (like holding a key on a real keyboard). Used for
 * Backspace and arrow keys so erasing/moving is not one-character-per-tap.
 *
 * Implemented as a plain Box + pointerInput (NOT a Button/IconButton) so there
 * is no competing built-in clickable/ripple that consumes the down event
 * before our gesture handler sees it.
 */
@Composable
private fun HoldableKeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    repeatDelayMs: Long = 400L,
    repeatIntervalMs: Long = 60L,
    content: @Composable () -> Unit
) {
    // Keep a stable reference to the latest onClick so the pointerInput block
    // (keyed on Unit) never restarts mid-gesture on recomposition.
    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    // Fire once immediately, then repeat while held.
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
                        // Suspends until the finger lifts (or the gesture is cancelled).
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

/**
 * Parses an "#RRGGBB" or "#AARRGGBB" hex color string into a [Color],
 * falling back to [fallback] when the string is malformed.
 */
private fun colorFromHex(hex: String, fallback: Color): Color {
    return try {
        val clean = hex.removePrefix("#")
        val argb = when (clean.length) {
            6 -> (0xFF000000L or clean.toLong(16)).toInt()
            8 -> clean.toLong(16).toInt()
            else -> return fallback
        }
        // Color(Int) is the ARGB constructor; Color(ULong) would interpret the
        // value as a packed color (low 6 bits = color space id) and crash.
        Color(argb)
    } catch (e: Exception) {
        fallback
    }
}

/**
 * Horizontal strip of open sessions (one chip per session). Tap to switch,
 * "×" to close, "+" to open a fresh session for a server.
 */
@Composable
private fun SessionTabRow(
    openSessions: Map<Int, Int>,
    allServers: List<Server>,
    currentSessionId: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onAddServer: (Int) -> Unit
) {
    var addMenuExpanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 6.dp)
        ) {
            openSessions.entries.sortedBy { it.key }.forEach { (sid, serverId) ->
                val name = allServers.firstOrNull { it.id == serverId }?.name ?: "#$serverId"
                val selected = sid == currentSessionId
                Surface(
                    onClick = { onSelect(sid) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .height(42.dp)
                            .width(150.dp)
                            .padding(start = 12.dp, end = 4.dp)
                    ) {
                        Text(
                            name,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onClose(sid) }, modifier = Modifier.size(26.dp)) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = AppStrings.closeSession,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.width(6.dp))
            }
            Box {
                Surface(
                    onClick = { addMenuExpanded = true },
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .height(42.dp)
                            .width(44.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = AppStrings.addSession,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
}

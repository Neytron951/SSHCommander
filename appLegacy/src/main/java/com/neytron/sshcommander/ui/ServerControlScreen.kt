package com.neytron.sshcommander.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.data.TerminalDimensions
import com.neytron.sshcommander.security.BiometricUtils
import com.neytron.sshcommander.ui.MonitoringDashboard
import com.neytron.sshcommander.ui.PrivacyUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerControlScreen(
    serverId: Int,
    activity: FragmentActivity,
    viewModel: SshViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onManageCommands: () -> Unit,
    onManageLogins: () -> Unit,
    onNavigateToSftp: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ServerRepository(context) }
    val settingsManager = remember { SettingsManager(context) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Terminal, 1: Monitoring

    // In landscape the vertical space is tight: the quick-command row is collapsed
    // by default (can be toggled via the top bar button), and the bottom panel
    // is rendered more compact.
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var quickCommandsVisible by remember { mutableStateOf(!isLandscape) }
    
    var customCommandInput by remember { mutableStateOf("") }
    var templateToFill by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var isPendingDangerous by remember { mutableStateOf(false) }
    
    fun handleExecute(rawCmd: String) {
        if (rawCmd.contains("{{") && rawCmd.contains("}}")) {
            templateToFill = rawCmd
        } else {
            viewModel.executeCommand(rawCmd)
        }
    }
    
    var showExitConfirmation by remember { mutableStateOf(false) }
    
    val fontFamilyStr by settingsManager.fontFamily.collectAsState(initial = "monospace")
    val rebootConfirmMode by settingsManager.rebootConfirm.collectAsState(initial = "always")
    
    val termBgColor by viewModel.termBgColor.collectAsState()
    val termTextColor by viewModel.termTextColor.collectAsState()
    val termFontSizePx by viewModel.termFontSizePx.collectAsState()
    val privacyMode by settingsManager.privacyMode.collectAsState(initial = false)

    val consoleFontFamily = when(fontFamilyStr) {
        "monospace" -> FontFamily.Monospace
        "sans_serif" -> FontFamily.SansSerif
        "serif" -> FontFamily.Serif
        else -> FontFamily.Default
    }

    // List of useful base commands
    val baseCommands = listOf(
        "ls -la" to stringResource(R.string.cmd_list),
        "top" to stringResource(R.string.cmd_top),
        "df -h" to stringResource(R.string.cmd_disk),
        "free -m" to stringResource(R.string.cmd_ram),
        "uptime" to stringResource(R.string.cmd_uptime),
        "ps aux" to stringResource(R.string.cmd_processes),
        "dmesg" to stringResource(R.string.cmd_logs)
    )

    LaunchedEffect(serverId) {
        val server = repository.getServerById(serverId)
        if (server != null) {
            viewModel.setServer(server)
        }
    }

    // Handle back press to show confirmation
    BackHandler {
        showExitConfirmation = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(viewModel.currentServer?.name ?: stringResource(R.string.loading), style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (privacyMode) PrivacyUtils.maskHost(viewModel.currentServer?.host ?: "")
                            else viewModel.currentServer?.host ?: "",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitConfirmation = true }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
                                contentDescription = stringResource(R.string.select_login),
                                modifier = Modifier.size(if (isLandscape) 16.dp else 18.dp)
                            )
                        }
                        DropdownMenu(expanded = loginMenuExpanded, onDismissRequest = { loginMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.main_login_label, viewModel.currentServer?.username ?: "")) },
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
                                text = { Text(stringResource(R.string.manage_logins)) },
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
                            contentDescription = stringResource(R.string.quick_commands),
                            tint = if (quickCommandsVisible) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = onNavigateToSftp) {
                        Icon(Icons.Default.Folder, contentDescription = stringResource(R.string.sftp_explorer))
                    }
                    IconButton(onClick = onManageCommands) {
                        Icon(Icons.Default.Build, contentDescription = stringResource(R.string.manage_commands))
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                    Text("Terminal", modifier = Modifier.padding(12.dp))
                }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                    Text("Monitoring", modifier = Modifier.padding(12.dp))
                }
            }

            if (selectedTab == 1) {
                MonitoringDashboard(viewModel)
            } else {
                // QUICK COMMANDS
                if (quickCommandsVisible) {
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
                                    containerColor = try { Color(cmd.colorHex.toColorInt()) } catch(e: Exception) { MaterialTheme.colorScheme.primary }
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
                val terminalRevision by viewModel.terminalRevision.collectAsState()
                val parsedOutput = remember(terminalRevision, termTextColor) {
                    viewModel.terminalScreen.render(
                        try { Color(termTextColor.toColorInt()) } catch(e: Exception) { Color(0xFF00FF00) }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(try { Color(termBgColor.toColorInt()) } catch(e: Exception) { Color.Black })
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
                                color = try { Color(termTextColor.toColorInt()) } catch(e: Exception) { Color.Unspecified }
                            )
                        }
                    }

                    LaunchedEffect(terminalRevision) {
                        if (!viewModel.terminalScreen.isFullScreen) {
                            scrollState.scrollTo(scrollState.maxValue)
                        }
                    }

                    val loading by viewModel.isLoading.collectAsState()
                    if (loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = try { Color(termTextColor.toColorInt()) } catch(e: Exception) { Color.Green },
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
                            onClick = { customCommandInput = viewModel.navigateHistory(true, customCommandInput) },
                            modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, stringResource(R.string.history_up), modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                        }
                        IconButton(
                            onClick = { customCommandInput = viewModel.navigateHistory(false, customCommandInput) },
                            modifier = Modifier.size(if (isLandscape) 36.dp else 48.dp)
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, stringResource(R.string.history_down), modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                        }
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { viewModel.sendInput("\t") },
                            contentPadding = PaddingValues(horizontal = if (isLandscape) 6.dp else 12.dp)
                        ) {
                            Text(stringResource(R.string.tab_key), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp)
                        }
                        VerticalDivider(modifier = Modifier.height(if (isLandscape) 20.dp else 24.dp).padding(horizontal = 4.dp))
                        TextButton(
                            onClick = { viewModel.sendCtrlC() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = if (isLandscape) 6.dp else 12.dp)
                        ) {
                            Text(stringResource(R.string.ctrl_c_key), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp)
                        }
                        VerticalDivider(modifier = Modifier.height(if (isLandscape) 20.dp else 24.dp).padding(horizontal = 4.dp))
                        TextButton(
                            onClick = { viewModel.clearTerminal() },
                            contentPadding = PaddingValues(horizontal = if (isLandscape) 6.dp else 12.dp)
                        ) {
                            Text(stringResource(R.string.clear_key), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = if (isLandscape) 12.sp else 14.sp)
                        }
                    }
                }

                // Control Keys Bar
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(vertical = if (isLandscape) 2.dp else 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (isLandscape) 2.dp else 4.dp),
                    contentPadding = PaddingValues(horizontal = if (isLandscape) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item { ControlKeyButton(stringResource(R.string.esc_key), { viewModel.sendEscape() }, isLandscape) }
                    item {
                        HoldableKeyButton(
                            onClick = { viewModel.sendBackspace() },
                            modifier = Modifier.height(if (isLandscape) 32.dp else 40.dp)
                        ) {
                            Text("⌫", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = if (isLandscape) 11.sp else 13.sp)
                        }
                    }
                    item { ControlKeyButton(stringResource(R.string.enter_key), { viewModel.sendEnter() }, isLandscape) }
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
                }

                // Input Section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(if (isLandscape) 4.dp else 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customCommandInput,
                        onValueChange = { 
                            customCommandInput = it
                            viewModel.onInputChanged(it)
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.command_placeholder), fontSize = if (isLandscape) 13.sp else 14.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontFamily = consoleFontFamily, fontSize = if (isLandscape) 13.sp else 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (customCommandInput.isNotBlank()) {
                                viewModel.executeCommand(customCommandInput)
                                customCommandInput = ""
                            }
                        }),
                        shape = MaterialTheme.shapes.medium
                    )
                    Spacer(Modifier.width(if (isLandscape) 4.dp else 8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (customCommandInput.isNotBlank()) {
                                viewModel.executeCommand(customCommandInput)
                                customCommandInput = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(if (isLandscape) 40.dp else 56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.run), modifier = Modifier.size(if (isLandscape) 20.dp else 24.dp))
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(stringResource(R.string.confirm_execution)) },
            text = { Text(stringResource(R.string.execute_confirm_msg, pendingCommand ?: "")) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    val cmdToRun = pendingCommand ?: return@TextButton
                    if (isPendingDangerous && BiometricUtils.canAuthenticate(activity)) {
                        BiometricUtils.showBiometricPrompt(activity, { viewModel.executeCommand(cmdToRun) }, {})
                    } else {
                        viewModel.executeCommand(cmdToRun)
                    }
                }) {
                    Text(stringResource(R.string.execute), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text(stringResource(R.string.confirm_exit)) },
            text = { Text(stringResource(R.string.exit_ssh_session_msg)) },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirmation = false
                    onNavigateBack()
                }) {
                    Text(stringResource(R.string.exit), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
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
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
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

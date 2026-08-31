package com.neytron.sshcommander.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.neytron.sshcommander.ui.theme.SSHCommanderTheme
import com.neytron.sshcommander.data.ExportImportManager
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onWidgetSettingsClick: () -> Unit,
    onSshKeysClick: () -> Unit,
    onAboutClick: () -> Unit,
    onScriptMarketClick: () -> Unit = {},
    appVersion: String = ""
) {
    val deps = LocalAppDeps.current
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(deps.repository, deps.settings) }

    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val privacyMode by viewModel.privacyMode.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val biometricLock by viewModel.biometricLock.collectAsState()

    val termBgColor by viewModel.termBgColor.collectAsState()
    val termTextColor by viewModel.termTextColor.collectAsState()
    val termThemeId by viewModel.termThemeId.collectAsState()
    val termFontFamily by viewModel.termFontFamily.collectAsState()

    var themeToPreview by remember { mutableStateOf<TerminalTheme?>(null) }

    val scope = rememberCoroutineScope()

    // Keep the runtime string catalog in sync with the persisted setting.
    LaunchedEffect(language) { AppStrings.language = language }

    var isTransitioning by remember { mutableStateOf(false) }

    val presetBgColors = listOf("#000000", "#1A1A1B", "#2D2D2D", "#FFFFFF", "#F5F5F5", "#002B36", "#073642")
    val presetTextColors = listOf("#00FF00", "#008000", "#FFFFFF", "#000000", "#FFD700", "#FFA500", "#FF0000", "#268BD2")

    val savePicker = rememberSavePicker { target ->
        if (target != null) {
            scope.launch {
                try {
                    val json = viewModel.exportData()
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
                    val text = f.openInput()?.let { readAllText(it) } ?: ""
                    viewModel.importData(text)
                    platformToast(AppStrings.importSuccess)
                } catch (e: Exception) {
                    platformToast(String.format(AppStrings.errorPrefix, e.message ?: ""))
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.settings, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!isTransitioning) {
                            isTransitioning = true
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStrings.back)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Theme
            SettingsSection(title = AppStrings.theme) {
                Column {
                    RadioButtonOption(AppStrings.themeLight, themeMode == "light", !isTransitioning) { viewModel.setThemeMode("light") }
                    RadioButtonOption(AppStrings.themeDark, themeMode == "dark", !isTransitioning) { viewModel.setThemeMode("dark") }
                    RadioButtonOption(AppStrings.themeSystem, themeMode == "system", !isTransitioning) { viewModel.setThemeMode("system") }
                }
            }

            // 2. Language
            SettingsSection(title = AppStrings.languageLabel) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButtonOption(AppStrings.langEn, AppStrings.language == "en", !isTransitioning) {
                        viewModel.setLanguage("en")
                    }
                    RadioButtonOption(AppStrings.langRu, AppStrings.language == "ru", !isTransitioning) {
                        viewModel.setLanguage("ru")
                    }
                }
            }

            // Terminal Customization
            SettingsSection(title = AppStrings.terminalStyle) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        if (AppStrings.language == "ru") "Тема терминала" else "Terminal Theme",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    ThemePresetSelector(
                        selectedThemeId = termThemeId,
                        enabled = !isTransitioning,
                        onThemeSelected = { themeId ->
                            val theme = TerminalThemes.presets.find { it.id == themeId }
                            if (theme != null && themeId != "custom") {
                                themeToPreview = theme
                            } else {
                                viewModel.setTermThemeId(themeId)
                            }
                        }
                    )

                    if (termThemeId == "custom") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(AppStrings.backgroundColor, style = MaterialTheme.typography.labelSmall)
                            ColorPickerRow(presetBgColors, termBgColor, !isTransitioning) { viewModel.setTermBgColor(it) }

                            Text(AppStrings.textColor, style = MaterialTheme.typography.labelSmall)
                            ColorPickerRow(presetTextColors, termTextColor, !isTransitioning) { viewModel.setTermTextColor(it) }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        if (AppStrings.language == "ru") "Шрифт терминала" else "Terminal Font",
                        style = MaterialTheme.typography.labelLarge
                    )
                    
                    FontSelector(
                        selectedFont = termFontFamily,
                        enabled = !isTransitioning,
                        onFontSelected = { viewModel.setTermFontFamily(it) }
                    )
                }
            }

            // Privacy Mode
            SettingsSection(title = AppStrings.privacyMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransitioning) { viewModel.setPrivacyMode(!privacyMode) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(AppStrings.privacyMode, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            AppStrings.privacyModeDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = privacyMode,
                        onCheckedChange = { viewModel.setPrivacyMode(it) },
                        enabled = !isTransitioning
                    )
                }
            }

            // Auto-reconnect
            SettingsSection(title = AppStrings.autoReconnect) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransitioning) { viewModel.setAutoReconnect(!autoReconnect) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(AppStrings.autoReconnect, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            AppStrings.autoReconnectDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoReconnect,
                        onCheckedChange = { viewModel.setAutoReconnect(it) },
                        enabled = !isTransitioning
                    )
                }
            }

            // Biometric app lock
            SettingsSection(title = AppStrings.biometricLock) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransitioning) {
                            toggleBiometricLock(deps.biometric, !biometricLock) { viewModel.setBiometricLock(it) }
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(AppStrings.biometricLock, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            AppStrings.biometricLockDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricLock,
                        onCheckedChange = { toggleBiometricLock(deps.biometric, it) { viewModel.setBiometricLock(it) } },
                        enabled = !isTransitioning
                    )
                }
            }

            // SSH Keys Management
            SettingsSection(title = AppStrings.manageKeys) {
                OutlinedButton(
                    onClick = { if (!isTransitioning) onSshKeysClick() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTransitioning
                ) {
                    Icon(Icons.Default.VpnKey, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.sshKeys)
                }
            }

            // Script Market
            SettingsSection(title = AppStrings.scriptMarket) {
                Button(
                    onClick = { if (!isTransitioning) onScriptMarketClick() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTransitioning,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(AppStrings.scriptMarket)
                }
            }

            // Widget Settings
            SettingsSection(title = widgetCustomizationLabel) {
                OutlinedButton(
                    onClick = { if (!isTransitioning) onWidgetSettingsClick() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTransitioning
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(customizeWidgetsLabel)
                }
            }

            HorizontalDivider()

            // Export / Import
            SettingsSection(title = AppStrings.dataManagement) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { if (!isTransitioning) savePicker("ssh_commander_backup.json") }, modifier = Modifier.fillMaxWidth(), enabled = !isTransitioning) {
                        Text(AppStrings.exportData)
                    }
                    OutlinedButton(onClick = { if (!isTransitioning) uploadPicker() }, modifier = Modifier.fillMaxWidth(), enabled = !isTransitioning) {
                        Text(AppStrings.importData)
                    }
                }
            }

            HorizontalDivider()

            // Cloud Sync
            CloudSyncSection(backupManager = com.neytron.sshcommander.data.ExportImportManager(deps.repository))

            HorizontalDivider()

            // About App
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isTransitioning) { onAboutClick() }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        AppStrings.aboutApp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        String.format(AppStrings.aboutVersion, appVersion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // Theme Preview Dialog
        themeToPreview?.let { theme ->
            ThemePreviewDialog(
                theme = theme,
                fontFamily = termFontFamily,
                onDismiss = { themeToPreview = null },
                onApply = {
                    viewModel.setTermThemeId(theme.id)
                    themeToPreview = null
                }
            )
        }
    }
}

/**
 * Enables/disables the biometric app lock. When turning it on we first verify
 * the device actually has biometrics available — otherwise the app could lock
 * itself out with no way in.
 *
 * [targetEnabled] is the desired state (what the user toggled to).
 */
private fun toggleBiometricLock(
    biometric: BiometricAuthenticator?,
    targetEnabled: Boolean,
    setEnabled: (Boolean) -> Unit
) {
    if (targetEnabled && (biometric == null || !biometric.canAuthenticate())) {
        platformToast(AppStrings.biometricNotAvailable)
        return
    }
    setEnabled(targetEnabled)
}

@Composable
fun ColorPickerRow(colors: List<String>, selectedColor: String, enabled: Boolean, onColorSelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(colors) { colorStr ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parseHexColor(colorStr))
                    .border(
                        width = if (selectedColor.uppercase() == colorStr.uppercase()) 3.dp else 1.dp,
                        color = if (selectedColor.uppercase() == colorStr.uppercase()) MaterialTheme.colorScheme.primary else Color.LightGray,
                        shape = CircleShape
                    )
                    .clickable(enabled = enabled) { onColorSelected(colorStr) }
            )
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        content()
    }
}

@Composable
fun RadioButtonOption(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 8.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick, enabled = enabled)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
fun ThemePresetSelector(
    selectedThemeId: String,
    enabled: Boolean,
    onThemeSelected: (String) -> Unit
) {
    // Increased height to show more rows of the grid
    Box(modifier = Modifier.height(420.dp).fillMaxWidth()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 100.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(TerminalThemes.presets) { theme ->
                val isSelected = selectedThemeId == theme.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                    else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = enabled) { onThemeSelected(theme.id) }
                        .padding(8.dp)
                ) {
                    // Mini preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(parseHexColor(theme.backgroundColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "abc",
                            color = parseHexColor(theme.textColor),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(Modifier.height(6.dp))
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

@OptIn(ExperimentalTextApi::class)
@Composable
fun ThemePreviewDialog(
    theme: TerminalTheme,
    fontFamily: String,
    onDismiss: () -> Unit,
    onApply: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { 
        kotlinx.coroutines.delay(100)
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
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Large Modern terminal preview box with animation
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + expandVertically(tween(600)),
                    exit = fadeOut(tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(parseHexColor(theme.backgroundColor))
                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                "neytron@commander:~$ ls -la /var/log",
                                color = parseHexColor(theme.textColor).copy(alpha = 0.6f),
                                fontSize = 14.sp,
                                fontFamily = currentFontFamily
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "drwxr-xr-x 2 root root 4096 Aug 29 auth.log",
                                color = parseHexColor(theme.textColor),
                                fontSize = 14.sp,
                                fontFamily = currentFontFamily
                            )
                            Text(
                                "-rw-r----- 1 root adm  1285 Aug 29 syslog",
                                color = parseHexColor(theme.textColor),
                                fontSize = 14.sp,
                                fontFamily = currentFontFamily
                            )
                            Spacer(Modifier.height(4.dp))
                            Row {
                                Text(
                                    "neytron@commander:~$ ",
                                    color = parseHexColor(theme.textColor).copy(alpha = 0.6f),
                                    fontSize = 14.sp,
                                    fontFamily = currentFontFamily
                                )
                                // Flashing cursor effect
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
                }

                Text(
                    theme.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onApply,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun FontSelector(
    selectedFont: String,
    enabled: Boolean,
    onFontSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedFont,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
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
                    text = { 
                        Text(
                            text = font,
                            fontFamily = getSystemFontFamily(font)
                        )
                    },
                    onClick = {
                        onFontSelected(font)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Parses "#RRGGBB" / "#AARRGGBB" into a Compose color; falls back to gray. */
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
        Color.Gray
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

private val widgetCustomizationLabel: String
    get() = if (AppStrings.language == "ru") "Настройка виджетов" else "Widget Customization"

private val customizeWidgetsLabel: String
    get() = if (AppStrings.language == "ru") "Настроить виджеты панели" else "Customize Dashboard Widgets"

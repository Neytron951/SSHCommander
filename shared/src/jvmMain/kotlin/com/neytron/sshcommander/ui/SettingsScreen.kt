package com.neytron.sshcommander.ui

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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onWidgetSettingsClick: () -> Unit,
    onAboutClick: () -> Unit,
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(AppStrings.backgroundColor, style = MaterialTheme.typography.labelLarge)
                    ColorPickerRow(presetBgColors, termBgColor, !isTransitioning) { viewModel.setTermBgColor(it) }

                    Text(AppStrings.textColor, style = MaterialTheme.typography.labelLarge)
                    ColorPickerRow(presetTextColors, termTextColor, !isTransitioning) { viewModel.setTermTextColor(it) }
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

package com.neytron.sshcommander.ui

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.R
import com.neytron.sshcommander.security.BiometricUtils
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onWidgetSettingsClick: () -> Unit,
    onAboutClick: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val privacyMode by viewModel.privacyMode.collectAsState()
    val autoReconnect by viewModel.autoReconnect.collectAsState()
    val biometricLock by viewModel.biometricLock.collectAsState()
    
    val termBgColor by viewModel.termBgColor.collectAsState()
    val termTextColor by viewModel.termTextColor.collectAsState()
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
        }.getOrDefault("")
    }
    
    var isTransitioning by remember { mutableStateOf(false) }

    val presetBgColors = listOf("#000000", "#1A1A1B", "#2D2D2D", "#FFFFFF", "#F5F5F5", "#002B36", "#073642")
    val presetTextColors = listOf("#00FF00", "#008000", "#FFFFFF", "#000000", "#FFD700", "#FFA500", "#FF0000", "#268BD2")

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = viewModel.exportData()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    Toast.makeText(context, R.string.export_success, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val json = stream.bufferedReader().readText()
                        viewModel.importData(json) {
                            Toast.makeText(context, R.string.import_success, Toast.LENGTH_SHORT).show()
                            (context as? Activity)?.recreate()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, context.getString(R.string.error_prefix, e.message ?: ""), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (!isTransitioning) {
                            isTransitioning = true
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            SettingsSection(title = stringResource(R.string.theme)) {
                Column {
                    RadioButtonOption(stringResource(R.string.theme_light), themeMode == "light", !isTransitioning) { viewModel.setThemeMode("light") }
                    RadioButtonOption(stringResource(R.string.theme_dark), themeMode == "dark", !isTransitioning) { viewModel.setThemeMode("dark") }
                    RadioButtonOption(stringResource(R.string.theme_system), themeMode == "system", !isTransitioning) { viewModel.setThemeMode("system") }
                }
            }

            // 2. Language
            SettingsSection(title = stringResource(R.string.language)) {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RadioButtonOption(stringResource(R.string.lang_en), language == "en", !isTransitioning) { 
                        viewModel.setLanguage("en")
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                    }
                    RadioButtonOption(stringResource(R.string.lang_ru), language == "ru", !isTransitioning) { 
                        viewModel.setLanguage("ru")
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ru"))
                    }
                }
            }

            // Terminal Customization
            SettingsSection(title = stringResource(R.string.terminal_style)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.background_color), style = MaterialTheme.typography.labelLarge)
                    ColorPickerRow(presetBgColors, termBgColor, !isTransitioning) { viewModel.setTermBgColor(it) }
                    
                    Text(stringResource(R.string.text_color), style = MaterialTheme.typography.labelLarge)
                    ColorPickerRow(presetTextColors, termTextColor, !isTransitioning) { viewModel.setTermTextColor(it) }
                }
            }

            // Privacy Mode
            SettingsSection(title = stringResource(R.string.privacy_mode)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransitioning) { viewModel.setPrivacyMode(!privacyMode) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.privacy_mode), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.privacy_mode_desc),
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
            SettingsSection(title = stringResource(R.string.auto_reconnect)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransitioning) { viewModel.setAutoReconnect(!autoReconnect) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.auto_reconnect), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.auto_reconnect_desc),
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
            SettingsSection(title = stringResource(R.string.biometric_lock)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isTransitioning) {
                            toggleBiometricLock(context, !biometricLock, viewModel::setBiometricLock)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.biometric_lock), style = MaterialTheme.typography.bodyLarge)
                        Text(
                            stringResource(R.string.biometric_lock_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricLock,
                        onCheckedChange = { toggleBiometricLock(context, it, viewModel::setBiometricLock) },
                        enabled = !isTransitioning
                    )
                }
            }

            // Widget Settings
            SettingsSection(title = stringResource(R.string.widget_customization)) {
                OutlinedButton(
                    onClick = { if (!isTransitioning) onWidgetSettingsClick() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTransitioning
                ) {
                    Icon(Icons.Default.Widgets, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.customize_widgets))
                }
            }

            HorizontalDivider()

            // Export / Import
            SettingsSection(title = stringResource(R.string.data_management)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { if (!isTransitioning) exportLauncher.launch("ssh_commander_backup.json") }, modifier = Modifier.fillMaxWidth(), enabled = !isTransitioning) {
                        Text(stringResource(R.string.export_data))
                    }
                    OutlinedButton(onClick = { if (!isTransitioning) importLauncher.launch(arrayOf("application/json")) }, modifier = Modifier.fillMaxWidth(), enabled = !isTransitioning) {
                        Text(stringResource(R.string.import_data))
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
                        stringResource(R.string.about_app),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.about_version, versionName),
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
    context: Context,
    targetEnabled: Boolean,
    setEnabled: (Boolean) -> Unit
) {
    val activity = context as? FragmentActivity
    if (targetEnabled && (activity == null || !BiometricUtils.canAuthenticate(activity))) {
        Toast.makeText(context, R.string.biometric_not_available, Toast.LENGTH_LONG).show()
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
                    .background(Color(colorStr.toColorInt()))
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

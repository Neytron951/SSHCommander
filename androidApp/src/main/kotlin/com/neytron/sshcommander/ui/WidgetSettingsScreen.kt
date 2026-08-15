package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.R
import com.neytron.sshcommander.SshCommanderApplication
import com.neytron.sshcommander.data.SettingsManager
import com.neytron.sshcommander.widget.ServerWidgetProvider
import com.neytron.sshcommander.data.Server
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as SshCommanderApplication
    val repository = remember { app.serverRepository }
    val settingsManager = remember { SettingsManager(context) }

    val servers by repository.allServers.collectAsState(initial = emptyList())
    val widgetBgColor by settingsManager.widgetBgColor.collectAsState(initial = "#121416")
    val widgetTextColor by settingsManager.widgetTextColor.collectAsState(initial = "#FFFFFF")
    val widgetAccentColor by settingsManager.widgetAccentColor.collectAsState(initial = "#03DAC6")
    val widgetItemBgColor by settingsManager.widgetItemBgColor.collectAsState(initial = "#1A1C1E")
    val privacyMode by settingsManager.privacyMode.collectAsState(initial = false)

    val presetCommands = listOf("Status", "Uptime", "Reboot", "Free -m", "Custom...")

    var showCustomCommandDialog by remember { mutableStateOf<Server?>(null) }
    var customCommandText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.widget_settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(stringResource(R.string.appearance), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ColorPickerRow(stringResource(R.string.widget_bg), widgetBgColor) { color ->
                            scope.launch {
                                settingsManager.setWidgetBgColor(color)
                                ServerWidgetProvider.triggerUpdate(context)
                            }
                        }
                        ColorPickerRow(stringResource(R.string.server_card_bg), widgetItemBgColor) { color ->
                            scope.launch {
                                settingsManager.setWidgetItemBgColor(color)
                                ServerWidgetProvider.triggerUpdate(context)
                            }
                        }
                        ColorPickerRow(stringResource(R.string.text_color), widgetTextColor) { color ->
                            scope.launch {
                                settingsManager.setWidgetTextColor(color)
                                ServerWidgetProvider.triggerUpdate(context)
                            }
                        }
                        ColorPickerRow(stringResource(R.string.accent_color), widgetAccentColor) { color ->
                            scope.launch {
                                settingsManager.setWidgetAccentColor(color)
                                ServerWidgetProvider.triggerUpdate(context)
                            }
                        }
                    }
                }
            }

            item {
                Text(stringResource(R.string.enabled_servers), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            items(servers, key = { it.id }) { server ->
                var expanded by remember { mutableStateOf(false) }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    server.host,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Checkbox(
                                checked = server.showInWidget,
                                onCheckedChange = { isChecked ->
                                    scope.launch {
                                        repository.updateServer(server.copy(showInWidget = isChecked), password = null)
                                        ServerWidgetProvider.triggerUpdate(context)
                                    }
                                }
                            )
                        }

                        if (server.showInWidget) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box {
                                OutlinedButton(
                                    onClick = { expanded = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    val isCustom = server.widgetCommand != null && !presetCommands.contains(server.widgetCommand)
                                    Text(
                                        if (isCustom) stringResource(R.string.custom_prefix, server.widgetCommand!!)
                                        else (server.widgetCommand ?: stringResource(R.string.select_command))
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false },
                                    modifier = Modifier.fillMaxWidth(0.8f)
                                ) {
                                    presetCommands.forEach { command ->
                                        DropdownMenuItem(
                                            text = { Text(command) },
                                            onClick = {
                                                if (command == "Custom...") {
                                                    customCommandText = if (server.widgetCommand != null && !presetCommands.contains(server.widgetCommand)) server.widgetCommand!! else ""
                                                    showCustomCommandDialog = server
                                                } else {
                                                    scope.launch {
                                                        repository.updateServer(server.copy(widgetCommand = command), password = null)
                                                        ServerWidgetProvider.triggerUpdate(context)
                                                    }
                                                }
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomCommandDialog != null) {
        AlertDialog(
            onDismissRequest = { showCustomCommandDialog = null },
            title = { Text(stringResource(R.string.custom_widget_cmd), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(stringResource(R.string.custom_widget_cmd_desc))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = customCommandText,
                        onValueChange = { customCommandText = it },
                        label = { Text(stringResource(R.string.ssh_command)) },
                        placeholder = { Text(stringResource(R.string.ssh_cmd_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showCustomCommandDialog?.let { server ->
                        scope.launch {
                            repository.updateServer(server.copy(widgetCommand = customCommandText), password = null)
                            ServerWidgetProvider.triggerUpdate(context)
                        }
                    }
                    showCustomCommandDialog = null
                }) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomCommandDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(label: String, currentColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf(
        "#000000", "#0A0C0E", "#121416", "#1E1E1E", "#252525", "#2C2C2C", "#3D3D3D",
        "#FFFFFF", "#F5F5F5", "#E0E0E0", "#BDBDBD", "#9E9E9E",
        "#03DAC6", "#018786", "#26A69A", "#4DB6AC", "#80CBC4",
        "#BB86FC", "#6200EE", "#3700B3", "#7E57C2", "#B39DDB",
        "#CF6679", "#B00020", "#EF5350", "#E57373", "#EF9A9A",
        "#FF9800", "#F57C00", "#FFB74D", "#FFE0B2",
        "#4CAF50", "#388E3C", "#81C784", "#C8E6C9",
        "#2196F3", "#1976D2", "#64B5F6", "#BBDEFB",
        "#E91E63", "#C2185B", "#F06292", "#F8BBD0",
        "#9C27B0", "#7B1FA2", "#BA68C8", "#E1BEE7"
    )

    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { colorHex ->
                val isSelected = currentColor.equals(colorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(android.graphics.Color.parseColor(colorHex)), CircleShape)
                        .clickable { onColorSelected(colorHex) }
                        .padding(2.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (colorHex.equals("#FFFFFF", true)) Color.Black.copy(alpha = 0.2f)
                                    else Color.White.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        )
                    }
                }
            }
        }
    }
}
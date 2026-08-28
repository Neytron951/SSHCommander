package com.neytron.sshcommander.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.data.MonitorWidget
import com.neytron.sshcommander.data.WidgetType
import com.neytron.sshcommander.terminal.TerminalController

@Composable
fun MonitoringDashboard(
    terminalSession: TerminalController?
) {
    if (terminalSession == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Terminal session required for monitoring")
        }
        return
    }

    val widgets by terminalSession.monitorWidgets.collectAsState()
    val results by terminalSession.widgetResults.collectAsState()
    val stats by terminalSession.sysStats.collectAsState()
    val isConnected by terminalSession.isConnected.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingWidget by remember { mutableStateOf<MonitorWidget?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.width(12.dp))
            if (isConnected) {
                Surface(Modifier.size(8.dp), shape = CircleShape, color = Color(0xFF4CAF50)) {}
            }
            Spacer(Modifier.weight(1f))
            Button(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add")
            }
        }
        
        Spacer(Modifier.height(24.dp))

        // Dynamic Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(180.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 1000.dp)
        ) {
            items(widgets, key = { it.id }) { widget ->
                val rawValue = results[widget.id] ?: "..."
                WidgetCard(
                    widget = widget,
                    value = rawValue,
                    onEdit = { editingWidget = widget },
                    onDelete = { terminalSession.deleteWidget(widget.id) }
                )
            }
        }

        Spacer(Modifier.height(32.dp))
        Text("System Logs", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Text(
                text = stats.rawLogs.ifBlank { "No logs captured yet..." },
                modifier = Modifier.padding(12.dp),
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }
    }

    if (showAddDialog || editingWidget != null) {
        WidgetEditDialog(
            widget = editingWidget,
            onDismiss = { 
                showAddDialog = false
                editingWidget = null 
            },
            onSave = { title, cmd, type, wide, color ->
                if (editingWidget != null) {
                    terminalSession.updateWidget(editingWidget!!.copy(
                        title = title, command = cmd, type = type, isWide = wide, colorHex = color
                    ))
                } else {
                    terminalSession.addWidget(title, cmd, type, wide, color)
                }
                showAddDialog = false
                editingWidget = null
            }
        )
    }
}

@Composable
private fun WidgetCard(
    widget: MonitorWidget,
    value: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val displayValue = remember(value, widget.type) {
        when (widget.type) {
            WidgetType.BYTES -> formatBytes(value.toLongOrNull() ?: 0L)
            WidgetType.PERCENTAGE -> if (value.contains(".")) "%.1f%%".format(value.toFloatOrNull() ?: 0f) else "$value%"
            else -> value
        }
    }

    val progress = remember(value, widget.type) {
        if (widget.type == WidgetType.PERCENTAGE) (value.toFloatOrNull() ?: 0f) / 100f else 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(widget.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                IconButton(onClick = onEdit, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(14.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(20.dp)) {
                    Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(displayValue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            
            if (widget.type == WidgetType.PERCENTAGE) {
                Spacer(Modifier.height(12.dp))
                val color = widget.colorHex?.let { Color(parseHex(it)) } ?: Color(0xFF4CAF50)
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = color,
                    trackColor = color.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun WidgetEditDialog(
    widget: MonitorWidget?,
    onDismiss: () -> Unit,
    onSave: (String, String, WidgetType, Boolean, String?) -> Unit
) {
    var title by remember { mutableStateOf(widget?.title ?: "") }
    var command by remember { mutableStateOf(widget?.command ?: "") }
    var type by remember { mutableStateOf(widget?.type ?: WidgetType.TEXT) }
    var isWide by remember { mutableStateOf(widget?.isWide ?: false) }
    var colorHex by remember { mutableStateOf(widget?.colorHex ?: "#4CAF50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (widget == null) "Add Widget" else "Edit Widget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = command, onValueChange = { command = it }, label = { Text("Command") }, modifier = Modifier.fillMaxWidth())
                
                Text("Display Type", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WidgetType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.name) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isWide, onCheckedChange = { isWide = it })
                    Text("Wide card")
                }
                
                if (type == WidgetType.PERCENTAGE) {
                    OutlinedTextField(value = colorHex, onValueChange = { colorHex = it }, label = { Text("Indicator Color (Hex)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, command, type, isWide, colorHex) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val i = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, 4)
    return "%.1f %s".format(bytes / Math.pow(1024.0, i.toDouble()), units[i])
}

private fun parseHex(hex: String): Long {
    return try {
        hex.removePrefix("#").toLong(16) or 0xFF000000L
    } catch (e: Exception) {
        0xFF4CAF50
    }
}

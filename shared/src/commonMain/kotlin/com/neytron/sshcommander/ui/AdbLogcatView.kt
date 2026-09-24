package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbLogcatView(controller: TerminalController, modifier: Modifier = Modifier) {
    var logsText by remember { mutableStateOf("") }
    var filterText by remember { mutableStateOf("") }
    var isAutoRefresh by remember { mutableStateOf(true) }
    var logLevelFilter by remember { mutableStateOf("ALL") } // ALL, D, I, W, E
    val listState = rememberLazyListState()

    // Poll real ADB logcat output every 2 seconds
    LaunchedEffect(controller, isAutoRefresh) {
        while (isActive && isAutoRefresh) {
            controller.executeAdbShell("logcat -d -v threadtime -t 150") { output ->
                if (output.isNotBlank() && !output.startsWith("Error")) {
                    logsText = output
                }
            }
            delay(2000)
        }
    }

    val logLines = remember(logsText, filterText, logLevelFilter) {
        logsText.lines().filter { line ->
            if (line.isBlank()) return@filter false
            val matchesSearch = filterText.isBlank() || line.contains(filterText, ignoreCase = true)
            val matchesLevel = when (logLevelFilter) {
                "E" -> line.contains(" E ") || line.contains(" F ")
                "W" -> line.contains(" W ") || line.contains(" E ") || line.contains(" F ")
                "I" -> line.contains(" I ") || line.contains(" W ") || line.contains(" E ") || line.contains(" F ")
                "D" -> line.contains(" D ") || line.contains(" I ") || line.contains(" W ") || line.contains(" E ") || line.contains(" F ")
                else -> true
            }
            matchesSearch && matchesLevel
        }
    }

    // Auto-scroll to bottom on new logs
    LaunchedEffect(logLines.size) {
        if (logLines.isNotEmpty()) {
            listState.animateScrollToItem(logLines.size - 1)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Control Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = filterText,
                onValueChange = { filterText = it },
                placeholder = { Text("Filter logs (Tag, PID, Text)...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                trailingIcon = {
                    if (filterText.isNotEmpty()) {
                        IconButton(onClick = { filterText = "" }) {
                            Icon(Icons.Default.Clear, null)
                        }
                    }
                }
            )

            // Log Level selector buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("ALL", "D", "I", "W", "E").forEach { lvl ->
                    FilterChip(
                        selected = logLevelFilter == lvl,
                        onClick = { logLevelFilter = lvl },
                        label = { Text(lvl) }
                    )
                }
            }

            IconButton(onClick = { isAutoRefresh = !isAutoRefresh }) {
                Icon(if (isAutoRefresh) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Toggle Pause")
            }

            IconButton(onClick = {
                controller.executeAdbShell("logcat -d -v threadtime -t 150") { output ->
                    if (output.isNotBlank()) logsText = output
                }
            }) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }

            Button(
                onClick = {
                    controller.executeAdbShell("logcat -c") {
                        logsText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Delete, null)
                Spacer(Modifier.width(4.dp))
                Text("Clear Buffer")
            }
        }

        // Logcat Terminal View
        Surface(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            color = Color(0xFF121212),
            shape = MaterialTheme.shapes.medium
        ) {
            SelectionContainer {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(logLines) { log ->
                        val color = when {
                            log.contains(" E ") || log.contains(" F ") -> Color(0xFFFF5252)
                            log.contains(" W ") -> Color(0xFFFFD740)
                            log.contains(" I ") -> Color(0xFF69F0AE)
                            log.contains(" D ") -> Color(0xFF81D4FA)
                            else -> Color(0xFFB0BEC5)
                        }
                        Text(
                            text = log,
                            color = color,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }
    }
}

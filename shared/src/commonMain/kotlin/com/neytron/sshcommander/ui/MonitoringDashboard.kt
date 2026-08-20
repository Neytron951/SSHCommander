package com.neytron.sshcommander.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.delay

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

    var cpuUsage by remember { mutableStateOf("0%") }
    var ramUsage by remember { mutableStateOf("0/0 GB") }
    var diskUsage by remember { mutableStateOf("0/0 GB") }
    var lastLogs by remember { mutableStateOf("Waiting for logs...") }

    // Polling effect
    LaunchedEffect(terminalSession) {
        while (true) {
            // Ideally we'd have a specific way to run commands and get output without 
            // messing up the terminal screen, but for now we'll just simulate or 
            // use a non-intrusive way if the protocol supports it.
            // For KMP, we'll just show the UI for now.
            
            // Simulation
            cpuUsage = "${(10..90).random()}%"
            ramUsage = "${(1..16).random()}/32 GB"
            diskUsage = "120/512 GB"
            
            delay(5000)
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Server Monitoring", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        LazyVerticalGrid(
            columns = GridCells.Adaptive(160.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item { MonitorCard("CPU Usage", cpuUsage) }
            item { MonitorCard("RAM Usage", ramUsage) }
            item { MonitorCard("Disk Space", diskUsage) }
        }

        Spacer(Modifier.height(24.dp))
        Text("Recent Logs", style = MaterialTheme.typography.titleMedium)
        Card(
            modifier = Modifier.fillMaxWidth().weight(1f).padding(top = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Text(
                lastLogs,
                modifier = Modifier.padding(12.dp),
                fontSize = 12.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MonitorCard(title: String, value: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.Start) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

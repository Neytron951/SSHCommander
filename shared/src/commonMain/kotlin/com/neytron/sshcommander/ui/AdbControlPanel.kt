package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.terminal.TerminalController
import kotlinx.coroutines.launch

@Composable
fun AdbControlPanel(controller: TerminalController, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var infoText by remember { mutableStateOf("Ready. Press buttons to control device via ADB shell.") }

    val onAction = { label: String, cmd: String ->
        infoText = "Executing: $label..."
        controller.executeCommand(cmd)
        scope.launch {
            infoText = "Sent command: $cmd"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.secondary)
                Spacer(Modifier.width(12.dp))
                Text(infoText, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // --- NAVIGATION & KEYS ---
        Text("Navigation & System", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { onAction("Back", "input keyevent 4") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowBack, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Back")
            }
            Button(onClick = { onAction("Home", "input keyevent 3") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Home, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Home")
            }
            Button(onClick = { onAction("Apps", "input keyevent 187") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Menu, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Apps")
            }
            Button(onClick = { onAction("Power", "input keyevent 26") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PowerSettingsNew, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Power")
            }
        }

        // --- TV DPAD REMOTE CONTROL ---
        Text("TV D-Pad Remote", style = MaterialTheme.typography.titleMedium)
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = { onAction("DPAD Up", "input keyevent 19") }) {
                Icon(Icons.Default.KeyboardArrowUp, null)
                Spacer(Modifier.width(4.dp))
                Text("Up")
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(onClick = { onAction("DPAD Left", "input keyevent 21") }) {
                    Icon(Icons.Default.KeyboardArrowLeft, null)
                    Text("Left")
                }
                Button(
                    onClick = { onAction("DPAD Center", "input keyevent 23") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                ) {
                    Icon(Icons.Default.RadioButtonChecked, null)
                    Text("OK")
                }
                Button(onClick = { onAction("DPAD Right", "input keyevent 22") }) {
                    Text("Right")
                    Icon(Icons.Default.KeyboardArrowRight, null)
                }
            }
            Button(onClick = { onAction("DPAD Down", "input keyevent 20") }) {
                Icon(Icons.Default.KeyboardArrowDown, null)
                Spacer(Modifier.width(4.dp))
                Text("Down")
            }
        }

        // --- VOLUME & MEDIA ---
        Text("Volume & Media", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { onAction("Volume Up", "input keyevent 24") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.VolumeUp, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Vol +")
            }
            FilledTonalButton(onClick = { onAction("Volume Down", "input keyevent 25") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.VolumeDown, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Vol -")
            }
            FilledTonalButton(onClick = { onAction("Mute", "input keyevent 164") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.VolumeMute, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Mute")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            FilledTonalButton(onClick = { onAction("Play/Pause", "input keyevent 85") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Play/Pause")
            }
            FilledTonalButton(onClick = { onAction("Media Next", "input keyevent 87") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.SkipNext, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Next")
            }
            FilledTonalButton(onClick = { onAction("Media Prev", "input keyevent 88") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.SkipPrevious, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Prev")
            }
        }

        // --- QUICK ACTIONS & TOOLS ---
        Text("Quick Actions & Tools", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onAction("Open Settings", "am start -a android.settings.SETTINGS") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Settings")
            }
            OutlinedButton(onClick = { onAction("Take Screenshot", "screencap -p /sdcard/screenshot.png") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Screenshot")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { onAction("Wake Up", "input keyevent 224") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.WbSunny, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Wake Up")
            }
            OutlinedButton(onClick = { onAction("Sleep", "input keyevent 223") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Bedtime, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sleep")
            }
        }

        // --- REBOOT & ADVANCED ---
        Text("Reboot & Advanced", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { onAction("Reboot System", "reboot") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Reboot")
            }
            Button(
                onClick = { onAction("Reboot Recovery", "reboot recovery") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.weight(1f)
            ) {
                Text("Recovery")
            }
            Button(
                onClick = { onAction("Reboot Bootloader", "reboot bootloader") },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                modifier = Modifier.weight(1f)
            ) {
                Text("Bootloader")
            }
        }
    }
}

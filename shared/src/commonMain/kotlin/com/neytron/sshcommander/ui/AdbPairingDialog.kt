package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.terminal.AdbPlatform
import kotlinx.coroutines.launch

@Composable
fun AdbPairingDialog(
    initialHost: String,
    initialPort: String = "",
    onPair: suspend (String, Int, String) -> Result<Unit>,
    onDismiss: () -> Unit
) {
    var host by remember { mutableStateOf(initialHost) }
    var port by remember { mutableStateOf(initialPort) }
    var pairingCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val downloadProgress by AdbPlatform.getDownloadProgress().collectAsState()
    val adbAvailable by remember(downloadProgress) { derivedStateOf { AdbPlatform.isAdbAvailable() } }

    var pairingMode by remember { mutableStateOf("CODE") }

    DisposableEffect(Unit) {
        println("[UI-Pair] AdbPairingDialog entered composition")
        onDispose {
            println("[UI-Pair] AdbPairingDialog left composition")
            AdbPlatform.stopPairingServer()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Android, null, tint = Color(0xFF3DDC84))
                Spacer(Modifier.width(12.dp))
                Text(AppStrings.adbPair, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!adbAvailable && downloadProgress == null) {
                    // ... (download UI)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(AppStrings.adbNotFound, fontWeight = FontWeight.Bold)
                            Text(AppStrings.adbDownloadDesc, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { AdbPlatform.startDownload() }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text(AppStrings.adbDownloadBtn)
                            }
                        }
                    }
                } else if (downloadProgress != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(AppStrings.adbDownloading, style = MaterialTheme.typography.labelMedium)
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { downloadProgress!! }, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    // Only CODE pairing supported (no QR)
                    Text(AppStrings.adbPairingInstructions, style = MaterialTheme.typography.bodySmall)

                    // Always show host and pairing port fields so user can enter port for PAIR.
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text(AppStrings.hostIp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text(AppStrings.adbPairingPort) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    // If initialHost provided, show it prominently for context
                    if (initialHost.isNotBlank()) {
                        Text(AppStrings.adbDeviceName.replace("%1\$s", initialHost), fontWeight = FontWeight.Bold, modifier = Modifier.alpha(0.9f))
                    }

                    OutlinedTextField(
                        value = pairingCode,
                        onValueChange = { pairingCode = it },
                        label = { Text(AppStrings.adbPairingCode) },
                        placeholder = { Text("000000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = androidx.compose.ui.unit.TextUnit.Unspecified,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                        )
                    )
                }

                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }

                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            if (pairingMode == "CODE") {
                Button(
                    onClick = {
                        val p = port.toIntOrNull()
                        if (p == null) { error = AppStrings.invalidPort; return@Button }
                        if (pairingCode.length != 6) { error = AppStrings.adbCodeDigits; return@Button }
                        isLoading = true
                        error = null
                        scope.launch {
                            val result = onPair(host, p, pairingCode)
                            isLoading = false
                            if (result.isSuccess) { onDismiss() } else { error = result.exceptionOrNull()?.message ?: AppStrings.pairingFailed }
                        }
                    },
                    enabled = !isLoading && adbAvailable && host.isNotBlank() && port.isNotBlank() && pairingCode.length == 6
                ) {
                    Text(AppStrings.adbPair)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onDismiss() }) {
                Text(AppStrings.cancel)
            }
        }
    )
}

package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCodeScanner
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

    var pairingMode by remember { mutableStateOf(if (initialPort.isNotEmpty()) "CODE" else "CHOOSE") }

    val qrServiceName = remember { AdbQRGenerator.generateRandomService() }
    val qrPassword = remember { AdbQRGenerator.generateRandomPassword() }
    val qrPayload = remember(qrServiceName, qrPassword) { AdbQRGenerator.formatAdbPayload(qrServiceName, qrPassword) }

    LaunchedEffect(pairingMode) {
        if (pairingMode == "QR") {
            AdbPlatform.startPairingServer(qrServiceName, qrPassword) {
                // Device paired successfully via QR
                onDismiss()
            }
        } else {
            AdbPlatform.stopPairingServer()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
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
                    if (pairingMode == "CHOOSE") {
                        Text(AppStrings.adbPairMethod, style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                onClick = { pairingMode = "CODE" },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(32.dp))
                                    Text(AppStrings.adbPairingCode, fontWeight = FontWeight.Bold)
                                }
                            }
                            Card(
                                onClick = { pairingMode = "QR" },
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(32.dp))
                                    Text(AppStrings.adbPairQR, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else if (pairingMode == "QR") {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(AppStrings.adbQRInstructions, style = MaterialTheme.typography.bodySmall)

                            Box(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                QRCodeImage(content = qrPayload, size = 200.dp)
                            }

                            Text("${AppStrings.adbServiceNameLabel}: $qrServiceName", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.7f))
                            Text("${AppStrings.adbPairingCode}: $qrPassword", style = MaterialTheme.typography.labelSmall, modifier = Modifier.alpha(0.7f))
                        }
                    } else {
                        Text(AppStrings.adbPairingInstructions, style = MaterialTheme.typography.bodySmall)

                        if (initialPort.isEmpty()) {
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
                        } else {
                            // Semi-auto mode: hide port as requested, only show device name
                            Text(AppStrings.adbDeviceName.replace("%1\$s", host), fontWeight = FontWeight.Bold)
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
            TextButton(onClick = { 
                if ((pairingMode == "CODE" || pairingMode == "QR") && initialPort.isEmpty()) {
                    pairingMode = "CHOOSE"
                } else {
                    onDismiss()
                }
            }) {
                Text(AppStrings.cancel)
            }
        }
    )
}

package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.Protocol
import com.neytron.sshcommander.data.ServerFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditServerScreen(
    serverId: Int? = null,
    onNavigateBack: () -> Unit,
    onManageLogins: (() -> Unit)? = null
) {
    val deps = LocalAppDeps.current
    val viewModel: AddEditServerViewModel = viewModel { AddEditServerViewModel(deps.repository, deps.settings) }

    val folders by deps.repository.allFolders.collectAsState(initial = emptyList())
    val sshKeys by deps.repository.allSshKeys.collectAsState(initial = emptyList())

    LaunchedEffect(serverId) {
        if (serverId != null) {
            viewModel.loadServer(serverId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (serverId == null) AppStrings.addServer else AppStrings.editServer) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Protocol selection
            Text(AppStrings.protocolLabel, style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                Protocol.entries.forEachIndexed { index, p ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = Protocol.entries.size),
                        onClick = { viewModel.onProtocolChanged(p) },
                        selected = viewModel.protocol == p,
                        icon = {
                            Icon(
                                if (p == Protocol.SSH) Icons.Default.VpnKey else Icons.Default.Android,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    ) {
                        Text(p.name)
                    }
                }
            }

            val isAdb = viewModel.protocol == Protocol.ADB

            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text(AppStrings.serverName) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.host,
                onValueChange = { viewModel.host = it },
                label = { Text(AppStrings.hostIp) },
                modifier = Modifier.fillMaxWidth()
            )

            if (isAdb) {
                var showWizard by remember { mutableStateOf(false) }
                var wizardStep by remember { mutableStateOf(1) } // 1: Guide, 2: Search, 3: Pair
                var selectedDevice by remember { mutableStateOf<com.neytron.sshcommander.data.DiscoveredDevice?>(null) }
                
                var discoveredDevices by remember { mutableStateOf(emptyList<com.neytron.sshcommander.data.DiscoveredDevice>()) }
                var isScanning by remember { mutableStateOf(false) }
                val downloadProgress by com.neytron.sshcommander.terminal.AdbPlatform.getDownloadProgress().collectAsState()
                val adbAvailable by remember(downloadProgress) { derivedStateOf { com.neytron.sshcommander.terminal.AdbPlatform.isAdbAvailable() } }

                var showPairingDialogManual by remember { mutableStateOf(false) }

                LaunchedEffect(isScanning) {
                    if (isScanning && adbAvailable) {
                        com.neytron.sshcommander.terminal.AdbPlatform.scanDevices().collect {
                            discoveredDevices = it
                        }
                    }
                }

                // Show prominent ADB missing banner with download action
                val downloadProgressLocal by com.neytron.sshcommander.terminal.AdbBinaryManager.getDownloadProgress().collectAsState()
                val downloadError by com.neytron.sshcommander.terminal.AdbBinaryManager.getDownloadError().collectAsState()
                val androidRuntime = com.neytron.sshcommander.terminal.AdbBinaryManager.isAndroidRuntime()

                if (!adbAvailable) {
                   Card(
                       modifier = Modifier.fillMaxWidth(),
                       colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                   ) {
                       Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                           Text(
                               if (androidRuntime) "ADB is not installed on this device." else "ADB not found on this device.",
                               color = MaterialTheme.colorScheme.onErrorContainer,
                               style = MaterialTheme.typography.bodyMedium
                           )
                           if (downloadError != null) {
                               Text("$downloadError", color = MaterialTheme.colorScheme.onErrorContainer)
                           }

                           if (androidRuntime) {
                               Text(
                                   "Install Android SDK platform-tools on your PC, then connect the device there. The app cannot install native platform-tools inside Android sandbox.",
                                   color = MaterialTheme.colorScheme.onErrorContainer
                               )
                           } else if (downloadProgressLocal != null) {
                               LinearProgressIndicator(progress = downloadProgressLocal!!, modifier = Modifier.fillMaxWidth())
                               Text("Downloading ADB...")
                           }

                           Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                               if (androidRuntime) {
                                   Button(onClick = { platformOpenUrl("https://developer.android.com/studio/releases/platform-tools") }) {
                                       Text("Open setup guide")
                                   }
                               } else {
                                   Button(
                                       onClick = { com.neytron.sshcommander.terminal.AdbBinaryManager.startDownload() },
                                       enabled = downloadProgressLocal == null
                                   ) {
                                       Text("Download ADB")
                                   }

                                   OutlinedButton(onClick = { platformOpenUrl("https://developer.android.com/studio/releases/platform-tools") }) {
                                       Text("How to use ADB")
                                   }
                               }
                           }
                       }
                   }

                   Spacer(Modifier.height(8.dp))
                }

                // --- PRIMARY PAIR AND CONNECT BUTTON ---
                Button(
                   onClick = { showWizard = true; wizardStep = 1 },
                   modifier = Modifier.fillMaxWidth(),
                   shape = RoundedCornerShape(12.dp),
                   contentPadding = PaddingValues(16.dp)
                ) {
                   Icon(Icons.Default.Android, null)
                   Spacer(Modifier.width(8.dp))
                   Text(AppStrings.adbPairAndConnect, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Text(AppStrings.adbManualConnection, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)

                // Manual Section: Step 1 Pair
                Column(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(AppStrings.adbFirstStepPair, style = MaterialTheme.typography.labelSmall)
                    OutlinedButton(
                        onClick = { showPairingDialogManual = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.adbEnterCode)
                    }
                }

                if (showPairingDialogManual) {
                    AdbPairingDialog(
                        initialHost = viewModel.host,
                        onPair = { host, port, code -> com.neytron.sshcommander.terminal.AdbPlatform.pair(host, port, code) },
                        onDismiss = { showPairingDialogManual = false }
                    )
                }

                // --- WIZARD DIALOGS ---
                if (showWizard) {
                    AlertDialog(
                        onDismissRequest = { showWizard = false; isScanning = false },
                        title = { 
                            Text(when(wizardStep) {
                                1 -> AppStrings.adbWizardGuideTitle
                                2 -> AppStrings.adbScan
                                else -> AppStrings.adbPair
                            })
                        },
                        text = {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                when(wizardStep) {
                                    1 -> {
                                        Text(AppStrings.adbWizardGuideDesc)
                                    }
                                    2 -> {
                                        LaunchedEffect(Unit) { isScanning = true }
                                        if (discoveredDevices.isEmpty()) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                                CircularProgressIndicator()
                                                Spacer(Modifier.height(8.dp))
                                                Text(AppStrings.adbScanning)
                                            }
                                        } else {
                                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                discoveredDevices.forEach { device ->
                                                    Card(
                                                        onClick = { 
                                                            selectedDevice = device
                                                            if (device.pairingPort != null) {
                                                                wizardStep = 3
                                                            } else {
                                                                // Already paired or plain IP
                                                                viewModel.host = device.host
                                                                viewModel.port = device.port.toString()
                                                                if (viewModel.name.isEmpty() || viewModel.name == viewModel.host) viewModel.name = device.name
                                                                showWizard = false
                                                                isScanning = false
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        ListItem(
                                                            headlineContent = { Text(device.name) },
                                                            supportingContent = { Text("${device.host}:${device.port}") },
                                                            leadingContent = { Icon(Icons.Default.Android, null) }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Spacer(Modifier.height(16.dp))
                                        HorizontalDivider(thickness = 0.5.dp)
                                        Spacer(Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { /* Experimental */ },
                                            modifier = Modifier.fillMaxWidth(),
                                            enabled = false
                                        ) {
                                            Icon(Icons.Default.QrCodeScanner, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text(AppStrings.adbPairQR + " (Experimental)")
                                        }
                                    }
                                    3 -> {
                                        // We reuse AdbPairingDialog logic or embed it
                                        Text(AppStrings.adbPairingInstructions)
                                        // For simplicity, we just close this wizard and open the specialized pairing dialog
                                        // But the user wants step-by-step, so let's just use the existing dialog if we can
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            if (wizardStep == 1) {
                                Button(onClick = { wizardStep = 2 }) { Text(AppStrings.next) }
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showWizard = false; isScanning = false }) { Text(AppStrings.cancel) }
                        }
                    )
                }

                // If we reached step 3, open the pairing dialog and close wizard
                if (showWizard && wizardStep == 3 && selectedDevice != null) {
                    AdbPairingDialog(
                        initialHost = selectedDevice!!.host,
                        initialPort = selectedDevice!!.pairingPort?.toString() ?: "",
                        onPair = { host, port, code -> 
                            val res = com.neytron.sshcommander.terminal.AdbPlatform.pair(host, port, code)
                            if (res.isSuccess) {
                                viewModel.host = host
                                // We don't know the connection port yet, usually it's discovery after pairing
                                // But often it's already in the selectedDevice if it was both pairing and connect
                                if (selectedDevice!!.port > 0) viewModel.port = selectedDevice!!.port.toString()
                                if (viewModel.name.isEmpty() || viewModel.name == host) viewModel.name = selectedDevice!!.name
                                showWizard = false
                                isScanning = false
                            }
                            res
                        },
                        onDismiss = { wizardStep = 2 }
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.port = it },
                    label = { Text(if (isAdb) AppStrings.adbConnectionPort else AppStrings.port) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = if (isAdb) {
                        { Text(AppStrings.adbConnectionPortHint) }
                    } else null
                )

                if (!isAdb) {
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it },
                        label = { Text(AppStrings.username) },
                        modifier = Modifier.weight(2f)
                    )
                }
            }

            if (!isAdb) {
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it },
                    label = { Text(AppStrings.password) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )

                OutlinedTextField(
                    value = viewModel.sftpStartPath,
                    onValueChange = { viewModel.sftpStartPath = it },
                    label = { Text(AppStrings.sftpStartPath) },
                    placeholder = { Text("/") },
                    supportingText = { Text(AppStrings.sftpStartPathHint2) },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                // Empty block or additional ADB specific fields if needed
            }

            // Folder selector (server grouping).
            Text(AppStrings.folders, style = MaterialTheme.typography.titleSmall)
            var folderMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(
                    onClick = { folderMenu = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = folders.firstOrNull { it.id == viewModel.folderId }?.name ?: AppStrings.noFolder,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null)
                }
                DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(AppStrings.noFolder) },
                        onClick = { viewModel.folderId = null; folderMenu = false }
                    )
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = { viewModel.folderId = folder.id; folderMenu = false }
                        )
                    }
                }
            }

            // SSH Key selector
            if (!isAdb && sshKeys.isNotEmpty()) {
                Text(AppStrings.sshKeys, style = MaterialTheme.typography.titleSmall)
                var keyMenu by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { keyMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = sshKeys.firstOrNull { it.id == viewModel.sshKeyId }?.name ?: "No key selected",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(expanded = keyMenu, onDismissRequest = { keyMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("No key") },
                            onClick = { viewModel.sshKeyId = null; keyMenu = false }
                        )
                        sshKeys.forEach { key ->
                            DropdownMenuItem(
                                text = { Text(key.name) },
                                onClick = { viewModel.sshKeyId = key.id; keyMenu = false }
                            )
                        }
                    }
                }
            }

            if (!isAdb && serverId != null && onManageLogins != null) {
                OutlinedButton(
                    onClick = onManageLogins,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(AppStrings.manageLogins)
                }
            }

            Text(AppStrings.chooseIcon, style = MaterialTheme.typography.titleSmall)

            // Icon Picker Grid
            Box(modifier = Modifier.height(150.dp)) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(80.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(IconUtils.availableIcons) { option ->
                        val isSelected = viewModel.iconName == option.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { viewModel.iconName = option.name }
                                .padding(8.dp)
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = option.label,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    viewModel.saveServer { onNavigateBack() }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(AppStrings.saveServer)
            }
        }
    }
}

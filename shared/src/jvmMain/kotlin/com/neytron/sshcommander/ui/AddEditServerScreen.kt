package com.neytron.sshcommander.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
                var showDeviceList by remember { mutableStateOf(false) }
                var discoveredDevices by remember { mutableStateOf(emptyList<com.neytron.sshcommander.data.DiscoveredDevice>()) }
                var isScanning by remember { mutableStateOf(false) }
                val adbAvailable = remember { com.neytron.sshcommander.terminal.AdbPlatform.isAdbAvailable() }
                val downloadProgress by com.neytron.sshcommander.terminal.AdbPlatform.getDownloadProgress().collectAsState()

                LaunchedEffect(isScanning) {
                    if (isScanning && adbAvailable) {
                        com.neytron.sshcommander.terminal.AdbPlatform.scanDevices().collect {
                            discoveredDevices = it
                        }
                    } else if (isScanning && !adbAvailable) {
                        isScanning = false
                    }
                }

                if (!adbAvailable && downloadProgress == null) {
                    OutlinedButton(
                        onClick = { com.neytron.sshcommander.terminal.AdbPlatform.startDownload() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Download, null)
                        Spacer(Modifier.width(8.dp))
                        Text(AppStrings.adbDownloadBtn)
                    }
                } else if (downloadProgress != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(AppStrings.adbDownloading, style = MaterialTheme.typography.labelSmall)
                        LinearProgressIndicator(
                            progress = { downloadProgress!! },
                            modifier = Modifier.fillMaxWidth().height(4.dp).padding(vertical = 4.dp)
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = { 
                            isScanning = !isScanning
                            showDeviceList = true 
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(AppStrings.adbScanning)
                        } else {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(AppStrings.adbScan)
                        }
                    }
                }

                if (showDeviceList && isScanning && discoveredDevices.isEmpty()) {
                    Text(
                        AppStrings.adbScanning + " (mDNS)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (showDeviceList && discoveredDevices.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            discoveredDevices.forEach { device ->
                                ListItem(
                                    headlineContent = { Text(device.name) },
                                    supportingContent = { Text("${device.host}:${device.port}") },
                                    leadingContent = { Icon(Icons.Default.Android, null) },
                                    modifier = Modifier.clickable {
                                        viewModel.host = device.host
                                        viewModel.port = device.port.toString()
                                        if (viewModel.name.isEmpty() || viewModel.name == viewModel.host) viewModel.name = device.name
                                        showDeviceList = false
                                        isScanning = false
                                    }
                                )
                            }
                        }
                    }
                } else if (showDeviceList && !isScanning && discoveredDevices.isEmpty() && adbAvailable) {
                    Text(AppStrings.adbNoDevices, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 8.dp))
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
                var showPairingDialog by remember { mutableStateOf(false) }
                OutlinedButton(
                    onClick = { showPairingDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.VpnKey, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Pair Device")
                }

                if (showPairingDialog) {
                    AdbPairingDialog(
                        initialHost = viewModel.host,
                        onPair = { host, port, code ->
                            com.neytron.sshcommander.terminal.AdbPlatform.pair(host, port, code)
                        },
                        onDismiss = { showPairingDialog = false }
                    )
                }
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

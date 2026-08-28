package com.neytron.sshcommander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.SshKey
import com.neytron.sshcommander.data.SshKeyUtils
import com.neytron.sshcommander.ui.LocalAppDeps
import com.neytron.sshcommander.ui.AppStrings
import com.neytron.sshcommander.ui.platformToast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SshKeyManagerScreen(onNavigateBack: () -> Unit) {
    val deps = LocalAppDeps.current
    val viewModel: SshKeyManagerViewModel = viewModel { SshKeyManagerViewModel(deps.repository) }
    val keys = viewModel.sshKeys
    val isLoading = viewModel.isLoading
    val clipboard = LocalClipboardManager.current

    var showAddDialog by remember { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<SshKey?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.sshKeys) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStrings.back)
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = AppStrings.addKey)
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (keys.isEmpty() && !isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No SSH keys added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(keys, key = { it.id }) { key ->
                        KeyCard(
                            key = key,
                            onCopyPublic = {
                                key.publicKeyContent?.let {
                                    clipboard.setText(AnnotatedString(it))
                                    platformToast(AppStrings.publicKeyCopied)
                                }
                            },
                            onDelete = { keyToDelete = key }
                        )
                    }
                }
            }

            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    if (showAddDialog) {
        AddSshKeyDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, type, bits, passphrase ->
                try {
                    val (private, public) = SshKeyUtils.generateKeyPair(type, bits, passphrase)
                    viewModel.addKey(SshKey(name = name, type = type, privateKeyContent = private, publicKeyContent = public))
                    showAddDialog = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    platformToast("Error generating key: ${e.message}")
                }
            },
            onImport = { name, private, public ->
                viewModel.addKey(SshKey(name = name, type = "Imported", privateKeyContent = private, publicKeyContent = public))
                showAddDialog = false
            }
        )
    }

    keyToDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            title = { Text(AppStrings.delete) },
            text = { Text(AppStrings.deleteKeyConfirm) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteKey(key.id)
                    keyToDelete = null
                }) {
                    Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToDelete = null }) { Text(AppStrings.cancel) }
            }
        )
    }
}

@Composable
private fun KeyCard(
    key: SshKey,
    onCopyPublic: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(key.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("${key.type} Key", style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = onCopyPublic) {
                Icon(Icons.Default.ContentCopy, contentDescription = AppStrings.copyPublicKey)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = AppStrings.delete, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AddSshKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, type: String, bits: Int, passphrase: String?) -> Unit,
    onImport: (name: String, privateKey: String, publicKey: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("RSA") }
    var bits by remember { mutableIntStateOf(4096) }
    var passphrase by remember { mutableStateOf("") }
    var mode by remember { mutableIntStateOf(0) } // 0: Generate, 1: Import Content

    var importPrivate by remember { mutableStateOf("") }
    var importPublic by remember { mutableStateOf("") }
    
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (mode == 0) AppStrings.generateKey else AppStrings.addKey) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                TabRow(selectedTabIndex = mode) {
                    Tab(selected = mode == 0, onClick = { mode = 0 }) { Text("Generate", modifier = Modifier.padding(8.dp)) }
                    Tab(selected = mode == 1, onClick = { mode = 1 }) { Text("Import", modifier = Modifier.padding(8.dp)) }
                }
                
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(AppStrings.keyName) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (mode == 0) {
                    // Key Type Selection (Simplified to RSA only for stability)
                    Text(AppStrings.keyType, style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = true, onClick = { type = "RSA" })
                        Text("RSA", modifier = Modifier.clickable { type = "RSA" })
                    }

                    // Bits selection
                    Text("Bits:", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = bits == 2048, onClick = { bits = 2048 })
                        Text("2048")
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = bits == 4096, onClick = { bits = 4096 })
                        Text("4096")
                    }

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text(AppStrings.passphrase) },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = importPrivate,
                        onValueChange = { importPrivate = it },
                        label = { Text("Private Key Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        trailingIcon = {
                            IconButton(onClick = { 
                                clipboard.getText()?.text?.let { importPrivate = it } 
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        }
                    )
                    OutlinedTextField(
                        value = importPublic,
                        onValueChange = { importPublic = it },
                        label = { Text("Public Key Content (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        trailingIcon = {
                            IconButton(onClick = { 
                                clipboard.getText()?.text?.let { importPublic = it } 
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste")
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (mode == 0) onConfirm(name, type, bits, passphrase.takeIf { it.isNotBlank() })
                    else onImport(name, importPrivate, importPublic.takeIf { it.isNotBlank() })
                },
                enabled = name.isNotBlank() && (mode == 0 || importPrivate.isNotBlank())
            ) {
                Text(if (mode == 0) AppStrings.generateKey else AppStrings.addKey)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(AppStrings.cancel) }
        }
    )
}

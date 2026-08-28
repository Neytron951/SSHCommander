package com.neytron.sshcommander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.data.ServerLogin
import com.neytron.sshcommander.data.ServerRepository
import com.neytron.sshcommander.data.SshKey
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageLoginsViewModel(private val repository: ServerRepository, private val serverId: Int) : ViewModel() {
    val logins = mutableStateListOf<ServerLogin>()
    val sshKeys = mutableStateListOf<SshKey>()
    var isLoading by mutableStateOf(true)
    var isProvisioning by mutableStateOf(false)
    
    private val provisioningService = com.neytron.sshcommander.data.UserProvisioningService()

    init {
        viewModelScope.launch {
            repository.getLoginsForServer(serverId).collectLatest { list ->
                logins.clear()
                logins.addAll(list)
                isLoading = false
            }
        }
        viewModelScope.launch {
            repository.allSshKeys.collectLatest { list ->
                sshKeys.clear()
                sshKeys.addAll(list)
            }
        }
    }

    fun saveLogin(login: ServerLogin, password: String?, onComplete: () -> Unit) {
        viewModelScope.launch {
            if (login.id == 0) {
                repository.insertLogin(login, password ?: "")
            } else {
                repository.updateLogin(login, password)
            }
            onComplete()
        }
    }
    
    fun provisionAndSaveLogin(
        label: String,
        username: String,
        password: String?,
        sftpStartPath: String,
        genType: String = "RSA",
        genBits: Int = 4096,
        genPass: String? = null,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch {
            isProvisioning = true
            try {
                val session = com.neytron.sshcommander.data.SshConnectionManager.getActiveSession(serverId)
                if (session == null) {
                    onResult("No active session found. Connect to the server first.")
                    return@launch
                }
                
                // 1. Generate a new key for this user with selected settings
                val keyName = "Managed Key: $username @ ${serverId}"
                val (priv, pub) = com.neytron.sshcommander.data.SshKeyUtils.generateKeyPair(genType, genBits, genPass)
                val keyId = repository.insertSshKey(SshKey(
                    name = keyName, 
                    type = genType, 
                    privateKeyContent = priv, 
                    publicKeyContent = pub,
                    passphraseKey = genPass
                ))
                
                // 2. Provision on server
                val result = provisioningService.provisionUser(session, username, pub, password)
                
                if (result.isSuccess) {
                    // 3. Save login
                    repository.insertLogin(
                        ServerLogin(serverId = serverId, label = label, username = username, sftpStartPath = sftpStartPath, sshKeyId = keyId),
                        password ?: ""
                    )
                    onResult(null)
                } else {
                    onResult("Provisioning failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onResult(e.message ?: "Unknown error")
            } finally {
                isProvisioning = false
            }
        }
    }

    fun deleteLogin(login: ServerLogin) {
        viewModelScope.launch {
            repository.deleteLogin(login)
        }
    }

    fun setDefault(login: ServerLogin, isDefault: Boolean) {
        viewModelScope.launch {
            // Only one login can be default per server
            if (isDefault) {
                logins.filter { it.isDefault && it.id != login.id }.forEach {
                    repository.updateLogin(it.copy(isDefault = false), null)
                }
            }
            repository.updateLogin(login.copy(isDefault = isDefault), null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLoginsScreen(
    serverId: Int,
    onNavigateBack: () -> Unit
) {
    val deps = LocalAppDeps.current
    val viewModel: ManageLoginsViewModel = viewModel { ManageLoginsViewModel(deps.repository, serverId) }

    var showAddDialog by remember { mutableStateOf(false) }
    var loginToEdit by remember { mutableStateOf<ServerLogin?>(null) }
    var loginToDelete by remember { mutableStateOf<ServerLogin?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.manageLogins) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = AppStrings.back)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = AppStrings.addLogin)
            }
        }
    ) { padding ->
        if (viewModel.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(viewModel.logins) { login ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(login.label.ifBlank { login.username }, style = MaterialTheme.typography.titleMedium)
                                Text(login.username, style = MaterialTheme.typography.bodySmall)
                                if (!login.sftpStartPath.isNullOrBlank()) {
                                    Text(
                                        String.format(AppStrings.sftpStartPathHint, login.sftpStartPath),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.setDefault(login, !login.isDefault) }) {
                                Icon(
                                    if (login.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = AppStrings.setDefaultLogin,
                                    tint = if (login.isDefault) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { loginToEdit = login }) {
                                Icon(Icons.Default.Edit, contentDescription = AppStrings.edit)
                            }
                            IconButton(onClick = { loginToDelete = login }) {
                                Icon(Icons.Default.Delete, contentDescription = AppStrings.delete, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (viewModel.logins.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(AppStrings.noLogins, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        LoginDialog(
            sshKeys = viewModel.sshKeys,
            isProvisioning = viewModel.isProvisioning,
            onDismiss = { showAddDialog = false },
            onConfirm = { label, username, password, sftpStartPath, sshKeyId, autoProvision, genType, genBits, genPass ->
                if (autoProvision) {
                    viewModel.provisionAndSaveLogin(label, username, password, sftpStartPath, genType, genBits, genPass) { error ->
                        if (error == null) showAddDialog = false
                        else platformToast(error)
                    }
                } else {
                    viewModel.saveLogin(
                        ServerLogin(serverId = serverId, label = label, username = username, sftpStartPath = sftpStartPath, sshKeyId = sshKeyId),
                        password
                    ) { showAddDialog = false }
                }
            }
        )
    }

    loginToEdit?.let { login ->
        LoginDialog(
            initialLabel = login.label,
            initialUsername = login.username,
            initialSftpStartPath = login.sftpStartPath ?: "",
            initialSshKeyId = login.sshKeyId,
            sshKeys = viewModel.sshKeys,
            isProvisioning = viewModel.isProvisioning,
            onDismiss = { loginToEdit = null },
            onConfirm = { label, username, password, sftpStartPath, sshKeyId, autoProvision, genType, genBits, genPass ->
                viewModel.saveLogin(
                    login.copy(label = label, username = username, sftpStartPath = sftpStartPath.ifBlank { null }, sshKeyId = sshKeyId),
                    password.ifBlank { null }
                ) { loginToEdit = null }
            }
        )
    }

    loginToDelete?.let { login ->
        AlertDialog(
            onDismissRequest = { loginToDelete = null },
            title = { Text(AppStrings.deleteLoginTitle) },
            text = { Text(String.format(AppStrings.deleteLoginMsg, login.label.ifBlank { login.username })) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLogin(login)
                    loginToDelete = null
                }) {
                    Text(AppStrings.delete, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { loginToDelete = null }) {
                    Text(AppStrings.cancel)
                }
            }
        )
    }
}

@Composable
fun LoginDialog(
    initialLabel: String = "",
    initialUsername: String = "",
    initialPassword: String = "",
    initialSftpStartPath: String = "",
    initialSshKeyId: Int? = null,
    sshKeys: List<SshKey> = emptyList(),
    isProvisioning: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (label: String, username: String, password: String, sftpStartPath: String, sshKeyId: Int?, autoProvision: Boolean, keyType: String, keyBits: Int, keyPass: String?) -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var sftpStartPath by remember { mutableStateOf(initialSftpStartPath) }
    var sshKeyId by remember { mutableStateOf(initialSshKeyId) }
    var authMethod by remember { mutableStateOf(if (initialSshKeyId != null) 1 else 0) } // 0: Password, 1: Key
    var autoProvision by remember { mutableStateOf(false) }
    
    // Key generation settings
    var genBits by remember { mutableIntStateOf(4096) }
    var genPassphrase by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLabel.isEmpty()) AppStrings.addLogin else AppStrings.edit, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(AppStrings.loginLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(AppStrings.username) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                
                Text(AppStrings.authMethod, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = authMethod == 0, onClick = { authMethod = 0; autoProvision = false })
                    Text(AppStrings.usePassword, modifier = Modifier.clickable { authMethod = 0; autoProvision = false })
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = authMethod == 1, onClick = { authMethod = 1 })
                    Text(AppStrings.useSshKey, modifier = Modifier.clickable { authMethod = 1 })
                }

                if (authMethod == 0) {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(AppStrings.password) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Key selection / generation
                    if (initialLabel.isEmpty()) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = autoProvision, onCheckedChange = { autoProvision = it })
                                    Text(AppStrings.autoProvisionDesc, fontWeight = FontWeight.Bold)
                                }
                                Text(
                                    AppStrings.provisioningWarning,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(start = 32.dp)
                                )
                                
                                if (autoProvision) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                    Text("New Key Settings:", style = MaterialTheme.typography.labelMedium)
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("RSA Bits: ", style = MaterialTheme.typography.bodySmall)
                                        RadioButton(selected = genBits == 2048, onClick = { genBits = 2048 })
                                        Text("2048", style = MaterialTheme.typography.bodySmall)
                                        Spacer(Modifier.width(8.dp))
                                        RadioButton(selected = genBits == 4096, onClick = { genBits = 4096 })
                                        Text("4096", style = MaterialTheme.typography.bodySmall)
                                    }

                                    OutlinedTextField(
                                        value = genPassphrase,
                                        onValueChange = { genPassphrase = it },
                                        label = { Text(AppStrings.passphrase) },
                                        singleLine = true,
                                        visualTransformation = PasswordVisualTransformation(),
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                                    )

                                    Spacer(Modifier.height(4.dp))
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("Initial System Password (for useradd)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (!autoProvision) {
                        Text(AppStrings.selectExistingKey, style = MaterialTheme.typography.labelMedium)
                        var keyMenu by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { keyMenu = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = sshKeys.firstOrNull { it.id == sshKeyId }?.name ?: "No key selected",
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                            DropdownMenu(expanded = keyMenu, onDismissRequest = { keyMenu = false }) {
                                DropdownMenuItem(
                                    text = { Text("No key") },
                                    onClick = { sshKeyId = null; keyMenu = false }
                                )
                                sshKeys.forEach { key ->
                                    DropdownMenuItem(
                                        text = { Text(key.name) },
                                        onClick = { sshKeyId = key.id; keyMenu = false }
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = sftpStartPath,
                    onValueChange = { sftpStartPath = it },
                    label = { Text(AppStrings.sftpStartPath) },
                    placeholder = { Text("/") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        label.trim(), 
                        username.trim(), 
                        password, 
                        sftpStartPath.trim(), 
                        if (authMethod == 0) null else sshKeyId, 
                        autoProvision,
                        "RSA", 
                        genBits, 
                        genPassphrase.takeIf { it.isNotBlank() }
                    )
                },
                enabled = label.isNotBlank() && username.isNotBlank() && !isProvisioning
            ) {
                if (isProvisioning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isProvisioning) "Provisioning..." else AppStrings.save)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isProvisioning) { Text(AppStrings.cancel) }
        }
    )
}

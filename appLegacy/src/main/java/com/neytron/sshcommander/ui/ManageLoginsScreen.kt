package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.text.KeyboardOptions
import com.neytron.sshcommander.R
import com.neytron.sshcommander.data.ServerLogin
import com.neytron.sshcommander.data.ServerRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageLoginsViewModel(private val repository: ServerRepository, private val serverId: Int) : ViewModel() {
    val logins = mutableStateListOf<ServerLogin>()
    var isLoading by mutableStateOf(true)

    init {
        viewModelScope.launch {
            repository.getLoginsForServer(serverId).collectLatest { list ->
                logins.clear()
                logins.addAll(list)
                isLoading = false
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

    class Factory(private val repository: ServerRepository, private val serverId: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ManageLoginsViewModel(repository, serverId) as T
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageLoginsScreen(
    serverId: Int,
    onNavigateBack: () -> Unit,
    viewModel: ManageLoginsViewModel = viewModel(factory = ManageLoginsViewModel.Factory(
        ServerRepository(LocalContext.current), serverId
    ))
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var loginToEdit by remember { mutableStateOf<ServerLogin?>(null) }
    var loginToDelete by remember { mutableStateOf<ServerLogin?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manage_logins)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_login))
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
                                        stringResource(R.string.sftp_start_path_hint, login.sftpStartPath),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.setDefault(login, !login.isDefault) }) {
                                Icon(
                                    if (login.isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = stringResource(R.string.set_default_login),
                                    tint = if (login.isDefault) MaterialTheme.colorScheme.primary else LocalContentColor.current
                                )
                            }
                            IconButton(onClick = { loginToEdit = login }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = { loginToDelete = login }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                if (viewModel.logins.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.no_logins), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        LoginDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { label, username, password, sftpStartPath ->
                viewModel.saveLogin(
                    ServerLogin(serverId = serverId, label = label, username = username, sftpStartPath = sftpStartPath),
                    password
                ) { showAddDialog = false }
            }
        )
    }

    loginToEdit?.let { login ->
        LoginDialog(
            initialLabel = login.label,
            initialUsername = login.username,
            initialSftpStartPath = login.sftpStartPath ?: "",
            onDismiss = { loginToEdit = null },
            onConfirm = { label, username, password, sftpStartPath ->
                viewModel.saveLogin(
                    login.copy(label = label, username = username, sftpStartPath = sftpStartPath.ifBlank { null }),
                    password.ifBlank { null }
                ) { loginToEdit = null }
            }
        )
    }

    loginToDelete?.let { login ->
        AlertDialog(
            onDismissRequest = { loginToDelete = null },
            title = { Text(stringResource(R.string.delete_login_title)) },
            text = { Text(stringResource(R.string.delete_login_msg, login.label.ifBlank { login.username })) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteLogin(login)
                    loginToDelete = null
                }) {
                    Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { loginToDelete = null }) {
                    Text(stringResource(R.string.cancel))
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
    onDismiss: () -> Unit,
    onConfirm: (label: String, username: String, password: String, sftpStartPath: String) -> Unit
) {
    var label by remember { mutableStateOf(initialLabel) }
    var username by remember { mutableStateOf(initialUsername) }
    var password by remember { mutableStateOf(initialPassword) }
    var sftpStartPath by remember { mutableStateOf(initialSftpStartPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initialLabel.isEmpty()) R.string.add_login else R.string.edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.login_label)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                OutlinedTextField(
                    value = sftpStartPath,
                    onValueChange = { sftpStartPath = it },
                    label = { Text(stringResource(R.string.sftp_start_path)) },
                    placeholder = { Text("/") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(label, username, password, sftpStartPath) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

package com.neytron.sshcommander.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.neytron.sshcommander.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditServerScreen(
    serverId: Int? = null,
    viewModel: AddEditServerViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onManageLogins: (() -> Unit)? = null
) {
    LaunchedEffect(serverId) {
        if (serverId != null) {
            viewModel.loadServer(serverId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (serverId == null) R.string.add_server else R.string.edit_server)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text(stringResource(R.string.server_name)) },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = viewModel.host,
                onValueChange = { viewModel.host = it },
                label = { Text(stringResource(R.string.host_ip)) },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.port,
                    onValueChange = { viewModel.port = it },
                    label = { Text(stringResource(R.string.port)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it },
                    label = { Text(stringResource(R.string.username)) },
                    modifier = Modifier.weight(2f)
                )
            }

            OutlinedTextField(
                value = viewModel.password,
                onValueChange = { viewModel.password = it },
                label = { Text(stringResource(R.string.password)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            OutlinedTextField(
                value = viewModel.sftpStartPath,
                onValueChange = { viewModel.sftpStartPath = it },
                label = { Text(stringResource(R.string.sftp_start_path)) },
                placeholder = { Text("/") },
                supportingText = { Text(stringResource(R.string.sftp_start_path_hint2)) },
                modifier = Modifier.fillMaxWidth()
            )

            if (serverId != null && onManageLogins != null) {
                OutlinedButton(
                    onClick = onManageLogins,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manage_logins))
                }
            }

            Text(stringResource(R.string.choose_icon), style = MaterialTheme.typography.titleSmall)
            
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
                                contentDescription = stringResource(option.labelRes), 
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(option.labelRes),
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
                Text(stringResource(R.string.save_server))
            }
        }
    }
}

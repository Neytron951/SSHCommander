package com.neytron.sshcommander.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    appVersion: String = ""
) {
    val deps = LocalAppDeps.current
    val scope = rememberCoroutineScope()
    val language by deps.settings.language.collectAsState(initial = AppStrings.language)
    val adsEnabled by deps.settings.adsEnabled.collectAsState(initial = true)

    val content = remember(language) { AboutContent.forLanguage(language) }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showLicenseDialog by remember { mutableStateOf(false) }
    var showAdConfirm1 by remember { mutableStateOf(false) }
    var showAdConfirm2 by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(AppStrings.aboutApp, fontWeight = FontWeight.Bold) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Icon(
                imageVector = Icons.Filled.Terminal,
                contentDescription = AppStrings.appName,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(content.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                String.format(AppStrings.aboutVersion, appVersion),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(32.dp))
            
            // List Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    ListItem(
                        headlineContent = { Text(AppStrings.aboutApp) },
                        leadingContent = { Icon(Icons.Default.Info, null) },
                        modifier = Modifier.clickable { showAboutDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    ListItem(
                        headlineContent = { Text(AppStrings.license) },
                        leadingContent = { Icon(Icons.Default.Policy, null) },
                        modifier = Modifier.clickable { showLicenseDialog = true }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Ads Switch
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                ListItem(
                    headlineContent = { Text(AppStrings.disableAds) },
                    supportingContent = { Text(AppStrings.disableAdsDesc) },
                    trailingContent = {
                        Switch(
                            checked = !adsEnabled,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    // User wants to DISABLE ads
                                    showAdConfirm1 = true
                                } else {
                                    // User wants to ENABLE ads (no confirmation)
                                    scope.launch { deps.settings.setAdsEnabled(true) }
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    // Dialogs
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text(AppStrings.aboutApp) },
            text = {
                Column {
                    content.description.forEach { paragraph ->
                        Text(paragraph, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("OK") }
            }
        )
    }

    if (showLicenseDialog) {
        AlertDialog(
            onDismissRequest = { showLicenseDialog = false },
            title = { Text(AppStrings.license) },
            text = {
                Text("SSH Commander is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.\n\n" +
                     "This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.")
            },
            confirmButton = {
                TextButton(onClick = { showLicenseDialog = false }) { Text("OK") }
            }
        )
    }

    if (showAdConfirm1) {
        AlertDialog(
            onDismissRequest = { showAdConfirm1 = false },
            title = { Text(AppStrings.disableAdsConfirmTitle) },
            text = { Text(AppStrings.disableAdsConfirmMsg) },
            confirmButton = {
                Button(onClick = {
                    showAdConfirm1 = false
                    showAdConfirm2 = true
                }) { Text(AppStrings.yes) }
            },
            dismissButton = {
                TextButton(onClick = { showAdConfirm1 = false }) { Text(AppStrings.no) }
            }
        )
    }

    if (showAdConfirm2) {
        AlertDialog(
            onDismissRequest = { showAdConfirm2 = false },
            title = { Text(AppStrings.disableAdsConfirmFinalTitle) },
            text = { Text(AppStrings.disableAdsConfirmFinalMsg) },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch { deps.settings.setAdsEnabled(false) }
                        showAdConfirm2 = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(AppStrings.disableAds) }
            },
            dismissButton = {
                TextButton(onClick = { showAdConfirm2 = false }) { Text(AppStrings.cancel) }
            }
        )
    }
}

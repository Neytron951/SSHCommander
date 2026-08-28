package com.neytron.sshcommander.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neytron.sshcommander.data.DataBackupManager
import kotlinx.coroutines.launch

@Composable
expect fun CloudSyncSection(
    backupManager: DataBackupManager? = null
)

@Composable
fun CloudSyncSectionContent(
    backupManager: DataBackupManager?,
    onSignInClick: () -> Unit
) {
    val deps = LocalAppDeps.current
    val authManager = deps.authManager
    val settings = deps.settings
    val scope = rememberCoroutineScope()
    
    if (authManager == null) return

    val isAuthorized by authManager.isAuthorized.collectAsState()
    val userEmail by authManager.userEmail.collectAsState()
    val lastSync by settings.lastSyncTime.collectAsState(initial = 0L)

    val driveService = remember(deps) {
        if (deps.httpClient != null) {
            com.neytron.sshcommander.sync.GoogleDriveService(deps.httpClient) {
                authManager.getAccessToken()
            }
        } else null
    }

    Text(
        text = if (AppStrings.language == "ru") "Облачная синхронизация" else "Cloud Sync",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
    
    if (!isAuthorized) {
        Button(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Cloud, null)
            Spacer(Modifier.width(8.dp))
            Text(if (AppStrings.language == "ru") "Войти в Google Drive" else "Sign in with Google Drive")
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(Modifier.padding(12.dp)) {
                Text(userEmail ?: "Authorized", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                if (lastSync > 0) {
                    Text(
                        text = (if (AppStrings.language == "ru") "Последняя синхр.: " else "Last sync: ") + 
                               formatDate(lastSync),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                try {
                                                    val backup = backupManager ?: return@launch
                                                    val json = backup.exportJson()
                                                    val success = driveService?.uploadBackup(json) ?: false
                                                    if (success) {
                                                        settings.setLastSyncTime(System.currentTimeMillis())
                                                        platformToast(if (AppStrings.language == "ru") "Данные отправлены!" else "Data uploaded!")
                                                    } else {
                                                        platformToast(if (AppStrings.language == "ru") "Ошибка загрузки" else "Upload failed")
                                                    }
                                                } catch (e: Exception) {
                                                    platformToast("Error: ${e.message}")
                                                }
                                            }
                                        },
                                        enabled = driveService != null,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (AppStrings.language == "ru") "Загрузить в облако" else "Upload")
                                    }

                                    Button(
                                        onClick = { 
                                            scope.launch {
                                                try {
                                                    val backup = backupManager ?: return@launch
                                                    val json = driveService?.downloadBackup()
                                                    if (json != null) {
                                                        backup.importJson(json)
                                                        settings.setLastSyncTime(System.currentTimeMillis())
                                                        platformToast(if (AppStrings.language == "ru") "Данные получены! Перезапустите приложение." else "Data restored! Please restart app.")
                                                    } else {
                                                        platformToast(if (AppStrings.language == "ru") "В облаке нет данных" else "No data in cloud")
                                                    }
                                                } catch (e: Exception) {
                                                    platformToast("Error: ${e.message}")
                                                }
                                            }
                                        },
                                        enabled = driveService != null,
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Text(if (AppStrings.language == "ru") "Скачать из облака" else "Download")
                                    }
                                }
                                OutlinedButton(
                                    onClick = { scope.launch { authManager.signOut() } },
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                                ) {
                                    Text(if (AppStrings.language == "ru") "Выйти из аккаунта" else "Sign Out")
                                }
            }
        }
    }
}

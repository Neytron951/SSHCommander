package com.neytron.sshcommander.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.neytron.sshcommander.data.DataBackupManager
import kotlinx.coroutines.launch

@Composable
actual fun CloudSyncSection(backupManager: DataBackupManager?) {
    val deps = LocalAppDeps.current
    val authManager = deps.authManager
    val scope = rememberCoroutineScope()

    CloudSyncSectionContent(backupManager) {
        scope.launch {
            authManager?.signIn()
        }
    }
}

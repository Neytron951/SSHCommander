package com.neytron.sshcommander.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.neytron.sshcommander.data.DataBackupManager
import com.neytron.sshcommander.sync.AndroidAuthManager
import kotlinx.coroutines.launch

@Composable
actual fun CloudSyncSection(backupManager: DataBackupManager?) {
    val deps = LocalAppDeps.current
    val authManager = deps.authManager
    val scope = rememberCoroutineScope()
    
    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                (authManager as? AndroidAuthManager)?.handleSignInResult(account)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    CloudSyncSectionContent(backupManager) {
        val intent = (authManager as? AndroidAuthManager)?.getSignInIntent()
        if (intent != null) {
            googleSignInLauncher.launch(intent)
        }
    }
}

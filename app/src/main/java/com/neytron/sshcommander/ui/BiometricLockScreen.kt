package com.neytron.sshcommander.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.neytron.sshcommander.R
import com.neytron.sshcommander.security.BiometricUtils

/**
 * Full-screen lock shown when biometric app lock is enabled.
 * Triggers the fingerprint prompt immediately and offers a manual retry
 * button if the prompt is dismissed.
 */
@Composable
fun BiometricLockScreen(
    activity: FragmentActivity,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pendingPrompt by remember { mutableStateOf(false) }

    fun triggerPrompt() {
        if (pendingPrompt) return
        pendingPrompt = true
        errorMessage = null
        BiometricUtils.showBiometricPrompt(
            activity = activity,
            title = context.getString(R.string.app_locked),
            subtitle = context.getString(R.string.app_locked_subtitle),
            negativeButtonText = context.getString(R.string.cancel),
            onSuccess = {
                pendingPrompt = false
                onUnlocked()
            },
            onError = { err ->
                pendingPrompt = false
                errorMessage = err
            }
        )
    }

    LaunchedEffect(Unit) {
        // Give the window a moment to fully resume before showing the prompt.
        kotlinx.coroutines.delay(300)
        triggerPrompt()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.app_locked),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.app_locked_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = { triggerPrompt() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.unlock))
            }
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { triggerPrompt() }) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

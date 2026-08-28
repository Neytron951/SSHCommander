package com.neytron.sshcommander.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.ServerRepository

/**
 * Shared dependencies injected into the ported phone-style screens.
 * androidApp provides the real implementations; desktop provides its own.
 */
data class AppDeps(
    val repository: ServerRepository,
    val settings: AppSettings,
    val biometric: BiometricAuthenticator?,
    val authManager: com.neytron.sshcommander.sync.AuthManager? = null,
    val httpClient: io.ktor.client.HttpClient? = null
)

val LocalAppDeps = staticCompositionLocalOf<AppDeps> {
    error("LocalAppDeps not provided — wrap content in CompositionLocalProvider(LocalAppDeps provides ...)")
}

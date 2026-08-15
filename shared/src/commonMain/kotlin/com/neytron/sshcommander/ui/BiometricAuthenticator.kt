package com.neytron.sshcommander.ui

/**
 * Cross-platform biometric authentication.
 * - Android: backed by androidx.biometric BiometricPrompt.
 * - Desktop: not supported — callers pass `null` and skip the check.
 */
interface BiometricAuthenticator {
    fun canAuthenticate(): Boolean
    /** True when a prompt can actually be shown right now (e.g. the host
     *  activity is resumed). Callers should skip triggering otherwise, or the
     *  prompt callbacks may never fire and the UI gets stuck. */
    fun canShowPromptNow(): Boolean = true
    fun showPrompt(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    )
}

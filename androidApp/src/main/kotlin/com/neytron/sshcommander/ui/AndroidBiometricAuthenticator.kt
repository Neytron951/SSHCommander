package com.neytron.sshcommander.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.neytron.sshcommander.ui.BiometricAuthenticator

/**
 * Android biometric authenticator backed by androidx.biometric BiometricPrompt.
 * Requires a FragmentActivity host.
 */
class AndroidBiometricAuthenticator(
    private val activity: FragmentActivity
) : BiometricAuthenticator {

    // Tracks whether a BiometricPrompt is currently shown. Calling
    // authenticate() while another prompt is active throws
    // IllegalArgumentException, which would crash the app when the lock
    // screen retries (e.g. after the screen turns off and back on).
    private var promptActive = false

    init {
        // When the activity stops (screen off, switching to another app) the
        // system auto-cancels a showing BiometricPrompt. The
        // onAuthenticationError callback is NOT guaranteed to fire in that
        // case, which would leave promptActive=true and silently kill every
        // later unlock attempt. Clear the flag on STOP so the next resume can
        // show the prompt again.
        activity.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                promptActive = false
            }
        })
    }

    override fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(activity)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    override fun canShowPromptNow(): Boolean =
        activity.lifecycle.currentState == Lifecycle.State.RESUMED

    override fun showPrompt(
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (promptActive) return
        // Don't attempt to authenticate while the activity isn't resumed — the
        // system won't show the prompt and the error callback may never fire.
        if (activity.lifecycle.currentState != Lifecycle.State.RESUMED) return
        promptActive = true
        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    promptActive = false
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    promptActive = false
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
        biometricPrompt.authenticate(promptInfo)
    }
}

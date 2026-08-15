package com.neytron.sshcommander.data

sealed class SshError {
    object ConnectionTimeout : SshError()
    object AuthenticationFailed : SshError()
    object HostUnreachable : SshError()
    object HostKeyMismatch : SshError()
    data class Unknown(val message: String?) : SshError()

    /** Machine-readable key; UI layer maps it to a localized string. */
    val messageKey: String
        get() = when (this) {
            is ConnectionTimeout -> "err_timeout"
            is AuthenticationFailed -> "err_auth_failed"
            is HostUnreachable -> "err_host_unreachable"
            is HostKeyMismatch -> "err_host_key_mismatch"
            is Unknown -> "err_unknown"
        }
}

package com.neytron.sshcommander.data

import android.content.Context
import com.neytron.sshcommander.R

sealed class SshError {
    object ConnectionTimeout : SshError()
    object AuthenticationFailed : SshError()
    object HostUnreachable : SshError()
    object HostKeyMismatch : SshError()
    data class Unknown(val message: String?) : SshError()

    fun getMessage(context: Context): String {
        return when (this) {
            is ConnectionTimeout -> context.getString(R.string.err_timeout)
            is AuthenticationFailed -> context.getString(R.string.err_auth_failed)
            is HostUnreachable -> context.getString(R.string.err_host_unreachable)
            is HostKeyMismatch -> context.getString(R.string.err_host_key_mismatch)
            is Unknown -> message ?: context.getString(R.string.err_unknown)
        }
    }
}

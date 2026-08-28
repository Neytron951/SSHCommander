package com.neytron.sshcommander.sync

import kotlinx.coroutines.flow.StateFlow

interface AuthManager {
    val userEmail: StateFlow<String?>
    val isAuthorized: StateFlow<Boolean>
    
    suspend fun signIn(): Boolean
    suspend fun signOut()
    suspend fun getAccessToken(): String?
    fun getSignInIntent(): Any? = null
}

expect fun createAuthManager(
    context: Any? = null,
    secureStorage: com.neytron.sshcommander.data.SecureStorage? = null
): AuthManager

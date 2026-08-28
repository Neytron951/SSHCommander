package com.neytron.sshcommander.sync

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidAuthManager(private val context: Context) : AuthManager {
    private val _userEmail = MutableStateFlow<String?>(null)
    override val userEmail: StateFlow<String?> = _userEmail

    private val _isAuthorized = MutableStateFlow(false)
    override val isAuthorized: StateFlow<Boolean> = _isAuthorized

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
        .build()

    private val signInClient = GoogleSignIn.getClient(context, gso)

    init {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        if (account != null) {
            _userEmail.value = account.email
            _isAuthorized.value = true
        }
    }

    override suspend fun signIn(): Boolean {
        // На Android signIn обычно запускается через Intent из UI.
        // Для простоты здесь мы просто возвращаем true, 
        // если аккаунт уже есть, иначе UI должен вызвать ActivityResultLauncher.
        return _isAuthorized.value
    }

    override suspend fun signOut() {
        withContext(Dispatchers.IO) {
            signInClient.signOut()
        }
        _isAuthorized.value = false
        _userEmail.value = null
    }

    override suspend fun getAccessToken(): String? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return withContext(Dispatchers.IO) {
            try {
                com.google.android.gms.auth.GoogleAuthUtil.getToken(
                    context,
                    account.account!!,
                    "oauth2:https://www.googleapis.com/auth/drive.appdata email profile"
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    override fun getSignInIntent(): android.content.Intent {
        return signInClient.signInIntent
    }

    fun handleSignInResult(account: com.google.android.gms.auth.api.signin.GoogleSignInAccount?) {
        if (account != null) {
            _userEmail.value = account.email
            _isAuthorized.value = true
        }
    }
}

actual fun createAuthManager(context: Any?, secureStorage: com.neytron.sshcommander.data.SecureStorage?): AuthManager {
    return AndroidAuthManager(context as Context)
}

package com.neytron.sshcommander.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.GeneralSecurityException

object SecurityUtils {
    private const val PREFS_NAME = "encrypted_ssh_creds"

    /**
     * Creates the encrypted preferences. If the master key in the Android Keystore can no
     * longer decrypt the stored file (happens after app restore, reinstall, or a device
     * lock-screen/keystore reset), the corrupted file is wiped and re-created so the app
     * keeps working instead of crashing with AEADBadTagException.
     */
    private fun getEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefs(context)
        } catch (e: GeneralSecurityException) {
            resetEncryptedPrefs(context)
            createEncryptedPrefs(context)
        }
    }

    private fun createEncryptedPrefs(context: Context) = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun resetEncryptedPrefs(context: Context) {
        // Delete the corrupted encrypted prefs file (and its .backup copy)
        // so it can be created fresh with a new master key.
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        File(dir, "$PREFS_NAME.xml").delete()
        File(dir, "$PREFS_NAME.backup").delete()
    }

    fun savePassword(context: Context, serverId: Int, password: String) {
        getEncryptedPrefs(context).edit().putString("pass_$serverId", password).apply()
    }

    fun getPassword(context: Context, serverId: Int): String {
        return getEncryptedPrefs(context).getString("pass_$serverId", "") ?: ""
    }

    fun deletePassword(context: Context, serverId: Int) {
        getEncryptedPrefs(context).edit().remove("pass_$serverId").apply()
    }

    fun saveLoginPassword(context: Context, loginId: Int, password: String) {
        getEncryptedPrefs(context).edit().putString("login_pass_$loginId", password).apply()
    }

    fun getLoginPassword(context: Context, loginId: Int): String {
        return getEncryptedPrefs(context).getString("login_pass_$loginId", "") ?: ""
    }

    fun deleteLoginPassword(context: Context, loginId: Int) {
        getEncryptedPrefs(context).edit().remove("login_pass_$loginId").apply()
    }
}

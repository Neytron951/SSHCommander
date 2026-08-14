package com.neytron.sshcommander.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.ExportImportManager
import com.neytron.sshcommander.data.SettingsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val exportImportManager = ExportImportManager(application)

    val themeMode: StateFlow<String> = settingsManager.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "system"
    )
    val fontSize: StateFlow<String> = settingsManager.fontSize.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "medium"
    )
    val fontFamily: StateFlow<String> = settingsManager.fontFamily.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "monospace"
    )
    val timeout: StateFlow<Int> = settingsManager.timeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 10
    )
    val rebootConfirm: StateFlow<String> = settingsManager.rebootConfirm.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "always"
    )
    val language: StateFlow<String> = settingsManager.language.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "en"
    )
    
    val termBgColor: StateFlow<String> = settingsManager.termBgColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000"
    )
    val termTextColor: StateFlow<String> = settingsManager.termTextColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "#00FF00"
    )
    val privacyMode: StateFlow<Boolean> = settingsManager.privacyMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val autoReconnect: StateFlow<Boolean> = settingsManager.autoReconnect.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val biometricLock: StateFlow<Boolean> = settingsManager.biometricLock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun setThemeMode(mode: String) = viewModelScope.launch { settingsManager.setThemeMode(mode) }
    fun setFontSize(size: String) = viewModelScope.launch { settingsManager.setFontSize(size) }
    fun setFontFamily(family: String) = viewModelScope.launch { settingsManager.setFontFamily(family) }
    fun setTimeout(seconds: Int) = viewModelScope.launch { settingsManager.setTimeout(seconds) }
    fun setRebootConfirm(mode: String) = viewModelScope.launch { settingsManager.setRebootConfirm(mode) }
    fun setLanguage(lang: String) = viewModelScope.launch { settingsManager.setLanguage(lang) }
    
    fun setTermBgColor(color: String) = viewModelScope.launch { settingsManager.setTermBgColor(color) }
    fun setTermTextColor(color: String) = viewModelScope.launch { settingsManager.setTermTextColor(color) }
    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch { settingsManager.setPrivacyMode(enabled) }
    fun setAutoReconnect(enabled: Boolean) = viewModelScope.launch { settingsManager.setAutoReconnect(enabled) }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { settingsManager.setBiometricLock(enabled) }

    suspend fun exportData(): String {
        return exportImportManager.exportData()
    }

    fun importData(json: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            exportImportManager.importData(json)
            onComplete()
        }
    }
}

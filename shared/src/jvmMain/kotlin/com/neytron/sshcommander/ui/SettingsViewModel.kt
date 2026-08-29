package com.neytron.sshcommander.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neytron.sshcommander.data.AppSettings
import com.neytron.sshcommander.data.ExportImportManager
import com.neytron.sshcommander.data.ServerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: ServerRepository,
    private val settings: AppSettings
) : ViewModel() {
    private val exportImportManager = ExportImportManager(repository)

    val themeMode: StateFlow<String> = settings.themeMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "system"
    )
    val fontSize: StateFlow<String> = settings.fontSize.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "medium"
    )
    val fontFamily: StateFlow<String> = settings.fontFamily.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "monospace"
    )
    val timeout: StateFlow<Int> = settings.timeout.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), 10
    )
    val rebootConfirm: StateFlow<String> = settings.rebootConfirm.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "always"
    )
    val language: StateFlow<String> = settings.language.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "en"
    )

    val termThemeId: StateFlow<String> = settings.termThemeId.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "tokyo_night"
    )
    val termFontFamily: StateFlow<String> = settings.termFontFamily.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "JetBrains Mono"
    )

    val termBgColor: StateFlow<String> = settings.termBgColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "#000000"
    )
    val termTextColor: StateFlow<String> = settings.termTextColor.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), "#00FF00"
    )
    val privacyMode: StateFlow<Boolean> = settings.privacyMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )
    val autoReconnect: StateFlow<Boolean> = settings.autoReconnect.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), true
    )
    val biometricLock: StateFlow<Boolean> = settings.biometricLock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), false
    )

    fun setThemeMode(mode: String) = viewModelScope.launch { settings.setThemeMode(mode) }
    fun setFontSize(size: String) = viewModelScope.launch { settings.setFontSize(size) }
    fun setFontFamily(family: String) = viewModelScope.launch { settings.setFontFamily(family) }
    fun setTimeout(seconds: Int) = viewModelScope.launch { settings.setTimeout(seconds) }
    fun setRebootConfirm(mode: String) = viewModelScope.launch { settings.setRebootConfirm(mode) }
    fun setLanguage(lang: String) = viewModelScope.launch {
        settings.setLanguage(lang)
        AppStrings.language = lang
    }

    fun setTermBgColor(color: String) = viewModelScope.launch { settings.setTermBgColor(color) }
    fun setTermTextColor(color: String) = viewModelScope.launch { settings.setTermTextColor(color) }
    fun setTermThemeId(themeId: String) = viewModelScope.launch { 
        settings.setTermThemeId(themeId)
        applyThemePreset(themeId)
    }
    fun setTermFontFamily(family: String) = viewModelScope.launch { settings.setTermFontFamily(family) }

    private suspend fun applyThemePreset(themeId: String) {
        if (themeId == "custom") return
        val theme = TerminalThemes.presets.find { it.id == themeId } ?: return
        settings.setTermBgColor(theme.backgroundColor)
        settings.setTermTextColor(theme.textColor)
    }

    fun setPrivacyMode(enabled: Boolean) = viewModelScope.launch { settings.setPrivacyMode(enabled) }
    fun setAutoReconnect(enabled: Boolean) = viewModelScope.launch { settings.setAutoReconnect(enabled) }
    fun setBiometricLock(enabled: Boolean) = viewModelScope.launch { settings.setBiometricLock(enabled) }

    suspend fun exportData(): String {
        return exportImportManager.exportData()
    }

    suspend fun importData(json: String) {
        exportImportManager.importData(json)
    }
}

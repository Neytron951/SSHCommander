package com.neytron.sshcommander.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Cross-platform application settings. Mirrors the Android DataStore-backed
 * [SettingsManager] API so the ported screens can depend on this interface.
 *
 * The desktop-only layout settings (menu-bar visibility toggles, pane widths)
 * have default implementations so platform implementations don't have to touch
 * them unless they want to persist them.
 */
interface AppSettings {
    val themeMode: Flow<String>
    val language: Flow<String>
    val fontFamily: Flow<String>
    val fontSize: Flow<String>
    val timeout: Flow<Int>
    val rebootConfirm: Flow<String>

    val termBgColor: Flow<String>
    val termTextColor: Flow<String>
    val termFontSizePx: Flow<Float>

    val privacyMode: Flow<Boolean>
    val autoReconnect: Flow<Boolean>
    val biometricLock: Flow<Boolean>

    /** Desktop: whether the server list pane is visible. */
    val showServerList: Flow<Boolean> get() = flowOf(true)

    /** Desktop: whether the quick-command panel is visible. */
    val showCommandPanel: Flow<Boolean> get() = flowOf(true)

    /** Desktop: whether the top interaction bar is visible. */
    val showTopBar: Flow<Boolean> get() = flowOf(true)

    /** Desktop: server list pane width in px. */
    val serverPaneWidthPx: Flow<Int> get() = flowOf(280)

    /** Desktop: command panel width in px. */
    val commandPaneWidthPx: Flow<Int> get() = flowOf(190)

    suspend fun setThemeMode(mode: String)
    suspend fun setLanguage(lang: String)
    suspend fun setFontFamily(family: String)
    suspend fun setFontSize(size: String)
    suspend fun setTimeout(seconds: Int)
    suspend fun setRebootConfirm(mode: String)

    suspend fun setTermBgColor(color: String)
    suspend fun setTermTextColor(color: String)
    suspend fun setTermFontSizePx(size: Float)

    suspend fun setPrivacyMode(enabled: Boolean)
    suspend fun setAutoReconnect(enabled: Boolean)
    suspend fun setBiometricLock(enabled: Boolean)

    /** Desktop layout setters (no-op by default). */
    suspend fun setShowServerList(enabled: Boolean) {}
    suspend fun setShowCommandPanel(enabled: Boolean) {}
    suspend fun setShowTopBar(enabled: Boolean) {}
    suspend fun setServerPaneWidthPx(px: Int) {}
    suspend fun setCommandPaneWidthPx(px: Int) {}
}

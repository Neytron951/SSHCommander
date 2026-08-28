package com.neytron.sshcommander.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Properties

/**
 * Desktop implementation of [AppSettings] backed by a properties file
 * (settings.properties in the data directory). Kept deliberately simple —
 * every setter rewrites the file, every getter reads the in-memory copy.
 */
class DesktopSettings(private val dataDir: File) : AppSettings {
    private val file = File(dataDir, "settings.properties")
    private val props = Properties()

    private val _version = MutableStateFlow(0)
    private val version: Flow<Int> = _version

    init {
        try {
            if (file.exists()) file.inputStream().use { props.load(it) }
        } catch (e: Exception) {
            // Corrupt file — start fresh.
        }
    }

    private suspend fun persist() = withContext(Dispatchers.IO) {
        try {
            dataDir.mkdirs()
            file.outputStream().use { props.store(it, "SSH Commander settings") }
        } catch (e: Exception) { /* ignore */ }
        _version.value = _version.value + 1
    }

    private fun str(key: String, default: String): Flow<String> = version.map { props.getProperty(key) ?: default }
    private fun int(key: String, default: Int): Flow<Int> = version.map { (props.getProperty(key) ?: default.toString()).toIntOrNull() ?: default }
    private fun bool(key: String, default: Boolean): Flow<Boolean> = version.map { (props.getProperty(key) ?: default.toString()).toBooleanStrictOrNull() ?: default }
    private fun flt(key: String, default: Float): Flow<Float> = version.map { (props.getProperty(key) ?: default.toString()).toFloatOrNull() ?: default }
    private fun long(key: String, default: Long): Flow<Long> = version.map { (props.getProperty(key) ?: default.toString()).toLongOrNull() ?: default }

    override val themeMode: Flow<String> = str("theme_mode", "system")
    override val language: Flow<String> = str("language", "en")
    override val fontFamily: Flow<String> = str("font_family", "monospace")
    override val fontSize: Flow<String> = str("font_size", "medium")
    override val timeout: Flow<Int> = int("timeout", 10)
    override val rebootConfirm: Flow<String> = str("reboot_confirm", "always")
    override val termBgColor: Flow<String> = str("term_bg_color", "#000000")
    override val termTextColor: Flow<String> = str("term_text_color", "#FFFFFF")
    override val termFontSizePx: Flow<Float> = flt("term_font_size_px", 14f)
    override val privacyMode: Flow<Boolean> = bool("privacy_mode", false)
    override val autoReconnect: Flow<Boolean> = bool("auto_reconnect", true)
    override val biometricLock: Flow<Boolean> = bool("biometric_lock", false)
    override val adsEnabled: Flow<Boolean> = bool("ads_enabled", true)
    override val monitorWidgets: Flow<String> = str("monitor_widgets", "")
    override val lastSyncTime: Flow<Long> = long("last_sync_time", 0L)
    override val isCloudSyncEnabled: Flow<Boolean> = bool("is_cloud_sync_enabled", false)
    override val onboardingCompleted: Flow<Boolean> = bool("onboarding_completed", false)

    // Desktop layout settings
    override val showServerList: Flow<Boolean> = bool("show_server_list", true)
    override val showCommandPanel: Flow<Boolean> = bool("show_command_panel", true)
    override val showTopBar: Flow<Boolean> = bool("show_top_bar", true)
    override val serverPaneWidthPx: Flow<Int> = int("server_pane_width_px", 280)
    override val commandPaneWidthPx: Flow<Int> = int("command_pane_width_px", 190)

    override suspend fun setThemeMode(mode: String) { props.setProperty("theme_mode", mode); persist() }
    override suspend fun setLanguage(lang: String) { props.setProperty("language", lang); persist() }
    override suspend fun setFontFamily(family: String) { props.setProperty("font_family", family); persist() }
    override suspend fun setFontSize(size: String) { props.setProperty("font_size", size); persist() }
    override suspend fun setTimeout(seconds: Int) { props.setProperty("timeout", seconds.toString()); persist() }
    override suspend fun setRebootConfirm(mode: String) { props.setProperty("reboot_confirm", mode); persist() }
    override suspend fun setTermBgColor(color: String) { props.setProperty("term_bg_color", color); persist() }
    override suspend fun setTermTextColor(color: String) { props.setProperty("term_text_color", color); persist() }
    override suspend fun setTermFontSizePx(size: Float) { props.setProperty("term_font_size_px", size.toString()); persist() }
    override suspend fun setPrivacyMode(enabled: Boolean) { props.setProperty("privacy_mode", enabled.toString()); persist() }
    override suspend fun setAutoReconnect(enabled: Boolean) { props.setProperty("auto_reconnect", enabled.toString()); persist() }
    override suspend fun setBiometricLock(enabled: Boolean) { props.setProperty("biometric_lock", enabled.toString()); persist() }
    override suspend fun setAdsEnabled(enabled: Boolean) { props.setProperty("ads_enabled", enabled.toString()); persist() }
    override suspend fun setMonitorWidgets(json: String) { props.setProperty("monitor_widgets", json); persist() }
    override suspend fun setLastSyncTime(time: Long) { props.setProperty("last_sync_time", time.toString()); persist() }
    override suspend fun setCloudSyncEnabled(enabled: Boolean) { props.setProperty("is_cloud_sync_enabled", enabled.toString()); persist() }
    override suspend fun setOnboardingCompleted(completed: Boolean) { props.setProperty("onboarding_completed", completed.toString()); persist() }
    override suspend fun setShowServerList(enabled: Boolean) { props.setProperty("show_server_list", enabled.toString()); persist() }
    override suspend fun setShowCommandPanel(enabled: Boolean) { props.setProperty("show_command_panel", enabled.toString()); persist() }
    override suspend fun setShowTopBar(enabled: Boolean) { props.setProperty("show_top_bar", enabled.toString()); persist() }
    override suspend fun setServerPaneWidthPx(px: Int) { props.setProperty("server_pane_width_px", px.toString()); persist() }
    override suspend fun setCommandPaneWidthPx(px: Int) { props.setProperty("command_pane_width_px", px.toString()); persist() }
}

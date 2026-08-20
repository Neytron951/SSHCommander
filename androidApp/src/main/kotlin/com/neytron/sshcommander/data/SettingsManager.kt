package com.neytron.sshcommander.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * DataStore-backed settings (Android-only). Kept out of the shared module —
 * the desktop build does not need DataStore. Implements [AppSettings] so the
 * ported shared screens can depend on the interface.
 */
class SettingsManager(context: Context) : AppSettings {
    private val dataStore = context.dataStore

    companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE = stringPreferencesKey("language")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val TIMEOUT = intPreferencesKey("timeout")
        val REBOOT_CONFIRM = stringPreferencesKey("reboot_confirm")

        val TERM_BG_COLOR = stringPreferencesKey("term_bg_color")
        val TERM_TEXT_COLOR = stringPreferencesKey("term_text_color")
        val TERM_FONT_SIZE_PX = floatPreferencesKey("term_font_size_px")

        // Widget Color Preferences
        val WIDGET_BG_COLOR = stringPreferencesKey("widget_bg_color")
        val WIDGET_TEXT_COLOR = stringPreferencesKey("widget_text_color")
        val WIDGET_ACCENT_COLOR = stringPreferencesKey("widget_accent_color")
        val WIDGET_ITEM_BG_COLOR = stringPreferencesKey("widget_item_bg_color")

        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    override val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }
    override val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "en" }
    override val fontFamily: Flow<String> = dataStore.data.map { it[FONT_FAMILY] ?: "monospace" }
    override val fontSize: Flow<String> = dataStore.data.map { it[FONT_SIZE] ?: "medium" }
    override val timeout: Flow<Int> = dataStore.data.map { it[TIMEOUT] ?: 10 }
    override val rebootConfirm: Flow<String> = dataStore.data.map { it[REBOOT_CONFIRM] ?: "always" }

    override val termBgColor: Flow<String> = dataStore.data.map { it[TERM_BG_COLOR] ?: "#000000" }
    override val termTextColor: Flow<String> = dataStore.data.map { it[TERM_TEXT_COLOR] ?: "#00FF00" }
    override val termFontSizePx: Flow<Float> = dataStore.data.map { it[TERM_FONT_SIZE_PX] ?: 14f }

    // Defaults for Widget: Deep Dark Theme
    val widgetBgColor: Flow<String> = dataStore.data.map { it[WIDGET_BG_COLOR] ?: "#0A0C0E" }
    val widgetTextColor: Flow<String> = dataStore.data.map { it[WIDGET_TEXT_COLOR] ?: "#FFFFFF" }
    val widgetAccentColor: Flow<String> = dataStore.data.map { it[WIDGET_ACCENT_COLOR] ?: "#03DAC6" }
    val widgetItemBgColor: Flow<String> = dataStore.data.map { it[WIDGET_ITEM_BG_COLOR] ?: "#1A1C1E" }

    override val privacyMode: Flow<Boolean> = dataStore.data.map { it[PRIVACY_MODE] ?: false }
    override val autoReconnect: Flow<Boolean> = dataStore.data.map { it[AUTO_RECONNECT] ?: true }
    override val biometricLock: Flow<Boolean> = dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }
    override val onboardingCompleted: Flow<Boolean> = dataStore.data.map { it[ONBOARDING_COMPLETED] ?: false }

    override suspend fun setThemeMode(mode: String) { dataStore.edit { it[THEME_MODE] = mode } }
    override suspend fun setLanguage(lang: String) { dataStore.edit { it[LANGUAGE] = lang } }
    override suspend fun setFontFamily(family: String) { dataStore.edit { it[FONT_FAMILY] = family } }
    override suspend fun setFontSize(size: String) { dataStore.edit { it[FONT_SIZE] = size } }
    override suspend fun setTimeout(value: Int) { dataStore.edit { it[TIMEOUT] = value } }
    override suspend fun setRebootConfirm(value: String) { dataStore.edit { it[REBOOT_CONFIRM] = value } }

    override suspend fun setTermBgColor(color: String) { dataStore.edit { it[TERM_BG_COLOR] = color } }
    override suspend fun setTermTextColor(color: String) { dataStore.edit { it[TERM_TEXT_COLOR] = color } }
    override suspend fun setTermFontSizePx(size: Float) { dataStore.edit { it[TERM_FONT_SIZE_PX] = size } }

    suspend fun setWidgetBgColor(color: String) { dataStore.edit { it[WIDGET_BG_COLOR] = color } }
    suspend fun setWidgetTextColor(color: String) { dataStore.edit { it[WIDGET_TEXT_COLOR] = color } }
    suspend fun setWidgetAccentColor(color: String) { dataStore.edit { it[WIDGET_ACCENT_COLOR] = color } }
    suspend fun setWidgetItemBgColor(color: String) { dataStore.edit { it[WIDGET_ITEM_BG_COLOR] = color } }

    override suspend fun setPrivacyMode(enabled: Boolean) { dataStore.edit { it[PRIVACY_MODE] = enabled } }
    override suspend fun setAutoReconnect(enabled: Boolean) { dataStore.edit { it[AUTO_RECONNECT] = enabled } }
    override suspend fun setBiometricLock(enabled: Boolean) { dataStore.edit { it[BIOMETRIC_LOCK] = enabled } }
    override suspend fun setOnboardingCompleted(completed: Boolean) { dataStore.edit { it[ONBOARDING_COMPLETED] = completed } }
}

package com.neytron.sshcommander.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {
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

        // New Widget Color Preferences
        val WIDGET_BG_COLOR = stringPreferencesKey("widget_bg_color")
        val WIDGET_TEXT_COLOR = stringPreferencesKey("widget_text_color")
        val WIDGET_ACCENT_COLOR = stringPreferencesKey("widget_accent_color")
        val WIDGET_ITEM_BG_COLOR = stringPreferencesKey("widget_item_bg_color")

        val PRIVACY_MODE = booleanPreferencesKey("privacy_mode")
        val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
    }

    val themeMode: Flow<String> = dataStore.data.map { it[THEME_MODE] ?: "system" }
    val language: Flow<String> = dataStore.data.map { it[LANGUAGE] ?: "en" }
    val fontFamily: Flow<String> = dataStore.data.map { it[FONT_FAMILY] ?: "monospace" }
    val fontSize: Flow<String> = dataStore.data.map { it[FONT_SIZE] ?: "medium" }
    val timeout: Flow<Int> = dataStore.data.map { it[TIMEOUT] ?: 10 }
    val rebootConfirm: Flow<String> = dataStore.data.map { it[REBOOT_CONFIRM] ?: "always" }
    
    val termBgColor: Flow<String> = dataStore.data.map { it[TERM_BG_COLOR] ?: "#000000" }
    val termTextColor: Flow<String> = dataStore.data.map { it[TERM_TEXT_COLOR] ?: "#00FF00" }
    val termFontSizePx: Flow<Float> = dataStore.data.map { it[TERM_FONT_SIZE_PX] ?: 14f }

    // Defaults for Widget: Deep Dark Theme
    val widgetBgColor: Flow<String> = dataStore.data.map { it[WIDGET_BG_COLOR] ?: "#0A0C0E" }
    val widgetTextColor: Flow<String> = dataStore.data.map { it[WIDGET_TEXT_COLOR] ?: "#FFFFFF" }
    val widgetAccentColor: Flow<String> = dataStore.data.map { it[WIDGET_ACCENT_COLOR] ?: "#03DAC6" }
    val widgetItemBgColor: Flow<String> = dataStore.data.map { it[WIDGET_ITEM_BG_COLOR] ?: "#1A1C1E" }

    val privacyMode: Flow<Boolean> = dataStore.data.map { it[PRIVACY_MODE] ?: false }
    val autoReconnect: Flow<Boolean> = dataStore.data.map { it[AUTO_RECONNECT] ?: true }
    val biometricLock: Flow<Boolean> = dataStore.data.map { it[BIOMETRIC_LOCK] ?: false }

    suspend fun setThemeMode(mode: String) { dataStore.edit { it[THEME_MODE] = mode } }
    suspend fun setLanguage(lang: String) { dataStore.edit { it[LANGUAGE] = lang } }
    suspend fun setFontFamily(family: String) { dataStore.edit { it[FONT_FAMILY] = family } }
    suspend fun setFontSize(size: String) { dataStore.edit { it[FONT_SIZE] = size } }
    suspend fun setTimeout(seconds: Int) { dataStore.edit { it[TIMEOUT] = seconds } }
    suspend fun setRebootConfirm(mode: String) { dataStore.edit { it[REBOOT_CONFIRM] = mode } }
    suspend fun setTermBgColor(color: String) { dataStore.edit { it[TERM_BG_COLOR] = color } }
    suspend fun setTermTextColor(color: String) { dataStore.edit { it[TERM_TEXT_COLOR] = color } }
    suspend fun setTermFontSizePx(size: Float) { dataStore.edit { it[TERM_FONT_SIZE_PX] = size } }

    suspend fun setWidgetBgColor(color: String) { dataStore.edit { it[WIDGET_BG_COLOR] = color } }
    suspend fun setWidgetTextColor(color: String) { dataStore.edit { it[WIDGET_TEXT_COLOR] = color } }
    suspend fun setWidgetAccentColor(color: String) { dataStore.edit { it[WIDGET_ACCENT_COLOR] = color } }
    suspend fun setWidgetItemBgColor(color: String) { dataStore.edit { it[WIDGET_ITEM_BG_COLOR] = color } }

    suspend fun setPrivacyMode(enabled: Boolean) { dataStore.edit { it[PRIVACY_MODE] = enabled } }
    suspend fun setAutoReconnect(enabled: Boolean) { dataStore.edit { it[AUTO_RECONNECT] = enabled } }
    suspend fun setBiometricLock(enabled: Boolean) { dataStore.edit { it[BIOMETRIC_LOCK] = enabled } }
}

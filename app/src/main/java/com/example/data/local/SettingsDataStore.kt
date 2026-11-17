package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore by preferencesDataStore(name = "sql_learning_settings")

@Serializable
data class AppSettings(
    val appThemeMode: String = "light",
    val editorFontSizeSp: Int = 14,
    val editorDarkTheme: Boolean = false,
    val autoPair: Boolean = true,
    val autoUppercase: Boolean = false,
    val autoIndent: Boolean = true,
    val hapticFeedback: Boolean = true,
    val defaultKeyboardMode: String = "syntax",
    val desiredRetention: Double = 0.90,
    val dailyNewCardLimit: Int = 20,
    val lastKpId: String = "kp01-01",
    val lastDatasetId: String = "retail",
    val lastOfflineCheckAtMs: Long = 0L,
    val lastOfflineCheckPassed: Boolean = false,
    val lastOfflineFileCount: Int = 0
)

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val APP_THEME_MODE = stringPreferencesKey("app_theme_mode")
        val EDITOR_FONT_SIZE = intPreferencesKey("editor_font_size_sp")
        val EDITOR_DARK_THEME = booleanPreferencesKey("editor_dark_theme")
        val AUTO_PAIR = booleanPreferencesKey("auto_pair")
        val AUTO_UPPERCASE = booleanPreferencesKey("auto_uppercase")
        val AUTO_INDENT = booleanPreferencesKey("auto_indent")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val DEFAULT_KEYBOARD_MODE = stringPreferencesKey("default_keyboard_mode")
        val DESIRED_RETENTION = doublePreferencesKey("desired_retention")
        val DAILY_NEW_CARD_LIMIT = intPreferencesKey("daily_new_card_limit")
        val LAST_KP_ID = stringPreferencesKey("last_kp_id")
        val LAST_DATASET_ID = stringPreferencesKey("last_dataset_id")
        val LAST_OFFLINE_CHECK_AT = longPreferencesKey("last_offline_check_at")
        val LAST_OFFLINE_PASSED = booleanPreferencesKey("last_offline_passed")
        val LAST_OFFLINE_COUNT = intPreferencesKey("last_offline_count")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            appThemeMode = prefs[Keys.APP_THEME_MODE] ?: "light",
            editorFontSizeSp = prefs[Keys.EDITOR_FONT_SIZE] ?: 14,
            editorDarkTheme = prefs[Keys.EDITOR_DARK_THEME] ?: false,
            autoPair = prefs[Keys.AUTO_PAIR] ?: true,
            autoUppercase = prefs[Keys.AUTO_UPPERCASE] ?: false,
            autoIndent = prefs[Keys.AUTO_INDENT] ?: true,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
            defaultKeyboardMode = prefs[Keys.DEFAULT_KEYBOARD_MODE] ?: "syntax",
            desiredRetention = prefs[Keys.DESIRED_RETENTION] ?: 0.90,
            dailyNewCardLimit = prefs[Keys.DAILY_NEW_CARD_LIMIT] ?: 20,
            lastKpId = prefs[Keys.LAST_KP_ID] ?: "kp01-01",
            lastDatasetId = prefs[Keys.LAST_DATASET_ID] ?: "retail",
            lastOfflineCheckAtMs = prefs[Keys.LAST_OFFLINE_CHECK_AT] ?: 0L,
            lastOfflineCheckPassed = prefs[Keys.LAST_OFFLINE_PASSED] ?: false,
            lastOfflineFileCount = prefs[Keys.LAST_OFFLINE_COUNT] ?: 0
        )
    }

    suspend fun current(): AppSettings = settingsFlow.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = AppSettings(
                appThemeMode = prefs[Keys.APP_THEME_MODE] ?: "light",
                editorFontSizeSp = prefs[Keys.EDITOR_FONT_SIZE] ?: 14,
                editorDarkTheme = prefs[Keys.EDITOR_DARK_THEME] ?: false,
                autoPair = prefs[Keys.AUTO_PAIR] ?: true,
                autoUppercase = prefs[Keys.AUTO_UPPERCASE] ?: false,
                autoIndent = prefs[Keys.AUTO_INDENT] ?: true,
                hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: true,
                defaultKeyboardMode = prefs[Keys.DEFAULT_KEYBOARD_MODE] ?: "syntax",
                desiredRetention = prefs[Keys.DESIRED_RETENTION] ?: 0.90,
                dailyNewCardLimit = prefs[Keys.DAILY_NEW_CARD_LIMIT] ?: 20,
                lastKpId = prefs[Keys.LAST_KP_ID] ?: "kp01-01",
                lastDatasetId = prefs[Keys.LAST_DATASET_ID] ?: "retail",
                lastOfflineCheckAtMs = prefs[Keys.LAST_OFFLINE_CHECK_AT] ?: 0L,
                lastOfflineCheckPassed = prefs[Keys.LAST_OFFLINE_PASSED] ?: false,
                lastOfflineFileCount = prefs[Keys.LAST_OFFLINE_COUNT] ?: 0
            )
            val next = transform(current)
            prefs[Keys.APP_THEME_MODE] = next.appThemeMode
            prefs[Keys.EDITOR_FONT_SIZE] = next.editorFontSizeSp
            prefs[Keys.EDITOR_DARK_THEME] = next.editorDarkTheme
            prefs[Keys.AUTO_PAIR] = next.autoPair
            prefs[Keys.AUTO_UPPERCASE] = next.autoUppercase
            prefs[Keys.AUTO_INDENT] = next.autoIndent
            prefs[Keys.HAPTIC_FEEDBACK] = next.hapticFeedback
            prefs[Keys.DEFAULT_KEYBOARD_MODE] = next.defaultKeyboardMode
            prefs[Keys.DESIRED_RETENTION] = next.desiredRetention
            prefs[Keys.DAILY_NEW_CARD_LIMIT] = next.dailyNewCardLimit
            prefs[Keys.LAST_KP_ID] = next.lastKpId
            prefs[Keys.LAST_DATASET_ID] = next.lastDatasetId
            prefs[Keys.LAST_OFFLINE_CHECK_AT] = next.lastOfflineCheckAtMs
            prefs[Keys.LAST_OFFLINE_PASSED] = next.lastOfflineCheckPassed
            prefs[Keys.LAST_OFFLINE_COUNT] = next.lastOfflineFileCount
        }
    }
}

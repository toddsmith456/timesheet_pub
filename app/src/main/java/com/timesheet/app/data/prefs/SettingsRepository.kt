package com.timesheet.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** How the alarm announces itself when a job reminder fires. */
enum class AlarmStyle { TONE, VIBRATE, SILENT }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Material You dynamic color (Android 12+). */
    val dynamicColor: Boolean = true,
    val alarmStyle: AlarmStyle = AlarmStyle.TONE,
    /** Null = system default alarm tone. */
    val alarmToneUri: String? = null,
    val vibrateWithTone: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 10
) {
    companion object {
        val Default = AppSettings()
    }
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val ALARM_STYLE = stringPreferencesKey("alarm_style")
        val ALARM_TONE_URI = stringPreferencesKey("alarm_tone_uri")
        val VIBRATE_WITH_TONE = booleanPreferencesKey("vibrate_with_tone")
        val SNOOZE_ENABLED = booleanPreferencesKey("snooze_enabled")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            themeMode = prefs[Keys.THEME_MODE]
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            alarmStyle = prefs[Keys.ALARM_STYLE]
                ?.let { runCatching { AlarmStyle.valueOf(it) }.getOrNull() }
                ?: AlarmStyle.TONE,
            alarmToneUri = prefs[Keys.ALARM_TONE_URI],
            vibrateWithTone = prefs[Keys.VIBRATE_WITH_TONE] ?: true,
            snoozeEnabled = prefs[Keys.SNOOZE_ENABLED] ?: true,
            snoozeMinutes = prefs[Keys.SNOOZE_MINUTES] ?: 10
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setDynamicColor(enabled: Boolean) = edit { it[Keys.DYNAMIC_COLOR] = enabled }

    suspend fun setAlarmStyle(style: AlarmStyle) = edit { it[Keys.ALARM_STYLE] = style.name }

    suspend fun setAlarmToneUri(uri: String?) = edit {
        if (uri == null) it.remove(Keys.ALARM_TONE_URI) else it[Keys.ALARM_TONE_URI] = uri
    }

    suspend fun setVibrateWithTone(enabled: Boolean) = edit { it[Keys.VIBRATE_WITH_TONE] = enabled }

    suspend fun setSnoozeEnabled(enabled: Boolean) = edit { it[Keys.SNOOZE_ENABLED] = enabled }

    suspend fun setSnoozeMinutes(minutes: Int) = edit { it[Keys.SNOOZE_MINUTES] = minutes }

    private suspend fun edit(block: (MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }
}

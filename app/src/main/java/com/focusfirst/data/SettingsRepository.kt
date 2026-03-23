package com.focusfirst.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.focusFirstSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "focusfirst_settings",
)

class SettingsRepository(
    private val appContext: Context,
    private val dataStore: DataStore<Preferences>,
) {

    private val legacyPrefs =
        appContext.getSharedPreferences(FOCUS_FIRST_PREFS, Context.MODE_PRIVATE)

    companion object {
        const val FOCUS_FIRST_PREFS = "focusfirst_prefs"
        const val LEGACY_VIBRATE_ENABLED = "vibrate_enabled"
        const val LEGACY_SOUND_TYPE = "sound_type"

        val KEY_FOCUS_MINUTES = intPreferencesKey("KEY_FOCUS_MINUTES")
        val KEY_SHORT_BREAK = intPreferencesKey("KEY_SHORT_BREAK")
        val KEY_LONG_BREAK = intPreferencesKey("KEY_LONG_BREAK")
        val KEY_SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("KEY_SESSIONS_BEFORE_LONG_BREAK")
        val KEY_SOUND_TYPE = stringPreferencesKey("KEY_SOUND_TYPE")
        val KEY_VIBRATE = booleanPreferencesKey("KEY_VIBRATE")
        val KEY_THEME_MODE = stringPreferencesKey("KEY_THEME_MODE")
        val KEY_AMOLED_MODE = booleanPreferencesKey("KEY_AMOLED_MODE")
        val KEY_BATTERY_PROMPT_DISMISSED = booleanPreferencesKey("KEY_BATTERY_PROMPT_DISMISSED")
        val KEY_NOTIFICATION_PERMISSION_ASKED =
            booleanPreferencesKey("KEY_NOTIFICATION_PERMISSION_ASKED")
        val KEY_PRO_UNLOCKED = booleanPreferencesKey("KEY_PRO_UNLOCKED")
        val KEY_DAILY_GOAL = intPreferencesKey("KEY_DAILY_GOAL")
        val KEY_AUTO_START = booleanPreferencesKey("KEY_AUTO_START")
    }

    val focusMinutes: Flow<Int> = dataStore.data.map { it[KEY_FOCUS_MINUTES] ?: 25 }

    val shortBreakMinutes: Flow<Int> = dataStore.data.map { it[KEY_SHORT_BREAK] ?: 5 }

    val longBreakMinutes: Flow<Int> = dataStore.data.map { it[KEY_LONG_BREAK] ?: 15 }

    val sessionsBeforeLongBreak: Flow<Int> =
        dataStore.data.map { it[KEY_SESSIONS_BEFORE_LONG_BREAK] ?: 4 }

    val soundType: Flow<String> = dataStore.data.map { it[KEY_SOUND_TYPE] ?: "Bell" }

    val vibrate: Flow<Boolean> = dataStore.data.map { it[KEY_VIBRATE] ?: true }

    val themeMode: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "System" }

    val amoledMode: Flow<Boolean> = dataStore.data.map { it[KEY_AMOLED_MODE] ?: false }

    val batteryPromptDismissed: Flow<Boolean> =
        dataStore.data.map { it[KEY_BATTERY_PROMPT_DISMISSED] ?: false }

    val notificationPermissionAsked: Flow<Boolean> =
        dataStore.data.map { it[KEY_NOTIFICATION_PERMISSION_ASKED] ?: false }

    val proUnlocked: Flow<Boolean> = dataStore.data.map { it[KEY_PRO_UNLOCKED] ?: false }

    val dailyGoal: Flow<Int> = dataStore.data.map { prefs ->
        (prefs[KEY_DAILY_GOAL] ?: 8).coerceIn(1, 20)
    }

    val autoStart: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_START] ?: false }

    suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
        when (key.name) {
            KEY_SOUND_TYPE.name ->
                legacyPrefs.edit().putString(LEGACY_SOUND_TYPE, value as String).apply()

            KEY_VIBRATE.name ->
                legacyPrefs.edit().putBoolean(LEGACY_VIBRATE_ENABLED, value as Boolean).apply()
        }
    }
}

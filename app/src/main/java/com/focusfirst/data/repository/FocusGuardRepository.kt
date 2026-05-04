package com.focusfirst.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * FocusGuardRepository
 *
 * Single source of truth for the set of blocked app package names.
 *
 * Storage strategy:
 *   • DataStore: canonical storage — UI reads from here (Flow-based reactive)
 *   • SharedPreferences: mirror written on every mutation so the
 *     ForegroundService can read the list synchronously without a coroutine.
 */
@Singleton
class FocusGuardRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    companion object {
        const val PREF_FOCUS_GUARD_ACTIVE = "focus_guard_active"
        const val PREF_BLOCKED_APPS = "blocked_apps"
        const val PREF_REMAINING_SECONDS = "focus_guard_remaining_seconds"

        /** Pipe-separated package names, e.g. "com.instagram.android|com.twitter.android" */
        val KEY_BLOCKED_APPS = stringPreferencesKey("focus_guard_blocked_apps")
    }

    /** Reactive flow of blocked package names. */
    val blockedApps: Flow<Set<String>> = dataStore.data.map { prefs ->
        val raw = prefs[KEY_BLOCKED_APPS] ?: ""
        if (raw.isBlank()) emptySet() else raw.split("|").toSet()
    }

    /**
     * Replace the entire blocked-apps list.
     * Also mirrors to SharedPreferences for the AccessibilityService.
     */
    suspend fun setBlockedApps(packages: Set<String>) {
        val joined = packages.joinToString("|")
        dataStore.edit { it[KEY_BLOCKED_APPS] = joined }
        mirrorToSharedPrefs(packages)
    }

    /**
     * Toggle a single package name in the blocked set.
     * Returns the new blocked set after toggling.
     */
    suspend fun toggleApp(packageName: String, currentSet: Set<String>): Set<String> {
        val newSet = if (packageName in currentSet) {
            currentSet - packageName
        } else {
            currentSet + packageName
        }
        setBlockedApps(newSet)
        return newSet
    }

    /** Clear all blocked apps. */
    suspend fun clearAll() = setBlockedApps(emptySet())

    // ─────────────────────────────────────────────────────────────────────────
    // Private — SharedPreferences mirror for cross-process accessibility service
    // ─────────────────────────────────────────────────────────────────────────

    private fun mirrorToSharedPrefs(packages: Set<String>) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putStringSet(PREF_BLOCKED_APPS, packages)
            .apply()
    }
}

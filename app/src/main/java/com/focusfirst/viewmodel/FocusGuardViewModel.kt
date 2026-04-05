package com.focusfirst.viewmodel

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusfirst.data.model.AppInfo
import com.focusfirst.data.repository.FocusGuardRepository
import com.focusfirst.service.FocusGuardAccessibilityService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// FocusGuardViewModel
// ─────────────────────────────────────────────────────────────────────────────

/**
 * ViewModel for the Focus Guard settings screen.
 *
 * Responsibilities:
 *  • Load the list of user-installed apps (off main thread)
 *  • Expose blocked-apps state from [FocusGuardRepository]
 *  • Check PACKAGE_USAGE_STATS and Accessibility Service permissions
 *  • Delegate toggle + preset mutations to the repository
 */
@HiltViewModel
class FocusGuardViewModel @Inject constructor(
    private val application: Application,
    private val repository:  FocusGuardRepository,
) : ViewModel() {

    // ── Installed apps ────────────────────────────────────────────────────────

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _appsLoading = MutableStateFlow(true)
    val appsLoading: StateFlow<Boolean> = _appsLoading.asStateFlow()

    // ── Blocked apps (from repository) ────────────────────────────────────────

    val blockedApps: StateFlow<Set<String>> = repository.blockedApps
        .stateIn(
            scope          = viewModelScope,
            started        = SharingStarted.WhileSubscribed(5_000L),
            initialValue   = emptySet(),
        )

    // ── Permission flags ──────────────────────────────────────────────────────

    private val _isAccessibilityEnabled = MutableStateFlow(false)
    val isAccessibilityEnabled: StateFlow<Boolean> = _isAccessibilityEnabled.asStateFlow()

    private val _hasUsagePermission = MutableStateFlow(false)
    val hasUsagePermission: StateFlow<Boolean> = _hasUsagePermission.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────

    init {
        loadInstalledApps()
        refreshPermissions()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun toggleApp(packageName: String) {
        viewModelScope.launch {
            val current = blockedApps.value
            repository.toggleApp(packageName, current)
        }
    }

    /** Apply a Social-Media preset (well-known package names). */
    fun applyPresetSocialMedia() {
        viewModelScope.launch {
            val preset = setOf(
                "com.instagram.android",
                "com.facebook.katana",
                "com.twitter.android",
                "com.zhiliaoapp.musically",   // TikTok
                "com.snapchat.android",
                "com.reddit.frontpage",
                "com.linkedin.android",
                "com.pinterest",
            )
            val current  = blockedApps.value
            val newSet   = current + preset
            repository.setBlockedApps(newSet)
        }
    }

    /** Apply a Games preset. */
    fun applyPresetGames() {
        viewModelScope.launch {
            // Match any installed package whose application category is GAME
            val pm        = application.packageManager
            val gameApps  = installedApps.value.filter { app ->
                runCatching {
                    val info = pm.getApplicationInfo(app.packageName, 0)
                    info.category == android.content.pm.ApplicationInfo.CATEGORY_GAME
                }.getOrDefault(false)
            }.map { it.packageName }.toSet()

            repository.setBlockedApps(blockedApps.value + gameApps)
        }
    }

    /** Apply an Entertainment preset. */
    fun applyPresetEntertainment() {
        viewModelScope.launch {
            val preset = setOf(
                "com.google.android.youtube",
                "com.netflix.mediaclient",
                "com.amazon.avod.thirdpartyclient",
                "com.disney.disneyplus",
                "tv.twitch.android.app",
                "com.spotify.music",
            )
            repository.setBlockedApps(blockedApps.value + preset)
        }
    }

    fun clearAll() {
        viewModelScope.launch { repository.clearAll() }
    }

    /** Re-check permission status (call from onResume). */
    fun refreshPermissions() {
        _isAccessibilityEnabled.value = checkAccessibilityEnabled()
        _hasUsagePermission.value     = checkUsagePermission()
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadInstalledApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm    = application.packageManager
                val flags = PackageManager.GET_META_DATA
                pm.getInstalledApplications(flags)
                    .filter { info ->
                        // Skip system apps and ourselves
                        info.packageName != application.packageName &&
                        (info.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM == 0 ||
                         pm.getLaunchIntentForPackage(info.packageName) != null)
                    }
                    .mapNotNull { info ->
                        // Only include apps that have a launcher entry (user-visible)
                        if (pm.getLaunchIntentForPackage(info.packageName) == null) null
                        else AppInfo(
                            packageName = info.packageName,
                            appName     = pm.getApplicationLabel(info).toString(),
                            icon        = runCatching { pm.getApplicationIcon(info.packageName) }.getOrNull(),
                        )
                    }
                    .sortedBy { it.appName.lowercase() }
            }
            _installedApps.value = apps
            _appsLoading.value   = false
        }
    }

    private fun checkAccessibilityEnabled(): Boolean {
        val am = application.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = Settings.Secure.getString(
            application.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val serviceId = "${application.packageName}/${FocusGuardAccessibilityService::class.java.name}"
        return enabledServices.split(":").any { it.equals(serviceId, ignoreCase = true) }
    }

    private fun checkUsagePermission(): Boolean {
        return runCatching {
            val appOps = application.getSystemService(Context.APP_OPS_SERVICE)
                    as android.app.AppOpsManager
            val mode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    application.packageName,
                )
            } else {
                @Suppress("DEPRECATION")
                appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    application.packageName,
                )
            }
            mode == android.app.AppOpsManager.MODE_ALLOWED
        }.getOrDefault(false)
    }
}

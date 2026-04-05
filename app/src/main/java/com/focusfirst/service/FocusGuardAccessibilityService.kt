package com.focusfirst.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import androidx.preference.PreferenceManager
import com.focusfirst.ui.screens.BlockedActivity

/**
 * FocusGuardAccessibilityService
 *
 * Monitors foreground window changes via TYPE_WINDOW_STATE_CHANGED.
 * When Focus Guard is active and the user opens a blocked app, this service:
 *  1. Launches BlockedActivity as an overlay (full-screen interstitial)
 *  2. Performs GLOBAL_ACTION_BACK to evict the blocked app from the foreground
 *
 * Activation flag and blocked-app list are stored in the default
 * SharedPreferences so they are readable across process boundaries
 * (this service may run in a separate process).
 *
 * NOTE — SharedPreferences keys must stay in sync with:
 *   • TimerViewModel  (writes focus_guard_active)
 *   • FocusGuardRepository  (writes blocked_apps)
 *   • BlockedActivity  (reads focus_guard_active to reset on panic-escape)
 */
class FocusGuardAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "FocusGuardService"

        /** Set to true by TimerViewModel when a FOCUS phase starts. */
        const val PREF_FOCUS_GUARD_ACTIVE = "focus_guard_active"

        /** StringSet of package names that should be blocked. */
        const val PREF_BLOCKED_APPS = "blocked_apps"

        /** Remaining seconds in the current session (written by TimerViewModel). */
        const val PREF_REMAINING_SECONDS = "focus_guard_remaining_seconds"
    }

    override fun onServiceConnected() {
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes  = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags        = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100L
        }
        Log.d(TAG, "onServiceConnected — Focus Guard accessibility service ready")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        // Only act when Focus Guard is currently active
        if (!isFocusGuardActive()) return

        val packageName = event.packageName?.toString() ?: return

        // Never block ourselves or system UI
        if (packageName == this.packageName) return
        if (packageName == "com.android.systemui") return
        if (packageName == "android") return

        if (isBlockedApp(packageName)) {
            Log.d(TAG, "Blocked app detected: $packageName — showing overlay")
            showBlockOverlay(packageName)
            // Give the overlay a moment to appear before going back
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "onInterrupt")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun isFocusGuardActive(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        return prefs.getBoolean(PREF_FOCUS_GUARD_ACTIVE, false)
    }

    private fun isBlockedApp(pkg: String): Boolean {
        val prefs   = PreferenceManager.getDefaultSharedPreferences(this)
        val blocked = prefs.getStringSet(PREF_BLOCKED_APPS, emptySet()) ?: emptySet()
        return pkg in blocked
    }

    private fun showBlockOverlay(blockedPackage: String) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockedActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)
        }
        startActivity(intent)
    }
}

package com.focusfirst.analytics

import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import com.google.firebase.analytics.logEvent

/**
 * TokiAnalytics
 *
 * Thin wrapper around [FirebaseAnalytics] that keeps all event names + param
 * keys in one place. Every method is a no-op-safe fire-and-forget call — if
 * Firebase is not fully initialised the SDK queues the event internally, so
 * no defensive try/catch is needed here.
 */
object TokiAnalytics {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    // ── Session lifecycle ─────────────────────────────────────────────────────

    fun logSessionStarted(
        durationMinutes: Int,
        soundEnabled:    Boolean,
        dndEnabled:      Boolean,
        hasTask:         Boolean,
    ) {
        analytics.logEvent("session_started") {
            param("duration_minutes", durationMinutes.toLong())
            param("sound_enabled",   if (soundEnabled) "yes" else "no")
            param("dnd_enabled",     if (dndEnabled)   "yes" else "no")
            param("has_task",        if (hasTask)       "yes" else "no")
        }
    }

    fun logSessionCompleted(
        durationMinutes: Int,
        sessionNumber:   Int,
    ) {
        analytics.logEvent("session_completed") {
            param("duration_minutes", durationMinutes.toLong())
            param("session_number",   sessionNumber.toLong())
        }
    }

    fun logSessionAbandoned(
        durationMinutes: Int,
        elapsedMinutes:  Int,
    ) {
        analytics.logEvent("session_abandoned") {
            param("duration_minutes", durationMinutes.toLong())
            param("elapsed_minutes",  elapsedMinutes.toLong())
        }
    }

    // ── Sound ─────────────────────────────────────────────────────────────────

    fun logSoundSelected(soundName: String) {
        analytics.logEvent("sound_selected") {
            param("sound_name", soundName)
        }
    }

    // ── Billing ───────────────────────────────────────────────────────────────

    fun logUpgradeScreenViewed() = analytics.logEvent("upgrade_screen_viewed", null)
    fun logUpgradePurchased()    = analytics.logEvent("upgrade_purchased",      null)
    fun logUpgradeCancelled()    = analytics.logEvent("upgrade_cancelled",      null)

    // ── DND ───────────────────────────────────────────────────────────────────

    fun logDndToggled(enabled: Boolean) {
        analytics.logEvent("dnd_toggled") {
            param("enabled", if (enabled) "yes" else "no")
        }
    }

    // ── Misc events ───────────────────────────────────────────────────────────

    fun logBreakSuggestionDismissed() = analytics.logEvent("break_suggestion_dismissed", null)
    fun logExportTriggered()          = analytics.logEvent("csv_export_triggered",        null)

    fun logTabViewed(tabName: String) {
        analytics.logEvent("tab_viewed") {
            param("tab_name", tabName)
        }
    }

    fun logStreakMilestone(days: Int) {
        analytics.logEvent("streak_milestone") {
            param("streak_days", days.toLong())
        }
    }

    // ── User properties ───────────────────────────────────────────────────────

    /**
     * Call after every session completes (and in [onCleared]) so the cohort
     * properties stay fresh without requiring a separate dedicated call site.
     */
    fun setUserProperties(
        isPro:          Boolean,
        totalSessions:  Int,
        streakDays:     Int,
    ) {
        analytics.setUserProperty(
            "is_pro",
            if (isPro) "true" else "false",
        )
        analytics.setUserProperty(
            "session_range",
            when {
                totalSessions < 10  -> "0-9"
                totalSessions < 50  -> "10-49"
                totalSessions < 100 -> "50-99"
                totalSessions < 500 -> "100-499"
                else                -> "500+"
            },
        )
        analytics.setUserProperty(
            "streak_range",
            when {
                streakDays == 0  -> "0"
                streakDays < 7   -> "1-6"
                streakDays < 30  -> "7-29"
                else             -> "30+"
            },
        )
    }
}

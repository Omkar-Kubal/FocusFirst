package com.focusfirst.util

import com.focusfirst.data.db.SessionEntity
import com.focusfirst.data.model.AllBadges
import java.util.Calendar

/**
 * BadgeEvaluator
 *
 * Pure, stateless evaluation of which badges a user has just earned.
 * Called after every session save (in TimerViewModel coroutine scope).
 *
 * Only returns badge IDs that are **newly** unlocked — IDs already in
 * [alreadyUnlocked] are filtered out so callers can persist + notify
 * without duplication.
 *
 * All session history is passed in so this function remains testable
 * without a database or Android context.
 */
object BadgeEvaluator {

    /**
     * @param sessions       Full session history, any order.
     * @param currentStreak  Current consecutive-day streak (from TimerViewModel).
     * @param alreadyUnlocked Badge IDs that have already been persisted.
     * @return List of badge IDs that are newly earned by this evaluation.
     */
    fun evaluate(
        sessions:        List<SessionEntity>,
        currentStreak:   Int,
        alreadyUnlocked: Set<String>,
    ): List<String> {
        val completed = sessions.filter { it.wasCompleted }
        val totalCompleted = completed.size

        val newIds = mutableListOf<String>()

        fun maybeUnlock(id: String, condition: Boolean) {
            if (condition && id !in alreadyUnlocked) newIds += id
        }

        // ── First session ─────────────────────────────────────────────────
        maybeUnlock("first_step", totalCompleted >= 1)

        // ── Streak badges ─────────────────────────────────────────────────
        maybeUnlock("on_a_roll",    currentStreak >= 3)
        maybeUnlock("week_warrior", currentStreak >= 7)
        maybeUnlock("unstoppable",  currentStreak >= 30)

        // ── Session count ─────────────────────────────────────────────────
        maybeUnlock("getting_started", totalCompleted >= 10)
        maybeUnlock("focused",         totalCompleted >= 50)
        maybeUnlock("deep_worker",     totalCompleted >= 100)
        maybeUnlock("zen_master",      totalCompleted >= 500)

        // ── Single-day challenges ─────────────────────────────────────────
        // Group completed sessions by calendar day (UTC epoch-day)
        val sessionsByDay: Map<Long, List<SessionEntity>> = completed.groupBy {
            it.startedAt / 86_400_000L
        }

        val maxInADay = sessionsByDay.values.maxOfOrNull { it.size } ?: 0
        maybeUnlock("flow_state", maxInADay >= 8)
        maybeUnlock("marathon",   maxInADay >= 12)

        // Deep Work Sunday: ≥ 8 completed sessions on any Sunday
        val hasSunday = sessionsByDay.any { (epochDay, daySessions) ->
            if (daySessions.size < 8) return@any false
            val cal = Calendar.getInstance().apply {
                timeInMillis = epochDay * 86_400_000L
            }
            cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
        }
        maybeUnlock("deep_work_sunday", hasSunday)

        // ── Time-of-day badges ────────────────────────────────────────────
        // Early Bird: any completed session that started before 07:00 local time
        val hasEarlyBird = completed.any { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startedAt }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour < 7
        }
        maybeUnlock("early_bird", hasEarlyBird)

        // Night Owl: any completed session that started at or after 22:00
        val hasNightOwl = completed.any { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startedAt }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour >= 22
        }
        maybeUnlock("night_owl", hasNightOwl)

        // Lunchtime Focus: any completed session that started between 12:00 and 12:59
        val hasLunch = completed.any { session ->
            val cal = Calendar.getInstance().apply { timeInMillis = session.startedAt }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hour == 12
        }
        maybeUnlock("lunchtime_focus", hasLunch)

        return newIds
    }
}

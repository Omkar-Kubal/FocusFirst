package com.focusfirst.data.model

/**
 * Represents a single achievement badge.
 *
 * @param id          Stable identifier used for DataStore serialization.
 * @param name        Display name shown on the badge card.
 * @param emoji       Emoji rendered large on the card.
 * @param description Short human-readable unlock condition.
 * @param isUnlocked  Whether the current user has earned this badge.
 * @param unlockedAt  Epoch-milliseconds when it was first earned (null = locked).
 */
data class Badge(
    val id:          String,
    val name:        String,
    val emoji:       String,
    val description: String,
    val isUnlocked:  Boolean = false,
    val unlockedAt:  Long?   = null,
)

/**
 * Master list of all achievement badges shipped with Toki.
 *
 * IDs are intentionally stable strings — never rename them once released
 * because they are persisted in DataStore as the unlock record.
 */
object AllBadges {

    val list: List<Badge> = listOf(

        // ── First session ──────────────────────────────────────────────────
        Badge(
            id          = "first_step",
            name        = "First Step",
            emoji       = "🌱",
            description = "Complete your very first focus session",
        ),

        // ── Streak badges ──────────────────────────────────────────────────
        Badge(
            id          = "on_a_roll",
            name        = "On a Roll",
            emoji       = "🔥",
            description = "Maintain a 3-day focus streak",
        ),
        Badge(
            id          = "week_warrior",
            name        = "Week Warrior",
            emoji       = "⚡",
            description = "Maintain a 7-day focus streak",
        ),
        Badge(
            id          = "unstoppable",
            name        = "Unstoppable",
            emoji       = "💎",
            description = "Maintain a 30-day focus streak",
        ),

        // ── Session count ──────────────────────────────────────────────────
        Badge(
            id          = "getting_started",
            name        = "Getting Started",
            emoji       = "🍅",
            description = "Complete 10 focus sessions",
        ),
        Badge(
            id          = "focused",
            name        = "Focused",
            emoji       = "🎯",
            description = "Complete 50 focus sessions",
        ),
        Badge(
            id          = "deep_worker",
            name        = "Deep Worker",
            emoji       = "🧠",
            description = "Complete 100 focus sessions",
        ),
        Badge(
            id          = "zen_master",
            name        = "Zen Master",
            emoji       = "🏆",
            description = "Complete 500 focus sessions",
        ),

        // ── Single-day challenges ──────────────────────────────────────────
        Badge(
            id          = "flow_state",
            name        = "Flow State",
            emoji       = "🌊",
            description = "Complete 8 sessions in a single day",
        ),
        Badge(
            id          = "deep_work_sunday",
            name        = "Deep Work Sunday",
            emoji       = "☀️",
            description = "Complete 8 sessions on a Sunday",
        ),
        Badge(
            id          = "marathon",
            name        = "Marathon",
            emoji       = "🏃",
            description = "Complete 12 sessions in a single day",
        ),

        // ── Time-of-day badges ─────────────────────────────────────────────
        Badge(
            id          = "early_bird",
            name        = "Early Bird",
            emoji       = "🌅",
            description = "Start a focus session before 7 AM",
        ),
        Badge(
            id          = "night_owl",
            name        = "Night Owl",
            emoji       = "🦉",
            description = "Start a focus session after 10 PM",
        ),
        Badge(
            id          = "lunchtime_focus",
            name        = "Lunchtime Focus",
            emoji       = "☕",
            description = "Complete a session between 12 PM and 1 PM",
        ),
    )

    /** Fast O(1) lookup by badge ID. */
    val byId: Map<String, Badge> = list.associateBy { it.id }
}

package com.focusfirst.data.model

data class BreakActivity(
    val emoji: String,
    val title: String,
    val description: String,
    val durationSeconds: Int,
)

val SHORT_BREAK_ACTIVITIES = listOf(
    BreakActivity("👁️", "Eye Rest",      "Look at something 20 feet away",    60),
    BreakActivity("💧", "Drink Water",   "Hydrate — you probably forgot",      30),
    BreakActivity("🧘", "Breathe",       "4 counts in, hold 4, out 4",         60),
    BreakActivity("🚶", "Walk Around",   "Stand up and take 20 steps",         60),
    BreakActivity("🤸", "Stretch",       "Neck rolls and shoulder shrugs",     90),
    BreakActivity("📵", "Phone Down",    "No scrolling — rest your brain",    300),
)

val LONG_BREAK_ACTIVITIES = listOf(
    BreakActivity("🍵", "Make a Drink",      "Tea, coffee or water — your call",   300),
    BreakActivity("🚶", "Short Walk",        "5 minutes outside if possible",      300),
    BreakActivity("🥗", "Eat Something",     "Light snack to refuel",              600),
    BreakActivity("🧘", "Meditate",          "Close your eyes and breathe slowly", 300),
    BreakActivity("🤸", "Full Stretch",      "Full body stretch routine",          300),
    BreakActivity("💧", "Hydrate + Rest Eyes", "Water and look away from screen",  120),
)

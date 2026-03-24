package com.focusfirst.data.model

enum class PlanetSkin(
    val displayName: String,
    val isPro: Boolean,
    val description: String,
) {
    EARTH("Earth", false, "A living world grows with every session"),
    MARS("Mars", true, "Transform a dead planet into New Mars"),
    OCEAN("Ocean World", true, "A bioluminescent deep ocean world"),
    ICE("Ice Giant", true, "Crystal formations and dancing auroras"),
    LAVA("Lava World", true, "Volcanic inferno slowly cooling"),
    ALIEN("Alien World", true, "Exotic purple lifeforms emerge"),
}

fun PlanetSkin.modelPath(stage: Int): String =
    "models/${name.lowercase()}_stage${stage}.glb"

fun getStageForSessions(sessions: Int): Int = when {
    sessions < 5   -> 1
    sessions < 15  -> 2
    sessions < 30  -> 3
    sessions < 60  -> 4
    sessions < 100 -> 5
    else           -> 6
}

fun getStageLabel(skin: PlanetSkin, stage: Int): String {
    val earthLabels = listOf(
        "Barren rock", "First life", "Forests grow",
        "Oceans form", "Civilization", "Thriving world",
    )
    val marsLabels = listOf(
        "Dead rock", "Dust storms", "Ancient valleys",
        "Ice caps form", "Terraforming", "New Mars",
    )
    return when (skin) {
        PlanetSkin.EARTH -> earthLabels[stage - 1]
        PlanetSkin.MARS  -> marsLabels[stage - 1]
        else             -> "Stage $stage"
    }
}

fun getNextStageAt(sessions: Int): Int? = when {
    sessions < 5   -> 5
    sessions < 15  -> 15
    sessions < 30  -> 30
    sessions < 60  -> 60
    sessions < 100 -> 100
    else           -> null
}

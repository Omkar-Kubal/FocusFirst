package com.focusfirst.data.model

enum class AmbientSound(
    val displayName: String,
    val emoji: String,
    val fileName: String,
    val isPro: Boolean,
) {
    NONE("None", "🔇", "", false),
    RAIN("Rain", "🌧️", "sounds/rain.mp3", false),
    WHITE_NOISE("White Noise", "⬜", "sounds/white_noise.mp3", false),
    COFFEE_SHOP("Coffee Shop", "☕", "sounds/coffee_shop.mp3", true),
    OCEAN("Ocean Waves", "🌊", "sounds/ocean.mp3", true),
    FIREPLACE("Fireplace", "🔥", "sounds/fireplace.mp3", true),
    FOREST("Forest", "🌿", "sounds/forest.mp3", true),
    BROWN_NOISE("Brown Noise", "🟤", "sounds/brown_noise.mp3", true),
    LIBRARY("Library", "📚", "sounds/library.mp3", true),
}

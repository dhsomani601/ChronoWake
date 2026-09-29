package com.example.sound

data class SoundProfile(
    val id: String,
    val name: String,
    val description: String,
    val category: SoundCategory,
    val carrierFreqHz: Float,
    val beatFreqHz: Float,
    val defaultRampMinutes: Int,
    val isProOnly: Boolean = false,
    val iconName: String = "waves"
)

enum class SoundCategory {
    BINAURAL_BEAT,
    GENTLE_NATURE,
    ZEN_MEDITATION,
    CLASSIC_ALARM,
    ENERGETIC
}

object SoundProfiles {
    val ALL = listOf(
        SoundProfile(
            id = "binaural_theta",
            name = "Binaural Theta (6Hz)",
            description = "Gentle brainwave entrainment from theta rest to awake alpha. Zero cortisol spike.",
            category = SoundCategory.BINAURAL_BEAT,
            carrierFreqHz = 216f,
            beatFreqHz = 6f,
            defaultRampMinutes = 3,
            isProOnly = false,
            iconName = "graphic_eq"
        ),
        SoundProfile(
            id = "gentle_sunrise",
            name = "Golden Sunrise Harmonizer",
            description = "Ascending harmonic chords mimicking early morning sunlight warming the room.",
            category = SoundCategory.GENTLE_NATURE,
            carrierFreqHz = 261.63f, // Middle C
            beatFreqHz = 8f,
            defaultRampMinutes = 5,
            isProOnly = false,
            iconName = "wb_sunny"
        ),
        SoundProfile(
            id = "zen_chimes",
            name = "Tibetan Singing Bowl & Chimes",
            description = "Rich 432Hz harmonic acoustic resonance with soothing deep decay.",
            category = SoundCategory.ZEN_MEDITATION,
            carrierFreqHz = 432f,
            beatFreqHz = 4f,
            defaultRampMinutes = 2,
            isProOnly = false,
            iconName = "spa"
        ),
        SoundProfile(
            id = "binaural_alpha",
            name = "Alpha Flow (10Hz)",
            description = "Stimulates relaxed alertness and immediate cognitive clarity.",
            category = SoundCategory.BINAURAL_BEAT,
            carrierFreqHz = 300f,
            beatFreqHz = 10f,
            defaultRampMinutes = 2,
            isProOnly = true,
            iconName = "psychology"
        ),
        SoundProfile(
            id = "forest_dawn",
            name = "Cosmic Dawn Resonance",
            description = "Deep meditative carrier with gentle rhythmic morning pulses.",
            category = SoundCategory.GENTLE_NATURE,
            carrierFreqHz = 194.18f, // Earth frequency
            beatFreqHz = 7.83f, // Schumann resonance
            defaultRampMinutes = 4,
            isProOnly = true,
            iconName = "nature_people"
        ),
        SoundProfile(
            id = "energetic_pulse",
            name = "Dynamic Wake Pulse",
            description = "Clear, modern rhythmic tone for deep sleepers needing punctual activation.",
            category = SoundCategory.ENERGETIC,
            carrierFreqHz = 528f, // Solfeggio 528Hz
            beatFreqHz = 12f,
            defaultRampMinutes = 1,
            isProOnly = false,
            iconName = "bolt"
        ),
        SoundProfile(
            id = "classic_bell",
            name = "Heritage Mechanical Chime",
            description = "Crisp, traditional alarm chime with modern progressive loudness curve.",
            category = SoundCategory.CLASSIC_ALARM,
            carrierFreqHz = 880f,
            beatFreqHz = 0f,
            defaultRampMinutes = 2,
            isProOnly = false,
            iconName = "alarm"
        )
    )

    fun getById(id: String): SoundProfile {
        return ALL.firstOrNull { it.id == id } ?: ALL[0]
    }
}

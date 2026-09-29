package com.example.sound

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class SoundProfile(
    val id: String,
    val name: String,
    val description: String,
    val category: SoundCategory,
    val carrierFreqHz: Float,
    val beatFreqHz: Float,
    val defaultRampMinutes: Int,
    val isProOnly: Boolean = false,
    val iconName: String = "waves",
    val customUriString: String? = null
)

enum class SoundCategory {
    BINAURAL_BEAT,
    GENTLE_NATURE,
    ZEN_MEDITATION,
    CLASSIC_ALARM,
    ENERGETIC,
    CUSTOM_RINGTONE
}

object SoundProfiles {
    val BUILT_IN = listOf(
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

    val ALL: List<SoundProfile>
        get() = BUILT_IN

    private const val PREFS_NAME = "custom_ringtones_prefs"
    private const val KEY_CUSTOM_RINGTONES = "saved_custom_ringtones"

    fun getCustomProfiles(context: Context): List<SoundProfile> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonString = prefs.getString(KEY_CUSTOM_RINGTONES, null) ?: return emptyList()
        val list = mutableListOf<SoundProfile>()
        try {
            val arr = JSONArray(jsonString)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SoundProfile(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        description = obj.optString("description", "Custom Selected Ringtone"),
                        category = SoundCategory.CUSTOM_RINGTONE,
                        carrierFreqHz = 440f,
                        beatFreqHz = 0f,
                        defaultRampMinutes = 2,
                        isProOnly = false,
                        iconName = "music_note",
                        customUriString = obj.optString("uri")
                    )
                )
            }
        } catch (_: Exception) {}
        return list
    }

    fun addCustomProfile(context: Context, name: String, uriString: String): SoundProfile {
        val current = getCustomProfiles(context).toMutableList()
        val id = "custom_" + System.currentTimeMillis()
        val newProfile = SoundProfile(
            id = id,
            name = name,
            description = "Custom User Ringtone",
            category = SoundCategory.CUSTOM_RINGTONE,
            carrierFreqHz = 440f,
            beatFreqHz = 0f,
            defaultRampMinutes = 2,
            isProOnly = false,
            iconName = "music_note",
            customUriString = uriString
        )
        current.add(newProfile)
        saveCustomProfiles(context, current)
        return newProfile
    }

    fun removeCustomProfile(context: Context, id: String) {
        val current = getCustomProfiles(context).toMutableList()
        current.removeAll { it.id == id }
        saveCustomProfiles(context, current)
    }

    private fun saveCustomProfiles(context: Context, list: List<SoundProfile>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (item in list) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("description", item.description)
            obj.put("uri", item.customUriString ?: "")
            arr.put(obj)
        }
        prefs.edit().putString(KEY_CUSTOM_RINGTONES, arr.toString()).apply()
    }

    fun getAllProfiles(context: Context?): List<SoundProfile> {
        val custom = if (context != null) getCustomProfiles(context) else emptyList()
        return custom + BUILT_IN
    }

    fun getById(id: String, context: Context? = null): SoundProfile {
        if (context != null) {
            val custom = getCustomProfiles(context).firstOrNull { it.id == id }
            if (custom != null) return custom
        }
        return BUILT_IN.firstOrNull { it.id == id } ?: BUILT_IN[0]
    }
}

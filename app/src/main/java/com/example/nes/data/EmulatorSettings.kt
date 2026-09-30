package com.example.nes.data

import android.content.Context
import android.content.SharedPreferences

enum class AspectRatioMode {
    ORIGINAL_4_3,
    FULL_STRETCH,
    SQUARE_1_1
}

enum class ControlSize {
    SMALL,
    NORMAL,
    LARGE
}

data class EmulatorSettings(
    val aspectRatio: AspectRatioMode = AspectRatioMode.ORIGINAL_4_3,
    val smoothFilter: Boolean = false,
    val audioEnabled: Boolean = true,
    val audioVolume: Float = 0.8f,
    val controlsOpacity: Float = 0.75f,
    val controlSize: ControlSize = ControlSize.NORMAL,
    val hapticFeedback: Boolean = true,
    val currentSaveSlot: Int = 1,
    val languageArabic: Boolean = true
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nes_emulator_settings", Context.MODE_PRIVATE)

    fun loadSettings(): EmulatorSettings {
        val aspectName = prefs.getString("aspect_ratio", AspectRatioMode.ORIGINAL_4_3.name)
        val aspect = try {
            AspectRatioMode.valueOf(aspectName ?: AspectRatioMode.ORIGINAL_4_3.name)
        } catch (_: Exception) {
            AspectRatioMode.ORIGINAL_4_3
        }

        val sizeName = prefs.getString("control_size", ControlSize.NORMAL.name)
        val size = try {
            ControlSize.valueOf(sizeName ?: ControlSize.NORMAL.name)
        } catch (_: Exception) {
            ControlSize.NORMAL
        }

        return EmulatorSettings(
            aspectRatio = aspect,
            smoothFilter = prefs.getBoolean("smooth_filter", false),
            audioEnabled = prefs.getBoolean("audio_enabled", true),
            audioVolume = prefs.getFloat("audio_volume", 0.8f),
            controlsOpacity = prefs.getFloat("controls_opacity", 0.75f),
            controlSize = size,
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            currentSaveSlot = prefs.getInt("save_slot", 1),
            languageArabic = prefs.getBoolean("lang_arabic", true)
        )
    }

    fun saveSettings(settings: EmulatorSettings) {
        prefs.edit()
            .putString("aspect_ratio", settings.aspectRatio.name)
            .putBoolean("smooth_filter", settings.smoothFilter)
            .putBoolean("audio_enabled", settings.audioEnabled)
            .putFloat("audio_volume", settings.audioVolume)
            .putFloat("controls_opacity", settings.controlsOpacity)
            .putString("control_size", settings.controlSize.name)
            .putBoolean("haptic_feedback", settings.hapticFeedback)
            .putInt("save_slot", settings.currentSaveSlot)
            .putBoolean("lang_arabic", settings.languageArabic)
            .apply()
    }
}

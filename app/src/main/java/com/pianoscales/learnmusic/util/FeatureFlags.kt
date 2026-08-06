package com.pianoscales.learnmusic.util

import com.pianoscales.learnmusic.BuildConfig

object FeatureFlags {
    /**
     * Flag to enable/disable PianoBuddy functionality.
     * Hardware is not ready for public release, so this should only be enabled in debug builds.
     */
    val PIANO_BUDDY_ENABLED = BuildConfig.DEBUG

    /**
     * Flag to show/hide diagnostic information (pitch detection, frequency, etc.)
     */
    val AUDIO_DIAGNOSTICS_ENABLED = BuildConfig.DEBUG
}

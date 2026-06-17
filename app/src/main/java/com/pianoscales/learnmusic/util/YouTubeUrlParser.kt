package com.pianoscales.learnmusic.util

import android.util.Log

object YouTubeUrlParser {
    private const val TAG = "YouTubeUrlParser"

    /**
     * Extracts the YouTube video ID from various URL formats.
     * Supported formats:
     * - Standard: https://www.youtube.com/watch?v=VIDEO_ID
     * - Shorts: https://youtube.com/shorts/VIDEO_ID
     * - Shortened: https://youtu.be/VIDEO_ID
     * - Raw ID: VIDEO_ID
     *
     * Returns null if extraction fails or input is blank.
     */
    fun extractVideoId(input: String?): String? {
        if (input.isNullOrBlank()) return null

        Log.d(TAG, "Original value received: $input")

        val videoId = when {
            // Raw ID (usually 11 characters, no slashes or dots)
            !input.contains("/") && !input.contains(".") -> input

            // Shorts: https://youtube.com/shorts/VIDEO_ID
            input.contains("/shorts/") -> {
                input.substringAfter("/shorts/").substringBefore("?").substringBefore("/")
            }

            // Shortened: https://youtu.be/VIDEO_ID
            input.contains("youtu.be/") -> {
                input.substringAfter("youtu.be/").substringBefore("?").substringBefore("/")
            }

            // Standard: https://www.youtube.com/watch?v=VIDEO_ID
            input.contains("v=") -> {
                input.substringAfter("v=").substringBefore("&").substringBefore("/")
            }

            else -> null
        }

        if (videoId != null && videoId.isNotBlank()) {
            Log.d(TAG, "Extracted video ID: $videoId")
            return videoId
        }

        Log.e(TAG, "Extraction failed for input: $input")
        return null
    }
}

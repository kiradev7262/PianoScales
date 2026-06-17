package com.example.pianoscales.data.models

data class VideoMetadata(
    val conceptId: String,
    val title: String,
    val description: String,
    val youtubeUrl: String?,
    val available: Boolean
)

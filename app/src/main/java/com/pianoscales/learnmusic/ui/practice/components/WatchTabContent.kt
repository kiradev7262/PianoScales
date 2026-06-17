package com.pianoscales.learnmusic.ui.practice.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.pianoscales.learnmusic.data.models.VideoMetadata
import com.pianoscales.learnmusic.theory.ConceptType
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.theme.*
import com.pianoscales.learnmusic.util.YouTubeUrlParser
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

@Composable
fun WatchTabContent(
    rootNote: Note,
    conceptType: ConceptType,
    videoMetadata: VideoMetadata?,
    modifier: Modifier = Modifier
) {
    val videoId = YouTubeUrlParser.extractVideoId(videoMetadata?.youtubeUrl)
    val lifecycleOwner = LocalLifecycleOwner.current
    val isAvailable = videoMetadata?.available == true && videoId != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        if (isAvailable) {
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                color = CardSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        YouTubePlayerView(context).apply {
                            lifecycleOwner.lifecycle.addObserver(this)
                            addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                                override fun onReady(youTubePlayer: YouTubePlayer) {
                                    Log.d("WatchTabContent", "Player ready, cueing video: $videoId")
                                    youTubePlayer.cueVideo(videoId, 0f)
                                }
                            })
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Video coming soon.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Concept Description Card
        PracticeTheoryCard(
            title = "Lesson Overview"
        ) {
            Text(
                text = videoMetadata?.title ?: "${rootNote.displayName} ${conceptType.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = videoMetadata?.description ?: "In this lesson, you'll learn the structure and fingering for the ${rootNote.displayName} ${conceptType.displayName}. Watch the video above to see the correct technique in action.",
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(32.dp)) // Extra padding at bottom
    }
}

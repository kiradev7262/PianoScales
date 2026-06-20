package com.pianoscales.learnmusic.ui.songs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pianoscales.learnmusic.ui.components.PianoScalesHomeTopBar
import com.pianoscales.learnmusic.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsPackScreen(
    onStartSong: (Song) -> Unit,
    viewModel: SongsPackViewModel = hiltViewModel()
) {
    val songs by viewModel.songs.collectAsState()

    Scaffold(
        containerColor = PrimaryBackground,
        topBar = {
            PianoScalesHomeTopBar(
                title = "Play a Song",
                showAvatar = false
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(
                text = "Learn your favorite melodies one note at a time.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            songs.forEach { song ->
                SongTile(
                    song = song,
                    onClick = { onStartSong(song) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun SongTile(
    song: Song,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardSurface)
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = song.difficulty,
                        style = MaterialTheme.typography.bodyMedium,
                        color = PrimaryAccent
                    )
                    Text(
                        text = " • ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                    Text(
                        text = "${song.lines.size} Lines",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
            
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Play",
                tint = PrimaryAccent,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

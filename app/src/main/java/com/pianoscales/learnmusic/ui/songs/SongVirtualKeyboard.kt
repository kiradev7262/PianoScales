package com.pianoscales.learnmusic.ui.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pianoscales.learnmusic.theory.Note
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun SongVirtualKeyboard(
    layout: SongKeyboardLayout,
    targetNote: NoteWithOctave?,
    enabled: Boolean,
    onNotePlayed: (Note, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(CardSurface)
    ) {
        val totalWidth = maxWidth
        val totalHeight = maxHeight

        val whiteKeyWidth = totalWidth / layout.totalWhiteKeys
        val blackKeyWidth = whiteKeyWidth * 0.62f
        val blackKeyHeight = totalHeight * 0.58f

        // White Keys Layer
        Row(modifier = Modifier.fillMaxSize()) {
            layout.whiteKeys.forEach { whiteKey ->
                val isTarget = targetNote != null &&
                        targetNote.note == whiteKey.note &&
                        targetNote.octave == whiteKey.octave

                SongWhiteKey(
                    noteWithOctave = whiteKey,
                    isTarget = isTarget,
                    enabled = enabled,
                    onNotePlayed = onNotePlayed,
                    modifier = Modifier
                        .width(whiteKeyWidth)
                        .fillMaxHeight()
                )
            }
        }

        // Black Keys Layer
        layout.blackKeys.forEach { blackInfo ->
            val blackKey = blackInfo.noteWithOctave
            val isTarget = targetNote != null &&
                    targetNote.note == blackKey.note &&
                    targetNote.octave == blackKey.octave

            val xOffset = whiteKeyWidth * blackInfo.boundaryIndex - (blackKeyWidth / 2f)

            SongBlackKey(
                noteWithOctave = blackKey,
                isTarget = isTarget,
                enabled = enabled,
                onNotePlayed = onNotePlayed,
                modifier = Modifier
                    .offset(x = xOffset)
                    .width(blackKeyWidth)
                    .height(blackKeyHeight)
            )
        }
    }
}

@Composable
private fun SongWhiteKey(
    noteWithOctave: NoteWithOctave,
    isTarget: Boolean,
    enabled: Boolean,
    onNotePlayed: (Note, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor = when {
        isPressed -> PrimaryAccent
        isTarget -> PrimaryAccent.copy(alpha = 0.25f)
        else -> Color.White
    }

    val textColor = when {
        isPressed -> PrimaryBackground
        isTarget -> PrimaryAccent
        else -> TextMuted
    }

    Box(
        modifier = modifier
            .padding(horizontal = 0.5.dp)
            .clip(RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp))
            .background(bgColor)
            .border(
                width = if (isTarget) 1.5.dp else 0.5.dp,
                color = if (isTarget) PrimaryAccent else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp)
            )
            .pointerInput(enabled, noteWithOctave) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        onNotePlayed(noteWithOctave.note, noteWithOctave.octave)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.id == down.id && it.pressed })
                        isPressed = false
                    }
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            if (isTarget && !isPressed) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(3.dp)
                        .background(PrimaryAccent, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = "${noteWithOctave.note.displayName}${noteWithOctave.octave}",
                fontSize = 11.sp,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (isTarget || isPressed) FontWeight.ExtraBold else FontWeight.Medium,
                color = textColor
            )
        }
    }
}

@Composable
private fun SongBlackKey(
    noteWithOctave: NoteWithOctave,
    isTarget: Boolean,
    enabled: Boolean,
    onNotePlayed: (Note, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    val bgColor = when {
        isPressed -> PrimaryAccent
        isTarget -> PrimaryAccent
        else -> Color(0xFF0F172A)
    }

    val textColor = when {
        isPressed || isTarget -> PrimaryBackground
        else -> TextMuted.copy(alpha = 0.8f)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp))
            .background(bgColor)
            .border(
                width = if (isTarget) 1.5.dp else 0.5.dp,
                color = if (isTarget) Color.White else Color.Transparent,
                shape = RoundedCornerShape(bottomStart = 5.dp, bottomEnd = 5.dp)
            )
            .pointerInput(enabled, noteWithOctave) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        onNotePlayed(noteWithOctave.note, noteWithOctave.octave)
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.id == down.id && it.pressed })
                        isPressed = false
                    }
                }
            },
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "${noteWithOctave.note.displayName}${noteWithOctave.octave}",
            fontSize = 9.sp,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

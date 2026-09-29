package com.pianoscales.learnmusic.ui.songs

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pianoscales.learnmusic.ui.theme.*

@Composable
fun SongFallingNotesView(
    song: Song,
    currentNoteIndex: Int,
    isDemoPlaying: Boolean,
    layout: SongKeyboardLayout,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val notes = song.notes
    val animatedNoteIndex = remember { Animatable(currentNoteIndex.toFloat()) }

    // Smoothly animate when note index advances
    LaunchedEffect(currentNoteIndex, isDemoPlaying) {
        if (isDemoPlaying) {
            animatedNoteIndex.animateTo(
                targetValue = currentNoteIndex.toFloat(),
                animationSpec = tween(durationMillis = 400, easing = LinearEasing)
            )
        } else {
            animatedNoteIndex.animateTo(
                targetValue = currentNoteIndex.toFloat(),
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(PrimaryBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val widthPx = size.width
            val heightPx = size.height

            if (widthPx <= 0f || heightPx <= 0f || notes.isEmpty()) return@Canvas

            val whiteKeyWidthPx = widthPx / layout.totalWhiteKeys.toFloat()
            val targetY = heightPx - with(density) { 20.dp.toPx() }
            val stepY = with(density) { 60.dp.toPx() }
            val blockHeight = with(density) { 32.dp.toPx() }

            // 1. Draw subtle vertical key dividers (lanes)
            for (k in 1 until layout.totalWhiteKeys) {
                val x = k * whiteKeyWidthPx
                drawLine(
                    color = Color.White.copy(alpha = 0.04f),
                    start = Offset(x, 0f),
                    end = Offset(x, heightPx),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Target / Play Line
            drawLine(
                color = PrimaryAccent.copy(alpha = 0.3f),
                start = Offset(0f, targetY),
                end = Offset(widthPx, targetY),
                strokeWidth = with(density) { 6.dp.toPx() }
            )
            drawLine(
                color = PrimaryAccent,
                start = Offset(0f, targetY),
                end = Offset(widthPx, targetY),
                strokeWidth = with(density) { 2.dp.toPx() }
            )

            // 3. Draw Falling Notes
            val f = animatedNoteIndex.value

            notes.forEachIndexed { index, noteWithOctave ->
                val noteY = targetY - (index - f) * stepY

                // Render if within canvas vertical bounds (+ padding)
                if (noteY >= -blockHeight && noteY <= heightPx + blockHeight) {
                    val rect = layout.getNoteRect(noteWithOctave.note, noteWithOctave.octave, widthPx)
                    if (rect != null) {
                        val (xOffsetPx, keyWidthPx) = rect
                        val blockLeft = xOffsetPx + with(density) { 2.dp.toPx() }
                        val blockWidth = (keyWidthPx - with(density) { 4.dp.toPx() }).coerceAtLeast(8f)
                        val blockTop = noteY - blockHeight / 2f

                        val isActive = index == currentNoteIndex
                        val isCompleted = index < currentNoteIndex

                        val completionFactor = if (isCompleted) (currentNoteIndex - index) else 0
                        val alpha = if (isCompleted) {
                            (1.0f - completionFactor * 0.4f).coerceIn(0f, 1f)
                        } else {
                            1.0f
                        }

                        if (alpha > 0.01f) {
                            val fillColor = when {
                                isActive -> PrimaryAccent
                                isCompleted -> SuccessAccent.copy(alpha = 0.8f * alpha)
                                else -> CardSurface.copy(alpha = 0.9f)
                            }

                            val borderColor = when {
                                isActive -> Color.White
                                isCompleted -> SuccessAccent.copy(alpha = alpha)
                                else -> PrimaryAccent.copy(alpha = 0.6f)
                            }

                            // Draw note block background
                            drawRoundRect(
                                color = fillColor,
                                topLeft = Offset(blockLeft, blockTop),
                                size = Size(blockWidth, blockHeight),
                                cornerRadius = CornerRadius(with(density) { 6.dp.toPx() }),
                                alpha = alpha
                            )

                            // Draw note block border
                            drawRoundRect(
                                color = borderColor,
                                topLeft = Offset(blockLeft, blockTop),
                                size = Size(blockWidth, blockHeight),
                                cornerRadius = CornerRadius(with(density) { 6.dp.toPx() }),
                                style = Stroke(width = if (isActive) with(density) { 2.dp.toPx() } else with(density) { 1.dp.toPx() }),
                                alpha = alpha
                            )

                            // Draw note name text
                            val noteText = if (isCompleted && completionFactor > 0) "✓" else "${noteWithOctave.note.displayName}${noteWithOctave.octave}"
                            val textLayoutResult = textMeasurer.measure(
                                text = noteText,
                                style = TextStyle(
                                    color = if (isActive || isCompleted) PrimaryBackground else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold
                                )
                            )

                            val textX = blockLeft + (blockWidth - textLayoutResult.size.width) / 2f
                            val textY = blockTop + (blockHeight - textLayoutResult.size.height) / 2f

                            drawText(
                                textLayoutResult = textLayoutResult,
                                topLeft = Offset(textX, textY),
                                alpha = alpha
                            )
                        }
                    }
                }
            }
        }
    }
}

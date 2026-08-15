package com.pianoscales.learnmusic.audio_intelligence.voice_training

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pianoscales.learnmusic.ui.theme.PrimaryAccent
import com.pianoscales.learnmusic.ui.theme.TextMuted
import com.pianoscales.learnmusic.ui.theme.TextPrimary

@Composable
fun VoicePitchMeter(
    pitchDistance: Float, // in semitones, e.g. -1.0 to 1.0
    isPitchStable: Boolean,
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedPitchDistance by animateFloatAsState(
        targetValue = if (isPitchStable) pitchDistance else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "PitchDistanceAnimation"
    )

    val indicatorColor by animateColorAsState(
        targetValue = if (isCorrect) Color.Green else PrimaryAccent,
        label = "IndicatorColorAnimation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("LOW", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text("HIGH", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                // Draw background track
                drawLine(
                    color = TextPrimary.copy(alpha = 0.1f),
                    start = Offset(0f, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Draw ticks
                val tickCount = 21
                for (i in 0 until tickCount) {
                    val x = width * i / (tickCount - 1)
                    val isCenter = i == tickCount / 2
                    val isMajor = i % 5 == 0 || isCenter
                    val tickHeight = when {
                        isCenter -> 20.dp.toPx()
                        isMajor -> 12.dp.toPx()
                        else -> 6.dp.toPx()
                    }
                    val tickColor = when {
                        isCenter && isCorrect -> Color.Green
                        isCenter -> TextPrimary.copy(alpha = 0.6f)
                        else -> TextPrimary.copy(alpha = 0.2f)
                    }
                    drawLine(
                        color = tickColor,
                        start = Offset(x, centerY - tickHeight),
                        end = Offset(x, centerY + tickHeight),
                        strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Draw indicator/needle
                if (isPitchStable) {
                    // Map -1.0 to 1.0 semitones to 0 to width
                    // Range of 1 semitone each side is usually enough for a tuner
                    val range = 1.0f 
                    val normalizedPos = ((animatedPitchDistance.coerceIn(-range, range) / range) + 1) / 2
                    val indicatorX = width * normalizedPos

                    // Draw needle line
                    drawLine(
                        color = indicatorColor,
                        start = Offset(indicatorX, centerY - 25.dp.toPx()),
                        end = Offset(indicatorX, centerY + 25.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    
                    // Draw indicator head
                    drawCircle(
                        color = indicatorColor,
                        radius = 4.dp.toPx(),
                        center = Offset(indicatorX, centerY - 25.dp.toPx())
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        if (isCorrect) {
            Text(
                "PERFECT",
                color = Color.Green,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        } else if (isPitchStable) {
             val cents = (pitchDistance * 100).toInt()
             val text = when {
                 cents > 10 -> "TOO HIGH"
                 cents < -10 -> "TOO LOW"
                 else -> "ALMOST THERE"
             }
             Text(
                 text, 
                 color = if (Math.abs(cents) <= 10) PrimaryAccent else TextMuted, 
                 fontSize = 14.sp, 
                 fontWeight = FontWeight.Medium
             )
        } else {
            Text(
                "ADJUST YOUR PITCH",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

package com.johnfatoma.sana.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.johnfatoma.sana.live.LiveVoiceState

/**
 * The glowing orb from the original voice-states diagram:
 * IDLE -> LISTENING -> THINKING -> SPEAKING (-> IDLE)
 *
 * Kept intentionally simple (a pulsing colored circle) — swap in
 * something fancier once the core interaction actually works.
 */
@Composable
fun VoiceOrb(state: LiveVoiceState, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orb")

    val pulseSpeedMs = when (state) {
        LiveVoiceState.LISTENING -> 1200
        LiveVoiceState.THINKING -> 700
        LiveVoiceState.SPEAKING -> 400
        else -> 2000
    }

    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == LiveVoiceState.IDLE) 1f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseSpeedMs),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orb-scale",
    )

    val color = when (state) {
        LiveVoiceState.IDLE -> MaterialTheme.colorScheme.surfaceVariant
        LiveVoiceState.LISTENING -> Color(0xFF6C5CE7)
        LiveVoiceState.THINKING -> Color(0xFFFDCB6E)
        LiveVoiceState.SPEAKING -> Color(0xFF00CEC9)
        LiveVoiceState.ERROR -> MaterialTheme.colorScheme.error
    }

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .background(color = color, shape = CircleShape),
    )
}

package com.focusmother.android.ui.conversation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays a "Zordon is thinking..." indicator with animated dots.
 *
 * Features:
 * - Animated pulsing dots
 * - Purple color matching theme
 * - Material Design 3 styling
 * - Smooth animations
 *
 * @param modifier Optional modifier for customization
 */
@Composable
fun TypingIndicator(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(
                color = Color(0xFF1A1A2E), // Dark background
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Zordon is thinking",
            color = Color(0xFFE0E0E0),
            fontSize = 14.sp
        )

        AnimatedDots()
    }
}

/**
 * Animated dots for typing indicator.
 *
 * Three dots that pulse in sequence to create a typing animation effect.
 */
@Composable
private fun AnimatedDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            AnimatedDot(delay = index * 200)
        }
    }
}

/**
 * Single animated dot.
 *
 * @param delay Delay before animation starts (for staggered effect)
 */
@Composable
private fun AnimatedDot(delay: Int = 0) {
    val infiniteTransition = rememberInfiniteTransition(label = "dotAnimation")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, delayMillis = delay),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Box(
        modifier = Modifier
            .size(8.dp)
            .alpha(alpha)
            .background(
                color = Color(0xFF7C0EDA), // Purple
                shape = CircleShape
            )
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A1A)
@Composable
fun TypingIndicatorPreview() {
    TypingIndicator()
}

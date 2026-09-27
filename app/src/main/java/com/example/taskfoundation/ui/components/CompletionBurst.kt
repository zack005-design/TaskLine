package com.example.taskfoundation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.math.*

/** A changing event number allows consecutive celebrations for the same task. */
@Composable
fun CompletionBurst(trigger: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
        }
    }
    val colors = listOf(Color(0xFFFFD700), Color(0xFF4CAF50), Color(0xFF2196F3), Color(0xFFE91E63))
    Canvas(modifier) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        repeat(12) { index ->
            val angle = index * PI / 6
            val radius = size.minDimension * .45f * p
            drawCircle(colors[index % colors.size].copy(alpha = 1f - p),
                radius = 4.dp.toPx() * (1f - p * .5f),
                center = center + Offset((cos(angle) * radius).toFloat(), (sin(angle) * radius).toFloat()))
        }
    }
}

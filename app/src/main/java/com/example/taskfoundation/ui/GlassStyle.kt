package com.example.taskfoundation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

/** Neutral content canvas. Translucency belongs to the navigation layer. */
@Composable
fun glassBackdrop(): Brush = Brush.linearGradient(
    listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)
)

/** Opaque grouped content preserves legibility in both appearances. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    prominence: Float = 0f,
    tintColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(modifier = modifier, shape = shape,
        color = tintColor?.copy(alpha = .09f)?.compositeOver(MaterialTheme.colorScheme.surfaceContainerLowest)
            ?: MaterialTheme.colorScheme.surfaceContainerLowest,
        contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(content = content)
    }
}

/** Android approximation of a floating translucent navigation surface. */
@Composable
fun GlassPill(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier, shape = androidx.compose.foundation.shape.RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f)),
        shadowElevation = 3.dp, content = content)
}

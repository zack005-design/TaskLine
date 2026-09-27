package com.example.taskfoundation.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun TaskLineTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(
        primary = Color(0xFF88B7FF), onPrimary = Color(0xFF002B62),
        primaryContainer = Color(0xFF213B60), onPrimaryContainer = Color(0xFFDCE9FF),
        background = Color(0xFF000000), surface = Color(0xFF1C1C1E),
        surfaceContainer = Color(0xFF2C2C2E), surfaceContainerLowest = Color(0xFF1C1C1E),
        secondaryContainer = Color(0xFF343C55), onSecondaryContainer = Color(0xFFE2E9FF),
        onSurface = Color(0xFFF1F3FA), onSurfaceVariant = Color(0xFFBEC5D8),
        outline = Color(0xFF788297), outlineVariant = Color(0xFF454D61),
    ) else lightColorScheme(
        primary = Color(0xFF0066D6), onPrimary = Color.White,
        primaryContainer = Color(0xFFDCEAFF), onPrimaryContainer = Color(0xFF143763),
        background = Color(0xFFF2F2F7), surface = Color.White,
        secondaryContainer = Color(0xFFE7EBF6), onSecondaryContainer = Color(0xFF283653),
        surfaceContainer = Color(0xFFE8E8ED), surfaceContainerLowest = Color.White,
        onSurface = Color(0xFF1C1C1E), onSurfaceVariant = Color(0xFF636369),
        outline = Color(0xFF7E889C), outlineVariant = Color(0xFFD4DAE6),
    )
    // Concentric corner radius hierarchy — matches Apple Liquid Glass principle:
    // hardware shape → screen corners → sheets → cards → controls → chips
    MaterialTheme(colorScheme = colors, typography = Typography(
        headlineLarge = TextStyle(fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp),
        headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
        titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
        titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
        bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
        labelSmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    ), shapes = Shapes(
        extraSmall = RoundedCornerShape(12.dp),  // badges, tiny chips
        small      = RoundedCornerShape(18.dp),  // metadata pills, search chips
        medium     = RoundedCornerShape(28.dp),  // dialogs, date pickers
        large      = RoundedCornerShape(20.dp),  // grouped content
        extraLarge = RoundedCornerShape(40.dp),  // GlassPill nav bar, FAB, sheets
    ), content = content)
}

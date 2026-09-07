package com.example.message.recovery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Accent,
    onPrimary = BgColor,
    secondary = Accent,
    background = BgColor,
    onBackground = TextPrimary,
    surface = BgColor,
    onSurface = TextPrimary,
    outline = StrokeColor,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content,
    )
}

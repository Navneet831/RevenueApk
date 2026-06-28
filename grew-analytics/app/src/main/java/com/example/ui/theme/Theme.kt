package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GrewColorScheme = lightColorScheme(
    primary = BrandGreen,
    onPrimary = Color.White,
    secondary = BrandBlue,
    onSecondary = SlateTextLight,
    tertiary = BrandTeal,
    background = SlateBg,
    onBackground = SlateTextLight,
    surface = SlateCard,
    onSurface = SlateTextLight,
    outline = SlateBorder,
    surfaceVariant = BrandPurple,
    onSurfaceVariant = SlateTextMuted
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GrewColorScheme,
        typography = Typography,
        content = content
    )
}

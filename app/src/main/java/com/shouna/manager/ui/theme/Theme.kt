package com.shouna.manager.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ShounaColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Card,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDeep,
    secondary = Amber,
    background = Bg,
    onBackground = TextPrimary,
    surface = Card,
    onSurface = TextPrimary,
    surfaceVariant = PrimaryLight,
    onSurfaceVariant = TextSecondary
)

@Composable
fun ShounaManagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ShounaColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

package com.kabata.sangoku.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 地図の色合いに合わせて、いつも明るい配色にする
private val Colors = lightColorScheme(
    primary = Color(0xFF8E2C2C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D9C8),
    onPrimaryContainer = Color(0xFF3A0A0A),
    secondary = Color(0xFFA67A1E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E6C4),
    background = Color(0xFFF4ECD8),
    surface = Color(0xFFFFFAEE),
    surfaceVariant = Color(0xFFE8DCC0),
    onSurface = Color(0xFF2B2014),
    onBackground = Color(0xFF2B2014),
)

@Composable
fun SangokuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, content = content)
}

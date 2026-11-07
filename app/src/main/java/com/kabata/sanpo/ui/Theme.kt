package com.kabata.sanpo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF8E2C2C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF3D9D2),
    onPrimaryContainer = Color(0xFF3A0A0A),
    secondary = Color(0xFFA67A1E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF4E6C4),
    background = Color(0xFFF6F1E6),
    surface = Color(0xFFFFFBF3),
    surfaceVariant = Color(0xFFE7DFCF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF561E18),
    primaryContainer = Color(0xFF6E2420),
    secondary = Color(0xFFE6C06A),
    secondaryContainer = Color(0xFF5A4300),
    background = Color(0xFF1A1714),
    surface = Color(0xFF221E1A),
    surfaceVariant = Color(0xFF3D3730),
)

@Composable
fun CastleTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

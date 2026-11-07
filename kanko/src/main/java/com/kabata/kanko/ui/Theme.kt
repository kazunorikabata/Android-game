package com.kabata.kanko.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1E6FA8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD3E6F5),
    onPrimaryContainer = Color(0xFF062A44),
    secondary = Color(0xFFD9534F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE0DC),
    background = Color(0xFFF3F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDDE4EA),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9CCBF0),
    onPrimary = Color(0xFF00344F),
    primaryContainer = Color(0xFF0F4C73),
    secondary = Color(0xFFFFB3AB),
    secondaryContainer = Color(0xFF7A2621),
    background = Color(0xFF111417),
    surface = Color(0xFF1A1E22),
    surfaceVariant = Color(0xFF353C43),
)

@Composable
fun KankoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

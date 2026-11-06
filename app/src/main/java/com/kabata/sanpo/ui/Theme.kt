package com.kabata.sanpo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF3E8E5A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8EBD2),
    secondary = Color(0xFFE0922F),
    secondaryContainer = Color(0xFFFFE2BC),
    background = Color(0xFFF7F3E8),
    surface = Color(0xFFFFFCF5),
    surfaceVariant = Color(0xFFE6E1D3),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF8FD3A4),
    primaryContainer = Color(0xFF1F4D2E),
    secondary = Color(0xFFFFB866),
    secondaryContainer = Color(0xFF5A3A10),
    background = Color(0xFF171A16),
    surface = Color(0xFF1F231E),
    surfaceVariant = Color(0xFF3A3F37),
)

@Composable
fun SanpoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}

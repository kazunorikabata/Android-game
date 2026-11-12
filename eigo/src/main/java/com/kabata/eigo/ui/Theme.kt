package com.kabata.eigo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DFFF),
    onPrimaryContainer = Color(0xFF1B0F6B),
    secondary = Color(0xFFF2A900),
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFFFFEFC2),
    background = Color(0xFFF5F3FF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE6E3F2),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFC6BFFF),
    onPrimary = Color(0xFF2A1A93),
    primaryContainer = Color(0xFF4130B8),
    onPrimaryContainer = Color(0xFFE4DFFF),
    secondary = Color(0xFFFFC94D),
    secondaryContainer = Color(0xFF5C4300),
    background = Color(0xFF141220),
    surface = Color(0xFF1D1A2B),
    surfaceVariant = Color(0xFF39354A),
)

@Composable
fun EigoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

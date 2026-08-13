package com.virdmatik.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VirdColors = lightColorScheme(
    primary = Color(0xFF2F6F55),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EEE2),
    background = Color(0xFFF7F9F6),
    surface = Color(0xFFFFFFFF),
    secondary = Color(0xFF6B7D70)
)

@Composable
fun VirdmatikTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VirdColors, content = content)
}

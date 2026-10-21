package com.example.recorridoactivo

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF1B7F5C),
    secondary = Color(0xFF3D6B5A),
    tertiary = Color(0xFFE07A1F)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF6FD8AE),
    secondary = Color(0xFF9CCBB9),
    tertiary = Color(0xFFFFB86B)
)

@Composable
fun RecorridoTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}

/** Estilo oscuro para Google Maps (se aplica cuando el sensor de luz activa el modo oscuro). */
const val DARK_MAP_STYLE = """[
{"elementType":"geometry","stylers":[{"color":"#242f3e"}]},
{"elementType":"labels.text.fill","stylers":[{"color":"#a3adb8"}]},
{"elementType":"labels.text.stroke","stylers":[{"color":"#242f3e"}]},
{"featureType":"water","elementType":"geometry","stylers":[{"color":"#17263c"}]},
{"featureType":"road","elementType":"geometry","stylers":[{"color":"#38414e"}]},
{"featureType":"poi","elementType":"labels","stylers":[{"visibility":"off"}]}
]"""

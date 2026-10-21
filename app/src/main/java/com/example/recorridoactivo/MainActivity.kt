package com.example.recorridoactivo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: TrackViewModel = viewModel()
            // null = el dispositivo no tiene sensor de luz (o aún no hay lectura) -> tema del sistema
            val sensorDark by vm.isDark.collectAsStateWithLifecycle()
            val dark = sensorDark ?: isSystemInDarkTheme()

            DisposableEffect(dark) {
                val transparent = android.graphics.Color.TRANSPARENT
                val style = SystemBarStyle.auto(transparent, transparent) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }

            RecorridoTheme(dark) { AppRoot(vm, dark) }
        }
    }
}

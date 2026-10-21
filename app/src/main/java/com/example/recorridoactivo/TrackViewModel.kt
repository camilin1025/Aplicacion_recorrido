package com.example.recorridoactivo

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PointOfInterest(
    val id: Long,
    val number: Int,
    val photoPath: String,
    val position: LatLng,
    val timestamp: Long
)

data class TrackUiState(
    val tracking: Boolean = false,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0,
    val location: LatLng? = null,
    val path: List<LatLng> = emptyList(),
    val pois: List<PointOfInterest> = emptyList()
)

class TrackViewModel(app: Application) : AndroidViewModel(app) {

    private companion object {
        const val STRIDE_METERS = 0.75   // longitud de paso promedio
        const val DARK_BELOW_LUX = 20f   // histéresis para evitar parpadeos
        const val LIGHT_ABOVE_LUX = 60f
    }

    private val hub = SensorHub(app)
    private val jobs = mutableListOf<Job>()

    private val _state = MutableStateFlow(TrackUiState())
    val state: StateFlow<TrackUiState> = _state.asStateFlow()

    /** true = modo oscuro, false = modo claro, null = sin sensor/lectura. */
    val isDark: StateFlow<Boolean?> = flow {
        var dark = false
        hub.lightFlow().collect { lux ->
            dark = when {
                lux < DARK_BELOW_LUX -> true
                lux > LIGHT_ABOVE_LUX -> false
                else -> dark
            }
            emit(dark)
        }
    }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun startTracking() {
        if (_state.value.tracking) return
        File(getApplication<Application>().cacheDir, "photos").deleteRecursively()
        _state.value = TrackUiState(tracking = true)

        val startMs = SystemClock.elapsedRealtime()
        jobs += viewModelScope.launch {
            while (true) {
                delay(1_000)
                _state.update { it.copy(elapsedSeconds = (SystemClock.elapsedRealtime() - startMs) / 1000) }
            }
        }
        jobs += viewModelScope.launch {
            hub.stepFlow().collect {
                _state.update { s ->
                    val steps = s.steps + 1
                    s.copy(steps = steps, distanceMeters = steps * STRIDE_METERS)
                }
            }
        }
        jobs += viewModelScope.launch {
            hub.locationFlow()
                .catch { if (it !is SecurityException) throw it }
                .collect { loc ->
                    val p = LatLng(loc.latitude, loc.longitude)
                    _state.update { it.copy(location = p, path = it.path + p) }
                }
        }
    }

    fun stopTracking() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        _state.update { it.copy(tracking = false) }
    }

    /** Registra la foto con la ubicación actual y la hora. Devuelve false si no hay ubicación. */
    fun addPhoto(file: File): Boolean {
        val s = _state.value
        val position = s.location ?: return false
        val now = System.currentTimeMillis()
        val poi = PointOfInterest(
            id = now,
            number = s.pois.size + 1,
            photoPath = file.absolutePath,
            position = position,
            timestamp = now
        )
        _state.update { it.copy(pois = it.pois + poi) }
        return true
    }
}

package com.example.recorridoactivo

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.runtime.key
import kotlin.math.roundToInt

// ------------------------------------------------------------------ Raíz

@Composable
fun AppRoot(vm: TrackViewModel, dark: Boolean) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var pendingPath by rememberSaveable { mutableStateOf<String?>(null) }

    // --- Cámara: la foto se guarda en cacheDir/photos y se vincula a la ubicación actual
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { saved ->
        val file = pendingPath?.let { File(it) }
        pendingPath = null
        if (file != null) {
            if (saved && vm.addPhoto(file)) {
                scope.launch { snackbar.showSnackbar("Punto de interés guardado") }
            } else {
                file.delete()
                if (saved) scope.launch { snackbar.showSnackbar("Ubicación no disponible, intenta de nuevo") }
            }
        }
    }

    fun takePhoto() {
        val dir = File(context.cacheDir, "photos").apply { mkdirs() }
        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        pendingPath = file.absolutePath
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraLauncher.launch(uri)
    }

    // --- Permisos: GPS + Cámara (y reconocimiento de actividad para el contador de pasos)
    val permissions = buildList {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
        add(Manifest.permission.ACCESS_COARSE_LOCATION)
        add(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(Manifest.permission.ACTIVITY_RECOGNITION)
    }.toTypedArray()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val gps = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val camera = result[Manifest.permission.CAMERA] == true
        if (gps && camera) {
            tab = 0
            vm.startTracking()
        } else {
            scope.launch { snackbar.showSnackbar("Se necesitan los permisos de ubicación (GPS) y cámara") }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null) },
                    label = { Text("Recorrido") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = {
                        BadgedBox(badge = {
                            if (state.pois.isNotEmpty()) Badge { Text("${state.pois.size}") }
                        }) { Icon(Icons.Default.PhotoLibrary, contentDescription = null) }
                    },
                    label = { Text("Puntos de Interés") }
                )
            }
        }
    ) { padding ->
        when {
            tab == 1 -> PointsScreen(state, padding)
            state.tracking -> TrackingScreen(
                state = state, dark = dark, padding = padding,
                onPhoto = ::takePhoto, onStop = vm::stopTracking
            )
            else -> IdleScreen(padding) { permissionLauncher.launch(permissions) }
        }
    }
}

// ------------------------------------------------------------------ Inicio

@Composable
fun IdleScreen(padding: PaddingValues, onStart: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = null,
            modifier = Modifier.size(96.dp), tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text("Recorrido Activo", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Registra tus pasos y distancia, y captura fotos de los puntos de interés que encuentres en el camino.",
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Iniciar Recorrido")
        }
    }
}

// ------------------------------------------------------------------ Recorrido + mapa

@Composable
fun TrackingScreen(
    state: TrackUiState,
    dark: Boolean,
    padding: PaddingValues,
    onPhoto: () -> Unit,
    onStop: () -> Unit
) {
    val context = LocalContext.current
    val hasLocation = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    val cameraState = rememberCameraPositionState()
    var firstFix by remember { mutableStateOf(true) }
    LaunchedEffect(state.location) {
        val loc = state.location ?: return@LaunchedEffect
        if (firstFix) {
            cameraState.move(CameraUpdateFactory.newLatLngZoom(loc, 17f))
            firstFix = false
        } else {
            cameraState.animate(CameraUpdateFactory.newLatLng(loc))
        }
    }

    val mapStyle = remember(dark) { if (dark) MapStyleOptions(DARK_MAP_STYLE) else null }
    val routeColor = MaterialTheme.colorScheme.primary

    Box(Modifier.fillMaxSize().padding(padding)) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = hasLocation, mapStyleOptions = mapStyle),
            uiSettings = MapUiSettings(zoomControlsEnabled = false, myLocationButtonEnabled = false)
        ) {
            if (state.path.size > 1) Polyline(points = state.path, color = routeColor, width = 12f)
            state.pois.forEach { poi ->
                key(poi.id) {
                    Marker(
                        state = rememberMarkerState(position = poi.position),
                        title = "Punto de interés #${poi.number}",
                        snippet = formatDateTime(poi.timestamp)
                    )
                }
            }
        }

        Column(Modifier.align(Alignment.TopCenter).padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatTile(Icons.AutoMirrored.Filled.DirectionsWalk, "Pasos", "${state.steps}", Modifier.weight(1f))
                StatTile(Icons.Default.Straighten, "Distancia", formatDistance(state.distanceMeters), Modifier.weight(1f))
                StatTile(Icons.Default.Timer, "Tiempo", formatDuration(state.elapsedSeconds), Modifier.weight(1f))
            }
            if (state.location == null) {
                Card(Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)) {
                    Text("Buscando señal GPS…", Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }
        }

        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.height(56.dp)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Finalizar")
            }
            ExtendedFloatingActionButton(
                onClick = onPhoto,
                icon = { Icon(Icons.Default.PhotoCamera, contentDescription = null) },
                text = { Text("Tomar foto") }
            )
        }
    }
}

// ------------------------------------------------------------------ Dashboard de Puntos de Interés

@Composable
fun PointsScreen(state: TrackUiState, padding: PaddingValues) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(170.dp),
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Puntos de Interés", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(Icons.Default.PhotoCamera, "Fotos", "${state.pois.size}", Modifier.weight(1f))
                    StatTile(Icons.AutoMirrored.Filled.DirectionsWalk, "Pasos", "${state.steps}", Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(Icons.Default.Straighten, "Distancia", formatDistance(state.distanceMeters), Modifier.weight(1f))
                    StatTile(Icons.Default.Timer, "Tiempo", formatDuration(state.elapsedSeconds), Modifier.weight(1f))
                }
            }
        }
        if (state.pois.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    "Aún no has tomado fotografías. Inicia un recorrido y usa el botón \"Tomar foto\".",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(state.pois.asReversed(), key = { it.id }) { PoiCard(it) }
        }
    }
}

@Composable
fun PoiCard(poi: PointOfInterest) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            AsyncImage(
                model = File(poi.photoPath),
                contentDescription = "Punto de interés #${poi.number}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
            )
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Punto #${poi.number}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(formatDateTime(poi.timestamp), style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(
                        "%.5f, %.5f".format(poi.position.latitude, poi.position.longitude),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------ Componentes y utilidades

@Composable
fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, Modifier.size(20.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

fun formatDistance(meters: Double): String =
    if (meters < 1000) "${meters.roundToInt()} m" else "%.2f km".format(meters / 1000)

fun formatDuration(seconds: Long): String =
    if (seconds >= 3600) "%d:%02d:%02d".format(seconds / 3600, (seconds % 3600) / 60, seconds % 60)
    else "%02d:%02d".format(seconds / 60, seconds % 60)

fun formatDateTime(millis: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(millis))

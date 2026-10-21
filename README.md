# Recorrido Activo

Aplicación Android (Kotlin + Jetpack Compose) para monitorear la actividad física durante un recorrido y registrar fotografías de puntos de interés.

## Funcionalidades

- Solicita permisos de GPS y cámara al pulsar **Iniciar Recorrido**.
- Cuenta pasos y calcula la distancia con sensores de movimiento (detector de pasos o acelerómetro como respaldo).
- Cambia automáticamente entre modo claro y oscuro con el sensor de luz.
- Muestra la ubicación actual, la ruta y los marcadores en Google Maps.
- Permite tomar fotos durante el recorrido; guarda coordenadas, fecha y hora, y crea un marcador en el mapa.
- Guarda las fotos en caché y las lista en **Puntos de Interés** con diseño tipo dashboard.


## Opción 1: Compilar el código fuente

Necesitas Android Studio (versión reciente), JDK 17 o superior y un dispositivo o emulador con **Google Play**.

### 1. Clona el repositorio

```
git clone https://github.com/TU_USUARIO/RecorridoActivo.git
```

### 2. Crea tu propia API key de Google Maps

La clave del autor **no se incluye** en el repositorio. Cada persona debe usar la suya:

1. Entra a [Google Cloud Console](https://console.cloud.google.com) y crea un proyecto.
2. Vincula una cuenta de facturación (Google Maps Platform lo exige).
3. En **APIs y servicios → Biblioteca**, habilita **Maps SDK for Android**.
4. En **Credenciales → Crear credenciales → Clave de API**, crea la clave.
5. Recomendado: restríngela a *Apps para Android* con el paquete `com.example.recorridoactivo` y el SHA-1 de tu certificado, y limítala solo a *Maps SDK for Android*.

Para obtener tu SHA-1 ejecuta en la raíz del proyecto:

```
./gradlew signingReport        # Windows: gradlew.bat signingReport
```

y copia el valor `SHA1` de la variante `debug`.

### 3. Agrega la clave a `local.properties`

Copia `local.properties.example` como `local.properties` (en la raíz) o edita el existente, y agrega:

```
MAPS_API_KEY=tu_clave_aqui
```

Este archivo está en `.gitignore`: no lo subas al repositorio.

### 4. Ejecuta

Sincroniza Gradle y pulsa **Run**. Sin clave la app funciona, pero el mapa se ve gris.

## Probar en el emulador

En **Extended controls (⋯)**:

- **Location:** define un punto o carga una ruta GPS.
- **Virtual sensors → Accelerometer:** mueve el dispositivo virtual para simular pasos.
- **Virtual sensors → Additional sensors → Light:** menos de 20 lux activa el modo oscuro, más de 60 el claro.

## Solución de problemas

| Síntoma | Causa probable |
|---|---|
| Mapa gris o en blanco | Clave ausente o mal copiada, SDK no habilitado, falta facturación, o paquete y SHA-1 no coinciden con la restricción. Filtra Logcat por `Google Android Maps SDK`. |
| No suben los pasos en el emulador | Mueve el acelerómetro virtual con fuerza. |
| No aparece la ubicación | Usa un emulador con Google Play y define una ubicación en Extended controls. |

## Estructura

```
app/src/main/java/com/example/recorridoactivo/
├── MainActivity.kt      # Actividad y tema según sensor de luz
├── Sensors.kt           # Luz, pasos y GPS como Flows
├── TrackViewModel.kt    # Estado del recorrido y puntos de interés
├── Screens.kt           # Pantallas Compose (inicio, mapa, dashboard)
└── Theme.kt             # Colores y estilo oscuro del mapa
```

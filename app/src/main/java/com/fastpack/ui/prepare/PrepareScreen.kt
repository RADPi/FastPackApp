package com.fastpack.ui.prepare

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fastpack.ui.prepare.composables.CameraPreviewView
import com.fastpack.ui.prepare.composables.ShipmentDetailsView // Asumo que este composable existe
import com.google.accompanist.permissions.*

@ExperimentalMaterial3Api
@OptIn(ExperimentalPermissionsApi::class) // Para Accompanist Permissions
@Composable
fun PrepareScreen(
    viewModel: PrepareViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Manejo de permisos de cámara con Accompanist
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    // Efecto para reaccionar a los cambios en el estado del permiso
    // y para la lógica inicial de comprobación de permisos.
    LaunchedEffect(cameraPermissionState.status) {
        Log.d("PrepareScreen", "Camera permission status: ${cameraPermissionState.status}")
        viewModel.onCameraPermissionResult(cameraPermissionState.status == PermissionStatus.Granted)
    }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Text(
                    when (uiState) {
                        is PrepareScreenState.Scanning -> "Escanee la etiqueta del envío"
                        is PrepareScreenState.ShowResult -> "Detalles del Envío"
                        is PrepareScreenState.NoResult -> "Envío no Encontrado"
                        is PrepareScreenState.Error -> "Error"
                        is PrepareScreenState.Loading -> "Buscando..."
                        else -> "Preparar Envío"
                    }
                )
            })
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when (val state = uiState) {
                PrepareScreenState.Idle -> {
                    // Este estado ahora se maneja principalmente por la lógica de permisos.
                    // Si los permisos no están concedidos, se mostrará RequestingPermission.
                    // Si están concedidos, debería pasar a Scanning.
                    // Podrías mostrar un loader aquí si hay alguna comprobación inicial.
                    // O si el permiso se deniega y el usuario vuelve a la app.
                    Log.d("PrepareScreen", "Current state: Idle. Waiting for permission check.")
                    // Si llegamos aquí y el permiso NO está concedido, el usuario verá el estado RequestingPermission.
                    // Si el permiso SÍ está concedido, onCameraPermissionResult lo llevará a Scanning.
                    // Si el permiso está permanentemente denegado, RequestingPermission lo manejará.
                    if (cameraPermissionState.status != PermissionStatus.Granted) {
                        // Esto duplicaría la lógica de RequestingPermission,
                        // viewModel.onCameraPermissionResult ya debería haber puesto el estado correcto.
                        // Considera mostrar un texto genérico o un loader si es necesario un estado intermedio.
                        Text("Verificando permisos...")
                    } else {
                        // Si los permisos están concedidos, el LaunchedEffect debería haber llamado
                        // a viewModel.onCameraPermissionResult(true) -> que pone el estado en Scanning.
                        // Por lo tanto, no deberíamos estar mucho tiempo en Idle si los permisos están OK.
                        Text("Iniciando cámara...") // O un CircularProgressIndicator
                    }
                }

                PrepareScreenState.RequestingPermission -> {
                    PermissionRequestHandler(
                        permissionState = cameraPermissionState,
                        onPermissionGranted = {
                            // El LaunchedEffect ya maneja esto llamando a viewModel.onCameraPermissionResult(true)
                            // viewModel.onCameraPermissionResult(true) // -> esto cambiará el estado a Scanning
                            Log.d("PrepareScreen", "Permission granted via RequestingPermission UI.")
                        },
                        onPermissionDenied = {
                            // El LaunchedEffect también llamará a viewModel.onCameraPermissionResult(false)
                            // Aquí podrías mostrar un mensaje más persistente si fue denegado permanentemente.
                            Log.d("PrepareScreen", "Permission denied via RequestingPermission UI.")
                        }
                    )
                }

                PrepareScreenState.Scanning -> {
                    Log.d("PrepareScreen", "Current state: Scanning. Showing CameraPreviewView.")
                    CameraPreviewView(
                        modifier = Modifier.fillMaxSize(),
                        onQrCodeScanned = { qrCode ->
                            viewModel.onQrCodeScanned(qrCode)
                        }
                    )
                    // Puedes superponer el título aquí si lo deseas, aunque ya está en el TopAppBar
                    // Text("Escanee la etiqueta del envío", modifier = Modifier.align(Alignment.TopCenter).padding(16.dp))
                }

                PrepareScreenState.Loading -> {
                    Log.d("PrepareScreen", "Current state: Loading.")
                    CircularProgressIndicator()
                }

                is PrepareScreenState.ShowResult -> {
                    Log.d("PrepareScreen", "Current state: ShowResult. Shipment ID: ${state.shipment.id}")
                    // Aquí deberías tener un Composable para mostrar los detalles del envío
                    // Ejemplo: ShipmentDetailsView(shipment = state.shipment)
                    // Con un botón para escanear de nuevo
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        ShipmentDetailsView(shipment = state.shipment) // Usa tu composable real
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { viewModel.retryScanOrRequestPermission() }) { // O goBackToIdle() y que la UI maneje el flujo
                            Text("Escanear Otro Envío")
                        }
                    }
                }

                is PrepareScreenState.NoResult -> {
                    Log.d("PrepareScreen", "Current state: NoResult. Scanned code: ${state.scannedCode}")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        Text("No se encontró el envío con el código: ${state.scannedCode}", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryScanOrRequestPermission() }) {
                            Text("Reintentar Escaneo")
                        }
                    }
                }

                is PrepareScreenState.Error -> {
                    Log.d("PrepareScreen", "Current state: Error. Message: ${state.message}")
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    ) {
                        Text("Error: ${state.message}", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.retryScanOrRequestPermission() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionRequestHandler(
    permissionState: PermissionState,
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit // Puedes usar esto para lógicas más específicas
) {
    // Referencia: https://google.github.io/accompanist/permissions/
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val textToShow = if (permissionState.status.shouldShowRationale) {
            // Si el usuario denegó el permiso previamente, muestra una justificación.
            "El permiso de cámara es importante para escanear los códigos QR. Por favor, concédelo."
        } else {
            // Si es la primera vez o el permiso fue denegado y "No volver a preguntar" fue seleccionado.
            "Necesitamos permiso para acceder a la cámara y poder escanear los códigos QR."
        }

        Text(text = textToShow, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { permissionState.launchPermissionRequest() }) {
            Text("Solicitar Permiso")
        }

        // CORRECCIÓN AQUÍ:
        // Si el permiso no está concedido Y no debemos mostrar una justificación (podría ser la primera vez o denegado permanentemente)
        // Y el estado del permiso indica que no está concedido (esto es un poco redundante con la primera parte de la condición,
        // pero asegura que estamos en un estado de "no concedido").
        if (!permissionState.status.isGranted && !permissionState.status.shouldShowRationale) {
            // Este bloque se mostrará si:
            // 1. Es la primera vez que se pide el permiso (shouldShowRationale es false).
            // 2. El usuario denegó el permiso y marcó "No volver a preguntar" (shouldShowRationale es false).
            // No podemos distinguir fácilmente entre estos dos solo con esta información,
            // pero en ambos casos, si el usuario no otorga el permiso después de hacer clic en "Solicitar Permiso",
            // y shouldShowRationale sigue siendo false, entonces es probable que sea una denegación permanente.
            // Para una lógica más robusta sobre "denegado permanentemente", a menudo se guarda un estado propio.
            // Por ahora, este mensaje puede servir como una guía general.

            // Una forma más simple de pensar en esto: si el permiso NO está concedido
            // Y la UI NO debería mostrar una justificación para volver a solicitarlo,
            // entonces es probable que el usuario necesite ir a la configuración.
            if (permissionState.status != PermissionStatus.Granted) { // Chequeo adicional para claridad
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Si denegaste el permiso permanentemente o es la primera vez, " +
                            "haz clic en 'Solicitar Permiso'. Si no funciona, puede que necesites " +
                            "habilitarlo desde la configuración de la aplicación.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
                // Button(onClick = { /* Lógica para abrir config de la app */ }) { Text("Abrir Configuración") }
            }
        }
    }
}
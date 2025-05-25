package com.fastpack.ui.prepare

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.fastpack.data.model.ShipmentResponse
import com.fastpack.data.model.ShippedItemsPhoto
import com.fastpack.data.repository.ShipmentRepository // Asumo que existe
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Estados de la UI para PrepareScreen
sealed class PrepareScreenState {
    data object Idle : PrepareScreenState()
    data object RequestingPermission : PrepareScreenState()
    data object Scanning : PrepareScreenState()
    data object Loading : PrepareScreenState()
    data class ShowResult(
        val shipment: ShipmentResponse,
        val photoUri: Uri? = null,
        val isUploading: Boolean = false,
        val uploadSuccess: Boolean? = null
    ) : PrepareScreenState() // Modificado

    data class NoResult(val scannedCode: String) : PrepareScreenState()
    data class Error(val message: String) : PrepareScreenState()
}

@HiltViewModel
class PrepareViewModel @Inject constructor(
    private val shipmentRepository: ShipmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<PrepareScreenState>(PrepareScreenState.Idle)
    val uiState: StateFlow<PrepareScreenState> = _uiState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome = _navigateToHome.asSharedFlow()


    // Para almacenar la URI temporal de la foto tomada
    var tempPhotoUri by mutableStateOf<Uri?>(null)
        private set

    var resetScannerAction: (() -> Unit)? = null

    fun onReadyToScanAgain() {
        _uiState.value = PrepareScreenState.Scanning
        resetScannerAction?.invoke()
        Log.d("PrepareViewModel", "Retrying scan. Transitioning to Scanning and reset analyzer.")
    }

    fun onCameraPermissionResult(isGranted: Boolean) {
        Log.d(
            "PrepareViewModel",
            "onCameraPermissionResult: isGranted=$isGranted. Current state: ${_uiState.value}"
        )
        if (isGranted) {
            // Si el permiso se concede, y no estamos ya mostrando un resultado o cargando, vamos a escanear.
            // Esto cubre el caso inicial y los reintentos después de conceder el permiso.
            if (_uiState.value !is PrepareScreenState.Loading && _uiState.value !is PrepareScreenState.ShowResult) {
                _uiState.value = PrepareScreenState.Scanning
                Log.d(
                    "PrepareViewModel",
                    "Permission granted. Transitioning to Scanning."
                )
            }
        } else {
            // Si el permiso no está concedido (o es revocado y no estamos mostrando un resultado),
            // vamos a RequestingPermission.
            if (_uiState.value !is PrepareScreenState.ShowResult) {
                _uiState.value = PrepareScreenState.RequestingPermission
                Log.d(
                    "PrepareViewModel",
                    "Permission denied. Transitioning to RequestingPermission."
                )
            }
        }
    }

    // Este método es llamado por la UI cuando el usuario quiere iniciar el escaneo (si estaba en Idle o RequestingPermission y obtiene permiso)
    // O directamente por onCameraPermissionResult si el permiso se concede
    fun startScanning() {
        // Solo cambia a Scanning si estamos en un estado donde tiene sentido (Idle o después de obtener permiso)
        // y no estamos ya cargando o mostrando resultados.
        val currentState = _uiState.value
        if (currentState == PrepareScreenState.Idle || currentState == PrepareScreenState.RequestingPermission) {
            // La transición a Scanning ahora la maneja onCameraPermissionResult
            // Esta función podría ser llamada por la UI para *intentar* escanear,
            // pero la lógica de permiso en la UI (o aquí) decidirá.
            // Por ahora, asumimos que la UI llama a onCameraPermissionResult (o un equivalente)
            // y luego, si el permiso está OK, onCameraPermissionResult cambia a Scanning.
            // Si quieres un botón "Escanear" explícito incluso con permisos, esta función sería el onClick.
            Log.d(
                "PrepareViewModel",
                "startScanning called. Requesting UI to check permissions or transition to Scanning if already granted."
            )
            // En este modelo, si los permisos ya están, onCameraPermissionResult(true) debería haber llevado a Scanning.
            // Si no, la UI debería solicitar permisos y luego llamar a onCameraPermissionResult.
            // Para simplificar, si se llama a startScanning y el estado es Idle, es porque la UI
            // ya verificó permisos o es la primera vez.
            if (currentState == PrepareScreenState.Idle) {
                _uiState.value =
                    PrepareScreenState.Scanning // Asumimos que los permisos se comprobarán/concederán en la UI
            }

        } else if (currentState is PrepareScreenState.NoResult || currentState is
                    PrepareScreenState.Error
        ) {
            // Para reintentos desde NoResult o Error
            _uiState.value = PrepareScreenState.Scanning
            Log.d("PrepareViewModel", "Retrying scan. Transitioning to Scanning.")
        }
        Log.d(
            "PrepareViewModel",
            "startScanning called. Current state: ${_uiState.value}. New state attempt: Scanning (if conditions met)."
        )
    }


    fun onQrCodeScanned(qrCode: String) {
        // Solo procesar si realmente estamos escaneando
        if (_uiState.value == PrepareScreenState.Scanning) {
            Log.d(
                "PrepareViewModel",
                "onQrCodeScanned: $qrCode. Current state: Scanning. Transitioning to Loading."
            )
            _uiState.value = PrepareScreenState.Loading
            fetchShipmentDetails(qrCode)
        } else {
            Log.w(
                "PrepareViewModel",
                "onQrCodeScanned called but state is not Scanning. State: ${_uiState.value}. QR: $qrCode"
            )
        }
    }

    fun updateTempPhotoUri(uri: Uri?) {
        val currentState = _uiState.value
        if (currentState is PrepareScreenState.ShowResult) {
            _uiState.value = currentState.copy(photoUri = uri, uploadSuccess = null, isUploading = false) // Resetea uploadSuccess y isUploading
        }
    }

    fun clearTempPhoto() {
        val currentState = _uiState.value
        if (currentState is PrepareScreenState.ShowResult) {
            _uiState.value = currentState.copy(photoUri = null, uploadSuccess = null, isUploading = false)
        }
    }

    fun uploadPhotoAndUpdateShipment(
        context: Context,
        shipment: ShipmentResponse,
        photoUri: Uri
    ) {
        val currentState = _uiState.value
        if (currentState is PrepareScreenState.ShowResult) {
            _uiState.value = currentState.copy(isUploading = true, uploadSuccess = null)
        } else {
            Log.e(
                "PrepareViewModel",
                "uploadPhotoAndUpdateShipment llamado en estado incorrecto: $currentState"
            )
            _uiState.value = PrepareScreenState.Error("Error al iniciar subida.")
            return
        }

        MediaManager.get().upload(photoUri)
            .unsigned("FastPackApp")
            .option("folder", "packing")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("PrepareViewModel", "Cloudinary: Upload onStart")
                }

                override fun onProgress(
                    requestId: String,
                    bytes: Long,
                    totalBytes: Long
                ) {
                    // Puedes usar esto para mostrar progreso si lo deseas
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    Log.d("PrepareViewModel", "Cloudinary: Upload onSuccess: $resultData")
                    val secureUrl = resultData["secure_url"] as? String
                    val publicId =
                        resultData["public_id"] as? String // Obtener el public_id

                    if (secureUrl != null && publicId != null) {
                        // Crear la instancia de ShippedItemsPhoto
                        val photoInfo =
                            ShippedItemsPhoto(url = secureUrl, publicId = publicId)

                        // Actualizar el shipment con el objeto ShippedItemsPhoto
                        val updatedShipment = shipment.copy(shippedItemsPhoto = photoInfo)
                        saveUpdatedShipment(updatedShipment)
                    } else {
                        var errorMessage = "Cloudinary: "
                        if (secureUrl == null) errorMessage += "secure_url no encontrada. "
                        if (publicId == null) errorMessage += "public_id no encontrado."
                        Log.e("PrepareViewModel", errorMessage)

                        // Actualizar el estado de la UI para reflejar el error
                        val currentUiState = _uiState.value // Re-check current state
                        if (currentUiState is PrepareScreenState.ShowResult) {
                            _uiState.value = currentUiState.copy(
                                isUploading = false,
                                uploadSuccess = false
                            )
                        } else {
                            _uiState.value =
                                PrepareScreenState.Error("Error al obtener datos de Cloudinary.")
                        }
                    }
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e(
                        "PrepareViewModel",
                        "Cloudinary: Upload onError: ${error.description}"
                    )
                    val currentUiState = _uiState.value // Re-check current state
                    if (currentUiState is PrepareScreenState.ShowResult) {
                        _uiState.value = currentUiState.copy(
                            isUploading = false,
                            uploadSuccess = false
                        )
                    } else {
                        _uiState.value =
                            PrepareScreenState.Error("Error al subir imagen: ${error.description}")
                    }
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.w(
                        "PrepareViewModel",
                        "Cloudinary: Upload onReschedule: ${error.description}"
                    )
                    // Manejar si la subida se reprograma
                    val currentUiState = _uiState.value
                    if (currentUiState is PrepareScreenState.ShowResult) {
                        // Podrías mantener isUploading en true o informar al usuario.
                        // Por ahora, lo marcamos como un fallo para simplificar.
                        _uiState.value = currentUiState.copy(
                            isUploading = false,
                            uploadSuccess = false
                        )
                    }
                }
            }).dispatch(context) // Asegúrate de pasar el contexto correcto
    }

    private fun saveUpdatedShipment(shipmentToUpdate: ShipmentResponse) {
        viewModelScope.launch {
            Log.d(
                "PrepareViewModel",
                "Guardando shipment actualizado: ${shipmentToUpdate.id} con foto: ${shipmentToUpdate.shippedItemsPhoto?.url}"
            ) // Accede a la URL dentro del objeto
            val result = shipmentRepository.updateShipment(shipmentToUpdate)
            result.fold(
                onSuccess = { updatedShipmentFromServer ->
                    if (updatedShipmentFromServer != null) {
                        Log.d(
                            "PrepareViewModel",
                            "Shipment actualizado en servidor con éxito. Foto URL: ${updatedShipmentFromServer.shippedItemsPhoto?.url}"
                        )
                        _uiState.value = PrepareScreenState.ShowResult(
                            shipment = updatedShipmentFromServer,
                            photoUri = null,
                            isUploading = false,
                            uploadSuccess = true
                        )
                        tempPhotoUri = null
                        _navigateToHome.emit(Unit)
                    } else {
                        Log.e(
                            "PrepareViewModel",
                            "Error al guardar: el servidor devolvió null."
                        )
                        val currentUiState = _uiState.value
                        if (currentUiState is PrepareScreenState.ShowResult) {
                            // Mantenemos la información local del shipment (que incluye el objeto ShippedItemsPhoto con la URL fallida o los datos de Cloudinary)
                            _uiState.value = currentUiState.copy(
                                shipment = shipmentToUpdate, // El que tiene la info de cloudinary aunque no se guardó en backend
                                isUploading = false,
                                uploadSuccess = false
                            )
                        } else {
                            _uiState.value =
                                PrepareScreenState.Error("Error al guardar: respuesta nula del servidor.")
                        }
                    }
                },
                onFailure = { exception ->
                    Log.e(
                        "PrepareViewModel",
                        "Error al guardar shipment actualizado.",
                        exception
                    )
                    val currentUiState = _uiState.value
                    if (currentUiState is PrepareScreenState.ShowResult) {
                        _uiState.value = currentUiState.copy(
                            shipment = shipmentToUpdate, // El que tiene la info de cloudinary aunque no se guardó en backend
                            isUploading = false,
                            uploadSuccess = false
                        )
                    } else {
                        _uiState.value =
                            PrepareScreenState.Error("Error al guardar: ${exception.message}")
                    }
                }
            )
        }
    }

    private fun fetchShipmentDetails(shipmentIdString: String) {
        viewModelScope.launch {
            Log.d("PrepareViewModel", "Fetching details for: $shipmentIdString")

            val shipmentId = shipmentIdString.toLongOrNull()
            if (shipmentId == null) {
                Log.e("PrepareViewModel", "Invalid shipment ID format: $shipmentIdString")
                _uiState.value = PrepareScreenState.Error("ID de envío inválido.")
                return@launch
            }

            // Tu repositorio devuelve: kotlin.Result<com.fastpack.data.model.ShipmentResponse?>
            val result: Result<ShipmentResponse?> =
                shipmentRepository.getShipmentById(shipmentId)

            result.fold(
                onSuccess = { shipmentResponseOrNull -> // shipmentResponseOrNull es de tipo ShipmentResponse?
                    if (shipmentResponseOrNull != null) {
                        // Ahora shipmentResponseOrNull es un ShipmentResponse (no nulo)
                        Log.d(
                            "PrepareViewModel",
                            "Successfully fetched shipment: ${shipmentResponseOrNull.id}"
                        ) // Ahora .id es accesible
                        _uiState.value =
                            PrepareScreenState.ShowResult(shipmentResponseOrNull) // Pasa el ShipmentResponse no nulo
                    } else {
                        // El Result fue exitoso, pero el valor dentro era null
                        // Esto podría significar "no encontrado" si tu API devuelve un success con cuerpo null para 404
                        Log.d(
                            "PrepareViewModel",
                            "Shipment not found (success with null data) for ID: $shipmentId"
                        )
                        _uiState.value = PrepareScreenState.NoResult(shipmentIdString)
                    }
                },
                onFailure = { exception ->
                    // El Result fue un failure, contiene una excepción
                    Log.e(
                        "PrepareViewModel",
                        "Error fetching shipment details for ID: $shipmentId",
                        exception
                    )
                    // Aquí podrías querer diferenciar tipos de excepciones si es necesario
                    // Por ejemplo, si una excepción específica de tu red significa "no encontrado" vs. un error de red genérico.
                    // Por ahora, un error genérico o NoResult.
                    _uiState.value =
                        PrepareScreenState.Error("Error al obtener detalles: ${exception.message ?: "Error desconocido"}")
                    // O si consideras cualquier fallo de la llamada como "No Encontrado":
                    // _uiState.value = PrepareScreenState.NoResult(shipmentIdString)
                }
            )
        }
    }

    fun retryScanOrRequestPermission() {
        // Esta función es para que la UI llame cuando quiera reintentar.
        // La UI debería manejar la solicitud de permisos si es necesario ANTES de llamar a esto,
        // o esta función podría evolucionar para verificar el estado de los permisos.
        // Por ahora, asumimos que si se llama a esto, es para volver al estado de escaneo.
        Log.d(
            "PrepareViewModel",
            "retryScanOrRequestPermission called. Current state: ${_uiState.value}"
        )
        // Si el estado actual es NoResult, Error, o incluso Idle/RequestingPermission (y los permisos ya se verificaron/concedieron)
        // Deberíamos ir a Scanning.
        // La lógica de permisos es clave y se maneja mejor en la UI con Accompanist y luego notificando al VM.
        // Aquí, simplemente intentamos volver a escanear.
        _uiState.value = PrepareScreenState.Scanning
    }

    fun goBackToIdle() {
        // Para volver al estado inicial desde un resultado o error, permitiendo un nuevo escaneo
        // si el usuario lo desea (la UI manejaría el flujo de permisos nuevamente si es necesario).
        _uiState.value = PrepareScreenState.Idle
        Log.d("PrepareViewModel", "Transitioning to Idle.")
    }

    // Inicialización: Comprobar el estado del permiso al inicio
    // Esto es más un rol de la UI (Activity/Composable) que notifica al ViewModel.
    // El ViewModel reacciona a los eventos de permiso a través de `onCameraPermissionResult`.
    // Así que el estado inicial `Idle` es correcto. La UI determinará si necesita
    // pedir permisos y luego llamará a `onCameraPermissionResult`.
}
package com.fastpack.ui.prepare.composables

// import androidx.camera.core.ExperimentalGetImage // Comentado si no se usa directamente ImageProxy.image
import android.annotation.SuppressLint
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (String) -> Unit,
    onAnalyzerReady: ((() -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    val barcodeScanner = remember {
        Log.d("CameraPreviewView", "Creating BarcodeScanner instance")
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    val qrCodeAnalyzer = remember(barcodeScanner) {
        QrCodeAnalyzer(
            onBarcodeScanned = onBarcodeScanned,
            scanner = barcodeScanner
        )
    }

    LaunchedEffect(qrCodeAnalyzer) {
        onAnalyzerReady?.invoke { qrCodeAnalyzer.reset() }
    }

    // Ya no necesitamos 'var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }'

    // LaunchedEffect para configurar la cámara y DisposableEffect para limpiar,
    // ambos vinculados a lifecycleOwner y barcodeScanner.
    // El cameraProvider se obtendrá y usará dentro del mismo efecto.
    DisposableEffect(lifecycleOwner, barcodeScanner) {
        Log.d("CameraPreviewView", "DisposableEffect setup/re-evaluation for camera.")
        val cameraExecutor = Executors.newSingleThreadExecutor()
        var boundCameraProvider: ProcessCameraProvider? = null // Para mantener referencia al provider usado

        val job = CoroutineScope(Dispatchers.Main + SupervisorJob()).launch {
            // ... tu código de configuración de la cámara aquí dentro ...
            try {
                val obtainedCameraProvider = ProcessCameraProvider.getInstance(context).await()
                boundCameraProvider = obtainedCameraProvider // Guardar para la limpieza

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, qrCodeAnalyzer)
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                obtainedCameraProvider.unbindAll() // Buena práctica
                Log.d("CameraPreviewView", "Unbinding all before binding.")

                obtainedCameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
                Log.d("CameraPreviewView", "Camera bound to lifecycle. Analyzer configured.")

            } catch (e: Exception) {
                Log.e("CameraPreviewView", "Error setting up camera: ${e.message}", e)
                if (e is CameraUnavailableException) {
                    Log.e("CameraPreviewView", "Camera is unavailable: ${e.message}", e)
                } else if (e is IllegalStateException && e.message?.contains("CameraSelector references unresolved camera") == true) {
                    Log.e("CameraPreviewView", "No camera available for CameraSelector.DEFAULT_BACK_CAMERA: ${e.message}", e)
                }
            }
        }

        onDispose {
            Log.d("CameraPreviewView", "DisposableEffect: onDispose. boundCameraProvider: $boundCameraProvider")
            job.cancel() // Cancelar la corutina si aún está activa
            boundCameraProvider?.unbindAll()
            // barcodeScanner.close() // Se puede cerrar aquí o si el scanner se gestiona fuera, fuera.
            // Ya que está en el remember del Composable, se cerrará cuando el composable se vaya.
            // Pero cerrarlo aquí es más seguro si el ciclo de vida del composable es complejo.
            cameraExecutor.shutdown() // ¡Importante liberar el executor!
            Log.d("CameraPreviewView", "Camera unbound, executor shutdown. BarcodeScanner might be closed by its remember scope.")
        }
    }
    // Mueve el cierre de barcodeScanner a un DisposableEffect separado si quieres ser explícito
    // o si el `remember` de barcodeScanner tiene una vida más larga que este DisposableEffect.
    // Por ahora, el remember { barcodeScanner.close() } debería ser suficiente cuando el Composable se va.
    // Para mayor seguridad, puedes hacer esto:
    DisposableEffect(barcodeScanner) {
        onDispose {
            Log.d("CameraPreviewView", "Closing BarcodeScanner in its own DisposableEffect.")
            barcodeScanner.close()
        }
    }


    Box(modifier = modifier) {
        AndroidView(
            factory = {
                Log.d("CameraPreviewView", "AndroidView factory for PreviewView.")
                previewView
            },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

class QrCodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit,
    private val scanner: BarcodeScanner
) : ImageAnalysis.Analyzer {

    private var isProcessing = AtomicBoolean(false)
    private var hasScanned = AtomicBoolean(false)

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        if (isProcessing.getAndSet(true) || hasScanned.get()) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        for (barcode in barcodes) {
                            val rawValue = barcode.rawValue
                            // NUEVO: Obtener y registrar el formato del código de barras
                            val barcodeFormat = barcode.format
                            val formatString = getBarcodeFormatString(barcodeFormat) // Función helper

                            Log.d("QrCodeAnalyzer", "Barcode detected. Value: $rawValue, Format ID: $barcodeFormat, Format Name: $formatString, Type: ${barcode.valueType}")

                            // Validación existente: 11 caracteres y todos numéricos
                            if (rawValue != null && rawValue.length == 11 && rawValue.all { it.isDigit() }) {
                                if (!hasScanned.getAndSet(true)) {
                                    Log.i("QrCodeAnalyzer", "VALID barcode found. Value: $rawValue, Format: $formatString. Processing...")
                                    onBarcodeScanned(rawValue) // Llama al callback
                                }
                            } else {
                                Log.d("QrCodeAnalyzer", "Ignored barcode (failed validation or already processed). Value: $rawValue, Format: $formatString")
                            }
                        }
                    } else {
                        // Log.v("QrCodeAnalyzer", "No barcodes found in this frame.") // Opcional: para mucho detalle
                    }
                }
                .addOnFailureListener {
                    Log.e("QrCodeAnalyzer", "Barcode scanning failed", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                    isProcessing.set(false)
                }
        } else {
            imageProxy.close()
            isProcessing.set(false)
        }
    }

    // Método para permitir un nuevo escaneo si es necesario
    fun reset() {
        hasScanned.set(false)
        isProcessing.set(false)
        Log.d("QrCodeAnalyzer", "Analyzer reset for new scan.")
    }

    // NUEVO: Función helper para convertir el ID del formato a un nombre legible
    private fun getBarcodeFormatString(format: Int): String {
        return when (format) {
            Barcode.FORMAT_UNKNOWN -> "UNKNOWN"
            Barcode.FORMAT_ALL_FORMATS -> "ALL_FORMATS"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "OTHER (${format})"
        }
    }
}

@Composable
private fun rememberPreviewView(context: android.content.Context): PreviewView {
    return remember {
        PreviewView(context).apply {
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }
}
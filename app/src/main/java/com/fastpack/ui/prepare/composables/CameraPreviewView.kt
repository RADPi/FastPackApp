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
import com.fastpack.services.BarcodeAnalyzer
import com.fastpack.ui.prepare.PrepareViewModel
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
    barcodeAnalyzer: BarcodeAnalyzer,
    onAnalyzerReady: ((resetAction: () -> Unit) -> Unit)? = null,
    viewModel: PrepareViewModel
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

    LaunchedEffect(barcodeAnalyzer, viewModel) {
        onAnalyzerReady?.invoke {
            barcodeAnalyzer.reset()
        }
    }

    DisposableEffect(lifecycleOwner, barcodeAnalyzer) {
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


                val imageAnalyzerUseCase = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        if (barcodeAnalyzer is ImageAnalysis.Analyzer) {
                            it.setAnalyzer(cameraExecutor, barcodeAnalyzer)
                        } else {
                            Log.e("CameraPreviewView", "Provided barcodeAnalyzer does not implement ImageAnalysis.Analyzer")
                            // Podrías lanzar una excepción o manejarlo de otra forma
                        }
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                obtainedCameraProvider.unbindAll() // Buena práctica
                Log.d("CameraPreviewView", "Unbinding all before binding.")

                obtainedCameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalyzerUseCase
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
    DisposableEffect(barcodeAnalyzer) {
        onDispose {
            barcodeAnalyzer.release() // Llama a scanner.close()
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


package com.fastpack.ui.prepare.composables

// import androidx.camera.core.ExperimentalGetImage // Comentado si no se usa directamente ImageProxy.image
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraSelector
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraPreviewView(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // PreviewView debe ser recordada para que sobreviva a las recomposiciones sin recrearse
    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER // O la que prefieras
            // Implementations mode es importante para el rendimiento y la correcta visualización
            // COMPATIBLE usa SurfaceView, PERFORMANCE usa TextureView.
            // PERFORMANCE suele ser mejor si no necesitas características específicas de SurfaceView.
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
        }
    }

    // El cameraProviderFuture se puede obtener dentro del LaunchedEffect para simplificar.
    // No necesitamos un estado mutable para cameraProvider si solo se usa dentro del LaunchedEffect y DisposableEffect.

    // LaunchedEffect para configurar la cámara.
    // Se ejecutará cuando lifecycleOwner cambie (lo cual es bueno, se adapta al ciclo de vida).
    // Usar 'Unit' como clave si solo necesitas que se ejecute una vez y se limpie al salir.
    // Sin embargo, depender de lifecycleOwner es correcto para la configuración de la cámara.
    LaunchedEffect(lifecycleOwner) {
        Log.d("CameraPreviewView", "LaunchedEffect: Setting up camera for lifecycleOwner: $lifecycleOwner")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraProvider = cameraProviderFuture.get() // Bloqueante, pero seguro en LaunchedEffect

        // Desvincular todo antes de empezar para evitar conflictos
        // Esto es útil si el composable se recompone y este LaunchedEffect se relanza.
        // Aunque bindToLifecycle debería manejar esto, una limpieza explícita puede ser más segura.
        try {
            cameraProvider.unbindAll() // Desvincula cualquier uso anterior de la cámara.
            Log.d("CameraPreviewView", "CameraProvider unbindAll() called before binding.")
        } catch (e: Exception) {
            Log.e("CameraPreviewView", "Error on unbindAll before binding: ${e.message}", e)
            // Considera cómo manejar este error. Podría significar que la cámara no estaba bien inicializada.
        }


        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(
                    Executors.newSingleThreadExecutor(), // Considera un Executor compartido/inyectado si es usado en múltiples lugares
                    QrCodeAnalyzer { qrCode ->
                        onQrCodeScanned(qrCode)
                    }
                )
            }

        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            // Vincula los casos de uso al ciclo de vida del composable.
            // CameraX maneja automáticamente la activación/desactivación de la cámara
            // basado en el estado del lifecycleOwner.
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            Log.d("CameraPreviewView", "Camera bound to lifecycle. LifecycleOwner: $lifecycleOwner")
        } catch (exc: Exception) {
            Log.e("CameraPreviewView", "Failed to bind camera use cases", exc)
        }

        // No necesitas un onDispose aquí dentro de LaunchedEffect para desvincular,
        // ya que bindToLifecycle ya asocia la cámara al ciclo de vida del lifecycleOwner.
        // CameraX desvinculará automáticamente cuando el lifecycleOwner sea destruido (ej. al navegar fuera de la pantalla).
        // Sin embargo, si quieres una limpieza más explícita o si unbindAll() al principio no es suficiente
        // en todos los escenarios de recomposición, un DisposableEffect separado podría ser considerado,
        // pero idealmente bindToLifecycle es suficiente.

        // Si tuvieras un DisposableEffect, sería así, pero ten cuidado con la clave.
        // Si la clave es solo `cameraProvider`, se desvinculará si `cameraProvider` cambia,
        // lo cual no debería suceder si lo obtienes una vez. Si la clave es `lifecycleOwner`,
        // se desvinculará cuando el `lifecycleOwner` cambie o sea destruido, lo cual es lo que `bindToLifecycle` ya hace.
        // Por lo tanto, un DisposableEffect explícito para desvincular aquí puede ser redundante y causar problemas
        // si no se maneja correctamente (como la desvinculación prematura que observaste).

        // -> La clave de tu problema original estaba probablemente en un DisposableEffect
        // -> con una clave que cambiaba innecesariamente, o que el mismo `cameraProvider`
        // -> estaba cambiando de instancia, provocando la ejecución del `onDispose`.

        // Vamos a confiar en que bindToLifecycle maneje la desvinculación cuando el lifecycleOwner
        // se destruya.
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { previewView }, // Simplemente devuelve la PreviewView recordada
            modifier = Modifier.fillMaxSize(),
            // El bloque update no es estrictamente necesario si la configuración inicial
            // de la previewView (como layoutParams, scaleType) se hace al crearla
            // y el surfaceProvider se establece en el LaunchedEffect.
            // update = { view ->
            //    Log.d("CameraPreviewView", "AndroidView update block. PreviewView: $view")
            //    // Aquí podrías reaccionar a cambios si `previewView` necesitara
            //    // reconfigurarse dinámicamente, pero es menos común para este caso.
            // }
        )
    }

    // Si decides que NECESITAS un DisposableEffect para desvincular explícitamente,
    // asegúrate de que sus claves sean correctas.
    // Usar `lifecycleOwner` como clave aquí significa que `onDispose` se llamará
    // cuando este `CameraPreviewView` deje la composición O cuando `lifecycleOwner` cambie
    // a una nueva instancia (lo que implicaría una nueva configuración de todos modos).
    // La clave `Unit` haría que `onDispose` se llame solo cuando `CameraPreviewView` deje la composición.
    // **Importante**: `bindToLifecycle` ya hace la limpieza cuando el `lifecycleOwner` se destruye.
    // Añadir un `DisposableEffect` adicional para `unbindAll()` podría ser redundante o
    // incluso causar problemas si las condiciones no son las correctas.
    //
    // El problema que tenías (desvinculación inmediata) podría haber sido porque:
    // 1. El `lifecycleOwner` estaba cambiando inmediatamente después de la configuración.
    // 2. La instancia de `cameraProvider` en el `DisposableEffect` cambiaba o era null inicialmente y luego no null,
    //    disparando el `onDispose` del efecto anterior.
    //
    // Si confías en `bindToLifecycle`, este `DisposableEffect` podría no ser necesario.
    // Si lo mantienes, asegúrate de que `cameraProviderFuture.get()` se llame una sola vez
    // y que la instancia de `cameraProvider` sea estable.
    //
    // **Por ahora, lo comentaré para basarnos en el comportamiento de `bindToLifecycle`.**
    // **Si sigues viendo problemas de recursos no liberados, podríamos reevaluarlo.**

    DisposableEffect(lifecycleOwner) { // O `Unit` si solo quieres limpieza al salir del composable
        onDispose {
            Log.d("CameraPreviewView", "DisposableEffect: onDispose. Attempting to unbind all. LifecycleOwner: $lifecycleOwner")
            // Es crucial obtener el cameraProvider de una manera que no cause
            // que este onDispose se llame prematuramente.
            // Si cameraProvider se obtiene de forma asíncrona y es null inicialmente,
            // este bloque podría ejecutarse cuando `lifecycleOwner` aún está activo.
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            try {
                // No es ideal hacer .get() en onDispose si puede bloquear,
                // pero si el provider ya fue obtenido, debería ser rápido.
                // Sin embargo, si el LaunchedEffect ya tiene el provider, podrías pasarlo
                // o recuperarlo de una manera más segura.
                // El problema es que cameraProvider (como estado mutable) podría haber cambiado.
                val provider = cameraProviderFuture.get() // Riesgo si aún no está listo o si el contexto ya no es válido
                provider.unbindAll()
                Log.d("CameraPreviewView", "Camera unbinding explicitly in DisposableEffect onDispose")
            } catch (e: Exception) {
                Log.e("CameraPreviewView", "Error unbinding camera in DisposableEffect: ${e.message}", e)
            }
        }
    }

}

class QrCodeAnalyzer(
    private val onQrCodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    // @ExperimentalGetImage // Anotación a nivel de clase o uso específico si accedes a imageProxy.image
    // Aunque no está en tu código original, si usas imageProxy.image debes tenerla.
    // La plantilla no la pone automáticamente en el constructor.

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
    )
    private var isProcessing = AtomicBoolean(false)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class) // Necesario para imageProxy.image
    override fun analyze(imageProxy: ImageProxy) {
        if (!isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    barcodes.firstOrNull()?.rawValue?.let { qrCode ->
                        Log.d("QrCodeAnalyzer", "Código QR detectado: $qrCode")
                        // Asegúrate de que este callback no tarde mucho o cause problemas de UI.
                        // Si necesitas cambiar de hilo, hazlo dentro del callback.
                        onQrCodeScanned(qrCode)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("QrCodeAnalyzer", "Fallo el escaneo de código de barras", e)
                }
                .addOnCompleteListener {
                    // Cierra la imagen SIEMPRE después del procesamiento.
                    // Esto es crucial para que CameraX pueda entregar el siguiente frame.
                    imageProxy.close()
                    isProcessing.set(false) // Permite el siguiente frame
                }
        } else {
            // Si mediaImage es null, aún debemos cerrar imageProxy y resetear isProcessing.
            imageProxy.close()
            isProcessing.set(false)
        }
    }
}
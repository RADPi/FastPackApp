package com.fastpack.services // O el paquete que prefieras para servicios/utilidades

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicBoolean

// Define una data class para deserializar el JSON del QR
@Serializable
data class QrCodeData(
    val id: String,
    val sender_id: Long? = null, // Opcional si no siempre está
    val hash_code: String? = null, // Opcional
    val security_digit: String? = null // Opcional
)

interface BarcodeAnalyzer {
    fun analyze(imageProxy: ImageProxy)
    fun reset()
    fun release() // Para liberar recursos como el scanner
}

class MLKitBarcodeAnalyzer(
    private val onBarcodeScanned: (String) -> Unit,
) : BarcodeAnalyzer, ImageAnalysis.Analyzer {

    private val scanner: BarcodeScanner
    private var isProcessing = AtomicBoolean(false)
    private var hasScanned = AtomicBoolean(false)

    // Configura el parser de JSON para ser permisivo con claves desconocidas
    private val jsonParser = Json { ignoreUnknownKeys = true }

    init {
        Log.d("MLKitBarcodeAnalyzer", "Initializing BarcodeScanner")
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                // Agrega otros formatos si es necesario
            )
            .build()
        scanner = BarcodeScanning.getClient(options)
    }

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
                            val barcodeFormat = barcode.format
                            val formatString = getBarcodeFormatString(barcodeFormat)

                            Log.d("MLKitBarcodeAnalyzer", "Barcode detected. Raw Value: $rawValue, Format: $formatString, Type: ${barcode.valueType}")

                            if (rawValue != null) {
                                val extractedValue = processBarcode(rawValue, barcodeFormat)

                                if (extractedValue != null && isValidBarcodeValue(extractedValue)) { // Lógica de validación sobre el valor extraído
                                    if (!hasScanned.getAndSet(true)) {
                                        Log.i("MLKitBarcodeAnalyzer", "VALID barcode found. Extracted Value: $extractedValue, Format: $formatString. Processing...")
                                        onBarcodeScanned(extractedValue)
                                    }
                                } else {
                                    Log.d("MLKitBarcodeAnalyzer", "Ignored barcode (failed validation, no value extracted, or already processed). Raw: $rawValue, Extracted: $extractedValue, Format: $formatString")
                                }
                            }
                        }
                    }
                }
                .addOnFailureListener {
                    Log.e("MLKitBarcodeAnalyzer", "Barcode scanning failed", it)
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

    private fun processBarcode(rawValue: String, format: Int): String? {
        return when (format) {
            Barcode.FORMAT_CODE_128 -> {
                // Para CODE_128, el rawValue es directamente el valor a validar
                rawValue
            }
            Barcode.FORMAT_QR_CODE -> {
                // Para QR_CODE, parsear JSON y extraer "id"
                try {
                    val qrData = jsonParser.decodeFromString<QrCodeData>(rawValue)
                    Log.d("MLKitBarcodeAnalyzer", "QR Data parsed: id='${qrData.id}'")
                    qrData.id
                } catch (e: Exception) {
                    Log.e("MLKitBarcodeAnalyzer", "Failed to parse QR code JSON: $rawValue", e)
                    null
                }
            }
            else -> {
                // Para otros formatos, podrías devolver rawValue o null si no los manejas específicamente
                Log.w("MLKitBarcodeAnalyzer", "Unhandled barcode format for processing: ${getBarcodeFormatString(format)}")
                null // O rawValue si quieres intentar validarlo directamente
            }
        }
    }

    private fun isValidBarcodeValue(value: String): Boolean {
        // Validación común: 11 caracteres y todos numéricos
        // Esta validación se aplica al valor extraído (sea directo de CODE_128 o el "id" del QR)
        val isValid = value.length == 11 && value.all { it.isDigit() }
        if (!isValid) {
            Log.d("MLKitBarcodeAnalyzer", "Validation failed for value: '$value'. Length: ${value.length}, IsDigit: ${value.all { it.isDigit() }}")
        }
        return isValid
    }

    override fun reset() {
        hasScanned.set(false)
        isProcessing.set(false)
        Log.d("MLKitBarcodeAnalyzer", "Analyzer reset for new scan.")
    }

    override fun release() {
        Log.d("MLKitBarcodeAnalyzer", "Closing BarcodeScanner.")
        scanner.close() // Asegúrate de que el scanner se cierre
    }

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
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "OTHER_FORMAT (${format})"
        }
    }
}
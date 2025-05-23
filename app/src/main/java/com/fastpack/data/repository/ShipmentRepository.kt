package com.fastpack.data.repository

import android.util.Log
import com.fastpack.data.model.ShipmentResponse
import com.fastpack.data.remote.ShipmentService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShipmentRepository @Inject constructor(
    private val shipmentService: ShipmentService // Inyecta el servicio correcto
) {
    suspend fun findShipments(
        trackingNumber: String? = null,
        status: String? = null,
        orderId: String? = null
    ): Result<List<ShipmentResponse>> { // Envuelve en Result para mejor manejo de errores
        return try {
            val response = shipmentService.getShipments(
                trackingNumber = trackingNumber,
                status = status,
                orderId = orderId
                // ... pasar otros parámetros
            )
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                // Puedes parsear el error del cuerpo aquí si es necesario
                // val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Error de la API: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e) // Captura errores de red, deserialización, etc.
        }
    }

    // Si usas el QueryMap
    suspend fun searchShipmentsAdvanced(queryParams: Map<String, String>): Result<List<ShipmentResponse>> {
        return try {
            val response = shipmentService.searchShipments(queryParams)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Respuesta vacía del servidor"))
            } else {
                Result.failure(Exception("Error de la API: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getShipmentById(shipmentId: Long): Result<ShipmentResponse?> {
        return try {
            val response = shipmentService.getShipmentById(shipmentId)
            if (response.isSuccessful) {
                Result.success(response.body())
            } else if (response.code() == 404) {
                Result.success(null) // Envío no encontrado
            } else {
                Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ShipmentRepository", "Error fetching shipment by ID $shipmentId", e)
            Result.failure(e)
        }
    }

    // Esta es la función que el ViewModel llamará
    suspend fun fetchAndAnalyzeShipmentsForPacking(): Result<Map<String, Int>> {
        // PASO 1 INTERNO DEL REPO: Llama a una función que usa el SERVICE
        val shipmentsResult = getShipmentsForPackingFromService()

        return shipmentsResult.fold(
            onSuccess = { shipmentList ->
                if (shipmentList.isEmpty()) {
                    Result.success(createEmptyAnalysisMap())
                } else {
                    // PASO 2 INTERNO DEL REPO: Llama a SU PROPIA función de análisis
                    val analysis = analyzeShipments(shipmentList)
                    Result.success(analysis)
                }
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }

    // Función interna del repo que usa el ShipmentService
    private suspend fun getShipmentsForPackingFromService(): Result<List<ShipmentResponse>> {
        return try {
            // AQUÍ SE LLAMA A shipmentService.getShipmentsForPacking()
            val response = shipmentService.getShipmentsForPacking()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Respuesta vacía del servidor..."))
            } else {
                Result.failure(Exception("Error de API [${response.code()}]..."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun analyzeShipments(shipments: List<ShipmentResponse>): Map<String, Int> {
        val totalEnvios = shipments.size
        var flexReadyToPrint = 0
        var flexPendientes = 0
        var despReadyToPrint = 0
        var despPendientes = 0

        for (shipment in shipments) {
            if (shipment.logisticType == "self_service") {
                if (shipment.substatus == "ready_to_print") {
                    flexReadyToPrint++
                } else {
                    flexPendientes++
                }
            } else {
                if (shipment.substatus == "ready_to_print") {
                    despReadyToPrint++
                } else {
                    despPendientes++
                }
            }
        }

        return mapOf(
            "TotalEnvios" to totalEnvios,
            "FlexReadyToPrint" to flexReadyToPrint,
            "FlexPendientes" to flexPendientes,
            "DespReadyToPrint" to despReadyToPrint,
            "DespPendientes" to despPendientes
        )
    }

    private fun createEmptyAnalysisMap(): Map<String, Int> {
        return mapOf(
            "TotalEnvios" to 0,
            "FlexReadyToPrint" to 0,
            "FlexPendientes" to 0,
            "DespReadyToPrint" to 0,
            "DespPendientes" to 0
        )
    }
}
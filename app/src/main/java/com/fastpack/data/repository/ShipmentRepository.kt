package com.fastpack.data.repository

import android.util.Log
import com.fastpack.data.model.ShipmentResponse
import com.fastpack.data.remote.ShipmentService
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.DESP_PENDIENTES
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.DESP_READY_TO_PREPARE
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.DESP_READY_TO_PRINT
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.FLEX_PENDIENTES
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.FLEX_READY_TO_PREPARE
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.FLEX_READY_TO_PRINT
import com.fastpack.data.repository.ShipmentRepository.ShipmentAnalysisKeys.TOTAL_ENVIOS
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

    suspend fun updateShipment(shipmentToUpdate: ShipmentResponse): Result<ShipmentResponse?> {

        return try {

            val response = shipmentService.updateShipment(shipmentToUpdate.id, shipmentToUpdate)

            if (response.isSuccessful) {
                Result.success(response.body())
            } else {
                Result.failure(Exception("Error de API al actualizar: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e("ShipmentRepository", "Excepción al actualizar shipment ${shipmentToUpdate.id}", e)
            Result.failure(e)
        }
    }

    suspend fun fetchAndAnalyzeShipmentsForPacking(): Result<Map<String, Int>> {
        val shipmentsResult = getShipmentsForPackingFromService()

        return shipmentsResult.fold(
            onSuccess = { shipmentList ->
                if (shipmentList.isEmpty()) {
                    Result.success(createEmptyAnalysisMap())
                } else {
                    val analysis = analyzeShipments(shipmentList)
                    Result.success(analysis)
                }
            },
            onFailure = { exception ->
                Result.failure(exception)
            }
        )
    }

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

    private fun analyzeShipments(shipments: List<ShipmentResponse>): Map<String, Int> {
        if (shipments.isEmpty()) return createEmptyAnalysisMap()

        val counts = mutableMapOf(
            TOTAL_ENVIOS to shipments.size,
            FLEX_READY_TO_PRINT to 0,
            FLEX_READY_TO_PREPARE to 0,
            FLEX_PENDIENTES to 0,
            DESP_READY_TO_PRINT to 0,
            DESP_READY_TO_PREPARE to 0,
            DESP_PENDIENTES to 0
        )

        for (shipment in shipments) {
            when (shipment.logisticType) {
                "self_service" -> {
                    if (shipment.substatus == "ready_to_print") {
                        if (shipment.shippedItemsPhoto != null) {
                            counts[FLEX_READY_TO_PREPARE] = counts[FLEX_READY_TO_PREPARE]!! + 1
                        } else counts[FLEX_READY_TO_PRINT] = counts[FLEX_READY_TO_PRINT]!! + 1
                    } else {
                        counts[FLEX_PENDIENTES] = counts[FLEX_PENDIENTES]!! + 1
                    }
                }
                else -> { // Asumiendo que cualquier otra cosa es "despacho" excepto Fullfilment que ni llegan
                    if (shipment.substatus == "ready_to_print") {
                        if (shipment.shippedItemsPhoto != null) {
                            counts[DESP_READY_TO_PREPARE] = counts[DESP_READY_TO_PREPARE]!! + 1
                        } else counts[DESP_READY_TO_PRINT] = counts[DESP_READY_TO_PRINT]!! + 1
                    } else {
                        counts[DESP_PENDIENTES] = counts[DESP_PENDIENTES]!! + 1
                    }
                }
            }
        }
        return counts
    }

    private fun createEmptyAnalysisMap(): Map<String, Int> {
        return mapOf(
            TOTAL_ENVIOS to 0,
            FLEX_READY_TO_PRINT to 0,
            FLEX_READY_TO_PREPARE to 0,
            FLEX_PENDIENTES to 0,
            DESP_READY_TO_PRINT to 0,
            DESP_READY_TO_PREPARE to 0,
            DESP_PENDIENTES to 0
        )
    }

    object ShipmentAnalysisKeys {
        const val TOTAL_ENVIOS = "TotalEnvios"
        const val FLEX_READY_TO_PRINT = "FlexReadyToPrint"
        const val FLEX_READY_TO_PREPARE = "FlexReadyToPrepare"
        const val FLEX_PENDIENTES = "FlexPendientes"
        const val DESP_READY_TO_PRINT = "DespReadyToPrint"
        const val DESP_READY_TO_PREPARE = "DespReadyToPrepare"
        const val DESP_PENDIENTES = "DespPendientes"
    }
}
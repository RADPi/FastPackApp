package com.fastpack.data.remote

import com.fastpack.data.model.ShipmentResponse // <-- IMPORTA TU NUEVO MODELO
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ShipmentService {

    @GET("api/shipments")
    suspend fun getShipments(
        @Query("tracking_number") trackingNumber: String? = null,
        @Query("status") status: String? = null,
        @Query("statuses") statuses: String? = null,
        @Query("order_id") orderId: String? = null,
        @Query("sender_id") senderId: Long? = null,
        @Query("date_from") dateFrom: String? = null,
        @Query("date_to") dateTo: String? = null,
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null
        // ... otros parámetros
    ): Response<List<ShipmentResponse>> // La API devuelve una lista de estos objetos

    /**
     * Alternativa para pasar un mapa de parámetros.
     * Útil si tienes muchos parámetros opcionales y no quieres listarlos todos.
     * Las claves del mapa deben ser los nombres de los parámetros de la URL.
     * Los valores deben ser Strings.
     */
    @GET("api/shipments")
    suspend fun searchShipments(@QueryMap options: Map<String, String>): Response<List<ShipmentResponse>>

    @GET("api/shipments/{id}")
    suspend fun getShipmentById(
        @Path("id") shipmentId: Long
    ): Response<ShipmentResponse> // Devuelve un solo objeto

    @GET("api/shipments/for-packing")
    suspend fun getShipmentsForPacking(): Response<List<ShipmentResponse>>

    @PUT("api/shipments/{id}")
    suspend fun updateShipment(
        @Path("id") shipmentId: Long,
        @Body shipment: ShipmentResponse
    ): Response<ShipmentResponse>
}
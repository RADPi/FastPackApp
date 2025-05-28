package com.fastpack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement // Para campos 'Object' o 'Array' con contenido heterogéneo

// Nota general: Las fechas están como String. Considera kotlinx-datetime para un manejo robusto.

@Serializable
data class ShipmentResponse(
    @SerialName("_id") val id: Long, // De Mongoose: Number, required: true
    val mode: String? = null,
    @SerialName("order_id") val orderId: String? = null,
    @SerialName("buyer_nickname") val buyerNickname: String? = null, // Nuevo de Mongoose
    @SerialName("order_cost") val orderCost: Double? = null, // Number en Mongoose
    @SerialName("base_cost") val baseCost: Double? = null,   // Number en Mongoose
    @SerialName("ml_bonus") val mlBonus: Double? = null,     // Nuevo de Mongoose (asumo Number/Double)
    @SerialName("seller_cost") val rootSellerCost: Double? = null,
    @SerialName("pack_id") val packId: Long? = null,         // Nuevo de Mongoose (asumo Number/Long o Int)
    val status: String? = null,
    val substatus: String? = null,
    @SerialName("status_history") val statusHistory: StatusHistory? = null,
    @SerialName("substatus_history") val substatusHistory: List<SubstatusHistoryEntry>? = null, // Mongoose: Array
    @SerialName("date_created") val dateCreated: String? = null, // Date en Mongoose
    @SerialName("last_updated") val lastUpdated: String? = null, // Date en Mongoose
    @SerialName("tracking_number") val trackingNumber: String? = null,
    @SerialName("tracking_method") val trackingMethod: String? = null,
    val qr: String? = null, // Nuevo de Mongoose
    @SerialName("sender_id") val senderId: Long? = null, // Number en Mongoose
    @SerialName("g_address") val gAddress: GAddress? = null, // Nuevo de Mongoose
    @SerialName("comment_edited") val commentEdited: String? = null, // Nuevo de Mongoose
    @SerialName("receiver_address") val receiverAddress: ReceiverAddress? = null,
    @SerialName("new_address") val newAddress: NewAddress? = null, // Nuevo de Mongoose
    @SerialName("fast_pack") val fastPack: FastPackInfo? = null,
    @SerialName("shipping_items") val shippingItems: List<ShippingItem>? = null, // Mongoose: Array
    @SerialName("shipped_items_photo") val shippedItemsPhoto: ShippedItemsPhoto? = null,
    @SerialName("shipping_option") val shippingOption: ShippingOption? = null,
    @SerialName("delivery_time") val deliveryTime: DeliveryTimeInfo? = null,
    val task: String? = null, // Nuevo de Mongoose
    @SerialName("amountToCollect") val amountToCollect: Double? = null, // Nuevo de Mongoose (asumo Number/Double)
    val comments: String? = null,
    @SerialName("packing_comment") val packingComment: String? = null, // Nuevo de Mongoose
    @SerialName("date_first_printed") val dateFirstPrinted: String? = null, // Date en Mongoose
    @SerialName("logistic_type") val logisticType: String? = null,
    @SerialName("change_history") val changeHistory: List<JsonElement>? = null, // Mongoose: Array. Si la estructura es conocida, reemplazar JsonElement. Ejemplo: List<String>?
    val costs: CostsInfo? = null, // Mongoose: Object. Asumiendo la estructura del JSON de ejemplo.
    // Si puede ser cualquier objeto JSON: kotlinx.serialization.json.JsonObject?

    // Campos de timestamps: true (Mongoose) - suelen ser 'createdAt' y 'updatedAt'
    // El JSON de ejemplo ya tiene 'updatedAt' y 'date_created' (que podría ser el 'createdAt')
    // Si Mongoose añade un 'createdAt' diferente a 'date_created', añádelo:
    // @SerialName("createdAt") val createdAtMongo: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null, // Date (de timestamps)

    @SerialName("__v") val version: Int? = null // versionKey: false podría omitirlo, pero el JSON lo tenía
)

@Serializable
data class StatusHistory(
    @SerialName("date_cancelled") val dateCancelled: String? = null, // Date
    @SerialName("date_delivered") val dateDelivered: String? = null, // Date, default: null
    @SerialName("date_first_visit") val dateFirstVisit: String? = null, // Date, default: null
    @SerialName("date_handling") val dateHandling: String? = null, // Date
    @SerialName("date_not_delivered") val dateNotDelivered: String? = null, // Date, default: null
    @SerialName("date_ready_to_ship") val dateReadyToShip: String? = null, // Date
    @SerialName("date_shipped") val dateShipped: String? = null, // Date
    @SerialName("date_returned") val dateReturned: String? = null // Date
)

@Serializable
data class SubstatusHistoryEntry( // Asumiendo que es una lista de objetos con esta estructura
    val date: String? = null,
    val substatus: String? = null,
    val status: String? = null
    // Si `substatus_history` puede contener otros tipos, List<JsonElement> sería más seguro.
)

@Serializable
data class GAddress( // Nuevo de Mongoose
    @SerialName("place_id") val placeId: String? = null,
    @SerialName("street_number") val streetNumber: String? = null,
    @SerialName("street_name") val streetName: String? = null,
    @SerialName("zip_code") val zipCode: String? = null,
    val locality: String? = null,
    @SerialName("administrative_area_level_1") val administrativeAreaLevel1: String? = null,
    @SerialName("administrative_area_level_2") val administrativeAreaLevel2: String? = null,
    @SerialName("formatted_address") val formattedAddress: String? = null,
    val lat: Double? = null, // Number
    val lng: Double? = null  // Number
)

@Serializable
data class ReceiverAddress(
    @SerialName("address_line") val addressLine: String? = null,
    @SerialName("street_name") val streetName: String? = null,
    @SerialName("street_number") val streetNumber: String? = null,
    val comment: String? = null,
    @SerialName("zip_code") val zipCode: String? = null,
    val city: String? = null,
    val state: String? = null,
    val latitude: Double? = null,  // Number
    val longitude: Double? = null, // Number
    @SerialName("geolocation_source") val geolocationSource: String? = null,
    @SerialName("delivery_preference") val deliveryPreference: String? = null,
    @SerialName("receiver_name") val receiverName: String? = null,
    @SerialName("receiver_phone") val receiverPhone: String? = null,
    @SerialName("buyer_phone") val buyerPhone: String? = null, // Nuevo de Mongoose
    @SerialName("buyer_name") val buyerName: String? = null,   // Nuevo de Mongoose
    @SerialName("DNI") val dni: String? = null               // Nuevo de Mongoose
)

@Serializable
data class NewAddress( // Nuevo de Mongoose
    @SerialName("street_number") val streetNumber: String? = null,
    @SerialName("street_name") val streetName: String? = null,
    @SerialName("zip_code") val zipCode: String? = null,
    val locality: String? = null,
    @SerialName("administrative_area_level_1") val administrativeAreaLevel1: String? = null,
    @SerialName("administrative_area_level_2") val administrativeAreaLevel2: String? = null,
    val lat: Double? = null, // Number
    val lng: Double? = null,  // Number
    @SerialName("place_id") val placeId: String? = null,
    @SerialName("receiver_name") val receiverName: String? = null,
    @SerialName("receiver_phone") val receiverPhone: String? = null,
    val comment: String? = null,
    @SerialName("formatted_address") val formattedAddress: String? = null
)

@Serializable
data class FastPackInfo(
    val shift: String? = null,
    val driver: String? = null, // Nuevo de Mongoose
    @SerialName("price_group") val priceGroup: String? = null, // Nuevo de Mongoose
    val price: Double? = null,    // Number
    @SerialName("ml_bonus") val mlBonusFastPack: Double? = null, // Nuevo de Mongoose (asumo Number/Double)
    @SerialName("seller_cost") val sellerCostInFastPack: Double? = null, // Number
    val handling: Boolean? = null, // Nuevo de Mongoose
    @SerialName("billed") val billed: String? = null, // Schema.Types.ObjectId se vuelve String. Ref: 'Bills'
    val photo: String? = null, // Nuevo de Mongoose
    val routed: Boolean? = null, // Nuevo de Mongoose
    val controlled: Boolean? = null // Nuevo de Mongoose
)

@Serializable
data class ShippingItem(
    @SerialName("id") val id: String? = null,
    val description: String? = null,
    val quantity: Int? = null,
    @SerialName("variation_id") val variationId: Long? = null, // Asumiendo Long por el ejemplo numérico
    @SerialName("variation_name") val variationName: String? = null,
    val picture: String? = null, // Ya se llama 'picture' en el JSON, no necesita @SerialName si el nombre de la propiedad es igual
    @SerialName("seller_custom_field") val sellerCustomField: String? = null,
    @SerialName("user_product_id") val userProductId: String? = null
)

@Serializable
data class DimensionsSource(
    val id: String? = null,
    val origin: String? = null
)

@Serializable
data class ShippedItemsPhoto( // Nuevo de Mongoose
    val url: String? = null,
    @SerialName("public_id") val publicId: String? = null
)

@Serializable
data class ShippingOption(
    val id: String? = null, // String en Mongoose
    val name: String? = null,
    @SerialName("list_cost") val listCost: Double? = null, // Number
    val cost: Double? = null, // Number
    @SerialName("loyal_discount") val loyalDiscount: Double? = null, // Number
    @SerialName("delivery_type") val deliveryType: String? = null,
    @SerialName("estimated_delivery_time") val estimatedDeliveryTime: EstimatedDeliveryTime? = null
)

@Serializable
data class EstimatedDeliveryTime(
    @SerialName("type") val type: String? = null, // Mongoose: { type: String }
    val date: String? = null, // Date
    @SerialName("pay_before") val payBefore: String? = null, // Date
    val shipping: Int? = null, // Number (asumo Int)
    val handling: Int? = null  // Number (asumo Int)
)

@Serializable
data class DeliveryTimeInfo(
    val from: String? = null, // trim: true, default: null
    val to: String? = null,   // trim: true, default: null
    val estimated: String? = null // trim: true, default: null
)

// CostsInfo y sus sub-clases (CostDetail, Discount, SenderCostDetail)
// se mantienen como en la versión anterior si la estructura del JSON de ejemplo para `costs` es consistente.
// El schema de Mongoose dice `costs: Object`, lo que es genérico.
// Si `costs` puede tener una estructura variable, necesitarías usar `kotlinx.serialization.json.JsonObject?`
// para `costs` en `ShipmentResponse` y manejarlo manualmente.
// Por ahora, mantendré las clases que teníamos basadas en tu JSON de ejemplo.

@Serializable
data class CostsInfo(
    val receiver: CostDetail? = null,
    @SerialName("gross_amount") val grossAmount: Double? = null,
    val senders: List<CostDetail>? = null
)

@Serializable
data class CostDetail(
    val cost: Double? = null,
    val discounts: List<Discount>? = null,
    @SerialName("user_id") val userId: Long? = null,
    @SerialName("cost_details") val costDetails: List<SenderCostDetail>? = null,
    val save: Double? = null,
    val compensation: Double? = null
)

@Serializable
data class Discount(
    val rate: Double? = null,
    val type: String? = null,
    @SerialName("promoted_amount") val promotedAmount: Double? = null
)

@Serializable
data class SenderCostDetail(
    @SerialName("sender_id") val senderId: Long? = null,
    val amount: Double? = null
)
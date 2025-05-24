package com.fastpack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val name: String? = null,
    val email: String,
    val password: String
)

// AuthResponse.kt
@Serializable
data class AuthResponse(
    @SerialName("user") val user: User?, // Hazlo nulable si puede no venir o fallar la deserialización parcial
    @SerialName("token") val token: String?
)

@Serializable
data class User(
    @SerialName("_id") val id: String?,
    @SerialName("name") val name: String?, // Nombre del usuario
    @SerialName("email") val email: String?,
    @SerialName("permissions") val permissions: List<String>?, // Asumiendo que es una lista de Strings
    @SerialName("roles") val roles: List<String>?, // Asumiendo que es una lista de Strings
    @SerialName("date") val date: String?, // Podrías usar un deserializador de fechas si lo necesitas como Date
    @SerialName("seller") val seller: Int?,
    @SerialName("CABA_ship") val cabaShip: String?,
    @SerialName("admin") val admin: Boolean?,
    @SerialName("adminFP") val adminFP: Boolean?,
    @SerialName("packer") val packer: Boolean?,
    @SerialName("nickname") val nickname: String? = null, // Si no viene en el
    @SerialName("fp_user") val fpUser: Int? = null,
    @SerialName("aka") val aka: String? = null,
    @SerialName("access_token") val accessToken: String? = null,
    @SerialName("expires_on") val expiresOn: String? = null,
)


// ApiError.kt (para errores genéricos de Retrofit)
@Serializable
data class ApiError(
    val message: String // Estructura del error JSON que devuelve tu API
)
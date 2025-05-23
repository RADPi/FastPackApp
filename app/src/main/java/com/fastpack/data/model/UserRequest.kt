package com.fastpack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserRequest(
    val email: String, // o username
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
    @SerialName("fp_user") val fpUser: Int?,
    @SerialName("date") val date: String?, // Podrías usar un deserializador de fechas si lo necesitas como Date
    @SerialName("seller") val seller: Int?,
    @SerialName("nickname") val nickname: String?,
    @SerialName("aka") val aka: String?,
    @SerialName("access_token") val accessToken: String?,
    @SerialName("expires_on") val expiresOn: String?,
    @SerialName("CABA_ship") val cabaShip: String?,
    @SerialName("admin") val admin: Boolean?,
    @SerialName("adminFP") val adminFP: Boolean?,
    @SerialName("packer") val packer: Boolean?,
)


// ApiError.kt (para errores genéricos de Retrofit)
@Serializable
data class ApiError(
    val message: String // Estructura del error JSON que devuelve tu API
)
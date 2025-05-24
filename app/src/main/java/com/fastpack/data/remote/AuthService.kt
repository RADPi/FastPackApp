package com.fastpack.data.remote

import com.fastpack.data.model.AuthResponse
import com.fastpack.data.model.UserRequest
import okhttp3.ResponseBody
import retrofit2.Response // Importante usar Response de Retrofit para manejar errores
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {
    @POST("api/auth/register")
    suspend fun registerUser(@Body userRequest: UserRequest): Response<AuthResponse> // La que tienes

    @POST("api/auth/login") // Cambia "login" por tu endpoint real
    suspend fun loginUser(@Body userRequest: UserRequest): Response<AuthResponse>

    // Podrías añadir un endpoint para logout si tu API lo requiere
    // @POST("api/auth/logout")
    // suspend fun logoutUser(@Header("Authorization") token: String): Response<Unit>
}
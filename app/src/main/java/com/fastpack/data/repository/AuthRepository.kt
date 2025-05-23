package com.fastpack.data.repository

import com.fastpack.data.model.AuthResponse
import com.fastpack.data.model.UserRequest
import com.fastpack.data.preferences.UserPreferencesRepository
import com.fastpack.data.remote.AuthService
import com.fastpack.util.Resource // Tu clase Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val userPreferencesRepository: UserPreferencesRepository
) {

    val authTokenFlow: Flow<String?> = userPreferencesRepository.authToken

    suspend fun loginUser(userRequest: UserRequest): Resource<AuthResponse> {
        return try {
            val response: Response<AuthResponse> = authService.loginUser(userRequest) // AuthService devuelve Response<AuthResponse>

            if (response.isSuccessful) {
                val authData = response.body()
                if (authData != null) {
                    if (authData.token != null) {
                        userPreferencesRepository.saveAuthToken(authData.token)
                        Resource.Success(authData)
                    } else {
                        // Respuesta exitosa pero sin token en el cuerpo
                        Resource.Error("Token no recibido del servidor (cuerpo de respuesta exitosa).", null)
                    }
                } else {
                    // Respuesta exitosa pero cuerpo vacío
                    Resource.Error("Respuesta exitosa pero cuerpo vacío.", null)
                }
            } else {
                // Error HTTP (4xx, 5xx)
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody.isNullOrBlank()) {
                    "Error de inicio de sesión: ${response.code()} ${response.message()}"
                } else {
                    // Aquí podrías intentar parsear el errorBody si es un JSON con un mensaje específico
                    // Por ahora, simplemente devolvemos el contenido del errorBody.
                    errorBody
                }
                Resource.Error(errorMessage, null)
            }
        } catch (e: Exception) {
            // Excepciones de red (no se pudo conectar), timeouts, o errores de deserialización
            // si el cuerpo del error no es el esperado por Retrofit/Gson/Moshi.
            Resource.Error(e.message ?: "Error de red o desconocido.", null)
        }
    }

    suspend fun registerUser(userRequest: UserRequest /* o RegisterRequest */): Resource<AuthResponse> {
        return try {
            val response: Response<AuthResponse> = authService.registerUser(userRequest)

            if (response.isSuccessful) {
                val authData = response.body()
                if (authData != null) {
                    // Decidir si guardar el token al registrar o no.
                    // Ejemplo:
                    // if (authData.token != null) {
                    //     userPreferencesRepository.saveAuthToken(authData.token)
                    // }
                    Resource.Success(authData)
                } else {
                    Resource.Error("Respuesta de registro exitosa pero cuerpo vacío.", null)
                }
            } else { // <--- ELSE CORRECTO CON BLOQUE DE LLAVES
                val errorBody = response.errorBody()?.string()
                val errorMessage = if (errorBody.isNullOrBlank()) {
                    "Error de registro: ${response.code()} ${response.message()}"
                } else {
                    errorBody
                }
                Resource.Error(errorMessage, null)
            } // <--- CIERRE DEL BLOQUE ELSE
        } catch (e: Exception) { // <--- CIERRE DEL BLOQUE TRY Y COMIENZO DEL CATCH
            Resource.Error(e.message ?: "Error de red o desconocido en registro.", null)
        }
    }
    suspend fun getAuthToken(): String? {
        return userPreferencesRepository.authToken.firstOrNull()
    }

    suspend fun clearAuthToken() {
        userPreferencesRepository.clearAuthToken()
    }

    suspend fun isLoggedIn(): Boolean {
        return getAuthToken() != null
    }
}
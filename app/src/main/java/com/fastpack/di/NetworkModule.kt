package com.fastpack.di

import android.content.Context
import com.fastpack.BuildConfig // Para diferentes Base URLs
import com.fastpack.data.preferences.UserPreferencesRepository
import com.fastpack.data.remote.AuthService
import com.fastpack.data.remote.ShipmentService
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideBaseUrl(): String = if (BuildConfig.DEBUG) {
//        "http://10.0.2.2:3000/" // URL para emulador Android (localhost de tu máquina)
        "http://192.168.86.39:3000/" // Si pruebas en dispositivo físico en la misma red
    } else {
        "http://127.0.0.1:3000/" // URL de producción
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true // Ignora campos en el JSON que no están en tu data class
        isLenient = true         // Permite un formato JSON más permisivo (ej. strings sin comillas para enums)
        prettyPrint = false      // Para producción, deshabilita el pretty print
        encodeDefaults = false   // No incluye valores predeterminados durante la serialización (envío de datos)
        coerceInputValues = true // Intenta coaccionar valores (ej. null a un valor por defecto si está definido)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        userPreferencesRepository: UserPreferencesRepository // Inyecta el repositorio de preferencias
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                // Obtener el token. runBlocking se usa aquí porque los interceptores de OkHttp
                // son síncronos por defecto. Considera alternativas si esto causa problemas de rendimiento.
                // Para aplicaciones más complejas, se podría usar un Authenticator de OkHttp para
                // refrescar el token de forma más elegante.
                val token = runBlocking {
                    userPreferencesRepository.authToken.firstOrNull()
                }
                val requestBuilder = chain.request().newBuilder()
                if (token != null) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(baseUrl: String, okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthService(retrofit: Retrofit): AuthService {
        return retrofit.create(AuthService::class.java)
    }

    @Provides
    @Singleton
    fun provideShipmentService(retrofit: Retrofit): ShipmentService {
        return retrofit.create(ShipmentService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesRepository(@ApplicationContext context: Context): UserPreferencesRepository {
        return UserPreferencesRepository(context)
    }
}
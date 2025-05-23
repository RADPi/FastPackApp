package com.fastpack.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// Declara la extensión de DataStore a nivel superior o en un objeto companion.
// El nombre "user_preferences" será el nombre del archivo donde se guardarán.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class UserPreferencesRepository @Inject constructor(private val context: Context) {

    // Define una clave para el token de autenticación
    private object PreferencesKeys {
        val AUTH_TOKEN = stringPreferencesKey("auth_token")
    }

    // Flow para observar el token de autenticación
    val authToken: Flow<String?> = context.dataStore.data
        .catch { exception ->
            // IOException se lanza si hay un error al leer los datos.
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN]
        }

    // Función suspendida para guardar el token de autenticación
    suspend fun saveAuthToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTH_TOKEN] = token
        }
    }

    // Función suspendida para borrar el token de autenticación (ej. al cerrar sesión)
    suspend fun clearAuthToken() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferencesKeys.AUTH_TOKEN)
            // o preferences.clear() para borrar todas las preferencias
        }
    }
}
package com.fastpack.di

import com.fastpack.data.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
// Si AuthRepository es una clase con @Inject constructor,
// no necesitas un @Binds o @Provides explícito para ÉL.
// Hilt lo manejará automáticamente.
// Si tenías un "abstract class RepositoryModule" con un "@Binds abstract fun bindAuthRepository...",
// puedes borrar ese método. Si el módulo queda vacío, incluso podrías borrar el archivo
// si no provee nada más.
object RepositoryModule { // O "class RepositoryModule" si en el futuro necesitas @Binds

    // Si tienes OTROS bindings aquí, déjalos.
    // Por ejemplo, si ApiService fuera una interfaz:
    // @Binds
    // abstract fun bindApiService(impl: ApiServiceImpl): ApiService

    // O si UserPreferencesRepository necesitara un @Provides:
    // @Provides
    // @Singleton
    // fun provideUserPreferencesRepository(application: Application): UserPreferencesRepository {
    //     return UserPreferencesRepository(application.dataStore)
    // }
}
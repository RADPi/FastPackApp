package com.fastpack

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializaciones globales si las necesitas

        // Configura Cloudinary (reemplaza con tus credenciales y cloud_name)
        val config = mapOf(
            "cloud_name" to "fastpack",
            "api_key"    to "964839831495833",
            "api_secret" to "qIGV6ScU5KDwGADdDTwGhn3C7JI"
            // "secure" to true // si usas HTTPS por defecto
        )
        MediaManager.init(this, config)
    }
}
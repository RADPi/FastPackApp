package com.fastpack

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class YourApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicializaciones globales si las necesitas
    }
}
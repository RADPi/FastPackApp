/*
 * Aplicación de plugins específicos para este módulo.
 * Estos plugins habilitan funcionalidades como la compilación de Android,
 * el soporte de Kotlin, Compose, Hilt, etc.
 */
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Habilita el compilador de Compose
    alias(libs.plugins.hilt.android)   // Habilita Hilt para este módulo
    alias(libs.plugins.kotlinx.serialization) // Habilita la serialización de Kotlin
    alias(libs.plugins.ksp) // Habilita KSP para procesadores de anotaciones
}

/*
 * Configuración específica de Android para este módulo de aplicación.
 */
android {
    namespace = "com.fastpack" // Reemplaza con tu namespace real
    compileSdk = 36 // O la versión que estés usando

    defaultConfig {
        applicationId = "com.fastpack"
        minSdk = 24 // O tu minSdkVersion
        targetSdk = 34 // O tu targetSdkVersion
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    /*
     * Tipos de compilación (Build Types), como debug y release.
     * Aquí se configuran opciones como la minificación para builds de release.
     */
    buildTypes {
        release {
            isMinifyEnabled = false // Cambia a true para producción y para probar reglas de ProGuard
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Configuraciones específicas para debug (ej. applicationIdSuffix)
            // isMinifyEnabled = false // Por defecto es false para debug
        }
    }

    /*
     * Opciones de compilación para Java y Kotlin.
     */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11 // O la que uses, ej. VERSION_17
        targetCompatibility = JavaVersion.VERSION_11 // O la que uses, ej. VERSION_17
    }
    kotlinOptions {
        jvmTarget = "11" // O la que uses, ej. "17"
    }

    /*
     * Opciones específicas para la compilación de Jetpack Compose.
     */
    buildFeatures {
        buildConfig = true
        compose = true // Habilita Compose para este módulo
        // Otras buildFeatures como viewBinding, dataBinding, etc., si las usas.
        // viewBinding = true
    }
//    composeOptions {
//         Aquí se configura la versión del compilador de Compose.
//         Usualmente se alinea con la versión de Kotlin.
//         Si usas el plugin `kotlin-compose` desde el catálogo de versiones,
//         la versión del compilador ya está ligada a tu versión de Kotlin.
//         kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get() // Ejemplo si tuvieras una versión específica para el compilador en el TOML
//    }

    /*
     * Configuración para empaquetar la app.
     */
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/*
 * Bloque de dependencias para este módulo.
 * Aquí se declaran todas las librerías que necesita el módulo :app.
 */
dependencies {

    // --- Kotlin ---
    implementation(libs.kotlin.stdlib) // ¡Asegúrate de que esta línea esté presente!

    // --- Jetpack Core & Lifecycle ---
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // Para ViewModel

    // --- Jetpack Compose ---
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom)) // BOM para gestionar versiones de Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview) // Para @Preview
    implementation(libs.androidx.material3) // Material Design 3 para Compose

    // --- Jetpack Navigation ---
    implementation(libs.androidx.navigation.compose)

    // --- Coroutines ---
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.guava)

    // --- Networking (OkHttp & Retrofit) ---
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor) // Solo para debug builds idealmente
    implementation(libs.retrofit)
    implementation(libs.retrofit2.kotlinx.serialization.converter)

    // --- Serialización (Kotlinx) ---
    implementation(libs.kotlinx.serialization.json)

    // --- Inyección de Dependencias (Hilt) ---
    implementation(libs.hilt.android)
    // Para el procesador de anotaciones de Hilt
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose) // Integración de Hilt con Navigation Compose

    // --- DatastorePreferences ---
    implementation(libs.androidx.datastore.preferences)

    // --- Zxing QR Reader ---
//    implementation(libs.journeyapps.zxing.android.embedded)

    // --- CameraX core y camera2 ---
    implementation(libs.guava)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    // --- Para análisis de códigos QR ---
    implementation(libs.barcode.scanning)

    // --- Coil para carga de imágenes ---
    implementation(libs.coil.compose)

    // --- Permissions with Accompanist  ---
    implementation(libs.accompanist.permissions) // Verifica la última versión

    // --- Cloudinary ---
    implementation(libs.cloudinary.android) // Revisa la última versión
    implementation(libs.cloudinary.core)

    // --- Pruebas Unitarias ---
    testImplementation(libs.junit)
    // testImplementation(libs.kotlinx.coroutines.test) // Para probar coroutines

    // --- Pruebas de Instrumentación (Android UI Tests) ---
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM también para tests de UI de Compose
    androidTestImplementation(libs.androidx.ui.test.junit4) // Para pruebas de UI con Compose

    // --- Debugging (Compose UI Tooling) ---
    debugImplementation(libs.androidx.ui.tooling) // Para herramientas como el Layout Inspector con Compose
    debugImplementation(libs.androidx.ui.test.manifest)
}
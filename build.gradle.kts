/*
 * Plugins de alto nivel aplicados al proyecto.
 * Estos plugins suelen configurar repositorios y cómo se aplican otros plugins a los submódulos.
 */
plugins {
    // Plugin para aplicaciones Android, aplicado aquí como 'false'
    // porque se aplicará específicamente en el módulo :app
    alias(libs.plugins.android.application) apply false

    // Plugin de Kotlin para Android, aplicado aquí como 'false'
    // porque se aplicará específicamente en el módulo :app
    alias(libs.plugins.kotlin.android) apply false

    // Plugin de Kotlin Compose Compiler, aplicado aquí como 'false'
    // se aplicará en el módulo :app
    alias(libs.plugins.kotlin.compose) apply false

    // Plugin de Hilt para inyección de dependencias, aplicado aquí como 'false'
    // se aplicará en el módulo :app y potencialmente otros módulos de librería.
    alias(libs.plugins.hilt.android) apply false

    // Plugin de Kotlinx Serialization, aplicado aquí como 'false'
    // se aplicará en módulos que necesiten serialización.
    alias(libs.plugins.kotlinx.serialization) apply false

    // KSP (Kotlin Symbol Processing), aplicado aquí como 'false'
    // se aplicará en módulos que usen procesadores de anotaciones compatibles con KSP.
    alias(libs.plugins.ksp) apply false
}

/*
 * Bloque 'allprojects' se aplica a este proyecto y a todos sus submódulos.
 * Comúnmente usado para definir repositorios.
 */
// allprojects {
//     repositories {
//         google()
//         mavenCentral()
//         // otros repositorios si son necesarios
//     }
// }

/*
 * Bloque 'subprojects' se aplica solo a los submódulos.
 */
// subprojects {
//     // configuraciones comunes para todos los submódulos
// }

/*
 * Tareas personalizadas o configuraciones a nivel de raíz del proyecto.
 * Por ejemplo, una tarea para limpiar directorios específicos.
 */
// tasks.register("clean", Delete::class) {
//     delete(rootProject.buildDir)
// }
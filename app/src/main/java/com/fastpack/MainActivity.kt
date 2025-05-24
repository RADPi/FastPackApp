package com.fastpack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.fastpack.navigation.AppScreen
import com.fastpack.ui.home.HomeScreen
import com.fastpack.ui.login.LoginScreen
import com.fastpack.ui.prepare.PrepareScreen
import com.fastpack.ui.register.RegisterScreen
import com.fastpack.ui.settings.SettingsScreen
import com.fastpack.ui.theme.FastPackTheme
import dagger.hilt.android.AndroidEntryPoint

// Define tus destinos para la Bottom Navigation Bar (si la usas)
sealed class MainScreen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : MainScreen(AppScreen.Home.route, "Inicio", Icons.Filled.Home)
    object Prepare : MainScreen(AppScreen.Prepare.route, "Preparar", Icons.Filled.ShoppingCart)
    object Settings : MainScreen(AppScreen.Settings.route, "Configuración", Icons.Filled.Settings)
    // Agrega más destinos principales aquí
}

val items = listOf(
    MainScreen.Home,
    MainScreen.Prepare,
    MainScreen.Settings,
)

@OptIn(ExperimentalMaterial3Api::class)
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FastPackTheme {
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                // Determinar si se debe mostrar el Scaffold principal con Top/Bottom bar
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                // Si currentDestination es null, asumimos que estamos yendo a una pantalla
                // que NO debería tener las barras principales (como Login, tu startDestination).
                // O puedes ser más explícito con la startDestination.
                val shouldShowMainScaffoldBars = currentDestination?.route?.let { route ->
                    route !in listOf(
                        AppScreen.Login.route,
                        AppScreen.Register.route
                    )
                } == true // Si currentDestination?.route es null, consideramos false (no mostrar barras principales)


                if (shouldShowMainScaffoldBars) {
                    // Scaffold para las pantallas principales (Home, Settings, etc.)
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            AppTopBar(navController = navController, currentDestination = currentDestination)
                        },
                        bottomBar = {
                            AppBottomBar(navController = navController, currentDestination = currentDestination)
                        }
                    ) { innerPadding ->
                        AppNavigation(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                } else {
                    // Scaffold simple para Login/Register o pantallas sin barras comunes
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                        topBar = {
                            LoginRegisterTopBar() // Usamos la TopBar dedicada
                        }
                    ) { innerPadding ->
                        AppNavigation(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginRegisterTopBar() {
    TopAppBar(
        title = { Text("FastPack") },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF343A40), // Tu estilo
            titleContentColor = Color.White
        )
        // Sin navigationIcon si no lo necesitas aquí
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavController, currentDestination: androidx.navigation.NavDestination?) {
    // Puedes personalizar el título y las acciones basado en el destino actual
    val title = when (currentDestination?.route) {
        MainScreen.Home.route -> "Resumen de Envíos Pendientes"
        MainScreen.Prepare.route -> "Preparar envío"
        MainScreen.Settings.route -> "Configuración"
        // ... otros títulos para pantallas principales
        else -> "FastPack - Defecto" // Título por defecto o para sub-pantallas
    }

    // Mostrar botón de retroceso si no estamos en un destino principal de la bottom bar
    val canNavigateBack = navController.previousBackStackEntry != null &&
            items.none { it.route == currentDestination?.route }


    TopAppBar(
        title = { Text(title) },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF343A40),
            titleContentColor = Color.White,
            navigationIconContentColor = Color.White
        ),
        navigationIcon = {
            if (canNavigateBack) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás")
                }
            }
        }
        // Puedes agregar actions aquí si son comunes
    )
}

@Composable
fun AppBottomBar(navController: NavController, currentDestination: androidx.navigation.NavDestination?) {
    NavigationBar(
        containerColor = Color(0xFF343A40) // Ejemplo de color
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label, tint = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) Color.White else Color.LightGray) },
                label = { Text(screen.label, color = if (currentDestination?.hierarchy?.any { it.route == screen.route } == true) Color.White else Color.LightGray) },
                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) // Color del indicador
                )
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(navController: androidx.navigation.NavHostController, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = AppScreen.Login.route, // O tu lógica para determinar la pantalla inicial
        modifier = modifier
    ) {
        composable(AppScreen.Login.route) {
            LoginScreen(
                // viewModel se inyecta automáticamente por Hilt
                onNavigateToHome = {
                    navController.navigate(AppScreen.Home.route) {
                        popUpTo(AppScreen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(AppScreen.Register.route)
                }
            )
        }
        composable(AppScreen.Register.route) {
            RegisterScreen(
                // viewModel se inyecta automáticamente por Hilt
                onNavigateToSettings = {
                    navController.navigate(AppScreen.Settings.route) {
                        // Limpiar backstack de login y register
                        popUpTo(AppScreen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack() // Simplemente regresa a la pantalla anterior (Login)
                }
            )
        }

        composable(AppScreen.Home.route) { // Usar AppScreen.Home.route
            HomeScreen(/*...*/)
        }

        composable(AppScreen.Prepare.route) { // Usar AppScreen.Prepare.route
            PrepareScreen(/*...*/)
        }

        composable(AppScreen.Settings.route) { // Usar AppScreen.Settings.route
            SettingsScreen(/*...*/)
        }
//         ... otras rutas
    }
}
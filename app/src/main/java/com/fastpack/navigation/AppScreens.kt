package com.fastpack.navigation

sealed class AppScreen(val route: String) {
    object Login : AppScreen("login_screen")
    object Register : AppScreen("register_screen")
    object Home : AppScreen("home_screen")
    object Prepare : AppScreen("prepare_screen")
    object Settings : AppScreen("settings_screen")
}
package com.ohstem.robot_controller.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Gamepad : Screen("gamepad")
    object Voice : Screen("voice")
    object Gesture : Screen("gesture")
    object Settings : Screen("settings")
}

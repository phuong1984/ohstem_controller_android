package com.ohstem.robot_controller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.ohstem.robot_controller.ui.navigation.Screen
import com.ohstem.robot_controller.ui.screens.gamepad.GamepadScreen
import com.ohstem.robot_controller.ui.screens.gesture.GestureScreen
import com.ohstem.robot_controller.ui.screens.home.HomeScreen
import com.ohstem.robot_controller.ui.screens.settings.SettingsScreen
import com.ohstem.robot_controller.ui.screens.voice.VoiceScreen
import com.ohstem.robot_controller.ui.theme.DarkBackground
import com.ohstem.robot_controller.ui.theme.OhStemControllerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OhStemControllerTheme {
                RobotControllerApp()
            }
        }
    }
}

@Composable
fun RobotControllerApp() {
    val navController = rememberNavController()
    val items = listOf(
        NavigationItem("Home", Screen.Home.route, Icons.Default.Home),
        NavigationItem("Gamepad", Screen.Gamepad.route, Icons.Default.Gamepad),
        NavigationItem("Voice", Screen.Voice.route, Icons.Default.Mic),
        NavigationItem("Gesture", Screen.Gesture.route, Icons.Default.PanTool),
        NavigationItem("Settings", Screen.Settings.route, Icons.Default.Settings),
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val isGamepad = currentDestination?.hierarchy?.any { it.route == Screen.Gamepad.route } == true
            
            NavigationBar(
                modifier = if (isGamepad) Modifier.height(50.dp) else Modifier.height(72.dp),
                containerColor = if (isGamepad) DarkBackground.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface
            ) {
                items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                item.icon, 
                                contentDescription = item.label, 
                                modifier = if (isGamepad) Modifier.size(18.dp) else Modifier.size(22.dp),
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isGamepad) {
                                    Color.White.copy(alpha = 0.5f)
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            ) 
                        },
                        label = if (isGamepad) null else { { Text(item.label) } },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Gamepad.route) { GamepadScreen() }
            composable(Screen.Voice.route) { VoiceScreen() }
            composable(Screen.Gesture.route) { GestureScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

data class NavigationItem(val label: String, val route: String, val icon: ImageVector)

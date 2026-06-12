package com.ohstem.robot_controller.ui.screens.gesture

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.gesture.GestureState
import com.ohstem.robot_controller.viewmodel.GestureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureScreen(viewModel: GestureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gesture Control") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Camera Preview Placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera Preview Area")
                // TODO: Add AndroidView for CameraX PreviewView
            }

            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (state) {
                            is GestureState.Idle -> "Camera is OFF"
                            is GestureState.Detecting -> "Detecting gestures..."
                            is GestureState.Result -> "Gesture: ${(state as GestureState.Result).gestureName}"
                            is GestureState.Error -> "Error: ${(state as GestureState.Error).message}"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = {
                            if (state is GestureState.Detecting) viewModel.stopDetection()
                            else viewModel.startDetection()
                        }
                    ) {
                        Text(if (state is GestureState.Detecting) "Stop Camera" else "Start Camera")
                    }
                }
            }
        }
    }
}

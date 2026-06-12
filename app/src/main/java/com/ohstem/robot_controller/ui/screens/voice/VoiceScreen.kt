package com.ohstem.robot_controller.ui.screens.voice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.viewmodel.VoiceViewModel
import com.ohstem.robot_controller.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Voice Control") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when (state) {
                    is VoiceState.Idle -> "Press button to speak"
                    is VoiceState.Initializing -> "Initializing model..."
                    is VoiceState.Listening -> "Listening..."
                    is VoiceState.Result -> "Result: ${(state as VoiceState.Result).text}"
                    is VoiceState.Error -> "Error: ${(state as VoiceState.Error).message}"
                },
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (state is VoiceState.Listening) viewModel.stopListening()
                    else viewModel.startListening()
                },
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(if (state is VoiceState.Listening) "Stop" else "Speak")
            }
        }
    }
}

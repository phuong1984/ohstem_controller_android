package com.ohstem.robot_controller.ui.screens.voice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.viewmodel.VoiceViewModel
import com.ohstem.robot_controller.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var lastCommand by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    LaunchedEffect(state) {
        if (state is VoiceState.Result) {
            lastCommand = (state as VoiceState.Result).text
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Voice Control") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                is VoiceState.Idle -> {
                    Text(
                        "Ready",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                is VoiceState.Initializing -> {
                    Text(
                        "Initializing model...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                is VoiceState.Listening -> {
                    Text(
                        "Listening...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is VoiceState.Partial -> {
                    Text(
                        "Đang nghe: ${(state as VoiceState.Partial).text}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is VoiceState.Result -> {
                    Text(
                        "Recognized: ${(state as VoiceState.Result).text}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is VoiceState.Error -> {
                    Text(
                        "Error: ${(state as VoiceState.Error).message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (lastCommand.isNotEmpty()) {
                Text(
                    "Last command sent: $lastCommand",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (state is VoiceState.Listening) viewModel.stopListening()
                    else viewModel.startListening()
                },
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state is VoiceState.Listening)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (state is VoiceState.Listening) "Stop"
                    else if (state is VoiceState.Error) "Retry"
                    else "Start",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}

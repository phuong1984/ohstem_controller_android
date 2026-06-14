package com.ohstem.robot_controller.ui.screens.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.viewmodel.VoiceViewModel
import com.ohstem.robot_controller.voice.VoiceRecognitionMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val mode by viewModel.mode.collectAsState()
    var permissionRequested by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startListening()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopListening()
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
            val isActive = uiState is VoiceViewModel.UiState.Listening ||
                uiState is VoiceViewModel.UiState.Listened

            when (val s = uiState) {
                is VoiceViewModel.UiState.Initializing -> {
                    Text(
                        "Initializing...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                is VoiceViewModel.UiState.Ready -> {
                    Text(
                        "Ready",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                is VoiceViewModel.UiState.Listening -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Listening...",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (s.partialText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                s.partialText,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is VoiceViewModel.UiState.Listened -> {
                    Text(
                        if (s.isMatched) "Matched: ${s.text}" else "Listened: ${s.text}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = if (s.isMatched) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondary
                    )
                }
                is VoiceViewModel.UiState.Error -> {
                    Text(
                        "Error: ${s.message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            val buttonSize = 96.dp
            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
            val pulseProgress by infiniteTransition.animateFloat(
                initialValue = 0f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1200, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "pulseProgress"
            )
            val primaryColor = MaterialTheme.colorScheme.primary
            if (isActive) {
                val ringAlpha = (1f - pulseProgress) * 0.35f
                val ringScale = 1f + pulseProgress * 0.18f
                Canvas(modifier = Modifier.size(buttonSize * ringScale)) {
                    val strokePx = 3.dp.toPx()
                    drawCircle(
                        color = primaryColor.copy(alpha = ringAlpha),
                        radius = size.minDimension / 2 - strokePx / 2,
                        style = Stroke(width = strokePx)
                    )
                }
            }

            val isPermissionError = uiState is VoiceViewModel.UiState.Error &&
                (uiState as? VoiceViewModel.UiState.Error)?.message
                    ?.contains("permission", ignoreCase = true) == true

            Button(
                onClick = {
                    when {
                        isActive -> viewModel.stopListening()
                        isPermissionError -> permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        uiState is VoiceViewModel.UiState.Ready || uiState is VoiceViewModel.UiState.Error -> viewModel.startListening()
                    }
                },
                enabled = uiState !is VoiceViewModel.UiState.Initializing,
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isActive)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 10.dp
                )
            ) {
                Icon(
                    imageVector = if (isActive) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = if (isActive) "Stop listening" else "Start listening",
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Online Voice Recognition",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = mode == VoiceRecognitionMode.ONLINE,
                    onCheckedChange = { isOnline ->
                        viewModel.setMode(
                            if (isOnline) VoiceRecognitionMode.ONLINE
                            else VoiceRecognitionMode.OFFLINE
                        )
                    },
                    enabled = !isActive
                )
            }
        }
    }
}

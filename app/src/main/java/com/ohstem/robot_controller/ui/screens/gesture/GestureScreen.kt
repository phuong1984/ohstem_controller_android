package com.ohstem.robot_controller.ui.screens.gesture

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.gesture.GestureState
import com.ohstem.robot_controller.viewmodel.GestureViewModel
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureScreen(viewModel: GestureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val isDetecting by viewModel.isDetecting.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    var useFrontCamera by remember { mutableStateOf(true) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gesture Control") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
            ) {
                if (hasCameraPermission) {
                    key(useFrontCamera) {
                        AndroidView(
                            factory = { ctx ->
                                PreviewView(ctx).apply {
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()

                                        val preview = Preview.Builder().build().also {
                                            it.setSurfaceProvider(surfaceProvider)
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { analysis ->
                                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                                    viewModel.processFrame(imageProxy)
                                                }
                                            }

                                        val cameraSelector = if (useFrontCamera) {
                                            CameraSelector.DEFAULT_FRONT_CAMERA
                                        } else {
                                            CameraSelector.DEFAULT_BACK_CAMERA
                                        }

                                        try {
                                            cameraProvider.unbindAll()
                                            cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageAnalysis
                                            )
                                        } catch (_: Exception) {
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Camera permission required",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (state) {
                            is GestureState.Idle -> "Gesture Recognition is OFF"
                            is GestureState.Detecting -> "Detecting gestures..."
                            is GestureState.Result -> "Gesture: ${(state as GestureState.Result).gestureName}"
                            is GestureState.Error -> "Error: ${(state as GestureState.Error).message}"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (isDetecting) viewModel.stopDetection()
                                else viewModel.startDetection()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isDetecting) "Stop Recognition" else "Start Recognition")
                        }

                        OutlinedButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Cameraswitch, contentDescription = "Switch Camera")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (useFrontCamera) "Rear" else "Front")
                        }
                    }
                }
            }
        }
    }
}

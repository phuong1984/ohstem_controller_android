# Gesture Recognition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add MediaPipe-based gesture recognition to control robot movement with Open_Palm (forward) and Closed_Fist (stop).

**Architecture:** CameraX delivers live camera frames → GestureScreen feeds them to GestureViewModel → MediaPipeGestureManager converts to MPImage and runs GestureRecognizer → recognized gestures emit GestureState.Result → ViewModel routes to MappingEngine for BLE dispatch.

**Tech Stack:** Kotlin, Jetpack Compose, CameraX 1.3.1, MediaPipe tasks-vision 0.10.9, Hilt DI

---

### Task 1: Download Gesture Recognizer model

**Files:**
- Create: `app/src/main/assets/gesture_recognizer.task`

- [ ] **Download the model file**

```bash
mkdir -p app/src/main/assets
curl -L -o app/src/main/assets/gesture_recognizer.task \
  https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/latest/gesture_recognizer.task
```

Expected: file downloads (~5MB). Verify with `ls -lh app/src/main/assets/gesture_recognizer.task`.

- [ ] **Commit**

```bash
git add app/src/main/assets/gesture_recognizer.task
git commit -m "feat: add MediaPipe Gesture Recognizer model"
```

---

### Task 2: Update GestureManager interface

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/gesture/GestureManager.kt`

- [ ] **Add `processFrame` to interface**

Current file content:
```kotlin
package com.ohstem.robot_controller.gesture

import kotlinx.coroutines.flow.StateFlow

sealed class GestureState {
    object Idle : GestureState()
    object Detecting : GestureState()
    data class Result(val gestureName: String) : GestureState()
    data class Error(val message: String) : GestureState()
}

interface GestureManager {
    val state: StateFlow<GestureState>
    fun startDetection()
    fun stopDetection()
}
```

Replace with:
```kotlin
package com.ohstem.robot_controller.gesture

import androidx.camera.core.ImageProxy
import kotlinx.coroutines.flow.StateFlow

sealed class GestureState {
    object Idle : GestureState()
    object Detecting : GestureState()
    data class Result(val gestureName: String) : GestureState()
    data class Error(val message: String) : GestureState()
}

interface GestureManager {
    val state: StateFlow<GestureState>
    fun startDetection()
    fun stopDetection()
    fun processFrame(imageProxy: ImageProxy)
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/com/ohstem/robot_controller/gesture/GestureManager.kt
git commit -m "feat: add processFrame to GestureManager interface"
```

---

### Task 3: Implement MediaPipeGestureManager

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/gesture/MediaPipeGestureManager.kt`

- [ ] **Replace stub with full implementation**

Current content is a 25-line stub. Replace with:

```kotlin
package com.ohstem.robot_controller.gesture

import android.content.Context
import androidx.camera.core.ImageProxy
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.MPImage
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaPipeGestureManager @Inject constructor(
    private val context: Context
) : GestureManager {

    private val _state = MutableStateFlow<GestureState>(GestureState.Idle)
    override val state: StateFlow<GestureState> = _state

    private var recognizer: GestureRecognizer? = null
    private var lastGesture: String? = null
    private var gestureStreak = 0
    private val requiredStreak = 3

    override fun startDetection() {
        try {
            val modelPath = "gesture_recognizer.task"
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath(modelPath)
                .build()
            val options = GestureRecognizer.GestureRecognizerOptions.builder()
                .setBaseOptions(baseOptions)
                .build()
            recognizer = GestureRecognizer.createFromOptions(context, options)
            lastGesture = null
            gestureStreak = 0
            _state.value = GestureState.Detecting
        } catch (e: Exception) {
            _state.value = GestureState.Error("Failed to load model: ${e.message}")
        }
    }

    override fun stopDetection() {
        recognizer?.close()
        recognizer = null
        lastGesture = null
        gestureStreak = 0
        _state.value = GestureState.Idle
    }

    override fun processFrame(imageProxy: ImageProxy) {
        val rec = recognizer ?: return

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val rotation = when (rotationDegrees) {
            0 -> MPImage.ROTATION_0
            90 -> MPImage.ROTATION_90
            180 -> MPImage.ROTATION_180
            270 -> MPImage.ROTATION_270
            else -> MPImage.ROTATION_0
        }

        val mpImage = MPImage.fromMediaImage(mediaImage, rotation)
        val result = rec.recognize(mpImage)
        imageProxy.close()

        val topCategory = result?.gestureCategories()?.firstOrNull()?.firstOrNull()
        val gestureName = topCategory?.categoryName()

        if (gestureName != null && (gestureName == "Open_Palm" || gestureName == "Closed_Fist")) {
            if (gestureName == lastGesture) {
                gestureStreak++
            } else {
                lastGesture = gestureName
                gestureStreak = 1
            }

            if (gestureStreak >= requiredStreak) {
                _state.value = GestureState.Result(gestureName)
                gestureStreak = 0
            }
        } else {
            lastGesture = null
            gestureStreak = 0
        }
    }
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/com/ohstem/robot_controller/gesture/MediaPipeGestureManager.kt
git commit -m "feat: implement MediaPipeGestureManager with frame processing"
```

---

### Task 4: Update GestureViewModel

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/viewmodel/GestureViewModel.kt`

- [ ] **Add processFrame, seed default bindings, use collect instead of collectLatest**

Current file:
```kotlin
package com.ohstem.robot_controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.MappingEngine
import com.ohstem.robot_controller.gesture.GestureManager
import com.ohstem.robot_controller.gesture.GestureState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureViewModel @Inject constructor(
    private val gestureManager: GestureManager,
    private val mappingEngine: MappingEngine
) : ViewModel() {

    val state = gestureManager.state

    init {
        viewModelScope.launch {
            state.collectLatest { gestureState ->
                if (gestureState is GestureState.Result) {
                    mappingEngine.handleInput("GESTURE", gestureState.gestureName)
                }
            }
        }
    }

    fun startDetection() = gestureManager.startDetection()
    fun stopDetection() = gestureManager.stopDetection()
}
```

Replace with:
```kotlin
package com.ohstem.robot_controller.viewmodel

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.MappingEngine
import com.ohstem.robot_controller.data.model.InputBinding
import com.ohstem.robot_controller.data.model.VirtualAction
import com.ohstem.robot_controller.gesture.GestureManager
import com.ohstem.robot_controller.gesture.GestureState
import com.ohstem.robot_controller.repository.MappingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureViewModel @Inject constructor(
    private val gestureManager: GestureManager,
    private val mappingEngine: MappingEngine,
    private val repository: MappingRepository
) : ViewModel() {

    val state = gestureManager.state

    init {
        viewModelScope.launch {
            val profiles = repository.getProfiles().first()
            val profileId: Long = if (profiles.isEmpty()) {
                repository.createProfile("Default", isActive = true)
            } else {
                profiles.firstOrNull { it.isActive }?.id ?: profiles.first().id
            }

            val bindings = repository.getBindings(profileId).first()
            val existingGestureCodes = bindings
                .filter { it.sourceType == "GESTURE" }
                .map { it.sourceCode }
                .toSet()

            val nak = "\u0015"

            if ("Open_Palm" !in existingGestureCodes) {
                val actionId = repository.saveAction(VirtualAction(
                    name = "Move Forward (Gesture)",
                    activationCommand = "${nak}U=1",
                    type = "GESTURE",
                    profileId = profileId
                ))
                repository.saveBinding(InputBinding(
                    sourceType = "GESTURE",
                    sourceCode = "Open_Palm",
                    virtualActionId = actionId,
                    profileId = profileId
                ))
            }

            if ("Closed_Fist" !in existingGestureCodes) {
                val actionId = repository.saveAction(VirtualAction(
                    name = "Stop (Gesture)",
                    activationCommand = "${nak}U=0",
                    type = "GESTURE",
                    profileId = profileId
                ))
                repository.saveBinding(InputBinding(
                    sourceType = "GESTURE",
                    sourceCode = "Closed_Fist",
                    virtualActionId = actionId,
                    profileId = profileId
                ))
            }
        }

        viewModelScope.launch {
            state.collect { gestureState ->
                if (gestureState is GestureState.Result) {
                    mappingEngine.handleInput("GESTURE", gestureState.gestureName)
                }
            }
        }
    }

    fun startDetection() = gestureManager.startDetection()
    fun stopDetection() = gestureManager.stopDetection()

    fun processFrame(imageProxy: ImageProxy) {
        gestureManager.processFrame(imageProxy)
    }
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/com/ohstem/robot_controller/viewmodel/GestureViewModel.kt
git commit -m "feat: add processFrame and seed default gesture bindings"
```

---

### Task 5: Update GestureScreen

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/ui/screens/gesture/GestureScreen.kt`

- [ ] **Replace placeholder screen with camera preview + gesture UI**

Current file is a 67-line stub. Replace with:
```kotlin
package com.ohstem.robot_controller.ui.screens.gesture

import android.Manifest
import android.view.ViewGroup
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
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
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var useFrontCamera by remember { mutableStateOf(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startDetection()
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(useFrontCamera) {
        viewModel.stopDetection()
        viewModel.startDetection()
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
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == android.content.pm.PackageManager.PERMISSION_GRANTED
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
                                val cameraSelector = if (useFrontCamera)
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                else
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }
                                val analyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            viewModel.processFrame(imageProxy)
                                        }
                                    }
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner, cameraSelector, preview, analyzer
                                )
                                setOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                                    override fun onViewAttachedToWindow(v: View) {}
                                    override fun onViewDetachedFromWindow(v: View) {
                                        cameraExecutor.shutdown()
                                    }
                                })
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Camera permission required")
                    }
                }
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (state is GestureState.Detecting) viewModel.stopDetection()
                                else viewModel.startDetection()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (state is GestureState.Detecting) "Stop Camera" else "Start Camera")
                        }

                        OutlinedButton(
                            onClick = { useFrontCamera = !useFrontCamera },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Cameraswitch,
                                contentDescription = "Switch Camera",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (useFrontCamera) "Front" else "Rear")
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Commit**

```bash
git add app/src/main/java/com/ohstem/robot_controller/ui/screens/gesture/GestureScreen.kt
git commit -m "feat: implement gesture screen with camera preview and camera switch"
```

---

### Task 6: Build and verify

- [ ] **Build the project**

```bash
./gradlew assembleDebug 2>&1 | tail -20
```

Expected: BUILD SUCCESSFUL.

- [ ] **Install on device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Expected: Success.

- [ ] **Final commit**

```bash
git add -A && git commit -m "chore: finalize gesture recognition experiment"
```

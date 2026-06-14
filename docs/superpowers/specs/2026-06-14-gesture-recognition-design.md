# Gesture Recognition Experiment — Design Doc

**Date:** 2026-06-14
**Status:** Draft

## Overview

Add gesture recognition to the ohstem-controller Android app using MediaPipe GestureRecognizer. The initial experiment maps two hand gestures to robot movement commands, reusing the existing BLE command infrastructure.

## Architecture

```
GestureScreen (Compose UI)
  ├── AndroidView(PreviewView) ← CameraX Preview (live feed)
  ├── ImageAnalysis.Analyzer
  │    └── viewModel.processFrame(imageProxy)
  │
GestureViewModel
  ├── processFrame() delegates to gestureManager.processFrame()
  └── collects GestureManager.state → routes GestureState.Result to MappingEngine
       │
MediaPipeGestureManager (implements GestureManager)
  ├── Holds GestureRecognizer instance (MediaPipe)
  ├── startDetection(): load model from assets
  ├── stopDetection(): close model
  └── processFrame(imageProxy):
       1. Convert CameraX ImageProxy → MediaPipe MPImage
       2. Call GestureRecognizer.recognize()
       3. Check top classification category
        4. If Open_Palm → emit GestureState.Result("Open_Palm")
        5. If Closed_Fist → emit GestureState.Result("Closed_Fist")
```

### Data Flow

1. CameraX ImageAnalysis delivers each frame as ImageProxy
2. Screen calls viewModel.processFrame(imageProxy)
3. ViewModel delegates to MediaPipeGestureManager.processFrame()
4. Manager converts to MPImage, runs GestureRecognizer
5. If recognized gesture is Open_Palm or Closed_Fist, emit GestureState.Result
6. ViewModel collector routes Result → mappingEngine.handleInput("GESTURE", gestureName)
7. MappingEngine resolves binding → sends BLE command to robot

### Gesture → Command Mapping

| Gesture | Binding | BLE Command | Effect |
|---------|---------|-------------|--------|
| Open_Palm | ("GESTURE", "Open_Palm") → activationCommand = "\u0015U=1" | Forward | Robot moves forward |
| Closed_Fist | ("GESTURE", "Closed_Fist") → activationCommand = "\u0015U=0" | Stop | Robot stops |

## Files to Change

| File | Change |
|------|--------|
| `gesture/GestureManager.kt` | Add `fun processFrame(imageProxy: ImageProxy)` to interface |
| `gesture/MediaPipeGestureManager.kt` | Full implementation: model loading, frame processing, classification |
| `viewmodel/GestureViewModel.kt` | Add `processFrame()` delegate; seed default gesture bindings in init |
| `ui/screens/gesture/GestureScreen.kt` | Camera permission, PreviewView, ImageAnalysis, gesture overlay |
| `assets/gesture_recognizer.task` | Download MediaPipe Gesture Recognizer model file |

### No changes needed to:
- DI modules (GestureModule already wired)
- Navigation (route + bottom tab already exists)
- Gradle dependencies (MediaPipe tasks-vision + CameraX already present)
- AndroidManifest (CAMERA permission already declared)

## GestureManager Interface Change

```kotlin
interface GestureManager {
    val state: StateFlow<GestureState>
    fun startDetection()
    fun stopDetection()
    fun processFrame(imageProxy: ImageProxy)
}
```

`ImageProxy` is from `androidx.camera.core`. This couples the interface to CameraX, which is acceptable for this experiment.

## MediaPipe Integration

- Model: `gesture_recognizer.task` (bundled in `app/src/main/assets/`)
- Load: `GestureRecognizer.createFromOptions(context, options)` where `options` uses `setModelAssetPath("gesture_recognizer.task")`
- Classes used: `GestureRecognizer`, `GestureRecognizerResult`, `MPImage`
- Frame conversion: `MPImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)`
- Classification: recognizer returns `GestureRecognizerResult` with list of `Category` objects sorted by score
- For experiment: check `topCategory.categoryName` against `"Open_Palm"` and `"Closed_Fist"` (MediaPipe uses underscore-separated names)
- Debounce: only emit result if same gesture persists for 3+ consecutive frames (prevent flicker)

### Model File

Download URL: https://storage.googleapis.com/mediapipe-models/gesture_recognizer/gesture_recognizer/float16/latest/gesture_recognizer.task

## CameraX Setup (in GestureScreen)

- `useFrontCamera` state: `remember { mutableStateOf(false) }`
- Camera switch button toggles it and unbinds/rebinds CameraX
- `cameraSelector` derived: if `useFrontCamera` → `CameraSelector.DEFAULT_FRONT_CAMERA` else `DEFAULT_BACK_CAMERA`
- On camera switch: `cameraProvider.unbindAll()` then `cameraProvider.bindToLifecycle()` with new selector

```kotlin
val lifecycleOwner = LocalLifecycleOwner.current
val context = LocalContext.current
var useFrontCamera by remember { mutableStateOf(false) }

// Permission
val permissionLauncher = rememberLauncherForActivityResult(
    Contract = ActivityResultContracts.RequestPermission()
) { granted -> if (granted) viewModel.startDetection() }

// PreviewView
AndroidView(
    factory = { ctx ->
        PreviewView(ctx).apply {
            val cameraProvider = ProcessCameraProvider.getInstance(ctx).get()
            val cameraSelector = if (useFrontCamera)
                CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(surfaceProvider) }
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also { it.setAnalyzer(executor, ImageProxy -> viewModel.processFrame(it)) }
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, analyzer)
        }
    }
)
```

## UI Behavior

- Toggle button: Start/Stop Camera
- Camera switch button: toggle between `CameraSelector.DEFAULT_BACK_CAMERA` and `CameraSelector.DEFAULT_FRONT_CAMERA`. Button icon reflects current camera (e.g., flip camera icon). Switching restarts the camera lifecycle.
- Status card shows current state (Detecting, gesture name, errors)
- Live camera preview fills the screen above the status card
- Simple overlay text shows last detected gesture

## Success Criteria

1. OpenPalm gesture → robot moves forward
2. ClosedFist gesture → robot stops
3. No crashes on camera start/stop toggle
4. Gesture detection within ~500ms of pose

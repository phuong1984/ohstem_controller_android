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
        val rec = recognizer ?: run {
            imageProxy.close()
            return
        }

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

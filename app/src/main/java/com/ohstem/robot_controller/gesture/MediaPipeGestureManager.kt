package com.ohstem.robot_controller.gesture

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.gesturerecognizer.GestureRecognizer
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

    private val lock = Any()
    private var recognizer: GestureRecognizer? = null
    private var lastGesture: String? = null
    private var gestureStreak = 0
    private val requiredStreak = 3

    override fun startDetection() {
        try {
            recognizer = GestureRecognizer.createFromFile(context, "gesture_recognizer.task")
            lastGesture = null
            gestureStreak = 0
            _state.value = GestureState.Detecting
        } catch (e: Exception) {
            _state.value = GestureState.Error("Failed to load model: ${e.message}")
        }
    }

    override fun stopDetection() {
        synchronized(lock) {
            recognizer?.close()
            recognizer = null
        }
        lastGesture = null
        gestureStreak = 0
        _state.value = GestureState.Idle
    }

    override fun processFrame(imageProxy: ImageProxy) {
        val bitmap = imageProxy.toBitmap()
            ?: run {
                imageProxy.close()
                return
            }

        val mpImage = BitmapImageBuilder(bitmap).build()
        val rotation = imageProxy.imageInfo.rotationDegrees
        val options = ImageProcessingOptions.builder()
            .setRotationDegrees(rotation)
            .build()
        val result = synchronized(lock) {
            recognizer?.recognize(mpImage, options)
        }
        imageProxy.close()

        val topCategory = result?.gestures()?.firstOrNull()?.firstOrNull()
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

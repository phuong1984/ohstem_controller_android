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

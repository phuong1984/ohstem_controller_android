package com.ohstem.robot_controller.gesture

import android.content.Context
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

    override fun startDetection() {
        _state.value = GestureState.Detecting
        // TODO: Initialize MediaPipe Hand Landmarker and CameraX
    }

    override fun stopDetection() {
        _state.value = GestureState.Idle
        // TODO: Release CameraX and MediaPipe resources
    }
}

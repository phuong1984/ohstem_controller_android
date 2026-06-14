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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    private val _isDetecting = MutableStateFlow(false)
    val isDetecting: StateFlow<Boolean> = _isDetecting

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

    fun startDetection() {
        gestureManager.startDetection()
        _isDetecting.value = true
    }

    fun stopDetection() {
        gestureManager.stopDetection()
        _isDetecting.value = false
    }

    fun processFrame(imageProxy: ImageProxy) {
        gestureManager.processFrame(imageProxy)
    }
}

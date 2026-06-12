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

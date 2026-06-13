package com.ohstem.robot_controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.BleConnectionState
import com.ohstem.robot_controller.ble.BleManager
import com.ohstem.robot_controller.ble.MappingEngine
import com.ohstem.robot_controller.data.model.InputBinding
import com.ohstem.robot_controller.data.model.VirtualAction
import com.ohstem.robot_controller.repository.MappingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GamepadUiState(
    val isConnected: Boolean = false,
    val isReconnecting: Boolean = false,
    val robotName: String = "ESP32-Robot",
    val commandFps: Int = 0,
    val lastCommand: String = "",
    val showDebugOverlay: Boolean = false
)

@HiltViewModel
class GamepadViewModel @Inject constructor(
    private val mappingEngine: MappingEngine,
    private val bleManager: BleManager,
    private val repository: MappingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GamepadUiState())
    val uiState: StateFlow<GamepadUiState> = _uiState.asStateFlow()

    // Command rate tracking
    private var commandCount = 0
    private var fpsJob: Job? = null
    private var lastCommandTime = 0L

    init {
        // Seed default profile with UP D-Pad mapping
        viewModelScope.launch {
            val profiles = repository.getProfiles().first()
            val profileId: Long = if (profiles.isEmpty()) {
                repository.createProfile("Default", isActive = true)
            } else {
                profiles.firstOrNull { it.isActive }?.id ?: profiles.first().id
            }

            val bindings = repository.getBindings(profileId).first()
            val hasUp = bindings.any { it.sourceType == "GAMEPAD_BUTTON" && it.sourceCode == "UP" }

            if (!hasUp) {
                val nak = "\u0015"
                val actionId = repository.saveAction(VirtualAction(
                    name = "Move Forward",
                    activationCommand = "${nak}U=1",
                    deactivationCommand = "${nak}U=0",
                    type = "BUTTON",
                    profileId = profileId
                ))
                repository.saveBinding(InputBinding(
                    sourceType = "GAMEPAD_BUTTON",
                    sourceCode = "UP",
                    virtualActionId = actionId,
                    profileId = profileId
                ))
            }
        }

        // Track BLE connection state
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _uiState.update {
                    it.copy(
                        isConnected = state is BleConnectionState.Connected,
                        isReconnecting = state is BleConnectionState.Reconnecting,
                        robotName = when (state) {
                            is BleConnectionState.Connected -> state.name?.ifEmpty { "ESP32-Robot" } ?: "ESP32-Robot"
                            else -> it.robotName
                        }
                    )
                }
            }
        }

        // FPS counter: calculate commands per second
        fpsJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(commandFps = commandCount) }
                commandCount = 0
            }
        }
    }

    fun onButtonPressed(buttonCode: String) {
        viewModelScope.launch {
            val command = mappingEngine.handleInput("GAMEPAD_BUTTON", buttonCode, isActivation = true)
            updateCommandState(command)
        }
    }

    fun onButtonReleased(buttonCode: String) {
        viewModelScope.launch {
            val command = mappingEngine.handleInput("GAMEPAD_BUTTON", buttonCode, isActivation = false)
            updateCommandState(command)
        }
    }

    fun onJoystickMove(axis: String, value: Float) {
        viewModelScope.launch {
            val command = mappingEngine.handleJoystick(axis, value)
            updateCommandState(command)
        }
    }

    fun toggleDebugOverlay() {
        _uiState.update { it.copy(showDebugOverlay = !it.showDebugOverlay) }
    }

    private fun updateCommandState(command: String) {
        commandCount++
        if (command.isNotEmpty()) {
            _uiState.update { it.copy(lastCommand = command) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        fpsJob?.cancel()
    }
}
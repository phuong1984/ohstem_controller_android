package com.ohstem.robot_controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.MappingEngine
import com.ohstem.robot_controller.data.model.InputBinding
import com.ohstem.robot_controller.data.model.VirtualAction
import com.ohstem.robot_controller.repository.MappingRepository
import com.ohstem.robot_controller.voice.VoiceManager
import com.ohstem.robot_controller.voice.VoiceRecognitionMode
import com.ohstem.robot_controller.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceManager: VoiceManager,
    private val mappingEngine: MappingEngine,
    private val repository: MappingRepository
) : ViewModel() {

    val state = voiceManager.state

    private val _mode = MutableStateFlow(voiceManager.mode)
    val mode: StateFlow<VoiceRecognitionMode> = _mode

    sealed class UiState {
        object Initializing : UiState()
        object Ready : UiState()
        data class Listening(val partialText: String = "") : UiState()
        data class Listened(val text: String, val isMatched: Boolean = false) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Initializing)
    val uiState: StateFlow<UiState> = _uiState

    private var displayWindowJob: Job? = null
    private var captureWindowJob: Job? = null
    private var capturedText = ""
    private var captureSessionId = 0
    private var consumedPrefix = ""

    private fun stripConsumed(text: String): String {
        return if (text.startsWith(consumedPrefix) && text.length > consumedPrefix.length) {
            text.removePrefix(consumedPrefix).trim()
        } else {
            text
        }
    }

    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { voiceManager.initModel() }
            if (_uiState.value !is UiState.Error) {
                _uiState.value = UiState.Ready
            }
        }

        viewModelScope.launch {
            state.collect { voiceState ->
                when (voiceState) {
                    is VoiceState.Partial -> {
                        if (captureWindowJob == null) {
                            captureSessionId++
                            val session = captureSessionId
                            capturedText = voiceState.text
                            captureWindowJob = viewModelScope.launch capture@{
                                delay(1500)
                                if (session != captureSessionId) return@capture
                                captureWindowJob = null
                                val raw = capturedText
                                val stripped = stripConsumed(raw)
                                consumedPrefix = raw
                                capturedText = ""
                                displayWindowJob?.cancel()
                                _uiState.value = UiState.Listened(stripped)
                                displayWindowJob = viewModelScope.launch display@{
                                    delay(1500)
                                    if (session != captureSessionId) return@display
                                    _uiState.value = UiState.Listening()
                                }
                            }
                        } else {
                            capturedText = voiceState.text
                        }
                        _uiState.value = UiState.Listening(stripConsumed(capturedText))
                    }
                    is VoiceState.Result -> {
                        captureSessionId++
                        captureWindowJob?.cancel()
                        captureWindowJob = null
                        capturedText = ""
                        consumedPrefix = ""
                        displayWindowJob?.cancel()
                        val command = Normalizer.normalize(
                            voiceState.text.lowercase().trim(),
                            Normalizer.Form.NFC
                        )
                        try {
                            mappingEngine.handleInput("VOICE_COMMAND", command)
                        } catch (_: Exception) { }
                        _uiState.value = UiState.Listened("✓ " + voiceState.text, isMatched = true)
                        displayWindowJob = viewModelScope.launch {
                            delay(1500)
                            _uiState.value = UiState.Listening()
                        }
                    }
                    is VoiceState.UtteranceEnd -> {
                        captureSessionId++
                        captureWindowJob?.cancel()
                        captureWindowJob = null
                        val raw = voiceState.text
                        val stripped = stripConsumed(raw)
                        consumedPrefix = raw
                        capturedText = ""
                        displayWindowJob?.cancel()
                        _uiState.value = UiState.Listened(stripped)
                        displayWindowJob = viewModelScope.launch {
                            delay(1500)
                            _uiState.value = UiState.Listening()
                        }
                    }
                    is VoiceState.Listening -> {
                        captureSessionId++
                        captureWindowJob?.cancel()
                        captureWindowJob = null
                        capturedText = ""
                        consumedPrefix = ""
                        _uiState.value = UiState.Listening()
                    }
                    is VoiceState.Idle -> { }
                    is VoiceState.Initializing -> {
                        _uiState.value = UiState.Initializing
                    }
                    is VoiceState.Error -> {
                        _uiState.value = UiState.Error(voiceState.message)
                    }
                }
            }
        }

        viewModelScope.launch {
            try {
                val profiles = repository.getProfiles().first()
                val profileId: Long = if (profiles.isEmpty()) {
                    repository.createProfile("Default", isActive = true)
                } else {
                    profiles.firstOrNull { it.isActive }?.id ?: profiles.first().id
                }

                val bindings = repository.getBindings(profileId).first()
                val existingVoiceCodes = bindings
                    .filter { it.sourceType == "VOICE_COMMAND" }
                    .map { it.sourceCode }
                    .toSet()

                val nak = "\u0015"

                if ("đi tới" !in existingVoiceCodes) {
                    val actionId = repository.saveAction(VirtualAction(
                        name = "Đi tới",
                        activationCommand = "${nak}U=1",
                        type = "VOICE",
                        profileId = profileId
                    ))
                    repository.saveBinding(InputBinding(
                        sourceType = "VOICE_COMMAND",
                        sourceCode = "đi tới",
                        virtualActionId = actionId,
                        profileId = profileId
                    ))
                }

                if ("dừng lại" !in existingVoiceCodes) {
                    val actionId = repository.saveAction(VirtualAction(
                        name = "Dừng lại",
                        activationCommand = "${nak}U=0",
                        type = "VOICE",
                        profileId = profileId
                    ))
                    repository.saveBinding(InputBinding(
                        sourceType = "VOICE_COMMAND",
                        sourceCode = "dừng lại",
                        virtualActionId = actionId,
                        profileId = profileId
                    ))
                }

                val allBindings = repository.getBindings(profileId).first()
                val voiceWords = allBindings
                    .filter { it.sourceType == "VOICE_COMMAND" }
                    .map { it.sourceCode }
                voiceManager.setGrammar(voiceWords)
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Setup failed: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        displayWindowJob?.cancel()
        voiceManager.stopListening()
    }

    fun startListening() {
        displayWindowJob?.cancel()
        voiceManager.startListening()
    }

    fun stopListening() {
        displayWindowJob?.cancel()
        voiceManager.stopListening()
        _uiState.value = UiState.Ready
    }

    fun setMode(newMode: VoiceRecognitionMode) {
        if (newMode == voiceManager.mode) return
        voiceManager.mode = newMode
        _mode.value = newMode
        if (state.value is VoiceState.Listening) {
            voiceManager.startListening()
        }
    }
}

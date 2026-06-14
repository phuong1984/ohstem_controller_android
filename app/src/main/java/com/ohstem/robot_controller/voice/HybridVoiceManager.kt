package com.ohstem.robot_controller.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HybridVoiceManager @Inject constructor(
    private val sherpaOnnxManager: SherpaOnnxVoiceManager,
    private val onlineManager: OnlineVoiceManager,
) : VoiceManager {

    override var mode = VoiceRecognitionMode.OFFLINE

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    private var stateJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeManager: VoiceManager
        get() = if (mode == VoiceRecognitionMode.OFFLINE) sherpaOnnxManager else onlineManager

    override fun startListening() {
        stopListening()
        stateJob = scope.launch {
            activeManager.state.collect { s ->
                _state.value = s
            }
        }
        activeManager.startListening()
    }

    override fun stopListening() {
        stateJob?.cancel()
        stateJob = null
        sherpaOnnxManager.stopListening()
        onlineManager.stopListening()
        _state.value = VoiceState.Idle
    }

    override fun initModel() {
        sherpaOnnxManager.initModel()
    }

    override fun setGrammar(words: List<String>) {
        sherpaOnnxManager.setGrammar(words)
        onlineManager.setGrammar(words)
    }
}

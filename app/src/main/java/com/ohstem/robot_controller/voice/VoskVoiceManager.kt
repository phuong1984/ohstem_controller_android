package com.ohstem.robot_controller.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskVoiceManager @Inject constructor(
    private val context: Context
) : VoiceManager {
    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    override fun initModel() {
        _state.value = VoiceState.Initializing
        // TODO: Load Vosk model from assets
    }

    override fun startListening() {
        _state.value = VoiceState.Listening
        // TODO: Start SpeechService
    }

    override fun stopListening() {
        _state.value = VoiceState.Idle
        // TODO: Stop SpeechService
    }
}

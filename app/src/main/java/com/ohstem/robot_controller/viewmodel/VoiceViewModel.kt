package com.ohstem.robot_controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.MappingEngine
import com.ohstem.robot_controller.voice.VoiceManager
import com.ohstem.robot_controller.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceManager: VoiceManager,
    private val mappingEngine: MappingEngine
) : ViewModel() {

    val state = voiceManager.state

    init {
        viewModelScope.launch {
            state.collectLatest { voiceState ->
                if (voiceState is VoiceState.Result) {
                    // Normalize text and send to Mapping Engine
                    val command = voiceState.text.lowercase().trim()
                    mappingEngine.handleInput("VOICE_COMMAND", command)
                }
            }
        }
    }

    fun startListening() = voiceManager.startListening()
    fun stopListening() = voiceManager.stopListening()
}

package com.ohstem.robot_controller.voice

import kotlinx.coroutines.flow.StateFlow

sealed class VoiceState {
    object Idle : VoiceState()
    object Initializing : VoiceState()
    object Listening : VoiceState()
    data class Partial(val text: String) : VoiceState()
    data class Result(val text: String) : VoiceState()
    data class UtteranceEnd(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

enum class VoiceRecognitionMode { OFFLINE, ONLINE }

interface VoiceManager {
    val state: StateFlow<VoiceState>
    var mode: VoiceRecognitionMode
    fun startListening()
    fun stopListening()
    fun initModel()
    fun setGrammar(words: List<String>)
}

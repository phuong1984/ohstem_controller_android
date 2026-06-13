package com.ohstem.robot_controller.voice

import kotlinx.coroutines.flow.StateFlow

sealed class VoiceState {
    object Idle : VoiceState()
    object Initializing : VoiceState()
    object Listening : VoiceState()
    data class Partial(val text: String) : VoiceState()
    data class Result(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

interface VoiceManager {
    val state: StateFlow<VoiceState>
    fun startListening()
    fun stopListening()
    fun initModel()
    /**
     * Constrain recognition to the given vocabulary words.
     * Must be called before [startListening] to take effect.
     * Passing an empty list resets to free-form (open vocabulary) mode.
     */
    fun setGrammar(words: List<String>)
}

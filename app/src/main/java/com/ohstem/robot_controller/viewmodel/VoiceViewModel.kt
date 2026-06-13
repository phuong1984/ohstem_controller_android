package com.ohstem.robot_controller.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ohstem.robot_controller.ble.MappingEngine
import com.ohstem.robot_controller.data.model.InputBinding
import com.ohstem.robot_controller.data.model.VirtualAction
import com.ohstem.robot_controller.repository.MappingRepository
import com.ohstem.robot_controller.voice.VoiceManager
import com.ohstem.robot_controller.voice.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceManager: VoiceManager,
    private val mappingEngine: MappingEngine,
    private val repository: MappingRepository
) : ViewModel() {

    val state = voiceManager.state

    init {
        viewModelScope.launch {
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

            voiceManager.initModel()
        }

        viewModelScope.launch {
            state.collectLatest { voiceState ->
                if (voiceState is VoiceState.Result) {
                    val command = voiceState.text.lowercase().trim()
                    mappingEngine.handleInput("VOICE_COMMAND", command)
                }
            }
        }
    }

    fun startListening() = voiceManager.startListening()
    fun stopListening() = voiceManager.stopListening()
}

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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

            // ✅ FIX: Re-read bindings (may have just inserted new ones) and tell
            // the recognizer to constrain itself to exactly this vocabulary.
            // Vosk's grammar-constrained mode gives near-perfect accuracy for
            // a small fixed command set compared to free-form open-vocabulary.
            val allBindings = repository.getBindings(profileId).first()
            val voiceWords = allBindings
                .filter { it.sourceType == "VOICE_COMMAND" }
                .map { it.sourceCode }
            val decoys = listOf("vâng", "không", "ừ", "à", "ồ", "xin chờ")
            voiceManager.setGrammar(voiceWords + decoys)

            withContext(Dispatchers.IO) { voiceManager.initModel() }
        }

        viewModelScope.launch {
            state.collect { voiceState ->
                if (voiceState is VoiceState.Result) {
                    // ✅ FIX: Normalize to NFC so Vietnamese diacritics (e.g.
                    // "ới" as combining chars) compare equal to the stored
                    // binding codes regardless of Vosk's unicode normalization.
                    val command = Normalizer.normalize(
                        voiceState.text.lowercase().trim(),
                        Normalizer.Form.NFC
                    )
                    mappingEngine.handleInput("VOICE_COMMAND", command)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.stopListening()
    }

    fun startListening() = voiceManager.startListening()
    fun stopListening() = voiceManager.stopListening()
}

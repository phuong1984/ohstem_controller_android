package com.ohstem.robot_controller.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoskVoiceManager @Inject constructor(
    private val context: Context
) : VoiceManager {

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recognitionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun initModel() {
        _state.value = VoiceState.Initializing
        try {
            val modelPath = StorageService.sync(context, "vosk-model-small-vn", "vosk-model-small-vn")
            model = Model(modelPath)
            recognizer = Recognizer(model, 16000.0f)
            _state.value = VoiceState.Idle
        } catch (e: Exception) {
            _state.value = VoiceState.Error("Model init failed: ${e.message}")
        }
    }

    override fun startListening() {
        if (_state.value is VoiceState.Listening) return
        stopListening()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = VoiceState.Error("RECORD_AUDIO permission denied")
            return
        }

        val r = recognizer
        if (r == null) {
            _state.value = VoiceState.Error("Recognizer not initialized")
            return
        }

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _state.value = VoiceState.Error("Failed to get buffer size")
            return
        }

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            _state.value = VoiceState.Error("AudioRecord failed to initialize")
            return
        }

        audioRecord = record
        _state.value = VoiceState.Listening
        record.startRecording()

        val buffer = ByteArray(bufferSize)
        recognitionJob = scope.launch {
            while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val bytesRead = record.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    val isFinal = r.acceptWaveForm(buffer, bytesRead)
                    if (isFinal) {
                        val text = parseResultText(r.result)
                        _state.value = VoiceState.Result(text)
                        r.reset()
                    } else {
                        val text = parsePartialText(r.partialResult)
                        _state.value = VoiceState.Partial(text)
                    }
                }
            }
        }
    }

    override fun stopListening() {
        audioRecord?.apply {
            if (recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                stop()
            }
        }
        recognitionJob?.cancel()
        recognitionJob = null
        audioRecord?.release()
        audioRecord = null
        _state.value = VoiceState.Idle
    }

    private fun parseResultText(json: String): String {
        val prefix = "\"text\": \""
        val start = json.indexOf(prefix)
        if (start == -1) return ""
        val valueStart = start + prefix.length
        val end = json.indexOf("\"", valueStart)
        return if (end == -1) "" else json.substring(valueStart, end)
    }

    private fun parsePartialText(json: String): String {
        val prefix = "\"partial\": \""
        val start = json.indexOf(prefix)
        if (start == -1) return ""
        val valueStart = start + prefix.length
        val end = json.indexOf("\"", valueStart)
        return if (end == -1) "" else json.substring(valueStart, end)
    }
}

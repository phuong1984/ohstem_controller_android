package com.ohstem.robot_controller.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SherpaOnnxVoiceManager @Inject constructor(
    private val context: Context,
) : VoiceManager {

    companion object {
        private const val TAG = "SherpaOnnxVoiceManager"
    }

    @Volatile
    override var mode = VoiceRecognitionMode.OFFLINE

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    @Volatile
    private var isListening = false
    @Volatile
    private var recognizer: OfflineRecognizer? = null
    @Volatile
    private var vad: Vad? = null
    private var recognitionJob: Job? = null
    @Volatile
    private var grammarWords: List<String> = emptyList()

    private val sampleRate = 16000
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun initModel() {
        _state.value = VoiceState.Initializing
        try {
            val modelDir = "sherpa-onnx-zipformer-vi-30M-int8-2026-02-09"
            recognizer = OfflineRecognizer(
                context.assets,
                OfflineRecognizerConfig(
                    featConfig = FeatureConfig(
                        sampleRate = sampleRate,
                        featureDim = 80,
                    ),
                    modelConfig = OfflineModelConfig(
                        transducer = OfflineTransducerModelConfig(
                            encoder = "$modelDir/encoder.int8.onnx",
                            decoder = "$modelDir/decoder.onnx",
                            joiner = "$modelDir/joiner.int8.onnx",
                        ),
                        tokens = "$modelDir/tokens.txt",
                        numThreads = 2,
                        modelType = "transducer",
                    ),
                    decodingMethod = "greedy_search",
                )
            )
            vad = Vad(
                context.assets,
                VadModelConfig(
                    sileroVadModelConfig = SileroVadModelConfig(
                        model = "silero_vad.onnx",
                        threshold = 0.5f,
                        minSilenceDuration = 0.3f,
                        minSpeechDuration = 0.25f,
                        windowSize = 512,
                        maxSpeechDuration = 10.0f,
                    ),
                    sampleRate = sampleRate,
                    numThreads = 1,
                )
            )
            _state.value = VoiceState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "initModel failed", e)
            _state.value = VoiceState.Error("initModel failed: ${e.message}")
        }
    }

    override fun setGrammar(words: List<String>) {
        grammarWords = words
    }

    override fun startListening() {
        if (isListening) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            _state.value = VoiceState.Error("RECORD_AUDIO permission not granted")
            return
        }
        stopListening()

        if (recognizer == null || vad == null) {
            _state.value = VoiceState.Error("Recognizer not initialized")
            return
        }

        isListening = true
        _state.value = VoiceState.Listening

        recognitionJob = scope.launch {
            try {
                runRecognitionLoop()
            } catch (e: Exception) {
                Log.e(TAG, "Recognition loop crashed", e)
                _state.value = VoiceState.Error("Recognition error: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        isListening = false
        recognitionJob?.cancel()
        recognitionJob = null
        _state.value = VoiceState.Idle
    }

    fun destroy() {
        stopListening()
        vad?.release()
        vad = null
        recognizer?.release()
        recognizer = null
    }

    private fun runRecognitionLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize * 4, 4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord init failed, state=${record.state}")
            record.release()
            _state.value = VoiceState.Error("AudioRecord init failed")
            return
        }

        try {
            record.startRecording()

            val frameSamples = (sampleRate * 0.03).toInt()
            val readBuf = ShortArray(frameSamples)
            val floatBuf = FloatArray(frameSamples)
            var frameCount = 0

            while (isListening && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val samplesRead = record.read(readBuf, 0, readBuf.size)
                if (samplesRead <= 0) continue
                frameCount++

                for (i in 0 until samplesRead) {
                    floatBuf[i] = readBuf[i].toFloat() / 32768f
                }

                vad?.acceptWaveform(floatBuf.copyOfRange(0, samplesRead))

                while (vad?.empty() == false) {
                    val segment = vad!!.front()
                    vad!!.pop()
                    processSegment(segment)
                }

                if (frameCount % 100 == 0) {
                    Log.d(TAG, "Frame $frameCount: vad.isSpeechDetected=${vad?.isSpeechDetected()}")
                }
            }

            vad?.flush()
            while (vad?.empty() == false) {
                val segment = vad!!.front()
                vad!!.pop()
                processSegment(segment)
            }

            Log.d(TAG, "Recognition loop ended, isListening=$isListening")
        } finally {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            record.release()
        }
    }

    private fun processSegment(segment: SpeechSegment) {
        if (segment.samples.size < sampleRate / 5) {
            Log.d(TAG, "Segment too short: ${segment.samples.size} < ${sampleRate / 5}")
            return
        }

        Log.d(TAG, "Processing segment: len=${segment.samples.size} samples")

        val r = recognizer ?: return
        try {
            r.createStream().use { stream ->
                stream.acceptWaveform(segment.samples, sampleRate)
                r.decode(stream)
                val result = r.getResult(stream)
                val text = result.text.trim().lowercase()
                Log.d(TAG, "Recognition result: \"$text\"")
                if (text.isNotEmpty()) {
                    val matched = matchAndEmitResult(text)
                    if (!matched) {
                        Log.d(TAG, "No keyword match -> emit UtteranceEnd")
                        _state.value = VoiceState.UtteranceEnd(text)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognition error", e)
        }
    }

    private fun matchAndEmitResult(text: String): Boolean {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        Log.d(TAG, "Matching \"$normalized\" against $grammarWords")
        for (word in grammarWords) {
            val normalizedWord = Normalizer.normalize(word, Normalizer.Form.NFC)
            if (normalized.contains(normalizedWord, ignoreCase = true)) {
                Log.d(TAG, "Matched: \"$normalizedWord\" -> emit Result")
                _state.value = VoiceState.Result(normalizedWord)
                return true
            }
        }
        Log.d(TAG, "No match found for \"$normalized\"")
        return false
    }
}

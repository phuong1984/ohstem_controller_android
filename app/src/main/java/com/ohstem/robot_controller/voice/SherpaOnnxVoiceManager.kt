package com.ohstem.robot_controller.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SherpaOnnxVoiceManager @Inject constructor(
    private val context: Context,
) : VoiceManager {

    companion object {
        private const val TAG = "SherpaOnnxVoiceManager"
    }

    override var mode = VoiceRecognitionMode.OFFLINE

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    @Volatile
    private var isListening = false
    private var recognizer: OfflineRecognizer? = null
    private var recognitionJob: Job? = null
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
        stopListening()

        if (recognizer == null) {
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

    private fun runRecognitionLoop() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize * 4, 4096)

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            _state.value = VoiceState.Error("AudioRecord init failed")
            return
        }

        record.startRecording()

        val ringBufferSize = sampleRate * 5
        val ringBuffer = ShortArray(ringBufferSize)
        var ringWritePos = 0

        val frameSamples = (sampleRate * 0.03).toInt()
        val readBuf = ShortArray(frameSamples)

        var noiseFloor = 0.0
        var speechStart = -1
        var speechEnd = -1
        var silenceFrames = 0
        val silenceThresholdFrames = (0.5 / 0.03).toInt()
        val minSpeechFrames = (0.2 / 0.03).toInt()

        while (isListening && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val samplesRead = record.read(readBuf, 0, readBuf.size)
            if (samplesRead <= 0) continue

            var rms = 0.0
            for (i in 0 until samplesRead) {
                val s = readBuf[i].toDouble()
                rms += s * s
            }
            rms = sqrt(rms / samplesRead)

            if (noiseFloor == 0.0) {
                noiseFloor = rms
            } else {
                noiseFloor = noiseFloor * 0.95 + rms * 0.05
            }

            for (i in 0 until samplesRead) {
                ringBuffer[ringWritePos] = readBuf[i]
                ringWritePos = (ringWritePos + 1) % ringBufferSize
            }

            val speechThreshold = noiseFloor * 2.0
            val silenceThreshold = noiseFloor * 1.5

            if (speechStart < 0) {
                if (rms > speechThreshold) {
                    val preRollSamples = frameSamples * 2
                    speechStart = (ringWritePos - samplesRead - preRollSamples + ringBufferSize) % ringBufferSize
                    speechEnd = ringWritePos
                    silenceFrames = 0
                }
            } else {
                speechEnd = ringWritePos
                if (rms < silenceThreshold) {
                    silenceFrames++
                    if (silenceFrames >= silenceThresholdFrames) {
                        val segmentLen = if (speechEnd >= speechStart) {
                            speechEnd - speechStart
                        } else {
                            ringBufferSize - speechStart + speechEnd
                        }
                        if (segmentLen.toDouble() / frameSamples >= minSpeechFrames) {
                            processSpeechSegment(ringBuffer, speechStart, speechEnd, ringBufferSize)
                        }
                        speechStart = -1
                        speechEnd = -1
                        silenceFrames = 0
                    }
                } else {
                    silenceFrames = 0
                }
            }
        }

        if (speechStart >= 0) {
            val segmentLen = if (speechEnd >= speechStart) {
                speechEnd - speechStart
            } else {
                ringBufferSize - speechStart + speechEnd
            }
            if (segmentLen.toDouble() / frameSamples >= minSpeechFrames) {
                processSpeechSegment(ringBuffer, speechStart, speechEnd, ringBufferSize)
            }
        }

        runCatching {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) record.stop()
            record.release()
        }
    }

    private fun processSpeechSegment(
        ringBuffer: ShortArray,
        startPos: Int,
        endPos: Int,
        ringBufferSize: Int,
    ) {
        val segmentLen: Int
        val segment: ShortArray
        if (endPos > startPos) {
            segmentLen = endPos - startPos
            segment = ringBuffer.copyOfRange(startPos, endPos)
        } else {
            segmentLen = ringBufferSize - startPos + endPos
            segment = ShortArray(segmentLen)
            val firstPart = ringBufferSize - startPos
            System.arraycopy(ringBuffer, startPos, segment, 0, firstPart)
            System.arraycopy(ringBuffer, 0, segment, firstPart, endPos)
        }

        val floatAudio = FloatArray(segmentLen) { i ->
            segment[i].toFloat() / 32768f
        }

        val r = recognizer ?: return
        r.createStream().use { stream ->
            stream.acceptWaveform(floatAudio, sampleRate)
            r.decode(stream)
            val text = r.getResult(stream).text.trim().lowercase()
            if (text.isNotEmpty()) {
                _state.value = VoiceState.Partial(text)
                matchAndEmitResult(text)
            }
        }
    }

    private fun matchAndEmitResult(text: String) {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
        for (word in grammarWords) {
            val normalizedWord = Normalizer.normalize(word, Normalizer.Form.NFC)
            if (normalized.contains(normalizedWord, ignoreCase = true)) {
                _state.value = VoiceState.Result(normalizedWord)
                return
            }
        }
    }
}

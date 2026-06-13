package com.ohstem.robot_controller.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.StorageService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
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

    companion object {
        private const val TAG = "VoskVoiceManager"
        private const val VOSK_SAMPLE_RATE = 16000.0f
    }

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var audioRecord: AudioRecord? = null
    private var recognitionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Grammar JSON string; null → free-form (open vocabulary)
    private var grammarJson: String? = null

    // ────────────────────────────────────────────────────────────
    // VoiceManager interface
    // ────────────────────────────────────────────────────────────

    /**
     * Store the grammar to use on the next [startListening] call.
     * Constraining the recognizer to a small word-list dramatically
     * improves accuracy for command-based use-cases.
     */
    override fun setGrammar(words: List<String>) {
        grammarJson = if (words.isEmpty()) {
            null
        } else {
            val entries = (words.map { "\"$it\"" } + listOf("\"[unk]\"")).joinToString(", ")
            "[$entries]"
        }
        Log.d(TAG, "Grammar updated: $grammarJson")
    }

    override fun initModel() {
        _state.value = VoiceState.Initializing
        try {
            val modelPath = StorageService.sync(context, "vosk-model-vn", "vosk-model-vn")
            Log.d(TAG, "Model synced to path: $modelPath")
            model = Model(modelPath)
            Log.d(TAG, "Model loaded successfully")
            _state.value = VoiceState.Idle
        } catch (e: Exception) {
            Log.e(TAG, "Model init failed", e)
            _state.value = VoiceState.Error("Model init failed: ${e.message}")
        }
    }

    override fun startListening() {
        if (_state.value is VoiceState.Listening) return
        // Always clean up the previous session first
        stopListening()

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = VoiceState.Error("RECORD_AUDIO permission denied")
            return
        }

        // ✅ FIX #1 & #3: Create a fresh Recognizer for every session so there
        //    is no stale audio context from previous sessions, and apply the
        //    grammar constraint for the small command vocabulary.
        val r = createRecognizer()
        if (r == null) {
            _state.value = VoiceState.Error("Recognizer not initialized. Is the model loaded?")
            return
        }
        recognizer = r

        val record: AudioRecord
        val usedSampleRate: Int
        try {
            val result = createAudioRecord()
            usedSampleRate = result.first
            record = result.second
        } catch (e: Exception) {
            _state.value = VoiceState.Error("AudioRecord failed: ${e.message}")
            return
        }

        // ✅ FIX #9: Set Listening state AFTER startRecording() succeeds
        try {
            record.startRecording()
        } catch (e: Exception) {
            record.release()
            _state.value = VoiceState.Error("startRecording failed: ${e.message}")
            return
        }
        audioRecord = record
        _state.value = VoiceState.Listening

        val readBuf = ByteArray(8192)
        // Extra room for resampled output (44100→16000 is 2.76× compression;
        // 8000→16000 is 2× expansion — 16384 covers both directions safely)
        val resampleBuf = ByteArray(16384)

        recognitionJob = scope.launch {
            try {
                var totalBytes = 0L
                val sessionStartMs = System.currentTimeMillis()
                while (isActive && record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    val bytesRead = try {
                        record.read(readBuf, 0, readBuf.size)
                    } catch (e: Exception) {
                        Log.w(TAG, "AudioRecord read failed", e)
                        break
                    }
                    if (bytesRead > 0) {
                        totalBytes += bytesRead

                        val (feedBytes, feedBuffer) = when (usedSampleRate) {
                            44100 -> downsample16000(readBuf, bytesRead, resampleBuf) to resampleBuf
                            8000  -> upsample16000(readBuf, bytesRead, resampleBuf) to resampleBuf
                            else  -> bytesRead to readBuf
                        }

                        if (totalBytes % 65536 < bytesRead) {
                            Log.d(TAG, "Audio fed: $totalBytes bytes, sampleRate=$usedSampleRate")
                        }

                        val isFinal = r.acceptWaveForm(feedBuffer, feedBytes)
                        if (isFinal) {
                            val json = r.result
                            val text = parseJson(json, "text")
                            val confidence = parseAvgConfidence(json)
                            val audioMs = System.currentTimeMillis() - sessionStartMs

                            val isJunk = text.isEmpty() || text == "[unk]"
                            val isConfident = confidence < 0f || confidence >= 0.3f
                            val isLongEnough = audioMs >= 300

                            Log.d(TAG, "Result: text='$text' conf=%.2f audio=%dms junk=%s accept=%s"
                                .format(confidence, audioMs, isJunk, !isJunk && isConfident && isLongEnough))

                            if (!isJunk && isConfident && isLongEnough) {
                                _state.value = VoiceState.Result(text)
                                delay(300)
                            }
                            r.reset()
                            if (isActive) _state.value = VoiceState.Listening
                        } else {
                            val jsonPartial = r.partialResult
                            val text = parseJson(jsonPartial, "partial")
                            if (text.isNotEmpty()) {
                                _state.value = VoiceState.Partial(text)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Recognition loop crashed", e)
                _state.value = VoiceState.Error("Recognition error: ${e.message}")
            }
        }
    }

    override fun stopListening() {
        runCatching {
            audioRecord?.let { rec ->
                if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop()
            }
        }
        recognitionJob?.cancel()
        runCatching {
            audioRecord?.let { rec ->
                if (rec.recordingState != AudioRecord.RECORDSTATE_STOPPED) rec.stop()
                rec.release()
            }
        }
        audioRecord = null
        recognitionJob = null
        recognizer?.reset()
        _state.value = VoiceState.Idle
    }

    // ────────────────────────────────────────────────────────────
    // Private helpers
    // ────────────────────────────────────────────────────────────

    /**
     * Creates a brand-new [Recognizer] using the current grammar.
     *
     * ✅ FIX #2: Grammar-constrained recognizer limits decoding to only the
     *    command words, giving huge accuracy gains for a small vocabulary.
     * ✅ FIX #3: Called at the start of every [startListening] so each session
     *    begins with a clean internal state.
     */
    private fun createRecognizer(): Recognizer? {
        val m = model ?: return null
        return try {
            val grammar = grammarJson
            if (grammar != null) {
                Log.d(TAG, "Creating grammar-constrained recognizer: $grammar")
                Recognizer(m, VOSK_SAMPLE_RATE, grammar)
            } else {
                Log.d(TAG, "Creating free-form recognizer (no grammar set)")
                Recognizer(m, VOSK_SAMPLE_RATE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Recognizer creation failed", e)
            null
        }
    }

    /** Parse a Vosk JSON result safely using [JSONObject]. */
    private fun parseJson(json: String, key: String): String {
        return try {
            JSONObject(json).optString(key, "")
        } catch (e: Exception) {
            Log.w(TAG, "JSON parse failed for key='$key' in: $json", e)
            ""
        }
    }

    /** Average word confidence from Vosk grammar-mode result JSON. Returns -1f if unavailable. */
    private fun parseAvgConfidence(json: String): Float {
        return try {
            val arr = JSONObject(json).optJSONArray("result") ?: return -1f
            if (arr.length() == 0) return -1f
            var sum = 0.0
            for (i in 0 until arr.length()) {
                sum += arr.getJSONObject(i).optDouble("conf", 0.0)
            }
            (sum / arr.length()).toFloat()
        } catch (e: Exception) {
            Log.w(TAG, "parseAvgConfidence failed: $json", e)
            -1f
        }
    }

    /**
     * Downsample from 44 100 Hz → 16 000 Hz.
     *
     * ✅ FIX #4: Replaces the old linear-interpolation-only approach with an
     *    averaging (box) filter that acts as a low-pass filter, preventing the
     *    aliasing that was corrupting every audio frame.
     *
     * Assumes mono PCM-16 little-endian input.
     */
    private fun downsample16000(input: ByteArray, inputBytes: Int, output: ByteArray): Int {
        val inputSamples = inputBytes / 2
        val ratio = 44100.0 / 16000.0
        val outputSamples = (inputSamples / ratio).toInt()

        val inputShort = ShortArray(inputSamples) { i ->
            val lo = input[i * 2].toInt() and 0xFF
            val hi = input[i * 2 + 1].toInt() shl 8
            (hi or lo).toShort()
        }

        // Averaging window: for each output sample average all source samples
        // that fall inside its window → implicit low-pass, no aliasing
        for (i in 0 until outputSamples) {
            val srcStart = (i * ratio).toInt().coerceIn(0, inputSamples - 1)
            val srcEnd   = ((i + 1) * ratio).toInt().coerceAtMost(inputSamples)
            val count    = (srcEnd - srcStart).coerceAtLeast(1)
            var sum = 0L
            for (j in srcStart until srcEnd) sum += inputShort[j]
            val sample = (sum / count).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[i * 2]     = (sample and 0xFF).toByte()
            output[i * 2 + 1] = (sample shr 8 and 0xFF).toByte()
        }
        return outputSamples * 2
    }

    /**
     * Upsample from 8 000 Hz → 16 000 Hz using linear interpolation.
     *
     * ✅ FIX #5: Replaces nearest-neighbour sample duplication with smooth
     *    linear interpolation, removing the staircase distortion.
     *
     * Assumes mono PCM-16 little-endian input.
     */
    private fun upsample16000(input: ByteArray, inputBytes: Int, output: ByteArray): Int {
        val inputSamples  = inputBytes / 2
        val outputSamples = inputSamples * 2   // 8000 × 2 = 16000

        val inputShort = ShortArray(inputSamples) { i ->
            val lo = input[i * 2].toInt() and 0xFF
            val hi = input[i * 2 + 1].toInt() shl 8
            (hi or lo).toShort()
        }

        for (i in 0 until outputSamples) {
            val pos  = i * 0.5f
            val i0   = pos.toInt().coerceIn(0, inputSamples - 1)
            val i1   = (i0 + 1).coerceIn(0, inputSamples - 1)
            val frac = pos - i0
            val sample = (inputShort[i0] * (1f - frac) + inputShort[i1] * frac)
                .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            output[i * 2]     = (sample and 0xFF).toByte()
            output[i * 2 + 1] = (sample shr 8 and 0xFF).toByte()
        }
        return outputSamples * 2
    }

    private fun createAudioRecord(): Pair<Int, AudioRecord> {
        val hasMic = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        Log.d(TAG, "createAudioRecord: hasSystemFeature(MICROPHONE)=$hasMic")
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        Log.d(TAG, "createAudioRecord: mode=${audioManager?.mode}, isMicrophoneMute=${audioManager?.isMicrophoneMute}")

        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.UNPROCESSED,
            MediaRecorder.AudioSource.CAMCORDER,
        )
        val rates    = listOf(16000, 44100, 8000)
        val channels  = listOf(AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_DEFAULT, AudioFormat.CHANNEL_IN_STEREO)
        val encodings = listOf(AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_DEFAULT, AudioFormat.ENCODING_PCM_FLOAT)

        for (source in sources) {
            for (sr in rates) {
                for (ch in channels) {
                    for (enc in encodings) {
                        val tag = "src=$source sr=$sr ch=$ch enc=$enc"
                        try {
                            val fmt = AudioFormat.Builder()
                                .setSampleRate(sr)
                                .setChannelMask(ch)
                                .setEncoding(enc)
                                .build()
                            var bufSize = AudioRecord.getMinBufferSize(fmt.sampleRate, fmt.channelMask, fmt.encoding)
                            if (bufSize <= 0) bufSize = (sr * 2 * 0.4).toInt().coerceIn(2048, 32768)
                            val record = AudioRecord.Builder()
                                .setAudioSource(source)
                                .setAudioFormat(fmt)
                                .setBufferSizeInBytes(bufSize)
                                .build()
                            if (record.state == AudioRecord.STATE_INITIALIZED) {
                                Log.d(TAG, "createAudioRecord: SUCCESS with $tag (channelCount=${record.channelCount})")
                                return Pair(sr, record)
                            }
                            Log.w(TAG, "createAudioRecord: Builder failed state=${record.state} for $tag")
                            record.release()
                        } catch (e: Exception) {
                            Log.w(TAG, "createAudioRecord: Builder exception for $tag: $e")
                        }
                    }
                }
            }
        }

        // Legacy path
        for (source in sources) {
            for (sr in rates) {
                val tag = "Legacy src=$source sr=$sr"
                try {
                    val ch  = AudioFormat.CHANNEL_IN_MONO
                    val enc = AudioFormat.ENCODING_PCM_16BIT
                    var bufSize = AudioRecord.getMinBufferSize(sr, ch, enc)
                    if (bufSize <= 0) bufSize = (sr * 2 * 0.4).toInt().coerceIn(2048, 32768)
                    @Suppress("DEPRECATION")
                    val record = AudioRecord(source, sr, ch, enc, bufSize)
                    if (record.state == AudioRecord.STATE_INITIALIZED) {
                        Log.d(TAG, "createAudioRecord: LEGACY SUCCESS with $tag")
                        return Pair(sr, record)
                    }
                    Log.w(TAG, "createAudioRecord: Legacy failed state=${record.state} for $tag")
                    record.release()
                } catch (e: Exception) {
                    Log.w(TAG, "createAudioRecord: Legacy exception for $tag: $e")
                }
            }
        }

        throw RuntimeException("No mic input available on this device")
    }
}

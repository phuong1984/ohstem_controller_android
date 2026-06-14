package com.ohstem.robot_controller.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnlineVoiceManager @Inject constructor(
    private val context: Context
) : VoiceManager {

    override var mode = VoiceRecognitionMode.ONLINE

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var grammarWords: List<String> = emptyList()
    private var lastSentCommand: String? = null
    private var sessionId = 0
    private var restartPending = false
    private var recognitionIntent: Intent? = null
    private val restartHandler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "OnlineVoiceManager"
    }

    override fun initModel() {
        _state.value = VoiceState.Idle
    }

    override fun setGrammar(words: List<String>) {
        grammarWords = words
    }

    override fun startListening() {
        if (isListening) return
        stopListening()
        isListening = true
        createSession()
    }

    private fun createSession() {
        sessionId++
        val currentSessionId = sessionId
        Log.d(TAG, "createSession id=$currentSessionId")

        speechRecognizer?.destroy()
        speechRecognizer = null

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        if (recognizer == null) {
            isListening = false
            _state.value = VoiceState.Error("SpeechRecognizer not available (no Google Play Services?)")
            return
        }
        speechRecognizer = recognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra("android.speech.extra.SPEECH_INPUT_MINIMUM_LENGTH_MILLIS", 500L)
            putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 500L)
        }
        recognitionIntent = intent

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (currentSessionId != sessionId) return
                _state.value = VoiceState.Listening
                Log.d(TAG, "onReadyForSpeech id=$currentSessionId")
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (currentSessionId != sessionId) return
                if (restartPending) {
                    Log.d(TAG, "onEndOfSpeech id=$currentSessionId SKIP (restartPending)")
                    return
                }
                Log.d(TAG, "onEndOfSpeech id=$currentSessionId")
                restartHandler.removeCallbacks(restartRunnable)
                restartHandler.post(restartRunnable)
            }

            override fun onResults(results: Bundle?) {
                if (currentSessionId != sessionId) {
                    Log.d(TAG, "onResults STALE id=$currentSessionId != $sessionId")
                    return
                }
                if (restartPending) {
                    Log.d(TAG, "onResults id=$currentSessionId SKIP (restartPending)")
                    return
                }
                Log.d(TAG, "onResults id=$currentSessionId")
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.lowercase()
                    ?.trim() ?: ""
                val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
                var matched = false
                if (normalized.isNotEmpty()) {
                    val matchedWord = grammarWords.firstOrNull { normalized.contains(it, ignoreCase = true) }
                    if (matchedWord != null && matchedWord != lastSentCommand) {
                        lastSentCommand = matchedWord
                        _state.value = VoiceState.Result(matchedWord)
                        matched = true
                    }
                }
                if (!matched && normalized.isNotEmpty()) {
                    _state.value = VoiceState.UtteranceEnd(normalized)
                }
                lastSentCommand = null
                if (isListening) {
                    restartHandler.removeCallbacks(restartRunnable)
                    restartHandler.post(restartRunnable)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (currentSessionId != sessionId) return
                val text = partialResults
                    ?.getStringArrayList("results_recognition")
                    ?.firstOrNull()
                    ?.lowercase()
                    ?.trim() ?: ""
                val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
                if (normalized.isNotEmpty()) {
                    val matchedWord = grammarWords.firstOrNull { normalized.contains(it, ignoreCase = true) }
                    if (matchedWord != null && matchedWord != lastSentCommand) {
                        lastSentCommand = matchedWord
                        _state.value = VoiceState.Result(matchedWord)
                        if (!restartPending) {
                            restartPending = true
                            restartHandler.removeCallbacks(restartRunnable)
                            restartHandler.postDelayed(restartRunnable, 500L)
                        }
                        Log.d(TAG, "Partial keyword: $matchedWord (restart in 500ms)")
                    } else {
                        _state.value = VoiceState.Partial(text)
                    }
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onError(error: Int) {
                if (currentSessionId != sessionId) {
                    Log.d(TAG, "onError STALE id=$currentSessionId != $sessionId, ignoring")
                    return
                }
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech matched"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    else -> "Error code: $error"
                }
                Log.w(TAG, "onError id=$currentSessionId: $msg")
                lastSentCommand = null
                if (error in listOf(
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        11,
                    )) {
                    restartHandler.removeCallbacks(restartRunnable)
                    restartHandler.post(restartRunnable)
                } else {
                    isListening = false
                    _state.value = VoiceState.Error(msg)
                }
            }
        })

        recognizer.startListening(intent)
    }

    private val restartRunnable = Runnable {
        Log.d(TAG, "restartRunnable firing")
        restartPending = false
        if (isListening) {
            isListening = false
            startListening()
        }
    }

    override fun stopListening() {
        isListening = false
        lastSentCommand = null
        restartPending = false
        sessionId++
        speechRecognizer?.apply {
            stopListening()
            destroy()
        }
        speechRecognizer = null
        _state.value = VoiceState.Idle
    }
}

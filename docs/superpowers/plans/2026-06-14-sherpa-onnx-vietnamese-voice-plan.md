# Sherpa-onnx Vietnamese Voice Recognition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Vosk with Sherpa-onnx using the Vietnamese Zipformer-30M transducer model for offline voice command recognition ("đi tới" / "dừng lại") on the ohstem ESP32 robot controller Android app.

**Architecture:** Sherpa-onnx AAR via JitPack provides `OfflineRecognizer` with `OfflineTransducerModelConfig`. Model files live in `assets/`. New `SherpaOnnxVoiceManager` class replaces `VoskVoiceManager`, implementing the same `VoiceManager` interface. Audio capture uses AudioRecord at 16kHz with energy-based VAD; recognized text is matched against grammar words.

**Tech Stack:** Kotlin, Android SDK 26+, Sherpa-onnx v1.13.0 (JitPack), Zipformer-vi-30M-int8 transducer model, Coroutines, Hilt DI

---

### Task 1: Add Sherpa-onnx dependency and JitPack repository

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add JitPack repository to `settings.gradle.kts`**

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://alphacephei.com/maven/") }
        maven { url = uri("https://jitpack.io") }
    }
}
```

- [ ] **Step 2: Add sherpa-onnx version to `gradle/libs.versions.toml`**

Add `sherpa-onnx = "1.13.0"` under `[versions]`.

Remove `vosk = "0.3.47"` from `[versions]`.

- [ ] **Step 3: Add sherpa-onnx library entry to `gradle/libs.versions.toml`**

```toml
sherpa-onnx = { group = "com.github.k2-fsa", name = "sherpa-onnx", version.ref = "sherpa-onnx" }
```

Remove `vosk-android = { group = "com.alphacephei", name = "vosk-android", version.ref = "vosk" }`.

- [ ] **Step 4: Update `app/build.gradle.kts`**

Add `implementation(libs.sherpa-onnx)` and remove `implementation(libs.vosk.android)`.

- [ ] **Step 5: Verify Gradle sync**

Run: `./gradlew app:dependencies --configuration debugRuntimeClasspath | grep sherpa`

Expected: `com.github.k2-fsa:sherpa-onnx:*` appears.

---

### Task 2: Download and bundle Sherpa-onnx Vietnamese model; remove Vosk models

**Files:**
- Download: `app/src/main/assets/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09/`
- Delete: `app/src/main/assets/vosk-model-vn/`
- Delete: `app/src/main/assets/vosk-model-small-vn/`

- [ ] **Step 1: Download the model tarball**

```bash
cd /tmp
wget https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09.tar.bz2
tar xvf sherpa-onnx-zipformer-vi-30M-int8-2026-02-09.tar.bz2
```

- [ ] **Step 2: Copy model to assets**

```bash
cp -r /tmp/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09 \
  /home/bapi/Study/ohstem-controller/app/src/main/assets/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09
```

Verify:

```bash
ls -lh /home/bapi/Study/ohstem-controller/app/src/main/assets/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09/
```

Expected files: `encoder.int8.onnx`, `decoder.onnx`, `joiner.int8.onnx`, `tokens.txt`, `bpe.model`. Total ~34MB.

- [ ] **Step 3: Remove Vosk model directories**

```bash
rm -rf /home/bapi/Study/ohstem-controller/app/src/main/assets/vosk-model-vn
rm -rf /home/bapi/Study/ohstem-controller/app/src/main/assets/vosk-model-small-vn
```

- [ ] **Step 4: Verify final assets**

```bash
ls /home/bapi/Study/ohstem-controller/app/src/main/assets/
```

Expected: `sherpa-onnx-zipformer-vi-30M-int8-2026-02-09/` (no `vosk-model-*` directories).

---

### Task 3: Create SherpaOnnxVoiceManager

**Files:**
- Create: `app/src/main/java/com/ohstem/robot_controller/voice/SherpaOnnxVoiceManager.kt`

- [ ] **Step 1: Create the file with class skeleton**

```kotlin
package com.ohstem.robot_controller.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.k2fsa.sherpa.onnx.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

@Singleton
class SherpaOnnxVoiceManager @Inject constructor(
    private val context: Context,
) : VoiceManager {

    override var mode = VoiceRecognitionMode.OFFLINE

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    @Volatile
    private var isListening = false
    private var recognizer: OfflineRecognizer? = null
    private var recognitionJob: Job? = null
    private var grammarWords: List<String> = emptyList()

    private val sampleRate = 16000

    // ... remaining implementation
}
```

- [ ] **Step 2: Implement `initModel()`**

```kotlin
override fun initModel() {
    _state.value = VoiceState.Initializing
    try {
        val modelDir = "sherpa-onnx-zipformer-vi-30M-int8-2026-02-09"
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
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
        recognizer = OfflineRecognizer(context.assets, config)
        _state.value = VoiceState.Idle
    } catch (e: Exception) {
        _state.value = VoiceState.Error("Model init failed: ${e.message}")
    }
}
```

- [ ] **Step 3: Implement `startListening()` and `stopListening()`**

```kotlin
override fun startListening() {
    if (isListening) return
    isListening = true
    _state.value = VoiceState.Listening
    recognitionJob = CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
        runRecognitionLoop()
    }
}

override fun stopListening() {
    isListening = false
    recognitionJob?.cancel()
    recognitionJob = null
    _state.value = VoiceState.Idle
}
```

- [ ] **Step 4: Implement `setGrammar()`**

```kotlin
override fun setGrammar(words: List<String>) {
    grammarWords = words
}
```

- [ ] **Step 5: Implement the audio capture loop with energy-based VAD**

```kotlin
private fun runRecognitionLoop() {
    val bufferSize = AudioRecord.getMinBufferSize(
        sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
    ) * 4

    val audioRecord = AudioRecord(
        MediaRecorder.AudioSource.MIC,
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize,
    )

    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
        _state.value = VoiceState.Error("AudioRecord init failed")
        return
    }

    audioRecord.startRecording()

    val shortBuffer = ShortArray(bufferSize / 2)
    val ringBuffer = ShortArray(sampleRate * 5) // 5 second ring buffer
    var ringWritePos = 0

    var isSpeaking = false
    var speechStartPos = 0
    var silenceFrames = 0
    var noiseFloor = 100.0
    val frameSize = sampleRate / 33 // ~30ms frames
    val speechThresholdMultiplier = 2.0
    val silenceThresholdMultiplier = 1.5
    val silenceFrameLimit = 15 // ~500ms of silence
    val minSpeechFrames = 6 // ~200ms min speech

    try {
        while (isListening) {
            val bytesRead = audioRecord.read(shortBuffer, 0, shortBuffer.size)
            if (bytesRead <= 0) continue

            // Copy to ring buffer
            for (i in 0 until bytesRead) {
                ringBuffer[ringWritePos] = shortBuffer[i]
                ringWritePos = (ringWritePos + 1) % ringBuffer.size
            }

            // Process frames for VAD
            var offset = 0
            while (offset + frameSize <= bytesRead) {
                var sumSq = 0.0
                for (i in 0 until frameSize) {
                    val s = shortBuffer[offset + i].toDouble()
                    sumSq += s * s
                }
                val rms = sqrt(sumSq / frameSize)

                if (!isSpeaking) {
                    // Update noise floor during silence
                    if (rms < noiseFloor * 2.0) {
                        noiseFloor = noiseFloor * 0.95 + rms * 0.05
                    }

                    if (rms > noiseFloor * speechThresholdMultiplier) {
                        // Speech detected
                        isSpeaking = true
                        speechStartPos = (ringWritePos - (bytesRead - offset) + ringBuffer.size) % ringBuffer.size
                        silenceFrames = 0
                    }
                } else {
                    if (rms < noiseFloor * silenceThresholdMultiplier) {
                        silenceFrames++
                        if (silenceFrames >= silenceFrameLimit) {
                            // End of utterance
                            val speechEndPos = (ringWritePos - (bytesRead - offset) + ringBuffer.size) % ringBuffer.size
                            processSpeechSegment(ringBuffer, speechStartPos, speechEndPos)
                            isSpeaking = false
                            silenceFrames = 0
                        }
                    } else {
                        silenceFrames = 0
                    }
                }

                offset += frameSize
            }
        }
    } finally {
        audioRecord.stop()
        audioRecord.release()
    }
}
```

- [ ] **Step 6: Implement `processSpeechSegment()` that extracts audio and runs recognition**

```kotlin
private fun processSpeechSegment(
    ringBuffer: ShortArray,
    startPos: Int,
    endPos: Int,
) {
    val recognizer = this.recognizer ?: return

    // Extract audio segment from ring buffer
    val segmentLen: Int
    val segment: ShortArray
    if (endPos > startPos) {
        segmentLen = endPos - startPos
        segment = ringBuffer.copyOfRange(startPos, endPos)
    } else {
        segmentLen = ringBuffer.size - startPos + endPos
        segment = ShortArray(segmentLen)
        val firstPart = ringBuffer.size - startPos
        System.arraycopy(ringBuffer, startPos, segment, 0, firstPart)
        System.arraycopy(ringBuffer, 0, segment, firstPart, endPos)
    }

    if (segmentLen < sampleRate / 5) return // minimum 200ms

    // Convert to float array
    val floatSamples = FloatArray(segmentLen) { segment[it].toFloat() / 32768f }

    _state.value = VoiceState.Partial("recognizing...")

    try {
        recognizer.createStream().use { stream ->
            stream.acceptWaveform(floatSamples, sampleRate)
            recognizer.decode(stream)
            val result = recognizer.getResult(stream)
            val text = result.text.trim()

            if (text.isNotBlank()) {
                _state.value = VoiceState.Partial(text)
                matchAndEmitResult(text)
            }
        }
    } catch (e: Exception) {
        _state.value = VoiceState.Error("Recognition error: ${e.message}")
    }
}
```

- [ ] **Step 7: Implement `matchAndEmitResult()`**

```kotlin
private fun matchAndEmitResult(text: String) {
    val normalized = java.text.Normalizer.normalize(text.uppercase(), java.text.Normalizer.Form.NFC)
    for (word in grammarWords) {
        val normalizedWord = java.text.Normalizer.normalize(word.uppercase(), java.text.Normalizer.Form.NFC)
        if (normalized.contains(normalizedWord)) {
            _state.value = VoiceState.Result(word)
            return
        }
    }
}
```

- [ ] **Step 8: Implement `destroy()` cleanup (optional, for future use)**

```kotlin
fun destroy() {
    stopListening()
    recognizer?.release()
    recognizer = null
}
```

---

### Task 4: Update VoiceModule and HybridVoiceManager; delete VoskVoiceManager

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/di/VoiceModule.kt`
- Modify: `app/src/main/java/com/ohstem/robot_controller/voice/HybridVoiceManager.kt`
- Delete: `app/src/main/java/com/ohstem/robot_controller/voice/VoskVoiceManager.kt`

- [ ] **Step 1: Replace `VoskVoiceManager` with `SherpaOnnxVoiceManager` in `VoiceModule.kt`**

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object VoiceModule {

    @Provides
    @Singleton
    fun provideSherpaOnnxVoiceManager(
        context: Context,
    ): SherpaOnnxVoiceManager {
        return SherpaOnnxVoiceManager(context)
    }

    @Provides
    @Singleton
    fun provideOnlineVoiceManager(
        context: Context,
    ): OnlineVoiceManager {
        return OnlineVoiceManager(context)
    }

    @Provides
    @Singleton
    fun provideVoiceManager(
        sherpaOnnxManager: SherpaOnnxVoiceManager,
        onlineManager: OnlineVoiceManager,
    ): VoiceManager {
        return HybridVoiceManager(sherpaOnnxManager, onlineManager)
    }
}
```

- [ ] **Step 2: Update `HybridVoiceManager.kt` constructor and references**

```kotlin
@Singleton
class HybridVoiceManager @Inject constructor(
    private val sherpaOnnxManager: SherpaOnnxVoiceManager,
    private val onlineManager: OnlineVoiceManager,
) : VoiceManager {

    override var mode = VoiceRecognitionMode.OFFLINE

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    override val state: StateFlow<VoiceState> = _state

    private var stateJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val activeManager: VoiceManager
        get() = if (mode == VoiceRecognitionMode.OFFLINE) sherpaOnnxManager else onlineManager

    override fun startListening() {
        stopListening()
        stateJob = scope.launch {
            activeManager.state.collect { s ->
                _state.value = s
            }
        }
        activeManager.startListening()
    }

    override fun stopListening() {
        stateJob?.cancel()
        stateJob = null
        sherpaOnnxManager.stopListening()
        onlineManager.stopListening()
        _state.value = VoiceState.Idle
    }

    override fun initModel() {
        sherpaOnnxManager.initModel()
    }

    override fun setGrammar(words: List<String>) {
        sherpaOnnxManager.setGrammar(words)
        onlineManager.setGrammar(words)
    }
}
```

- [ ] **Step 3: Delete `VoskVoiceManager.kt`**

```bash
rm /home/bapi/Study/ohstem-controller/app/src/main/java/com/ohstem/robot_controller/voice/VoskVoiceManager.kt
```

---

### Task 5: Build, install, and test

- [ ] **Step 1: Build the debug APK**

```bash
cd /home/bapi/Study/ohstem-controller && ./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL (can take 2-5 minutes for first build with new dependency).

- [ ] **Step 2: Install on device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

- [ ] **Step 3: Test offline recognition**

1. Enable airplane mode on device
2. Open the app
3. Tap microphone button (should use offline mode by default)
4. Say "đi tới" → expect BLE write `\x15U=1`
5. Say "dừng lại" → expect BLE write `\x15U=0`
6. Toggle to online mode → should still work (Android SpeechRecognizer)

- [ ] **Step 4: Monitor logs**

```bash
adb logcat -s SherpaOnnxVoiceManager,HybridVoiceManager,OhStemController:* | grep -E "(đi tới|dừng lại|Result|Error|BLE)"
```

Expected: log lines showing recognition results and BLE writes.

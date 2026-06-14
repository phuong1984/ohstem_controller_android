# Sherpa-onnx Vietnamese Voice Recognition

## Purpose

Replace Vosk with Sherpa-onnx for offline Vietnamese voice recognition on the ohstem ESP32 robot controller Android app. Uses the community-trained Vietnamese Zipformer transducer model for accurate keyword detection of "đi tới" (go) and "dừng lại" (stop), which map to BLE commands.

## Model

**Model:** `sherpa-onnx-zipformer-vi-30M-int8-2026-02-09`
- Creator: hynt/Zipformer-30M-RNNT-6000h, packaged by csukuangfj2
- Size: ~34MB total (encoder 26MB, decoder 4.9MB, joiner 1MB, tokens 23KB, bpe 262KB)
- Architecture: Zipformer Transducer with INT8 quantization
- Training: ~6000 hours of high-quality Vietnamese speech
- Input: 16kHz mono 16-bit PCM (no resampling needed)
- License: Apache-2.0

Files bundled in `app/src/main/assets/sherpa-onnx-zipformer-vi-30M-int8-2026-02-09/`:
- `encoder.int8.onnx`
- `decoder.onnx`
- `joiner.int8.onnx`
- `tokens.txt`
- `bpe.model`

## Dependencies

- **Add:** `com.github.k2-fsa:sherpa-onnx:v1.13.0` via JitPack (`https://jitpack.io`) in `settings.gradle.kts`
- **Remove:** `com.alphacephei:vosk-android:0.3.47` from version catalog and build file
- Update `gradle/libs.versions.toml`: remove `vosk` version, remove `vosk-android` library entry
- Update `app/build.gradle.kts`: remove `implementation(libs.vosk.android)` line

## New Class: SherpaOnnxVoiceManager

Location: `app/src/main/java/com/ohstem/robot_controller/voice/SherpaOnnxVoiceManager.kt`

Implements `VoiceManager` interface. Architecture:

### Model Initialization (`initModel`)
- Create `OfflineRecognizer` using Sherpa-onnx Kotlin API
- Configure `OfflineTransducerModelConfig` with paths to model files
- Load model files from Android `assets` directory via `AssetManager` (Sherpa-onnx supports this natively)
- Set `num_threads = 2`, `provider = "cpu"`, `decodingMethod = "greedy_search"`

### Audio Capture Loop
- `AudioRecord` at 16000Hz, 16-bit, mono, channel = `CHANNEL_IN_MONO`, encoding = `ENCODING_PCM_16BIT`
- Buffer size: `AudioRecord.getMinBufferSize(16000, CHANNEL_IN_MONO, ENCODING_PCM_16BIT) * 4`
- Audio captured in a background coroutine on `Dispatchers.IO`
- Ring buffer stores audio samples for the duration of a speech segment (~5s max)

### Voice Activity Detection (VAD)
- **Simple energy-based VAD** (no Silero VAD initially):
  - Compute RMS of each 30ms frame
  - `SPEECH_THRESHOLD`: RMS > `noiseFloor * 2.0`
  - `SILENCE_THRESHOLD`: RMS < `noiseFloor * 1.5`
  - `noiseFloor` adaptively updated during silence periods (exponential moving average)
  - `SPEECH_DURATION_MS` = 200ms minimum to trigger recognition
  - `SILENCE_DURATION_MS` = 500ms of silence ends utterance
- When speech starts → record start position in ring buffer
- When speech ends → extract audio segment

### Recognition
- Extract float audio segment from ring buffer
- Create `OfflineStream` via `recognizer.createStream()`
- `stream.acceptWaveform(16000f, floatArray)`
- `recognizer.decodeStream(stream)`
- Read `stream.result.text`
- Match against grammar words using `contains()` (case-insensitive, NFC-normalized)
- Emit `VoiceState.Partial` and `VoiceState.Result` via `_state` MutableStateFlow

### Grammar Matching (`setGrammar`)
- Store words list (e.g., `["đi tới", "dừng lại"]`)
- After recognition, check if `resultText` contains any grammar word
- If match found → emit `VoiceState.Result` with matched word → BLE write occurs in MappingEngine

### State Management
- `startListening()`: sets `isListening = true`, launches audio capture coroutine
- `stopListening()`: sets `isListening = false`, cancels coroutine, releases AudioRecord
- Backpressure: if recognition takes too long, skip frames rather than queue
- Thread safety: `isListening` is `volatile`, state updates via `MutableStateFlow`

## Integration Changes

### AppModule / VoiceModule
- Rename `provideVoskVoiceManager()` → `provideSherpaOnnxVoiceManager()`
- Replace `VoskVoiceManager` binding with `SherpaOnnxVoiceManager`
- Keep `OnlineVoiceManager` and `HybridVoiceManager` unchanged

### HybridVoiceManager
- Change constructor param `voskManager: VoskVoiceManager` → `sherpaOnnxManager: SherpaOnnxVoiceManager`
- Update `activeManager` getter and `initModel()`/`setGrammar()` to reference `sherpaOnnxManager` instead of `voskManager`
- `stopListening()`: also stop `sherpaOnnxManager` instead of `voskManager`
- `setGrammar()`: delegates to both managers as before

### Files to Delete
- `app/src/main/java/com/ohstem/robot_controller/voice/VoskVoiceManager.kt`

## No Changes To

- `VoiceManager.kt` interface — stays the same
- `OnlineVoiceManager.kt` — stays the same
- `VoiceViewModel.kt` — no changes needed
- `VoiceScreen.kt` — no changes needed
- `MappingEngine.kt` — handles BLE writes from VoiceState.Result, no changes

## Testing

- Build APK with `./gradlew assembleDebug`
- Install on Samsung Galaxy SM-M346B1
- Test "đi tới" → expects `\x15U=1` BLE write
- Test "dừng lại" → expects `\x15U=0` BLE write
- Test offline: airplane mode + verify recognition works
- Compare accuracy vs previous Vosk implementation

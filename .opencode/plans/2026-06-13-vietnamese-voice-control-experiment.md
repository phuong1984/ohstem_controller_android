# Vietnamese Voice Control Experiment

## Overview
Implement real-time Vietnamese speech recognition using Vosk to send BLE commands:
- "Đi tới" → `\x15U=1` (same as UP D-Pad pressed)
- "Dừng lại" → `\x15U=0` (same as UP D-Pad released)

## Architecture

```
Mic → AudioRecord → Vosk Recognizer → VoskVoiceManager → VoiceViewModel → MappingEngine → BleManager → ESP32
```

## Files to Modify

### 1. Asset: Vosk Vietnamese Model
- Download `vosk-model-small-vn-0.4.zip` from https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip
- Extract to `app/src/main/assets/vosk-model-small-vn/`
- Vosk loads directly from assets via `Model(context, "vosk-model-small-vn")`

### 2. `VoskVoiceManager.kt` — Full Implementation
- **initModel()**: Load Vosk `Model` from assets directory, create `Recognizer(model, 16000.0f)`
- **startListening()**: 
  - Create `AudioRecord` with 16kHz, 16-bit, mono
  - Launch coroutine that reads audio chunks and feeds to `recognizer.acceptWaveform()`
  - Parse `recognizer.partialResult` JSON → emit partial `VoiceState.Result` for real-time display
  - Parse `recognizer.result` JSON → emit final `VoiceState.Result` for command execution
- **Continuous mode**: After final result, call `recognizer.reset()` and continue audio capture loop
- **stopListening()**: Cancel coroutine, release AudioRecord, release Recognizer
- Error handling: catch exceptions, emit `VoiceState.Error` for model load failure, audio errors, etc.
- Lifecycle: use `CoroutineScope` managed internally

### 3. `VoiceViewModel.kt` — Seed Voice Bindings + Text Matching
- In `init{}`:
  - Query existing voice bindings in active profile
  - If "đi tới" / "dừng lại" bindings don't exist, seed them:
    - VirtualAction(name="Đi tới", activationCommand="\u0015U=1", type="VOICE", ...)
    - InputBinding(sourceType="VOICE_COMMAND", sourceCode="đi tới", ...)
    - VirtualAction(name="Dừng lại", activationCommand="\u0015U=0", type="VOICE", ...)
    - InputBinding(sourceType="VOICE_COMMAND", sourceCode="dừng lại", ...)
- Keep existing `state.collectLatest` that forwards `VoiceState.Result` to `mappingEngine.handleInput("VOICE_COMMAND", text)`

### 4. `VoiceScreen.kt` — Continuous Listening UI
- Auto-start listening on screen entry via `LaunchedEffect(Unit)`
- Show real-time partial recognition text
- Show last final recognized command and sent BLE command
- Toggle button to stop/start listening

### 5. `VoiceModule.kt` — No changes needed
- Already provides `VoskVoiceManager` as `VoiceManager`

### 6. `AndroidManifest.xml` — No changes needed
- `RECORD_AUDIO` permission already declared

### 7. `app/build.gradle.kts` — No changes needed
- `libs.vosk.android` dependency already declared

## Data Flow

```
User says "Đi tới"
  → AudioRecord captures PCM → Vosk Recognizer.acceptWaveform()
  → recognizer.result returns {"text": "đi tới"}
  → VoskVoiceManager emits VoiceState.Result("đi tới")  
  → VoiceViewModel maps → mappingEngine.handleInput("VOICE_COMMAND", "đi tới", isActivation=true)
  → MappingEngine: InputBinding("VOICE_COMMAND", "đi tới") → VirtualAction("\u0015U=1")
  → bleManager.sendCommand("\u0015U=1")

User says "Dừng lại"
  → Same flow → VirtualAction("\u0015U=0") → bleManager.sendCommand("\u0015U=0")
```

## Error Handling
- **Model load failure**: Emit `VoiceState.Error("Failed to load model")`
- **Audio recording failure**: Emit `VoiceState.Error("Microphone error")`
- **No match found**: Log silently, no error shown to user
- **BLE disconnected**: MappingEngine handles gracefully (no-op if no BLE connection)

## Verification
1. Build: `./gradlew assembleDebug`
2. Run on device with Vietnamese language support
3. Navigate to Voice screen (should auto-start listening)
4. Say "Đi tới" → verify `\x15U=1` is sent via BLE
5. Say "Dừng lại" → verify `\x15U=0` is sent via BLE

# Vietnamese Voice Control Experiment — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement real-time Vietnamese voice recognition using Vosk so saying "Đi tới" sends `\x15U=1` (UP pressed) and "Dừng lại" sends `\x15U=0` (UP released) over BLE.

**Architecture:** Vosk model loaded from Android assets → `AudioRecord` captures mic PCM → `Recognizer.acceptWaveform()` processes audio → emitted `VoiceState.Result` → `MappingEngine` looks up Room DB binding → `BleManager.sendCommand()`. Continuous listening mode with partial result display.

**Tech Stack:** Vosk Android 0.3.47, Kotlin coroutines, Jetpack Compose, Room, BLE Nordic UART

---

### Task 1: Download and Bundle Vosk Vietnamese Model

**Files:**
- Create: `app/src/main/assets/vosk-model-small-vn/` (directory with model files)
- No code changes

- [ ] **Step 1: Download the Vosk Vietnamese model**

```bash
# Using the Vosk Android library which can load models from assets directory
curl -L -o /tmp/vosk-model-small-vn-0.4.zip https://alphacephei.com/vosk/models/vosk-model-small-vn-0.4.zip
```

Expected: zip file downloaded to /tmp

- [ ] **Step 2: Extract model to assets directory**

```bash
mkdir -p app/src/main/assets/
unzip -o /tmp/vosk-model-small-vn-0.4.zip -d /tmp/vosk-extracted/
# Move just the model contents (the zip contains a single top-level directory)
mv /tmp/vosk-extracted/vosk-model-small-vn-0.4 app/src/main/assets/vosk-model-small-vn
# Verify structure
ls app/src/main/assets/vosk-model-small-vn/
# Should show: README, am/, conf/, graph/, ivector/, rescore/, runtime/
```

Expected: model files present under `app/src/main/assets/vosk-model-small-vn/`

- [ ] **Step 3: Commit**

```bash
git add app/src/main/assets/vosk-model-small-vn/
git commit -m "feat: add Vosk Vietnamese speech model for offline voice control"
```

---

### Task 2: Implement VoskVoiceManager with Full Vosk Integration

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/voice/VoiceManager.kt` (add Partial state)
- Modify: `app/src/main/java/com/ohstem/robot_controller/voice/VoskVoiceManager.kt` (full implementation)

- [ ] **Step 1: Add VoiceState.Partial to VoiceManager.kt**

```kotlin
package com.ohstem.robot_controller.voice

import kotlinx.coroutines.flow.StateFlow

sealed class VoiceState {
    object Idle : VoiceState()
    object Initializing : VoiceState()
    object Listening : VoiceState()
    data class Result(val text: String) : VoiceState()
    data class Partial(val text: String) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

interface VoiceManager {
    val state: StateFlow<VoiceState>
    fun startListening()
    fun stopListening()
    fun initModel()
}
```

- [ ] **Step 2: Implement VoskVoiceManager.kt**

Replace the entire file:

```kotlin
package com.ohstem.robot_controller.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import com.alphacephei.vosk.Model
import com.alphacephei.vosk.Recognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
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
    private var listeningJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun initModel() {
        _state.value = VoiceState.Initializing
        try {
            model = Model(context, "vosk-model-small-vn")
            recognizer = Recognizer(model, 16000.0f)
            _state.value = VoiceState.Idle
        } catch (e: Exception) {
            _state.value = VoiceState.Error("Model init failed: ${e.message}")
        }
    }

    override fun startListening() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            _state.value = VoiceState.Error("RECORD_AUDIO permission not granted")
            return
        }

        if (recognizer == null) {
            initModel()
            if (recognizer == null) return
        }

        _state.value = VoiceState.Listening

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(4096)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            _state.value = VoiceState.Error("Failed to initialize AudioRecord")
            audioRecord?.release()
            audioRecord = null
            return
        }

        audioRecord?.startRecording()

        listeningJob = scope.launch {
            val buffer = ByteArray(bufferSize)
            while (isActive) {
                val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                if (bytesRead > 0) {
                    if (recognizer?.acceptWaveform(buffer, bytesRead) == true) {
                        val json = recognizer?.result ?: ""
                        val text = parseVoskText(json)
                        if (text.isNotEmpty()) {
                            _state.value = VoiceState.Result(text)
                        }
                        recognizer?.reset()
                    } else {
                        val json = recognizer?.partialResult ?: ""
                        val partialText = parseVoskPartial(json)
                        if (partialText.isNotEmpty()) {
                            _state.value = VoiceState.Partial(partialText)
                        }
                    }
                } else if (bytesRead < 0) {
                    _state.value = VoiceState.Error("Audio read error: $bytesRead")
                    break
                }
            }
        }
    }

    override fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        _state.value = VoiceState.Idle
    }

    private fun parseVoskText(json: String): String {
        return try {
            val start = json.indexOf("\"text\":\"")
            if (start == -1) return ""
            val afterStart = start + 8
            val end = json.indexOf("\"", afterStart)
            if (end == -1) return ""
            json.substring(afterStart, end).trim()
        } catch (e: Exception) { "" }
    }

    private fun parseVoskPartial(json: String): String {
        return try {
            val start = json.indexOf("\"partial\":\"")
            if (start == -1) return ""
            val afterStart = start + 11
            val end = json.indexOf("\"", afterStart)
            if (end == -1) return ""
            json.substring(afterStart, end).trim()
        } catch (e: Exception) { "" }
    }
}
```

---

### Task 3: Seed Voice Command Bindings in VoiceViewModel

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/viewmodel/VoiceViewModel.kt`

- [ ] **Step 1: Update VoiceViewModel.kt**

Replace the entire file:

```kotlin
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
        // Seed voice command bindings for Vietnamese commands
        viewModelScope.launch {
            val profiles = repository.getProfiles().first()
            val activeProfile = profiles.find { it.isActive }
            if (activeProfile != null) {
                val bindings = repository.getBindings(activeProfile.id).first()
                val hasDiToi = bindings.any { it.sourceType == "VOICE_COMMAND" && it.sourceCode == "đi tới" }
                val hasDungLai = bindings.any { it.sourceType == "VOICE_COMMAND" && it.sourceCode == "dừng lại" }

                if (!hasDiToi) {
                    val actionId = repository.saveAction(VirtualAction(
                        name = "Đi tới",
                        activationCommand = "\u0015U=1",
                        deactivationCommand = null,
                        type = "VOICE",
                        profileId = activeProfile.id
                    ))
                    repository.saveBinding(InputBinding(
                        sourceType = "VOICE_COMMAND",
                        sourceCode = "đi tới",
                        virtualActionId = actionId,
                        profileId = activeProfile.id
                    ))
                }

                if (!hasDungLai) {
                    val actionId = repository.saveAction(VirtualAction(
                        name = "Dừng lại",
                        activationCommand = "\u0015U=0",
                        deactivationCommand = null,
                        type = "VOICE",
                        profileId = activeProfile.id
                    ))
                    repository.saveBinding(InputBinding(
                        sourceType = "VOICE_COMMAND",
                        sourceCode = "dừng lại",
                        virtualActionId = actionId,
                        profileId = activeProfile.id
                    ))
                }
            }

            voiceManager.initModel()
        }

        // Forward recognized text to MappingEngine
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
```

---

### Task 4: Update VoiceScreen for Continuous Listening

**Files:**
- Modify: `app/src/main/java/com/ohstem/robot_controller/ui/screens/voice/VoiceScreen.kt`

- [ ] **Step 1: Update VoiceScreen.kt**

Replace the entire file:

```kotlin
package com.ohstem.robot_controller.ui.screens.voice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ohstem.robot_controller.viewmodel.VoiceViewModel
import com.ohstem.robot_controller.voice.VoiceState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var lastCommand by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    LaunchedEffect(state) {
        if (state is VoiceState.Result) {
            lastCommand = (state as VoiceState.Result).text
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Voice Control") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                is VoiceState.Idle -> {
                    Text(
                        "Ready",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                is VoiceState.Initializing -> {
                    Text(
                        "Initializing model...",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
                is VoiceState.Listening -> {
                    Text(
                        "Listening...",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is VoiceState.Partial -> {
                    Text(
                        "Đang nghe: ${(state as VoiceState.Partial).text}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                is VoiceState.Result -> {
                    Text(
                        "Recognized: ${(state as VoiceState.Result).text}",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is VoiceState.Error -> {
                    Text(
                        "Error: ${(state as VoiceState.Error).message}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (lastCommand.isNotEmpty()) {
                Text(
                    "Last command sent: $lastCommand",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            Button(
                onClick = {
                    if (state is VoiceState.Listening) viewModel.stopListening()
                    else viewModel.startListening()
                },
                modifier = Modifier.size(120.dp),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state is VoiceState.Listening)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    if (state is VoiceState.Listening) "Stop"
                    else if (state is VoiceState.Error) "Retry"
                    else "Start",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
```

---

### Task 5: Verify Build

- [ ] **Step 1: Build the project**

```bash
./gradlew assembleDebug
```

Expected: BUILD SUCCESSFUL — no compilation errors

- [ ] **Step 2: Commit all changes**

```bash
git add app/src/main/java/com/ohstem/robot_controller/voice/VoiceManager.kt
git add app/src/main/java/com/ohstem/robot_controller/voice/VoskVoiceManager.kt
git add app/src/main/java/com/ohstem/robot_controller/viewmodel/VoiceViewModel.kt
git add app/src/main/java/com/ohstem/robot_controller/ui/screens/voice/VoiceScreen.kt
git commit -m "feat: implement Vietnamese voice control with Vosk"
```

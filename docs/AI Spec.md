# AI-Spec.md

# Artificial Intelligence Specification

Project: STEM Controller

Version: 1.0

Status: Draft

Last Updated: 2026-06-10

---

# 1. Overview

## Purpose

Tài liệu này mô tả toàn bộ hệ thống AI chạy Offline trên Android.

AI System bao gồm:

* Vietnamese Speech Recognition
* Hand Detection
* Hand Tracking
* Gesture Recognition
* Command Classification

---

# 2. Design Goals

## AG-001

Offline First

Không phụ thuộc Internet.

---

## AG-002

Low Latency

Phản hồi gần thời gian thực.

---

## AG-003

Low Resource Usage

Hoạt động trên điện thoại Android tầm trung.

---

## AG-004

Student Friendly

Dễ sử dụng cho học sinh.

---

## AG-005

Extensible

Có thể bổ sung thêm mô hình AI trong tương lai.

---

# 3. AI Architecture

```text
Speech
      │
      ▼
Speech Recognition
      │
      ▼
Command Mapping
      │
      ▼
Input Event

--------------------------------

Camera
      │
      ▼
Hand Detection
      │
      ▼
Gesture Recognition
      │
      ▼
Input Event

--------------------------------

Input Event
      │
      ▼
Mapping Engine
      │
      ▼
Robot Action
```

---

# 4. AI Modules

## Module A

Speech Recognition

---

## Module B

Gesture Recognition

---

## Module C

Command Classification

---

## Module D

Confidence Filtering

---

## Module E

AI Configuration

---

# 5. Speech Recognition System

## Requirements

Language

Vietnamese

---

Mode

Offline

---

Continuous Recognition

Supported

---

Push To Talk

Supported

---

# 6. Selected Framework

## Primary Choice

Software

Vosk

Reason

* Offline
* Stable
* Android Support
* Vietnamese Models Available

---

## Secondary Choice

Whisper.cpp

Reason

* Higher Accuracy
* Better Noise Tolerance

Disadvantages

* Large Model
* High RAM Usage

---

# 7. Speech Recognition Architecture

```text
Microphone

↓

Audio Buffer

↓

Vosk Engine

↓

Recognized Text

↓

Command Parser

↓

Input Event
```

---

# 8. Audio Configuration

Sample Rate

```text
16000 Hz
```

---

Channels

```text
Mono
```

---

Bit Depth

```text
16-bit
```

---

Frame Size

```text
20 ms
```

---

# 9. Voice Command Pipeline

Example

```text
User Speech

↓

"Đi tới"

↓

Recognized Text

↓

Command Mapping

↓

UP_PRESS

↓

Action

↓

MOVE_FORWARD

↓

BLE Command
```

---

# 10. Voice Commands

Default Command Set

---

Forward

```text
Đi tới

Tiến lên

Tiến
```

---

Backward

```text
Lùi lại

Đi lùi

Lùi
```

---

Left

```text
Rẽ trái

Quay trái
```

---

Right

```text
Rẽ phải

Quay phải
```

---

Stop

```text
Dừng

Dừng lại
```

---

# 11. Voice Command Configuration

Users may define custom commands.

Example

```text
Turbo

↓

SPEED_MAX
```

---

Stored In Database

```json
{
  "phrase":"Turbo",
  "event":"SPEED_MAX"
}
```

---

# 12. Confidence Threshold

Default

```text
0.70
```

---

Minimum

```text
0.50
```

---

Maximum

```text
0.95
```

---

# 13. Speech Error Handling

Low Confidence

Ignore

---

Unknown Command

Show Warning

---

Microphone Error

Show Error

---

# 14. Gesture Recognition System

## Requirements

Offline

Required

---

Real Time

Required

---

Camera Based

Required

---

# 15. Selected Framework

Primary Choice

MediaPipe

Reason

* Mature
* Offline
* Excellent Hand Tracking
* Android Optimized

---

# 16. Gesture Pipeline

```text
Camera

↓

Frame

↓

Hand Detection

↓

Landmark Extraction

↓

Gesture Classification

↓

Event Mapping

↓

Input Event
```

---

# 17. Hand Detection

Maximum Hands

```text
2
```

---

Detection Confidence

```text
0.5
```

---

Tracking Confidence

```text
0.5
```

---

# 18. Landmark System

MediaPipe provides:

```text
21 Hand Landmarks
```

---

Examples

```text
Thumb Tip

Index Tip

Middle Tip

Ring Tip

Pinky Tip
```

---

# 19. Gesture Types

MVP Supported

---

OPEN_PALM

```text
✋
```

---

CLOSED_FIST

```text
✊
```

---

POINT_UP

```text
☝
```

---

THUMB_UP

```text
👍
```

---

PEACE

```text
✌
```

---

# 20. Dynamic Gestures

Version 2.0

---

SWIPE_LEFT

---

SWIPE_RIGHT

---

SWIPE_UP

---

SWIPE_DOWN

---

WAVE

---

CIRCLE

---

# 21. Gesture Classification

Rule Based

MVP

---

Example

```text
Open Palm

↓

All fingers extended

↓

OPEN_PALM
```

---

Future

ML Classifier

---

# 22. Gesture Mapping

Example

```json
{
  "gesture":"OPEN_PALM",
  "event":"STOP"
}
```

---

Example

```json
{
  "gesture":"THUMB_UP",
  "event":"MOVE_FORWARD"
}
```

---

# 23. Camera Configuration

Resolution

```text
640x480
```

---

FPS

```text
30
```

---

Preferred Camera

Front Camera

---

# 24. AI Command Classification

Purpose

Normalize inputs.

---

Input

```text
Đi tới
```

↓

```text
VOICE_FORWARD
```

↓

```text
MOVE_FORWARD
```

---

Input

```text
THUMB_UP
```

↓

```text
MOVE_FORWARD
```

---

# 25. Unified Event Architecture

All AI outputs become InputEvent.

```text
Speech

↓

InputEvent

-----------------

Gesture

↓

InputEvent
```

---

Example

```text
VOICE_FORWARD

↓

MOVE_FORWARD
```

---

Example

```text
GESTURE_THUMB_UP

↓

MOVE_FORWARD
```

---

# 26. AI Settings

Voice Sensitivity

Range

```text
0.5 - 0.95
```

---

Gesture Confidence

Range

```text
0.5 - 0.95
```

---

Voice Timeout

Range

```text
1 - 10 sec
```

---

# 27. AI State Machine

Speech States

```text
Idle

Listening

Processing

Recognized

Error
```

---

Gesture States

```text
NoHand

Tracking

Recognized

Error
```

---

# 28. Performance Targets

Speech Recognition

Latency

```text
< 500 ms
```

---

Gesture Recognition

```text
20+ FPS
```

---

Hand Detection

```text
< 50 ms/frame
```

---

# 29. Resource Budget

Speech Engine

RAM

```text
50-150 MB
```

---

Gesture Engine

RAM

```text
50-100 MB
```

---

Total AI Budget

```text
< 300 MB
```

---

# 30. APK Size Budget

Core App

```text
20 MB
```

---

Vosk Model

```text
50-80 MB
```

---

MediaPipe

```text
10-20 MB
```

---

Target APK

```text
< 150 MB
```

---

# 31. Privacy Requirements

No cloud inference.

No audio upload.

No image upload.

No biometric storage.

No user tracking.

---

# 32. Failure Recovery

Speech Failure

Restart Recognition Engine

---

Camera Failure

Restart Camera Pipeline

---

Model Failure

Fallback To Manual Controller

---

# 33. Testing Requirements

Speech Recognition Accuracy

Target

```text
> 90%
```

---

Gesture Recognition Accuracy

Target

```text
> 90%
```

---

False Positive Rate

```text
< 5%
```

---

# 34. Future AI Features

Voice Training

Custom Wake Word

---

Custom Gesture Learning

---

Object Detection

---

Face Tracking

---

Robot Following

---

AI Assistant

---

Natural Language Commands

Example

```text
Đi tới rồi rẽ trái
```

↓

```text
MOVE_FORWARD

TURN_LEFT
```

---

# 35. Architecture Summary

Recommended Stack

```text
Speech Recognition

Vosk

---------------------

Gesture Recognition

MediaPipe

---------------------

Command Layer

Rule Based Mapping

---------------------

Execution

Mapping Engine

↓

BLE

↓

ESP32
```

MVP sử dụng:

* Vosk
* MediaPipe
* Rule-Based Command Mapping

để đạt độ ổn định cao, dễ triển khai và hoạt động tốt trên các thiết bị Android phổ thông dùng trong giáo dục STEM.

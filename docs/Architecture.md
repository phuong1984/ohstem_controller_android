# Architecture.md

# STEM Controller System Architecture

Version: 1.0

Status: Draft

Last Updated: 2026-06-10

---

# 1. Architecture Goals

## Primary Goals

* Maintainable
* Scalable
* Testable
* Offline First
* AI Friendly Codebase

---

## Secondary Goals

* Multi Robot Support
* Future Cloud Integration
* Future Blockly Integration
* Future Python Programming Support

---

# 2. Architectural Principles

## AP-001

Single Responsibility Principle

Mỗi module chỉ có một trách nhiệm.

---

## AP-002

Dependency Inversion

UI không phụ thuộc implementation.

---

## AP-003

Event Driven Architecture

Toàn bộ hệ thống hoạt động dựa trên Event.

---

## AP-004

Offline First

Mọi tính năng cốt lõi phải hoạt động không cần Internet.

---

## AP-005

Robot Agnostic

Không phụ thuộc loại robot cụ thể.

---

# 3. High Level Architecture

```text
┌───────────────────────────────┐
│          Android App          │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│            UI Layer           │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│         ViewModel Layer       │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│        Domain Layer           │
│                               │
│ Command Engine                │
│ Mapping Engine                │
│ Profile Engine                │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│         Data Layer            │
│                               │
│ BLE Repository                │
│ Settings Repository           │
│ Profile Repository            │
└──────────────┬────────────────┘
               │
               ▼
┌───────────────────────────────┐
│       Infrastructure          │
│                               │
│ BLE                           │
│ Room Database                 │
│ Voice Engine                  │
│ Gesture Engine                │
└───────────────────────────────┘
```

---

# 4. Clean Architecture

Project sử dụng:

```text
Presentation
Domain
Data
Infrastructure
```

---

# 5. Package Structure

```text
com.stemcontroller

├── app

├── core
│   ├── ble
│   ├── database
│   ├── voice
│   ├── gesture
│   ├── preferences
│   └── logging

├── domain
│   ├── model
│   ├── repository
│   ├── usecase
│   └── engine

├── data
│   ├── repository
│   ├── datasource
│   └── mapper

├── feature

│   ├── connection
│   ├── controller
│   ├── voice
│   ├── gesture
│   ├── mapping
│   ├── profile
│   └── settings

└── di
```

---

# 6. Module Architecture

## Module 1

BLE Module

Responsibilities:

* Scan
* Connect
* Disconnect
* Send
* Receive

No UI Logic.

---

## Module 2

Virtual Controller Module

Responsibilities:

* PS4 Layout
* Touch Detection
* Joystick Tracking
* Event Generation

Output:

ControllerEvent

---

## Module 3

Voice Module

Responsibilities:

* Audio Capture
* Speech Recognition
* Voice Command Detection

Output:

VoiceEvent

---

## Module 4

Gesture Module

Responsibilities:

* Camera Feed
* Hand Tracking
* Gesture Classification

Output:

GestureEvent

---

## Module 5

Mapping Engine

Responsibilities:

Convert:

```text
Input
↓
Event
↓
Action
↓
BLE Command
```

---

## Module 6

Profile Module

Responsibilities:

* Create Profile
* Save Profile
* Load Profile
* Delete Profile

---

# 7. Command Pipeline

Đây là luồng quan trọng nhất.

```text
PS4 Button
Voice Command
Gesture

      ↓

Unified Event

      ↓

Action Engine

      ↓

Robot Action

      ↓

BLE Command

      ↓

ESP32
```

---

# 8. Event System

## Event Types

```kotlin
sealed class InputEvent
```

---

### Controller Event

```text
UP_PRESS
UP_RELEASE

DOWN_PRESS
DOWN_RELEASE

LEFT_PRESS
LEFT_RELEASE

RIGHT_PRESS
RIGHT_RELEASE
```

---

### Button Event

```text
X_PRESS
X_RELEASE

O_PRESS
O_RELEASE

SQUARE_PRESS
SQUARE_RELEASE

TRIANGLE_PRESS
TRIANGLE_RELEASE
```

---

### Voice Event

```text
VOICE_COMMAND
```

---

### Gesture Event

```text
GESTURE_COMMAND
```

---

# 9. Action System

Actions độc lập với nguồn dữ liệu.

Ví dụ:

```text
MOVE_FORWARD

MOVE_BACKWARD

TURN_LEFT

TURN_RIGHT

STOP
```

---

# 10. BLE Layer

## BLE Stack

```text
Application
↓
BLE Repository
↓
Nordic BLE Library
↓
Android BLE Stack
↓
ESP32
```

---

## Communication Pattern

```text
Request

↓

BLE Packet

↓

ESP32

↓

Response
```

---

## Packet Format

JSON Mode

```json
{
  "action":"MOVE_FORWARD"
}
```

---

Compact Mode

```text
MF
```

---

Binary Mode

```text
0x01
```

---

Recommended:

Compact Mode

---

# 11. Voice Architecture

## Engine

Vosk

---

## Pipeline

```text
Microphone

↓

Audio Buffer

↓

Speech Recognition

↓

Text

↓

Command Mapping

↓

Event
```

---

## Command Cache

Frequently used commands cached in memory.

---

# 12. Gesture Architecture

## Engine

MediaPipe

---

## Pipeline

```text
Camera

↓

Hand Detection

↓

Landmark Extraction

↓

Gesture Classification

↓

Event
```

---

# 13. Database Architecture

## Storage

Room Database

---

Tables

```text
profiles

event_mappings

action_mappings

ble_mappings

settings
```

---

# 14. Profile Architecture

Profile chứa:

```text
Voice Mapping

Gesture Mapping

Action Mapping

BLE Mapping
```

---

Ví dụ

Micromouse Profile

Robot Arm Profile

Soccer Robot Profile

---

# 15. Settings Architecture

## Device Settings

```text
Auto Reconnect

MTU

Packet Rate
```

---

## UI Settings

```text
Theme

Sound

Vibration
```

---

## AI Settings

```text
Voice Sensitivity

Gesture Sensitivity
```

---

# 16. Dependency Injection

Framework:

Hilt

---

Example

```text
ViewModel

↓

UseCase

↓

Repository

↓

DataSource
```

---

# 17. State Management

Compose State

*

StateFlow

---

ViewModel owns state.

UI observes state.

---

# 18. Error Handling

## Domain Error

```text
BLE_DISCONNECTED

VOICE_FAILED

GESTURE_FAILED
```

---

## UI Error

```text
Snackbar

Dialog

Status Banner
```

---

# 19. Logging Architecture

Levels

```text
DEBUG

INFO

WARNING

ERROR
```

---

Log Categories

```text
BLE

VOICE

GESTURE

MAPPING

DATABASE
```

---

# 20. Security Architecture

No Cloud Dependency.

No User Tracking.

No Remote Execution.

All AI Processing Local.

---

# 21. Performance Targets

Startup

< 3 sec

---

BLE Latency

< 100 ms

---

Voice Command

< 500 ms

---

Gesture Recognition

> 20 FPS

---

Memory Usage

< 500 MB

---

# 22. Future Extension Points

## Plugin System

Future:

```text
Robot Plugin
```

---

## Blockly Integration

Future:

```text
Blockly

↓

Action

↓

Robot
```

---

## Python Script Engine

Future:

```python
robot.forward()

robot.turn_left()

robot.stop()
```

---

# 23. Architecture Decision Records

ADR-001

Use Jetpack Compose

Reason:

Modern Android UI

---

ADR-002

Use MVVM

Reason:

Maintainability

---

ADR-003

Use Room

Reason:

Offline First

---

ADR-004

Use Vosk

Reason:

Offline Vietnamese Speech

---

ADR-005

Use MediaPipe

Reason:

Best Offline Hand Tracking

---

ADR-006

Use Nordic BLE Library

Reason:

Stable BLE Implementation

---

# 24. MVP Architecture Scope

Included

* BLE
* Virtual PS4
* Mapping Engine
* Profiles
* Local Database

Excluded

* Cloud
* Authentication
* Multi User
* Online AI

---

# 25. Architecture Summary

Core Pipeline

```text
Controller
Voice
Gesture

      ↓

Input Event

      ↓

Mapping Engine

      ↓

Action

      ↓

BLE Packet

      ↓

ESP32
```

Toàn bộ hệ thống xoay quanh Input Event và Mapping Engine, giúp mọi robot có thể tái sử dụng cùng một ứng dụng điều khiển.

# UIUX-Spec.md

# STEM Controller UI/UX Specification

Version: 1.0

Status: Draft

Last Updated: 2026-06-10

---

# 1. Design Goals

## Primary Goals

* Dễ sử dụng cho học sinh
* Điều khiển robot nhanh
* Học STEM trực quan
* Hạn chế thao tác phức tạp

---

## Secondary Goals

* Giao diện hiện đại
* Hỗ trợ tablet
* Tương thích nhiều kích thước màn hình

---

# 2. Design Principles

## DP-001

One Tap Control

Các thao tác điều khiển robot phải thực hiện trong 1 lần chạm.

---

## DP-002

Visual Feedback

Mọi hành động phải có phản hồi ngay lập tức.

---

## DP-003

Large Touch Targets

Kích thước tối thiểu:

48dp x 48dp

---

## DP-004

Consistent Layout

Các màn hình sử dụng cùng hệ thống component.

---

# 3. Navigation Structure

```text
Home
│
├── Device Connection
│
├── Controller
│
├── Voice Control
│
├── Gesture Control
│
├── Mapping
│
├── Profiles
│
└── Settings
```

---

# 4. App Navigation

Navigation Pattern

Bottom Navigation

```text
┌─────────────────────┐
│                     │
│      Content        │
│                     │
├─────────────────────┤
│Home Ctrl AI Map Set│
└─────────────────────┘
```

---

Tabs

```text
Home
Controller
AI
Mapping
Settings
```

---

# 5. Design System

## Typography

### Heading

24sp

Bold

---

### Section Title

20sp

SemiBold

---

### Body

16sp

Regular

---

### Caption

12sp

Regular

---

# 6. Color System

## Connection States

Connected

```text
Green
```

---

Disconnected

```text
Red
```

---

Connecting

```text
Orange
```

---

# Voice States

Idle

Gray

Listening

Blue

Recognized

Green

Error

Red

---

# Gesture States

No Hand

Gray

Hand Detected

Blue

Gesture Recognized

Green

Error

Red

---

# 7. Home Screen

Screen ID

HOME_001

---

Purpose

Điểm truy cập chính.

---

Layout

```text
┌──────────────────────────┐
│ STEM Controller          │
├──────────────────────────┤
│ Robot Status             │
│ ● Connected              │
├──────────────────────────┤
│ [Controller]             │
│ [Voice Control]          │
│ [Gesture Control]        │
│ [Mapping]                │
│ [Profiles]               │
│ [Settings]               │
└──────────────────────────┘
```

---

Actions

* Open Controller
* Open Voice
* Open Gesture
* Open Mapping
* Open Profiles
* Open Settings

---

# 8. Device Connection Screen

Screen ID

BLE_001

---

Purpose

Quản lý kết nối BLE.

---

Layout

```text
┌──────────────────────────┐
│ Devices                  │
├──────────────────────────┤
│ Robot_001                │
│ Robot_002                │
│ Robot_003                │
├──────────────────────────┤
│ [Scan]                   │
└──────────────────────────┘
```

---

States

Scanning

Connecting

Connected

Disconnected

---

# 9. Controller Screen

Screen ID

CTRL_001

---

Purpose

Mô phỏng tay cầm PS4.

---

Landscape Mode Preferred

---

Layout

```text
┌────────────────────────────────┐
│ L1                     R1      │
│                                │
│   ↑                       △    │
│ ← ↓ →                  □  ○    │
│                         ✕       │
│                                │
│   Left Stick   Right Stick     │
│                                │
│ L2                     R2      │
└────────────────────────────────┘
```

---

Supported Controls

### DPad

* Up
* Down
* Left
* Right

---

### Buttons

* X
* O
* Square
* Triangle

---

### Shoulder Buttons

* L1
* L2
* R1
* R2

---

### System Buttons

* PS
* Share
* Options

---

### Analog Sticks

* Left Stick
* Right Stick

---

# 10. Controller Feedback

When Button Pressed

Visual:

```text
Scale 95%
```

---

Optional

Sound

Vibration

---

# 11. Voice Screen

Screen ID

VOICE_001

---

Purpose

Điều khiển robot bằng giọng nói.

---

Layout

```text
┌──────────────────────────┐
│ Voice Control            │
├──────────────────────────┤
│                          │
│         🎤               │
│                          │
│ Start Listening          │
│                          │
├──────────────────────────┤
│ Recognized Text          │
│                          │
│ Đi tới                   │
│                          │
├──────────────────────────┤
│ Event                    │
│ UP_PRESS                 │
└──────────────────────────┘
```

---

States

Idle

Listening

Recognized

Error

---

# 12. Gesture Screen

Screen ID

GESTURE_001

---

Purpose

Điều khiển robot bằng cử chỉ tay.

---

Layout

```text
┌──────────────────────────┐
│ Camera Preview           │
│                          │
│                          │
│        VIDEO             │
│                          │
├──────────────────────────┤
│ Gesture                  │
│ OPEN_PALM                │
├──────────────────────────┤
│ Action                   │
│ STOP                     │
└──────────────────────────┘
```

---

Overlay

### Bounding Box

Hiển thị vị trí bàn tay.

---

### Skeleton

Hiển thị landmarks.

---

# 13. Mapping Screen

Screen ID

MAP_001

---

Purpose

Tùy chỉnh điều khiển.

---

Layout

```text
┌──────────────────────────┐
│ Mappings                 │
├──────────────────────────┤
│ Voice  → UP_PRESS        │
│ Gesture→ STOP            │
│ X      → FIRE            │
├──────────────────────────┤
│ [+ Add Mapping]          │
└──────────────────────────┘
```

---

# 14. Add Mapping Dialog

Step 1

Select Source

```text
Controller
Voice
Gesture
```

---

Step 2

Select Trigger

---

Step 3

Select Event

---

Step 4

Save

---

# 15. Profiles Screen

Screen ID

PROFILE_001

---

Purpose

Quản lý nhiều robot.

---

Layout

```text
┌──────────────────────────┐
│ Profiles                 │
├──────────────────────────┤
│ Micromouse               │
│ Robot Arm                │
│ Soccer Robot             │
├──────────────────────────┤
│ [+ New Profile]          │
└──────────────────────────┘
```

---

Actions

Create

Edit

Delete

Load

Duplicate

---

# 16. Settings Screen

Screen ID

SETTINGS_001

---

Sections

BLE

Audio

Vibration

Voice

Gesture

Theme

---

# 17. BLE Settings

Controls

```text
Auto Reconnect

Packet Rate

MTU Size
```

---

# 18. Voice Settings

Controls

```text
Language

Sensitivity

Recognition Timeout
```

---

# 19. Gesture Settings

Controls

```text
Camera Selection

FPS

Detection Confidence
```

---

# 20. Theme Support

Theme

Light

Dark

System

---

# 21. Empty States

No Robot

```text
No robot connected.
Tap Scan to begin.
```

---

No Profile

```text
Create your first profile.
```

---

No Mapping

```text
Add a mapping to start.
```

---

# 22. Error States

BLE Error

```text
Unable to connect.
```

---

Voice Error

```text
Speech not recognized.
```

---

Gesture Error

```text
Hand not detected.
```

---

# 23. Tablet Layout

Width > 840dp

Use Two Pane Layout

```text
┌──────────┬───────────────┐
│ Menu     │ Content       │
└──────────┴───────────────┘
```

---

# 24. Accessibility

Minimum Text

14sp

---

Minimum Touch

48dp

---

Screen Reader Support

Required

---

Color Blind Safe

Required

---

# 25. Animation Guidelines

Duration

150ms–250ms

---

Button Press

Scale Animation

---

Screen Transition

Fade

---

Dialog

Slide Up

---

# 26. UX Success Metrics

User can connect robot

< 10 seconds

---

User can send command

< 3 seconds

---

User can create mapping

< 30 seconds

---

User can create profile

< 1 minute

---

# 27. MVP Screens

Included

* Home
* Device Connection
* Controller
* Voice
* Gesture
* Mapping
* Profiles
* Settings

---

# 28. Future Screens

Blockly Programming

Python Programming

Robot Dashboard

Telemetry Viewer

Firmware Update

Classroom Mode

Cloud Sync

---

# 29. UI Component Library

Core Components

* AppTopBar
* StatusBadge
* RobotCard
* ControllerButton
* AnalogJoystick
* VoiceIndicator
* GestureOverlay
* MappingRow
* ProfileCard
* SettingItem

---

# 30. Summary

UI được thiết kế theo triết lý:

```text
Simple
Visual
Fast
Offline First
Student Friendly
```

Mọi chức năng điều khiển robot phải thực hiện được trong tối đa 2 thao tác và luôn có phản hồi thị giác tức thời.

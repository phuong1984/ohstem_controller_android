# SRS.md

# Software Requirements Specification

Project: STEM Controller

Version: 1.0

Status: Draft

Last Updated: 2026-06-10

---

# 1. Introduction

## 1.1 Purpose

Tài liệu này mô tả đầy đủ các yêu cầu phần mềm của ứng dụng STEM Controller.

Tài liệu được sử dụng bởi:

* Product Owner
* UI/UX Designer
* Android Developers
* Firmware Developers
* QA Engineers

---

## 1.2 Scope

STEM Controller là ứng dụng Android cho phép điều khiển robot STEM thông qua:

* BLE
* Virtual PS4 Controller
* Voice Commands
* Hand Gestures

Ứng dụng hỗ trợ cấu hình mapping giữa:

* Input
* Event
* BLE Command

---

## 1.3 Definitions

### BLE

Bluetooth Low Energy

### Event

Sự kiện điều khiển nội bộ.

Ví dụ:

```text
UP_PRESS
UP_RELEASE
LEFT_PRESS
X_PRESS
```

### Action

Lệnh logic điều khiển robot.

Ví dụ:

```text
MOVE_FORWARD
TURN_LEFT
STOP
```

### Mapping

Ánh xạ giữa:

Input → Event → Action → BLE Command

---

# 2. System Overview

## 2.1 High Level Architecture

```text
UI Layer
    |
ViewModel
    |
Repository
    |
Command Engine
    |
BLE Service
    |
ESP32
```

---

## 2.2 Main Modules

### Module A

BLE Communication

### Module B

Virtual Controller

### Module C

Voice Recognition

### Module D

Gesture Recognition

### Module E

Mapping Engine

### Module F

Profile Management

### Module G

Settings

---

# 3. Functional Requirements

# FR-001

BLE Device Scan

Priority:

Critical

Description:

Ứng dụng phải quét được các thiết bị BLE xung quanh.

Input:

Scan Request

Output:

Device List

Acceptance Criteria:

* Danh sách hiển thị trong vòng 5 giây

---

# FR-002

BLE Connect

Priority:

Critical

Description:

Kết nối tới thiết bị BLE.

Input:

Device Selection

Output:

Connected State

Acceptance Criteria:

* Kết nối thành công dưới 10 giây

---

# FR-003

BLE Disconnect

Priority:

Critical

Description:

Ngắt kết nối khỏi thiết bị BLE.

---

# FR-004

Auto Reconnect

Priority:

High

Description:

Tự động kết nối lại sau khi mất kết nối.

---

# FR-005

Send BLE Data

Priority:

Critical

Description:

Gửi dữ liệu tới ESP32.

Input:

BLE Packet

Output:

Packet Sent

---

# FR-006

Receive BLE Data

Priority:

Medium

Description:

Nhận dữ liệu từ ESP32.

---

# FR-007

Virtual DPad

Priority:

Critical

Description:

Mô phỏng DPad.

Events:

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

# FR-008

Action Buttons

Priority:

Critical

Buttons:

```text
X
O
Square
Triangle
```

Events:

```text
PRESS
RELEASE
```

---

# FR-009

Shoulder Buttons

Priority:

Critical

Buttons:

```text
L1
L2
R1
R2
```

---

# FR-010

Analog Joystick

Priority:

Critical

Range:

```text
-100..100
```

Output:

```json
{
  "x": 50,
  "y": -25
}
```

Update Rate:

20Hz

---

# FR-011

Voice Recognition

Priority:

High

Description:

Nhận diện giọng nói tiếng Việt Offline.

---

# FR-012

Voice Trigger Mapping

Priority:

High

Description:

Map câu lệnh giọng nói thành Event.

Ví dụ:

```text
Đi tới
```

↓

```text
UP_PRESS
```

---

# FR-013

Gesture Recognition

Priority:

High

Description:

Nhận diện cử chỉ tay Offline.

---

# FR-014

Gesture Trigger Mapping

Priority:

High

Description:

Map gesture thành Event.

---

# FR-015

Event Router

Priority:

Critical

Description:

Nhận Event từ mọi nguồn.

Sources:

* PS4
* Voice
* Gesture

Output:

Unified Event

---

# FR-016

Action Engine

Priority:

Critical

Description:

Chuyển Event thành Robot Action.

---

# FR-017

BLE Command Engine

Priority:

Critical

Description:

Chuyển Action thành BLE Packet.

---

# FR-018

Create Profile

Priority:

High

Description:

Tạo profile điều khiển mới.

---

# FR-019

Edit Profile

Priority:

High

Description:

Chỉnh sửa profile.

---

# FR-020

Delete Profile

Priority:

High

Description:

Xóa profile.

---

# FR-021

Load Profile

Priority:

Critical

Description:

Nạp cấu hình hiện hành.

---

# FR-022

Button Sound

Priority:

Low

Description:

Âm thanh khi nhấn nút.

---

# FR-023

Vibration Feedback

Priority:

Low

Description:

Rung khi nhấn nút.

---

# 4. Use Cases

# UC-001

Connect Robot

Actor:

User

Precondition:

BLE Enabled

Flow:

1. Open App
2. Scan
3. Select Device
4. Connect
5. Success

---

# UC-002

Control Robot Using PS4

Actor:

User

Flow:

1. Press Virtual Button
2. Event Generated
3. Action Generated
4. BLE Packet Sent

---

# UC-003

Control Robot Using Voice

Actor:

User

Flow:

1. Start Listening
2. Speech Recognition
3. Mapping Engine
4. Event Generated
5. BLE Packet Sent

---

# UC-004

Control Robot Using Gesture

Actor:

User

Flow:

1. Camera Detects Hand
2. Gesture Recognition
3. Mapping Engine
4. Event Generated
5. BLE Packet Sent

---

# 5. Non Functional Requirements

# NFR-001

Startup Time

Target:

< 3 seconds

---

# NFR-002

BLE Latency

Target:

< 100 ms

---

# NFR-003

Memory Usage

Target:

< 500 MB

---

# NFR-004

Battery Usage

Target:

< 10% per hour

---

# NFR-005

Crash Free Rate

Target:

> 99%

---

# NFR-006

Offline Operation

Requirement:

Voice Recognition

Offline

---

# NFR-007

Offline Operation

Requirement:

Gesture Recognition

Offline

---

# NFR-008

Security

Requirement:

No cloud dependency.

---

# 6. Data Model

## Profile

```json
{
  "id":"profile_001",
  "name":"Micromouse",
  "createdAt":"2026-01-01"
}
```

---

## Event Mapping

```json
{
  "source":"VOICE",
  "trigger":"Đi tới",
  "event":"UP_PRESS"
}
```

---

## Action Mapping

```json
{
  "event":"UP_PRESS",
  "action":"MOVE_FORWARD"
}
```

---

## BLE Mapping

```json
{
  "action":"MOVE_FORWARD",
  "packet":"U=1"
}
```

---

# 7. Error Handling

## E001

BLE Disabled

Message:

Bluetooth chưa được bật.

---

## E002

Connection Failed

Message:

Không thể kết nối robot.

---

## E003

Voice Recognition Failed

Message:

Không nhận diện được giọng nói.

---

## E004

Camera Permission Denied

Message:

Cần quyền truy cập camera.

---

# 8. Permissions

Required:

```text
BLUETOOTH
BLUETOOTH_CONNECT
BLUETOOTH_SCAN

CAMERA

RECORD_AUDIO

VIBRATE
```

---

# 9. Acceptance Criteria

MVP được nghiệm thu khi:

* BLE hoạt động ổn định
* PS4 Virtual Controller đầy đủ
* Mapping Engine hoạt động
* Voice Offline hoạt động
* Gesture Offline hoạt động
* Profile lưu thành công
* Không có lỗi Critical hoặc Blocker

---

# 10. Future Extensions

* Blockly Integration
* Python Script Engine
* Cloud Sync
* Classroom Mode
* Multi Robot Control
* Robot Marketplace
* AI Teaching Assistant

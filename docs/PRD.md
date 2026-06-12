# Product Requirements Document (PRD)

# STEM Controller

Version: 1.0

Status: Draft

Owner: Product Team

Last Updated: 2026-06-10

---

# 1. Product Overview

## 1.1 Product Name

STEM Controller

## 1.2 Product Vision

STEM Controller là ứng dụng Android cho phép học sinh điều khiển robot STEM thông qua nhiều phương thức tương tác khác nhau:

* Tay cầm PS4 ảo
* Giọng nói tiếng Việt Offline
* Cử chỉ tay Offline
* Lập trình Python trên ESP32

Mục tiêu là xây dựng một nền tảng điều khiển robot thống nhất, dễ sử dụng, phù hợp với học sinh tiểu học, THCS và các chương trình STEM.

---

# 2. Problem Statement

Hiện nay các robot STEM thường gặp các hạn chế:

* Phụ thuộc vào tay cầm vật lý
* Khó mở rộng chức năng điều khiển
* Không hỗ trợ AI tương tác
* Không thân thiện với học sinh nhỏ tuổi
* Không có hệ thống mapping linh hoạt

Người dùng cần một nền tảng điều khiển robot:

* Đơn giản
* Offline
* Đa phương thức
* Có khả năng tùy biến

---

# 3. Product Goals

## Goal 1

Cho phép kết nối BLE tới robot ESP32 trong vòng dưới 10 giây.

## Goal 2

Cho phép điều khiển robot bằng PS4 Virtual Controller.

## Goal 3

Cho phép điều khiển robot bằng giọng nói tiếng Việt Offline.

## Goal 4

Cho phép điều khiển robot bằng cử chỉ tay Offline.

## Goal 5

Cho phép người dùng tùy chỉnh mapping giữa:

* Nút bấm
* Giọng nói
* Cử chỉ tay
* Lệnh gửi tới robot

## Goal 6

Tạo nền tảng mở rộng cho nhiều loại robot STEM khác nhau.

---

# 4. Target Users

## Primary Users

Học sinh

Độ tuổi:

* 8–15 tuổi

---

## Secondary Users

Giáo viên STEM

---

## Tertiary Users

Phụ huynh

---

# 5. Success Metrics

## Technical Metrics

BLE Connection Success Rate

Target:

> 95%

---

Voice Recognition Accuracy

Target:

> 90%

---

Gesture Recognition Accuracy

Target:

> 90%

---

App Startup Time

Target:

< 3 giây

---

BLE Latency

Target:

< 100ms

---

## Business Metrics

Weekly Active Users

Profile Usage

Robot Sessions

Average Session Duration

---

# 6. Core Features

## F001

BLE Device Connection

Priority:

P0

Description:

Kết nối tới ESP32 thông qua Bluetooth Low Energy.

---

## F002

Device Discovery

Priority:

P0

Description:

Quét các robot BLE xung quanh.

---

## F003

Auto Reconnect

Priority:

P1

Description:

Tự động kết nối lại khi mất kết nối.

---

## F004

Virtual PS4 Controller

Priority:

P0

Description:

Mô phỏng đầy đủ tay cầm PS4.

Bao gồm:

* DPad
* Analog Left
* Analog Right
* L1
* L2
* R1
* R2
* Share
* Options
* PS
* Triangle
* Circle
* Cross
* Square

---

## F005

Multi Touch Support

Priority:

P0

Description:

Cho phép nhấn nhiều nút cùng lúc.

---

## F006

Joystick Analog

Priority:

P0

Description:

Gửi giá trị analog liên tục.

Ví dụ:

LEFT_X=-50

LEFT_Y=80

---

## F007

Voice Recognition Offline

Priority:

P1

Description:

Nhận diện giọng nói tiếng Việt hoàn toàn Offline.

---

## F008

Voice Command Mapping

Priority:

P1

Description:

Ánh xạ câu lệnh giọng nói thành Action.

Ví dụ:

"Đi tới"

→ UP_PRESS

---

## F009

Hand Gesture Recognition

Priority:

P1

Description:

Nhận diện cử chỉ tay thông qua camera.

---

## F010

Gesture Mapping

Priority:

P1

Description:

Ánh xạ cử chỉ thành Action.

---

## F011

Command Mapping Engine

Priority:

P0

Description:

Cho phép người dùng định nghĩa:

Input → Action

---

## F012

Robot Action Mapping

Priority:

P0

Description:

Cho phép ánh xạ:

Action → BLE Command

---

## F013

Profile Management

Priority:

P1

Description:

Lưu nhiều cấu hình điều khiển khác nhau.

---

## F014

Sound Feedback

Priority:

P2

Description:

Âm thanh khi nhấn nút.

---

## F015

Haptic Feedback

Priority:

P2

Description:

Rung khi nhấn nút.

---

# 7. User Stories

## Connection

As a user

I want to connect to a robot

So that I can control it.

---

## Controller

As a user

I want to use a virtual PS4 controller

So that I do not need a physical controller.

---

## Voice

As a user

I want to control the robot by voice

So that interaction is more natural.

---

## Gesture

As a user

I want to control the robot by hand gestures

So that I can demonstrate AI concepts.

---

## Mapping

As a teacher

I want to customize commands

So that different robots can reuse the same application.

---

# 8. Constraints

## Platform

Android Only

Minimum:

Android 10

Target:

Android 15+

---

## Offline Requirement

Voice Recognition:

Offline

Gesture Recognition:

Offline

Robot Control:

Offline

---

## BLE Requirement

Bluetooth Low Energy Only

---

# 9. Risks

## Risk 1

Low-end devices may not support real-time gesture recognition.

Mitigation:

Reduce FPS.

---

## Risk 2

Vietnamese speech recognition quality.

Mitigation:

Limited command vocabulary.

---

## Risk 3

BLE packet flooding.

Mitigation:

Packet throttling.

---

# 10. Future Roadmap

Version 2.0

* Blockly Integration
* Python Programming
* Cloud Sync
* Classroom Management

Version 3.0

* Multi Robot Support
* Robot Marketplace
* AI Assistant

Version 4.0

* Full STEM Learning Platform
* Web Dashboard
* Cross Platform Support

---

# 11. MVP Scope

Included:

* BLE Connection
* Virtual PS4 Controller
* Mapping Engine
* Profile Management

Excluded:

* Cloud Features
* User Accounts
* Online AI Services

---

# 12. Release Criteria

Release 1.0 được chấp nhận khi:

* BLE ổn định
* PS4 Virtual hoạt động đầy đủ
* Mapping Engine hoạt động
* Tỷ lệ crash dưới 1%
* Test pass trên ít nhất 10 thiết bị Android

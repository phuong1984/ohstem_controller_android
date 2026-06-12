KẾ HOẠCH PHÂN CHIA TASK & THEO DÕI TIẾN ĐỘ

*Dự án: App Android điều khiển Robot ESP32 (BLE, PS4 Gamepad ảo, Giọng nói & Cử chỉ tay offline)*

1\. Tổng quan giai đoạn (Roadmap)

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Giai đoạn** | **Tên giai đoạn** | **Mục tiêu chính** | **Thời lượng dự kiến** | **Phụ thuộc** | **Trạng thái** | **Ghi chú** |
| GĐ0 | Khởi tạo dự án | Setup project, kiến trúc nền, thiết kế UI/UX cơ bản | 1 tuần | --- | Chưa bắt đầu |  |
| GĐ1 | Kết nối BLE | Quét, kết nối, gửi/nhận dữ liệu BLE với ESP32 | 1.5 tuần | GĐ0 | Chưa bắt đầu |  |
| GĐ2 | Tay cầm ảo PS4 | Giao diện gamepad đầy đủ + gửi lệnh qua BLE | 2 tuần | GĐ1 | Chưa bắt đầu |  |
| GĐ3 | Hệ thống cấu hình mapping | Bảng ánh xạ Input → Lệnh, Virtual Actions, Profiles | 1.5 tuần | GĐ2 | Chưa bắt đầu |  |
| GĐ4 | Nhận diện giọng nói offline | Tích hợp Vosk tiếng Việt, push-to-talk, wake word | 2 tuần | GĐ3 | Chưa bắt đầu |  |
| GĐ5 | Nhận diện cử chỉ tay offline | Tích hợp MediaPipe, state machine cử chỉ động | 2 tuần | GĐ3 | Chưa bắt đầu |  |
| GĐ6 | Cài đặt phản hồi (Haptic/Audio) | Rung, âm thanh, tùy chỉnh theme | 0.5 tuần | GĐ2 | Chưa bắt đầu |  |
| GĐ7 | Kiểm thử & tối ưu | Test tích hợp, tối ưu hiệu năng, sửa lỗi | 2 tuần | GĐ4,5,6 | Chưa bắt đầu |  |
| GĐ8 | Đóng gói & phát hành | Build release, tài liệu hướng dẫn, demo ESP32 | 1 tuần | GĐ7 | Chưa bắt đầu |  |

Tổng thời lượng ước tính: \~13.5 tuần (có thể chạy song song GĐ4 & GĐ5, GĐ6 lồng vào GĐ2 → rút ngắn còn \~10-11 tuần với 1-2 dev).

2\. Giai đoạn 0 --- Khởi tạo dự án

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T0.1 | Khởi tạo project Android (Kotlin + Jetpack Compose) |  | Cao | 4 | ✅ Hoàn thành | Root build.gradle.kts, app/build.gradle.kts, settings.gradle.kts |
| T0.2 | Cấu hình kiến trúc MVVM + Hilt (DI) + Coroutines/Flow |  | Cao | 6 | ✅ Hoàn thành | DataModule.kt + package structure (ui/, data/, di/, repository/, viewmodel/) |
| T0.3 | Thiết lập Navigation (5 màn hình chính) |  | Cao | 4 | ✅ Hoàn thành | Navigation.kt + 5 screens: Home, Gamepad, Voice, Gesture, Settings |
| T0.4 | Thiết lập theme Material 3 (dark theme, màu PS4) |  | Trung bình | 4 | ✅ Hoàn thành | Color.kt, Type.kt, Theme.kt với PS4 colors |
| T0.5 | Thiết kế wireframe/mockup chi tiết (Figma) cho 6 màn hình |  | Cao | 16 | Chuyển sang GĐ1-GĐ6 | Placeholder screens ready, chi tiết UI từng giai đoạn |
| T0.6 | Thiết lập Room Database + DataStore khung sườn |  | Trung bình | 4 | ✅ Hoàn thành | 4 entities, 4 DAOs, RobotControllerDatabase.kt, DataStore module |
| T0.7 | Thiết lập CI cơ bản (build, lint) |  | Thấp | 3 | Chờ gradle wrapper | Gradle config ready, cần test build thực tế |

3\. Giai đoạn 1 --- Kết nối BLE

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T1.1 | Tích hợp Nordic Android BLE Library |  | Cao | 4 | Chưa bắt đầu |  |
| T1.2 | Màn hình quét & danh sách thiết bị BLE (kèm RSSI) |  | Cao | 8 | Chưa bắt đầu |  |
| T1.3 | Kết nối GATT + khám phá service/characteristic (Nordic UART) |  | Cao | 8 | Chưa bắt đầu |  |
| T1.4 | Cài đặt firmware ESP32 MicroPython mẫu (BLE UART GATT server) |  | Cao | 8 | Chưa bắt đầu | Phối hợp team firmware |
| T1.5 | Cơ chế tự kết nối lại + xử lý mất kết nối |  | Trung bình | 6 | Chưa bắt đầu |  |
| T1.6 | Debug Console (log lệnh gửi/nhận realtime) |  | Trung bình | 6 | Chưa bắt đầu |  |
| T1.7 | Hàng đợi lệnh (command queue) + throttle/debounce |  | Cao | 8 | Chưa bắt đầu |  |
| T1.8 | Test kết nối thực tế với board ESP32 |  | Cao | 6 | Chưa bắt đầu |  |

4\. Giai đoạn 2 --- Tay cầm ảo PS4

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T2.1 | Layout tổng thể màn hình gamepad (landscape, immersive mode) |  | Cao | 6 | Chưa bắt đầu |  |
| T2.2 | Component nút bấm (Triangle/Circle/X/Square + màu chuẩn PS4) |  | Cao | 6 | Chưa bắt đầu |  |
| T2.3 | Component D-Pad 4 hướng |  | Cao | 6 | Chưa bắt đầu |  |
| T2.4 | Component Joystick analog (đa chạm, deadzone, tự hồi tâm) |  | Cao | 12 | Chưa bắt đầu | Phức tạp nhất |
| T2.5 | Component L1/R1 (digital) + L2/R2 (analog trigger) |  | Cao | 8 | Chưa bắt đầu |  |
| T2.6 | Component Touchpad, Share, Options, PS button |  | Trung bình | 6 | Chưa bắt đầu |  |
| T2.7 | Xử lý sự kiện ACTION_DOWN/UP → gửi lệnh BLE theo mapping |  | Cao | 8 | Chưa bắt đầu | Phụ thuộc T3.x |
| T2.8 | Overlay hiển thị lệnh đang gửi (debug) |  | Thấp | 3 | Chưa bắt đầu |  |
| T2.9 | Chế độ tùy chỉnh layout (kéo-thả, resize, lưu profile) |  | Trung bình | 12 | Chưa bắt đầu |  |
| T2.10 | Chế độ một tay (One-handed mode) |  | Thấp | 6 | Chưa bắt đầu |  |
| T2.11 | Tùy chỉnh độ trong suốt (opacity) các nút |  | Thấp | 3 | Chưa bắt đầu |  |

5\. Giai đoạn 3 --- Hệ thống cấu hình ánh xạ tín hiệu (Mapping)

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T3.1 | Thiết kế schema Room: VirtualAction, InputBinding, Synonym/Alias |  | Cao | 6 | Chưa bắt đầu |  |
| T3.2 | Mapping Engine (tra cứu binding → action → gửi lệnh BLE) |  | Cao | 10 | Chưa bắt đầu | Lõi hệ thống |
| T3.3 | Màn hình tab \"Tay cầm\": cấu hình lệnh nhấn/nhả cho từng nút |  | Cao | 8 | Chưa bắt đầu |  |
| T3.4 | Màn hình tab \"Hành động ảo\" (CRUD Virtual Actions) |  | Cao | 8 | Chưa bắt đầu |  |
| T3.5 | Màn hình tab \"Giọng nói\" (CRUD lệnh + đồng nghĩa, map action) |  | Trung bình | 6 | Chưa bắt đầu | Phụ thuộc UI; logic chờ GĐ4 |
| T3.6 | Màn hình tab \"Cử chỉ tay\" (CRUD cử chỉ, map action) |  | Trung bình | 6 | Chưa bắt đầu | Phụ thuộc UI; logic chờ GĐ5 |
| T3.7 | Quản lý nhiều Profile (tạo/xóa/chuyển đổi) |  | Trung bình | 6 | Chưa bắt đầu |  |
| T3.8 | Xuất/Nhập cấu hình JSON |  | Thấp | 6 | Chưa bắt đầu |  |
| T3.9 | Validate cú pháp lệnh + preview lệnh thực tế |  | Trung bình | 5 | Chưa bắt đầu |  |

6\. Giai đoạn 4 --- Nhận diện giọng nói tiếng Việt offline

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T4.1 | Tích hợp thư viện Vosk Android + tải mô hình tiếng Việt |  | Cao | 8 | Chưa bắt đầu |  |
| T4.2 | Module Push-to-talk (nhấn giữ → ghi âm → nhận diện) |  | Cao | 8 | Chưa bắt đầu |  |
| T4.3 | Module Always-on + Wake word (TFLite keyword spotting) |  | Trung bình | 12 | Chưa bắt đầu | Có thể làm sau |
| T4.4 | Màn hình Voice Control: waveform, transcript realtime, danh sách lệnh |  | Cao | 8 | Chưa bắt đầu |  |
| T4.5 | Kết nối kết quả nhận diện → Mapping Engine (T3.2) |  | Cao | 6 | Chưa bắt đầu |  |
| T4.6 | Xử lý từ đồng nghĩa / chuẩn hóa câu lệnh |  | Trung bình | 6 | Chưa bắt đầu |  |
| T4.7 | Cài đặt độ nhạy (confidence threshold) + quản lý mô hình |  | Thấp | 5 | Chưa bắt đầu |  |
| T4.8 | Test thực tế với nhiều giọng nói/môi trường khác nhau |  | Cao | 8 | Chưa bắt đầu |  |

7\. Giai đoạn 5 --- Nhận diện cử chỉ tay offline

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T5.1 | Tích hợp CameraX + preview |  | Cao | 6 | Chưa bắt đầu |  |
| T5.2 | Tích hợp MediaPipe Hand Landmarker (21 điểm landmark) |  | Cao | 10 | Chưa bắt đầu |  |
| T5.3 | Tích hợp MediaPipe Gesture Recognizer (cử chỉ tĩnh có sẵn) |  | Cao | 8 | Chưa bắt đầu |  |
| T5.4 | Vẽ overlay landmark + tên cử chỉ + độ tin cậy lên preview |  | Trung bình | 6 | Chưa bắt đầu |  |
| T5.5 | State machine phát hiện cử chỉ động (phất 4 hướng, vẽ vòng tròn) |  | Cao | 12 | Chưa bắt đầu | Logic phức tạp |
| T5.6 | Cơ chế debounce/cooldown chống kích hoạt nhầm |  | Trung bình | 5 | Chưa bắt đầu |  |
| T5.7 | Màn hình Gesture Control: danh sách cử chỉ, thumbnail, toggle camera |  | Trung bình | 6 | Chưa bắt đầu |  |
| T5.8 | Kết nối kết quả nhận diện → Mapping Engine (T3.2) |  | Cao | 5 | Chưa bắt đầu |  |
| T5.9 | Tối ưu hiệu năng/nhiệt độ khi chạy liên tục + cảnh báo |  | Trung bình | 8 | Chưa bắt đầu |  |
| T5.10 | Test thực tế với nhiều điều kiện ánh sáng/khoảng cách |  | Cao | 8 | Chưa bắt đầu |  |

8\. Giai đoạn 6 --- Cài đặt phản hồi (Haptic & Audio)

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T6.1 | Tích hợp Vibrator/VibratorManager + amplitude control |  | Trung bình | 4 | Chưa bắt đầu |  |
| T6.2 | Tích hợp SoundPool + bộ âm thanh mặc định (3 theme) |  | Trung bình | 4 | Chưa bắt đầu |  |
| T6.3 | Màn hình Settings: toggle rung/âm thanh, slider cường độ |  | Trung bình | 6 | Chưa bắt đầu |  |
| T6.4 | Tùy chỉnh rung riêng theo nhóm nút |  | Thấp | 4 | Chưa bắt đầu |  |
| T6.5 | Cho phép import âm thanh tùy chỉnh |  | Thấp | 4 | Chưa bắt đầu |  |
| T6.6 | Rung cảnh báo khi mất kết nối BLE |  | Thấp | 2 | Chưa bắt đầu |  |

9\. Giai đoạn 7 --- Kiểm thử & Tối ưu

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T7.1 | Test tích hợp toàn bộ luồng: gamepad/voice/gesture → BLE → ESP32 |  | Cao | 12 | Chưa bắt đầu |  |
| T7.2 | Test hiệu năng: độ trễ lệnh, FPS camera, pin tiêu thụ |  | Cao | 8 | Chưa bắt đầu |  |
| T7.3 | Test trên nhiều thiết bị Android (tầm thấp/trung/cao) |  | Cao | 10 | Chưa bắt đầu |  |
| T7.4 | Test failsafe khi mất BLE giữa lúc điều khiển |  | Cao | 4 | Chưa bắt đầu |  |
| T7.5 | Sửa lỗi UI/UX theo phản hồi (usability testing) |  | Trung bình | 10 | Chưa bắt đầu |  |
| T7.6 | Tối ưu bộ nhớ/CPU khi chạy đồng thời voice+gesture+BLE |  | Cao | 10 | Chưa bắt đầu |  |
| T7.7 | Viết unit test cho Mapping Engine |  | Trung bình | 6 | Chưa bắt đầu |  |

10\. Giai đoạn 8 --- Đóng gói & Phát hành

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Mã task** | **Tên task** | **Người phụ trách** | **Ưu tiên** | **Ước tính (giờ)** | **Trạng thái** | **Ghi chú** |
| T8.1 | Build release (signing, ProGuard/R8, giảm kích thước APK) |  | Cao | 6 | Chưa bắt đầu | Mô hình AI làm tăng size APK |
| T8.2 | Viết tài liệu hướng dẫn sử dụng (người dùng cuối) |  | Trung bình | 8 | Chưa bắt đầu |  |
| T8.3 | Viết tài liệu mẫu firmware ESP32 MicroPython + hướng dẫn cấu hình |  | Cao | 8 | Chưa bắt đầu |  |
| T8.4 | Quay video demo (gamepad, voice, gesture) |  | Thấp | 6 | Chưa bắt đầu |  |
| T8.5 | Phát hành nội bộ/Play Store (closed testing) |  | Trung bình | 4 | Chưa bắt đầu |  |

11\. Bảng theo dõi tiến độ tổng hợp (Mẫu cập nhật hàng tuần)

Hướng dẫn sử dụng: Cập nhật cột \"% Hoàn thành\" và \"Trạng thái\" mỗi tuần. Trạng thái gồm: Chưa bắt đầu / Đang thực hiện / Chờ review / Hoàn thành / Bị chặn (Blocked).

|  |  |  |  |  |  |  |
|----|----|----|----|----|----|----|
| **Tuần** | **Giai đoạn / Task chính** | **% Hoàn thành tuần trước** | **% Hoàn thành tuần này** | **Trạng thái** | **Vấn đề phát sinh** | **Kế hoạch tuần sau** |
| 1 | GĐ0 - Khởi tạo dự án | 0% | 100% | ✅ Hoàn thành | Không | GĐ1 - Bắt đầu BLE integration |
| 2 | GĐ1 - Kết nối BLE |  |  | Chưa bắt đầu |  |  |
| 3 | GĐ1 → GĐ2 |  |  | Chưa bắt đầu |  |  |
| 4 | GĐ2 - Tay cầm ảo PS4 |  |  | Chưa bắt đầu |  |  |
| 5 | GĐ2 (tiếp) + GĐ6 |  |  | Chưa bắt đầu |  |  |
| 6 | GĐ3 - Mapping |  |  | Chưa bắt đầu |  |  |
| 7 | GĐ3 (tiếp) + GĐ4 bắt đầu |  |  | Chưa bắt đầu |  |  |
| 8 | GĐ4 - Giọng nói + GĐ5 - Cử chỉ (song song) |  |  | Chưa bắt đầu |  |  |
| 9 | GĐ4/GĐ5 (tiếp) |  |  | Chưa bắt đầu |  |  |
| 10 | GĐ5 (hoàn tất) + GĐ7 bắt đầu |  |  | Chưa bắt đầu |  |  |
| 11 | GĐ7 - Kiểm thử & tối ưu |  |  | Chưa bắt đầu |  |  |
| 12 | GĐ7 (tiếp) |  |  | Chưa bắt đầu |  |  |
| 13 | GĐ8 - Đóng gói & phát hành |  |  | Chưa bắt đầu |  |  |

12\. Phân loại độ ưu tiên & rủi ro chính

Các task có rủi ro/độ phức tạp cao cần ưu tiên prototype sớm để giảm rủi ro tiến độ:

- T2.4 (Joystick analog đa chạm) --- nên làm proof-of-concept ngay từ đầu GĐ2.

- T3.2 (Mapping Engine) --- là lõi hệ thống, mọi tính năng đầu vào đều phụ thuộc, cần thiết kế kỹ schema trước.

- T4.1/T4.3 (Vosk + Wake word) --- cần test sớm khả năng nhận diện tiếng Việt thực tế, có phương án dự phòng (Whisper.cpp) nếu Vosk không đủ chính xác.

- T5.5 (State machine cử chỉ động) --- cần thử nghiệm với nhiều người dùng/tốc độ tay khác nhau để tinh chỉnh ngưỡng.

- T1.4 (Firmware ESP32 mẫu) --- cần hoàn thành sớm để có thiết bị test thực tế xuyên suốt dự án, tránh chỉ test giả lập.

- T7.6 (Tối ưu chạy đồng thời voice+gesture+BLE) --- rủi ro hiệu năng cao trên thiết bị tầm thấp, cần benchmark sớm.

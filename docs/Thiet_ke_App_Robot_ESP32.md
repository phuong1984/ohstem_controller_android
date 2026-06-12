TÀI LIỆU THIẾT KẾ ỨNG DỤNG ANDROID

*Điều khiển Robot ESP32 qua BLE --- Tay cầm ảo PS4, Giọng nói & Cử chỉ tay (Offline)*

1\. Tổng quan dự án

Ứng dụng Android cho phép người dùng kết nối tới mạch ESP32 (chạy MicroPython) qua Bluetooth Low Energy (BLE) để điều khiển robot. Người dùng có thể điều khiển bằng 4 phương thức đầu vào: tay cầm ảo mô phỏng PS4 (DualShock 4), giọng nói tiếng Việt offline, cử chỉ tay qua camera offline, và mọi đầu vào đều được ánh xạ qua một bảng cấu hình tín hiệu tùy biến gửi xuống ESP32 dưới dạng chuỗi lệnh.

1.1 Mục tiêu thiết kế

- Hoạt động hoàn toàn offline cho nhận diện giọng nói và cử chỉ (không cần internet).

- Độ trễ thấp (mục tiêu \< 100ms từ sự kiện đầu vào đến khi gửi lệnh BLE).

- Giao diện trực quan, dễ dùng một tay hoặc hai tay (tay cầm ảo).

- Cấu hình linh hoạt: người dùng tự định nghĩa lệnh gửi xuống ESP32 cho từng nút/cử chỉ/câu lệnh giọng nói.

- Phản hồi xúc giác (rung) và âm thanh có thể tùy chỉnh, giống cảm giác tay cầm PS4 thật.

1.2 Đối tượng người dùng

- Học sinh/sinh viên học robotics, lập trình ESP32 MicroPython.

- Người làm dự án DIY robot, xe điều khiển từ xa.

- Giáo viên/CLB STEM cần demo điều khiển robot bằng nhiều phương thức.

2\. Kiến trúc tổng thể hệ thống

Hệ thống gồm 3 lớp chính:

1.  Lớp đầu vào (Input Layer): Tay cầm ảo PS4, Speech Recognition Engine, Gesture Recognition Engine.

2.  Lớp xử lý & ánh xạ (Mapping Engine): Chuyển đổi sự kiện đầu vào thành chuỗi lệnh dựa trên bảng cấu hình do người dùng định nghĩa.

3.  Lớp giao tiếp (BLE Communication Layer): Gửi chuỗi lệnh tới ESP32 qua BLE GATT (Nordic UART Service hoặc custom service).

2.1 Sơ đồ luồng dữ liệu

Input (Gamepad / Voice / Gesture) → Input Event Normalizer → Command Mapping Engine (đọc cấu hình từ Local Database) → Command Queue/Debouncer → BLE GATT Write Characteristic → ESP32 (MicroPython UART/BLE callback) → Robot Actuator.

2.2 Kiến trúc phần mềm Android (đề xuất)

|  |  |
|----|----|
| **Layer** | **Mô tả / Công nghệ** |
| Ngôn ngữ | Kotlin (khuyến nghị) --- Jetpack Compose cho UI |
| Kiến trúc | MVVM + Repository Pattern + Hilt (Dependency Injection) |
| BLE | Android Bluetooth LE API (BluetoothGatt) hoặc thư viện Nordic Android BLE Library |
| Lưu trữ cấu hình | Room Database (SQLite) cho mapping config, DataStore cho settings |
| Speech (offline) | Vosk Android (mô hình tiếng Việt) hoặc Whisper.cpp |
| Gesture (offline) | MediaPipe Tasks Hand Landmarker (on-device) |
| Âm thanh phản hồi | SoundPool / MediaPlayer |
| Rung phản hồi | Vibrator / VibratorManager API (Android 12+) |

3\. Thiết kế UI/UX

3.1 Nguyên tắc thiết kế chung

- Dark theme mặc định (phù hợp môi trường thực tế ngoài trời/phòng lab, tiết kiệm pin OLED).

- Bố cục Landscape (ngang) là chính cho màn hình điều khiển --- vì tay cầm PS4 cầm ngang.

- Sử dụng Material Design 3, các nút bấm có kích thước tối thiểu 48dp, có phản hồi ripple + haptic.

- Màu sắc theo chuẩn PS4: nút Tam giác (xanh lá), Tròn (đỏ), X (xanh dương), Vuông (hồng/tím).

- Thanh trạng thái BLE luôn hiển thị (kết nối/mất kết nối/đang gửi lệnh) ở mọi màn hình.

3.2 Cấu trúc điều hướng (Navigation)

Bottom Navigation Bar (ở chế độ Portrait cho các màn hình cấu hình) hoặc Navigation Drawer, gồm 5 mục chính:

4.  Trang chủ / Kết nối BLE (Home/Connect)

5.  Tay cầm ảo (Gamepad Control) --- màn hình chính, landscape

6.  Giọng nói (Voice Control)

7.  Cử chỉ tay (Gesture Control)

8.  Cấu hình (Settings & Mapping)

3.3 Màn hình 1 --- Kết nối BLE

**Thành phần giao diện:**

- Header: Logo app + tên thiết bị đã kết nối (nếu có).

- Nút \"Quét thiết bị\" (Scan) --- hiển thị danh sách thiết bị BLE tìm thấy kèm RSSI (cường độ tín hiệu) dạng icon sóng.

- Mỗi item trong danh sách: Tên thiết bị, địa chỉ MAC, nút \"Kết nối\".

- Khi đã kết nối: hiển thị card lớn màu xanh lá \"Đã kết nối tới ESP32-Robot\" + nút \"Ngắt kết nối\" + thông tin Service/Characteristic UUID.

- Khu vực \"Console gỡ lỗi\" (Debug Console) thu gọn được --- hiển thị log lệnh gửi/nhận dạng text, hữu ích khi lập trình MicroPython.

- FAB (Floating Action Button) \"Bắt đầu điều khiển\" → chuyển sang màn hình Gamepad.

3.4 Màn hình 2 --- Tay cầm ảo PS4 (Gamepad Control)

Đây là màn hình quan trọng nhất, thiết kế mô phỏng layout DualShock 4, hiển thị landscape toàn màn hình (immersive mode).

**Bố cục (chia theo vùng màn hình, từ trái sang phải):**

|  |  |
|----|----|
| **Vùng** | **Thành phần** |
| Trên cùng (status bar mỏng) | Trạng thái BLE (icon), tên robot, FPS lệnh/giây, nút thoát (X) |
| Trái trên | Nút L1 (bumper), L2 (trigger - dạng thanh trượt áp lực hoặc nút bấm) |
| Trái giữa | D-Pad (4 hướng: Lên / Xuống / Trái / Phải) dạng hình thoi, kích thước 180dp (tỉ lệ 1x) |
| Trái dưới | Joystick trái (L3) --- phía dưới-phải của D-Pad, chồng lề dọc |
| Giữa | Nút Share, Options |
| Phải trên | Nút R1 (bumper), R2 (trigger) --- căn trái theo lề phải vùng nút hành động |
| Phải giữa | 4 nút hành động: Tam giác (△), Tròn (○), X (✕), Vuông (□) bố trí hình thoi trong khung 180dp |
| Phải dưới | Joystick phải (R3) --- bên trái nút hành động, cùng hàng dọc với L3 |

**Kích thước button cho thiết kế image (khi thay thế symbol Unicode):**
- Mỗi nút hành động là hình tròn đường kính **52dp** (tỉ lệ 1x).
- Image nên là PNG trong suốt, kích thước **52×52dp** (tương đương ~78×78px ở mật độ xhdpi).
- Nút ○ được scale font 150% so với các nút còn lại, cần image 52dp với nội dung to hơn nhưng vừa trong khung tròn.

**Responsive Scaling:**
- Toàn bộ kích thước sử dụng `BoxWithConstraints` với tham chiếu 800×360dp (landscape).
- Scale factor = `min(maxWidth/800, maxHeight/360).coerceIn(0.6, 1.5)`.
- Nội dung chính nằm trong Column `fillMaxWidth(0.9f)` căn giữa để tránh góc bo tròn thiết bị.

**Các hằng số kích thước (tỉ lệ 1x):**

| Biến | Giá trị | Mô tả |
|------|---------|-------|
| `sclTrigW` | 63dp | Rộng trigger L1/L2/R1/R2 |
| `sclTrigH` | 25dp | Cao trigger |
| `sclScreenPadding` | 16dp | Padding màn hình |
| `sclButtonSize` | 64dp | Nút Share/Options |
| `sclJoySize` | 90dp | Kích thước joystick L3/R3 |
| `sclPs4BtnSize` | 52dp | Đường kính nút hành động (△□○✕) |
| `sclActionBoxSize` | 180dp | Khung chứa 4 nút hành động (hình thoi) |
| `sclDpadButtonSize` | ~69dp | Kích thước mỗi cánh D-Pad |
| `sclFontStd` | ~33.8sp | Cỡ chữ symbol (52×0.65) |
| `sclFontO` | ~50.7sp | Cỡ chữ ○ (150%) |

**Bố cục chi tiết (theo implementation, GamepadScreen.kt):**
- Row chính chia 3 cột: weight(0.3) / weight(0.4) / weight(0.3).
- **Cột trái (0.3):** D-Pad (TopStart, offset x = screenPadding + trigW), L3 bên phải D-Pad (offset x = DPadRight, y = actionBoxSize - ps4BtnSize/2).
- **Cột giữa (0.4):** Hàng ngang Share (trái) và Options (phải), căn giữa.
- **Cột phải (0.3):** 4 nút hành động trong Box size actionBoxSize (TopEnd), R3 bên trái (offset x = -actionBoxSize, y = actionBoxSize - ps4BtnSize/2). Có padding phải = screenPadding + trigW để R1/R2 căn theo lề phải nút hành động.
- **Triggers (L1/L2/R1/R2):** Hàng riêng bên trên Row chính, chia 2 cột: trái (L1, L2) căn trái, phải (R1, R2) căn phải.
- **Status bar:** Thanh trạng thái phía trên cùng, background đen mờ.

**Thành phần Ps4Button (Ps4Button.kt):**
- Hình tròn, có scale animation khi nhấn (0.85x), shadow đen 30%.
- Background: 75% opacity khi không nhấn, 100% khi nhấn.
- Font chữ Unicode (△ □ ○ ✕) dùng `FontWeight.Bold`, `textAlign = TextAlign.Center`.
- Vertical offset để căn giữa glyph: `-(fontSizeValue * 0.12).dp`.

**Thành phần Joystick (Joystick.kt):**
- Vòng tròn ngoài 90dp, núm tròn trong 56dp vẽ bằng Canvas.
- Deadzone 10%, tự hồi tâm khi thả.
- Inner highlight ở tâm núm (không offset 3D).
- Click indicator (L3/R3) căn giữa Box.

**Hành vi tương tác:**

- Joystick: dùng MotionEvent đa chạm (multi-touch), tính toán góc & độ lệch (deadzone 10%), gửi giá trị trục X/Y dạng -100 đến 100, tốc độ gửi giới hạn (throttle) 20--50ms/lần để tránh nghẽn BLE.

- Nút bấm thường: Gửi lệnh \"nhấn\" (press=1) khi ACTION_DOWN, gửi \"nhả\" (press=0) khi ACTION_UP --- đúng theo yêu cầu cấu hình (ví dụ U=1 / U=0).

- Trigger L2/R2: thanh trượt dọc 0--100% (analog) hoặc chế độ digital on/off (tùy cấu hình).

- Mỗi lần nhấn: rung nhẹ (haptic feedback \~20ms) + âm thanh \"click\" (nếu bật trong Settings).

- Có thể \"khóa\" joystick về giữa bằng nút khóa nhanh, tránh gửi lệnh nhầm.

- Overlay mờ hiển thị nhãn lệnh hiện tại đang gửi (ví dụ \"U=1\") ở góc màn hình --- bật/tắt được, hữu ích khi debug.

- Tùy chọn thay đổi độ trong suốt (opacity) của các nút để không che camera/video stream từ robot (nếu robot có camera).

3.5 Màn hình 3 --- Điều khiển bằng giọng nói (Voice Control)

**Thành phần giao diện:**

- Nút Microphone lớn ở giữa màn hình --- 2 chế độ: \"Nhấn để nói\" (Push-to-talk) hoặc \"Luôn lắng nghe\" (Always-on, dùng wake word).

- Hiệu ứng sóng âm (waveform animation) quanh nút mic khi đang ghi âm/nhận diện.

- Vùng hiển thị văn bản nhận diện được (real-time transcript) --- ví dụ: đang nghe\... → \"Đi tới\".

- Danh sách lệnh giọng nói được hỗ trợ (lấy từ bảng cấu hình), hiển thị dạng chip/tag, lệnh nào vừa được nhận diện sẽ highlight.

- Toggle \"Wake word\" (từ khóa đánh thức, ví dụ \"Robot ơi\") để kích hoạt always-on mà không tốn pin liên tục.

- Thanh trượt độ nhạy nhận diện (confidence threshold).

- Nút \"Test giọng nói\" để kiểm tra mô hình mà không gửi lệnh thật xuống ESP32.

3.6 Màn hình 4 --- Điều khiển bằng cử chỉ tay (Gesture Control)

**Thành phần giao diện:**

- Camera preview toàn màn hình (camera trước hoặc sau, chọn được) với khung overlay vẽ landmark bàn tay (21 điểm khớp ngón tay theo MediaPipe) để người dùng thấy hệ thống nhận diện đúng.

- Góc màn hình: hiển thị tên cử chỉ vừa nhận diện (ví dụ \"Phất lên\" → icon mũi tên lên) + độ tin cậy (%).

- Danh sách cử chỉ hỗ trợ kèm hình minh họa nhỏ (thumbnail): Phất lên/xuống/trái/phải, Nắm tay (Fist), Bàn tay mở (Open Palm), Ngón cái (Thumbs up/down), chụm ngón (Pinch), v.v.

- Thanh thời gian giữ cử chỉ (debounce time) trước khi kích hoạt lệnh, tránh kích hoạt nhầm liên tục.

- Toggle bật/tắt nhanh, và nút chuyển camera trước/sau.

- Cảnh báo pin/hiệu năng: hiển thị mức sử dụng CPU/nhiệt độ máy nếu nhận diện liên tục gây nóng máy.

3.7 Màn hình 5 --- Cấu hình tín hiệu (Mapping Configuration)

Đây là màn hình cốt lõi cho phép người dùng tùy biến hoàn toàn ánh xạ Input → Lệnh gửi ESP32. Thiết kế dạng bảng/danh sách có thể chỉnh sửa (giống bảng tính đơn giản).

**Cấu trúc tab:**

- Tab \"Tay cầm\": liệt kê tất cả nút/joystick PS4. Mỗi dòng gồm: Tên nút (D-Pad Up, X, Triangle, L2, Joystick trái X/Y\...), 2 ô nhập \"Lệnh khi nhấn\" và \"Lệnh khi nhả\" (ví dụ U=1 / U=0), với joystick/trigger analog có thêm ô \"Định dạng lệnh analog\" (ví dụ LX={value}).

- Tab \"Giọng nói\": danh sách các câu lệnh tiếng Việt do người dùng tự thêm (ví dụ \"Đi tới\", \"Dừng lại\", \"Quay trái\"), mỗi câu lệnh map tới một \"hành động ảo\" (Virtual Action) --- tái sử dụng cùng định nghĩa với nút tay cầm (ví dụ map \"Đi tới\" → cùng hành động với \"D-Pad Up\").

- Tab \"Cử chỉ tay\": tương tự, mỗi cử chỉ (Phất lên, Nắm tay\...) map tới một Hành động ảo có sẵn.

- Tab \"Hành động ảo\" (Virtual Actions): danh sách trung tâm các hành động logic (ví dụ \"Tiến\", \"Lùi\", \"Rẽ trái\", \"Dừng khẩn cấp\") --- mỗi hành động định nghĩa chuỗi lệnh BLE thực tế (lệnh khi kích hoạt + lệnh khi kết thúc). Đây là lớp trừu tượng giúp 1 hành động được kích hoạt từ nhiều nguồn (nút, giọng nói, cử chỉ) mà không cần lặp cấu hình.

**Tính năng bổ sung của màn hình cấu hình:**

- Nút \"Khôi phục mặc định\" cho từng tab.

- Lưu nhiều \"Hồ sơ cấu hình\" (Profiles) --- ví dụ Profile \"Xe 2 bánh\", Profile \"Cánh tay robot\", chuyển đổi nhanh.

- Xuất/Nhập cấu hình dạng file JSON (chia sẻ giữa các thiết bị/dự án).

- Validate cú pháp lệnh (cảnh báo nếu trùng lệnh hoặc định dạng sai) trước khi lưu.

- Preview: ô nhập thử để xem chuỗi lệnh thực tế sẽ được gửi như thế nào với placeholder {value}.

3.8 Màn hình 6 --- Cài đặt phản hồi & cài đặt chung (Settings)

**Nhóm cài đặt \"Phản hồi tay cầm ảo\":**

- Bật/tắt rung khi nhấn nút (Haptic feedback) + thanh trượt cường độ rung (nếu thiết bị hỗ trợ Amplitude control).

- Bật/tắt âm thanh khi nhấn nút + chọn bộ âm thanh (theme: \"Click cơ học\", \"Điện tử\", \"Im lặng\").

- Tùy chỉnh rung riêng cho từng loại nút (ví dụ rung mạnh hơn cho nút hành động, rung nhẹ cho D-Pad).

- Rung phản hồi khi mất kết nối BLE đột ngột (cảnh báo).

**Nhóm cài đặt \"Kết nối & Hiệu năng\":**

- Tần suất gửi lệnh BLE (throttle rate) --- slider 10--100ms.

- Tự động kết nối lại khi mất tín hiệu.

- Chọn Service UUID / Characteristic UUID (cho người dùng nâng cao tự định nghĩa GATT service trên ESP32).

**Nhóm cài đặt \"Giọng nói & Cử chỉ\":**

- Chọn ngôn ngữ mô hình (Tiếng Việt --- miền Bắc/Nam nếu mô hình hỗ trợ).

- Tải/quản lý mô hình offline (kích thước file, nút tải xuống/xóa).

- Độ nhạy / ngưỡng tin cậy cho cả giọng nói và cử chỉ.

**Nhóm cài đặt chung:**

- Chủ đề sáng/tối (Light/Dark/Theo hệ thống).

- Ngôn ngữ giao diện app.

- Giới thiệu, phiên bản, hướng dẫn sử dụng.

4\. Mô tả chi tiết các tính năng

4.1 Kết nối BLE với ESP32

- Quét và liệt kê thiết bị BLE lân cận, lọc theo tên (ví dụ chỉ hiển thị thiết bị có tên bắt đầu bằng \"ESP32\").

- Kết nối GATT, khám phá services/characteristics tự động.

- Sử dụng Nordic UART Service (NUS) làm chuẩn mặc định --- vì MicroPython trên ESP32 dễ dàng cài đặt UART-over-BLE với UUID chuẩn (TX: 6E400003, RX: 6E400002, Service: 6E400001-\...).

- Gửi lệnh dạng chuỗi văn bản (text command) qua Write Characteristic, có thể nhận phản hồi/log từ ESP32 qua Notify Characteristic.

- Quản lý vòng đời kết nối: tự động phát hiện ngắt kết nối, hiển thị thông báo, tùy chọn tự kết nối lại.

- Hàng đợi lệnh (command queue) với cơ chế gộp lệnh joystick liên tục để tránh tràn bộ đệm BLE (MTU thường 20--512 bytes).

4.2 Tay cầm ảo PS4 đầy đủ chức năng

- Đầy đủ 17 nút bấm: Triangle, Circle, X, Square, D-Pad (4 hướng), L1, R1, L2, R2 (analog/digital), L3 (nhấn joystick trái), R3 (nhấn joystick phải), Share, Options, PS, Touchpad click.

- 2 joystick analog đầy đủ: vùng chạm lớn, hiệu ứng kéo-thả mượt, hỗ trợ đa điểm chạm đồng thời (ví dụ vừa giữ joystick trái vừa nhấn nút phải).

- Chế độ \"Layout tùy chỉnh\": cho phép kéo-thả, thay đổi vị trí/kích thước các nút trên màn hình (lưu theo profile) --- phù hợp với các kích thước màn hình điện thoại khác nhau.

- Chế độ một tay (One-handed mode): bố trí lại layout dồn về một bên màn hình.

4.3 Nhận diện giọng nói tiếng Việt offline

- Sử dụng mô hình nhận diện giọng nói chạy hoàn toàn trên thiết bị, không cần kết nối mạng.

- Hỗ trợ chế độ Push-to-talk (nhấn giữ nút mic) và chế độ liên tục (Continuous/Wake-word).

- Danh sách lệnh giọng nói tùy chỉnh hoàn toàn --- người dùng có thể thêm câu lệnh mới và gán cho hành động ảo bất kỳ.

- Xử lý các biến thể phát âm/từ đồng nghĩa (ví dụ \"Đi tới\", \"Tiến lên\", \"Chạy tới\" đều map cùng 1 hành động) thông qua danh sách từ đồng nghĩa trong cấu hình.

- Hiển thị phản hồi trực quan + rung khi lệnh giọng nói được nhận diện thành công.

4.4 Nhận diện cử chỉ tay offline

- Phát hiện và theo dõi 21 điểm landmark trên bàn tay theo thời gian thực qua camera.

- Phân loại cử chỉ tĩnh (Static gestures): Nắm tay, Bàn tay mở, Ngón cái lên/xuống, Chữ V, OK, Chỉ tay.

- Phân loại cử chỉ động (Dynamic gestures): Phất lên/xuống/trái/phải, Vẽ vòng tròn, Lắc tay (dựa trên chuỗi vị trí theo thời gian --- dùng cửa sổ trượt/state machine hoặc mô hình LSTM nhỏ).

- Cơ chế chống nhiễu: yêu cầu giữ cử chỉ ổn định trong khoảng thời gian cấu hình (ví dụ 300ms) trước khi kích hoạt, và \"cooldown\" sau khi kích hoạt để tránh lặp lại liên tục.

- Cho phép người dùng tự định nghĩa cử chỉ mới bằng cách \"ghi mẫu\" (record gesture sample) --- tùy chọn nâng cao, sử dụng mô hình phân loại có thể huấn luyện lại nhẹ trên thiết bị (few-shot/transfer learning) hoặc dùng rule-based đơn giản dựa trên góc khớp ngón tay.

4.5 Hệ thống cấu hình ánh xạ tín hiệu (Mapping System)

Mô hình dữ liệu đề xuất (3 bảng chính trong Room Database):

|  |  |
|----|----|
| **Bảng** | **Nội dung** |
| VirtualAction | id, tên hành động (vd \"Tiến\"), lệnh_kích_hoạt (vd \"U=1\"), lệnh_kết_thúc (vd \"U=0\"), kiểu (digital/analog), định dạng_analog (vd \"LX={value}\") |
| InputBinding | id, loại_nguồn (gamepad_button / gamepad_axis / voice / gesture), mã_nguồn (vd \"DPAD_UP\", \"voice:Đi tới\", \"gesture:swipe_up\"), virtual_action_id (khóa ngoại) |
| VoiceSynonym / GestureAlias | id, input_binding_id, từ/cụm_đồng_nghĩa bổ sung |

Khi có sự kiện đầu vào (nhấn nút / nhận diện giọng nói / cử chỉ), Mapping Engine tra cứu InputBinding tương ứng → lấy VirtualAction → gửi lệnh_kích_hoạt (hoặc lệnh_kết_thúc khi nhả/kết thúc) qua BLE.

Ví dụ luồng theo yêu cầu: Người dùng nhấn D-Pad Up → InputBinding(DPAD_UP) → VirtualAction \"Tiến\" (kích hoạt = \"U=1\", kết thúc = \"U=0\") → gửi \"U=1\". Người dùng nói \"Đi tới\" → InputBinding(voice:\"Đi tới\") → cùng VirtualAction \"Tiến\" → gửi \"U=1\" (và sau một khoảng thời gian định trước hoặc khi nói \"Dừng lại\" → gửi \"U=0\"). Cử chỉ \"Phất lên\" → InputBinding(gesture:swipe_up) → cùng VirtualAction \"Tiến\".

4.6 Cài đặt phản hồi (Haptic & Audio)

- Rung: dùng VibrationEffect.createOneShot()/createWaveform() (Android), với amplitude tùy chỉnh trên thiết bị hỗ trợ Vibrator.hasAmplitudeControl().

- Âm thanh: SoundPool để phát âm thanh ngắn (\~50--100ms) với độ trễ thấp, preload các file âm thanh khi khởi động app.

- Cho phép người dùng import file âm thanh tùy chỉnh (.wav/.mp3 ngắn) cho từng nhóm nút.

- Chế độ \"Im lặng hoàn toàn\" (tắt cả rung & âm thanh) cho môi trường yên tĩnh.

5\. Đề xuất Framework & Mô hình AI Offline

5.1 Framework phát triển Android tổng thể

|  |  |  |
|----|----|----|
| **Hạng mục** | **Đề xuất chính** | **Lựa chọn thay thế** |
| Ngôn ngữ & UI | Kotlin + Jetpack Compose | Kotlin + XML Views (View Binding) |
| Kiến trúc | MVVM + Hilt (DI) + Coroutines/Flow | MVI + Koin |
| BLE | Nordic Android BLE Library (no-bluetooth-le) | Kable (Kotlin Multiplatform BLE) hoặc Android BluetoothGatt thuần |
| Lưu trữ | Room (config) + DataStore (settings) | Realm Database |
| Camera (cử chỉ) | CameraX | Camera2 API trực tiếp |

5.2 Nhận diện giọng nói tiếng Việt offline

|  |  |  |
|----|----|----|
| **Giải pháp** | **Ưu điểm** | **Lưu ý** |
| Vosk (vosk-android) | Mã nguồn mở, có mô hình tiếng Việt nhỏ (\~50MB) chạy tốt trên thiết bị tầm trung, hỗ trợ streaming real-time, độ trễ thấp | Độ chính xác trung bình với câu phức tạp, phù hợp tập lệnh giới hạn (lệnh điều khiển ngắn) |
| Whisper.cpp (mô hình tiny/base, quantized) qua thư viện whisper.android | Độ chính xác cao hơn cho tiếng Việt, cộng đồng lớn | Mô hình lớn hơn (75MB--150MB), độ trễ cao hơn Vosk, không phù hợp streaming liên tục, tốn pin/CPU hơn |
| Mô hình tự huấn luyện Keyword Spotting (TensorFlow Lite / Edge Impulse) | Cực nhẹ, độ trễ rất thấp, lý tưởng cho \"wake word\" và tập lệnh cố định nhỏ (\~10-30 từ) | Cần thu thập dữ liệu giọng nói tiếng Việt và huấn luyện riêng, kém linh hoạt khi thêm lệnh mới |

Khuyến nghị: dùng Vosk (mô hình tiếng Việt nhỏ \"vosk-model-small-vn\") làm engine chính cho nhận diện liên tục các câu lệnh điều khiển, kết hợp với một mô hình Keyword Spotting TFLite siêu nhẹ cho wake-word (đánh thức) để tiết kiệm pin khi ở chế độ Always-on.

5.3 Nhận diện cử chỉ tay offline

|  |  |  |
|----|----|----|
| **Giải pháp** | **Ưu điểm** | **Lưu ý** |
| MediaPipe Tasks --- Hand Landmarker + Gesture Recognizer (Google, on-device) | Tối ưu cho di động, chạy GPU/NNAPI delegate, độ trễ thấp (\~15-30ms/frame), có sẵn bộ nhận diện 7 cử chỉ cơ bản (Open Palm, Closed Fist, Pointing Up, Thumb Up/Down, Victory, ILoveYou) | Cần huấn luyện thêm lớp custom (MediaPipe Model Maker) nếu muốn thêm cử chỉ riêng (ví dụ \"phất lên\") |
| MediaPipe Hand Landmarker + State Machine tự viết (Kotlin) | Cử chỉ động (phất tay 4 hướng) dễ implement bằng cách theo dõi vector di chuyển của điểm cổ tay/lòng bàn tay qua các frame, không cần huấn luyện mô hình mới | Cần tinh chỉnh ngưỡng tốc độ/khoảng cách thủ công cho từng thiết bị |
| TensorFlow Lite custom CNN/LSTM (huấn luyện từ dữ liệu landmark thu thập) | Linh hoạt nhất, có thể nhận diện cử chỉ phức tạp/tùy chỉnh hoàn toàn theo yêu cầu người dùng | Cần quy trình thu thập + huấn luyện dữ liệu, phức tạp hơn cho người mới |

Khuyến nghị kiến trúc lai (hybrid): dùng MediaPipe Hand Landmarker (chạy on-device, GPU delegate) để lấy 21 điểm landmark mỗi frame → dùng MediaPipe Gesture Recognizer có sẵn cho các cử chỉ tĩnh (Fist, Open Palm, Thumbs Up/Down, Victory) → dùng state-machine tự viết bằng Kotlin để phát hiện cử chỉ động (phất 4 hướng, vẽ vòng tròn) dựa trên chuyển động landmark theo thời gian. Cách này không yêu cầu huấn luyện mô hình mới, triển khai nhanh và đủ đáp ứng yêu cầu.

5.4 Tóm tắt ngăn xếp công nghệ (Tech Stack) đề xuất cuối cùng

|  |  |
|----|----|
| **Thành phần** | **Lựa chọn** |
| Ngôn ngữ | Kotlin |
| UI Toolkit | Jetpack Compose + Material Design 3 |
| Kiến trúc | MVVM + Hilt + Coroutines/Flow |
| BLE Communication | Nordic Android BLE Library (UART Service / GATT custom) |
| Local Database | Room (mapping config, profiles) + Jetpack DataStore (settings) |
| Speech-to-Text offline | Vosk Android (mô hình tiếng Việt nhỏ) + tùy chọn TFLite Keyword Spotting cho wake-word |
| Gesture Recognition offline | MediaPipe Tasks (Hand Landmarker + Gesture Recognizer) + state machine Kotlin cho cử chỉ động |
| Camera | CameraX |
| Audio feedback | SoundPool |
| Haptic feedback | Vibrator/VibratorManager (Android 12+) với amplitude control |

6\. Lưu ý triển khai phía ESP32 (MicroPython)

- Sử dụng module \"aioble\" hoặc \"bluetooth\" built-in của MicroPython để cài đặt GATT Server với Nordic UART Service (UUID chuẩn) --- giúp Android library kết nối dễ dàng mà không cần custom UUID phức tạp.

- Thiết lập callback nhận dữ liệu (RX characteristic write) để parse chuỗi lệnh dạng \"KEY=VALUE\" (ví dụ \"U=1\", \"LX=45\") và điều khiển động cơ/servo tương ứng.

- Nên thiết kế sẵn cơ chế \"failsafe\": nếu không nhận lệnh trong X giây (mất kết nối), tự động dừng robot (gửi U=0, D=0\... mặc định) để đảm bảo an toàn.

- Có thể gửi dữ liệu phản hồi (sensor/pin/trạng thái) ngược lại app qua Notify Characteristic để hiển thị trên Debug Console.

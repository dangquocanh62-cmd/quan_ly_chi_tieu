# 💰 Quản Lý Chi Tiêu - Smart Personal Expense Tracker

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?logo=kotlin)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Android-SDK%2026%2B%20(Target%2035)-green.svg?logo=android)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Room](https://img.shields.io/badge/Room%20DB-2.6.1-orange.svg)](https://developer.android.com/training/data-storage/room)
[![Gemini AI](https://img.shields.io/badge/AI-Google%20Gemini-4285F4.svg?logo=google)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](LICENSE)

Ứng dụng quản lý tài chính cá nhân toàn diện, trực quan và bảo mật dành cho Android, được xây dựng hoàn toàn bằng **Kotlin** và **Jetpack Compose (Material 3)**. Ứng dụng tích hợp **Trợ lý AI thông minh (Gemini)** cho phép ghi chép chi tiêu tự động chỉ bằng một câu ngôn ngữ tự nhiên tiếng Việt.

---

## ✨ Tính năng nổi bật

### 1. 💵 Quản lý thu chi thông minh
- Thêm giao dịch nhanh chóng với đầy đủ thông tin: số tiền, danh mục, ví nguồn tiền, thời gian, ghi chú.
- Bộ danh mục phong phú: Ăn uống, Mua sắm, Di chuyển, Hóa đơn, Tiền lương, Thưởng,...
- Tìm kiếm, lọc giao dịch linh hoạt theo thời gian, danh mục và nguồn ví.

### 2. 🤖 Trợ lý AI & Nhập liệu bằng ngôn ngữ tự nhiên
- **Nhập liệu siêu tốc**: Chỉ cần nhập hoặc nói câu tự nhiên (VD: *"Ăn trưa phở bò 45k bằng tiền mặt"*, *"Mua áo sơ mi 350k qua thẻ tín dụng"*), ứng dụng tự động phân tích và trích xuất đúng số tiền, danh mục và ví.
- **Hệ thống xử lý kép**: Tích hợp **Google Gemini API** cho độ chính xác cao và **Local Rule-based Parser** dự phòng hoạt động mượt mà ngay cả khi không có kết nối mạng.
- **Tư vấn tài chính**: Đặt câu hỏi cho AI để nhận lời khuyên tối ưu chi tiêu và tiết kiệm.

### 3. 👛 Quản lý đa ví (Multi-Wallet)
- Tạo và theo dõi nhiều tài khoản/ví: Tiền mặt, Tài khoản ngân hàng, Ví điện tử (MoMo, ZaloPay, Viettel Money), Thẻ tín dụng.
- Dễ dàng thực hiện và ghi nhận các giao dịch chuyển tiền qua lại giữa các ví.
- Thống kê biến động số dư và tổng tài sản hiện hữu theo thời gian thực.

### 4. 📊 Báo cáo & Thống kê tài chính chuyên sâu
- Biểu đồ phân bổ chi tiêu trực quan theo từng danh mục.
- So sánh tỷ lệ thu nhập - chi tiêu theo tuần, tháng và năm.
- Xu hướng biến động dòng tiền giúp bạn kiểm soát thói quen tiêu dùng.

### 5. 🎯 Quản lý ngân sách (Budgeting)
- Thiết lập hạn mức ngân sách linh hoạt cho từng danh mục chi tiêu theo tháng.
- Theo dõi tiến độ chi tiêu theo thời gian thực với thanh tiến trình trực quan.
- Cảnh báo kịp thời khi chi tiêu chạm ngưỡng 80% hoặc vượt quá hạn mức ngân sách.

### 6. 🤝 Sổ theo dõi Vay & Nợ (Debt & Loan Tracker)
- Ghi nhận chi tiết các khoản **Đi vay** và **Cho vay**.
- Quản lý trạng thái hoàn trả, hạn trả và số tiền còn lại.
- Tránh tình trạng quên hoặc thất thoát các khoản tiền giao dịch cá nhân.

### 7. 🍕 Chia tiền nhóm (Split Bills)
- Tính toán chia tiền ăn uống, liên hoan, du lịch theo nhóm cực kỳ tiện lợi.
- Hỗ trợ chia đều hoặc chia theo từng người tham gia.

### 8. ⏰ Nhắc nhở định kỳ (WorkManager)
- Thông báo nhắc nhở ghi chép chi tiêu đều đặn mỗi tối giúp duy trì thói quen quản lý tài chính.
- Sử dụng Android WorkManager đảm bảo hoạt động tin cậy và tiết kiệm pin.

### 9. 🔒 Bảo mật & Quyền riêng tư
- Dữ liệu hoàn toàn được lưu trữ cục bộ (Offline-first) trên thiết bị của bạn với **Room Database**.
- Gemini API Key được mã hóa an toàn qua **AndroidX EncryptedSharedPreferences**, tuyệt đối không gửi về máy chủ trung gian.

---

## 🛠️ Công nghệ & Kiến trúc

| Thành phần | Công nghệ sử dụng |
|---|---|
| **Ngôn ngữ** | Kotlin 100% (2.0.21) |
| **Giao diện (UI)** | Jetpack Compose + Material 3 Design Tokens |
| **Kiến trúc** | Clean Architecture + MVVM + Repository Pattern |
| **Xử lý bất đồng bộ** | Kotlin Coroutines + StateFlow / SharedFlow |
| **Cơ sở dữ liệu** | Room Database 2.6.1 + KSP |
| **Background Task** | AndroidX WorkManager |
| **Dependency Injection** | Manual Clean DI / ViewModel Factories |
| **Serialization** | Kotlinx Serialization + Gson |
| **Trí tuệ nhân tạo** | Google Gemini API + Custom Local NLP Fallback |
| **Bảo mật** | AndroidX Security Crypto (EncryptedSharedPreferences) |
| **Build Tool** | Gradle 8.11 (Kotlin DSL) + Version Catalog (`libs.versions.toml`) |

---

## 📁 Cấu trúc dự án

```text
app/src/main/kotlin/com/quanlychitieuapp/
├── ai/                     # Gemini AI Parser & Local Parser fallback
│   ├── GeminiParser.kt
│   ├── LocalParser.kt
│   └── ParserResult.kt
├── data/                   # Data Layer (Database, Entities, DAOs, Repositories)
│   ├── local/              # Room AppDatabase & TypeConverters
│   ├── model/              # Domain Models & Data Entities
│   └── repository/         # Repository Implementations
├── domain/                 # Domain Layer (Use cases & Business rules)
├── notification/           # Notification Channel & WorkManager Daily Reminder
├── ui/                     # UI Layer (Jetpack Compose Screens & ViewModels)
│   ├── budget/             # Quản lý ngân sách
│   ├── chat/               # Chat tư vấn tài chính cùng AI
│   ├── components/         # UI Components dùng chung (Cards, Dialogs, Charts)
│   ├── debt/               # Quản lý sổ nợ / cho vay
│   ├── home/               # Dashboard tổng quan
│   ├── settings/           # Cài đặt ứng dụng & cấu hình API Key
│   ├── split/              # Chia tiền nhóm
│   ├── statistics/         # Báo cáo biểu đồ tài chính
│   ├── theme/              # Theme, Color Palette & Typography
│   ├── transaction/        # Thêm / sửa / danh sách thu chi
│   └── wallet/             # Quản lý ví tiền
└── utils/                  # Tiện ích định dạng tiền tệ, ngày tháng, mã hóa
```

---

## 🚀 Hướng dẫn cài đặt & Chạy ứng dụng

### 1. Yêu cầu hệ thống
- **Android Studio**: Android Studio Ladybug | 2024.2.1 hoặc mới hơn.
- **JDK**: Java 17 trở lên.
- **Thiết bị / Giả lập**: Android 8.0 (API 26) trở lên.

### 2. Các bước thực hiện

1. **Clone mã nguồn về máy**:
   ```bash
   git clone https://github.com/dangquocanh62-cmd/quan_ly_chi_tieu.git
   cd quan_ly_chi_tieu
   ```

2. **Mở dự án trong Android Studio**:
   - Chọn **File** > **Open...** và duyệt đến thư mục `quan_ly_chi_tieu`.
   - Chờ Gradle Sync hoàn tất các dependencies.

3. **Build & Chạy ứng dụng**:
   - Nhấn nút **Run** (Shift + F10) trên thanh công cụ của Android Studio.
   - Hoặc biên dịch bằng dòng lệnh:
     - **Windows**:
       ```cmd
       gradlew.bat assembleDebug
       ```
     - **macOS / Linux**:
       ```bash
       ./gradlew assembleDebug
       ```

### 3. Cấu hình Gemini AI (Tùy chọn)
Để kích hoạt tính năng nhận diện ngôn ngữ tự nhiên thông minh bằng Gemini AI:
1. Lấy API key miễn phí tại [Google AI Studio](https://aistudio.google.com/).
2. Mở ứng dụng, vào tab **Cài đặt** (`Settings`).
3. Nhập API Key vào mục **Gemini API Key** và lưu lại.
4. Key sẽ được mã hóa và lưu trữ an toàn trong vùng nhớ bảo mật của thiết bị.

---

## 📄 Bản quyền (License)

Dự án được phân phối dưới giấy phép [MIT License](LICENSE).
Tác giả: [dangquocanh62-cmd](https://github.com/dangquocanh62-cmd).

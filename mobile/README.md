# SLIB Mobile App

Ứng dụng di động cho hệ thống **SLIB Smart Library** - Dành cho sinh viên đặt chỗ thư viện.

![Flutter](https://img.shields.io/badge/Flutter-3.x-02569B?style=flat-square&logo=flutter)
![Dart](https://img.shields.io/badge/Dart-3.9-0175C2?style=flat-square&logo=dart)
![Android](https://img.shields.io/badge/Android-Ready-3DDC84?style=flat-square&logo=android)
![iOS](https://img.shields.io/badge/iOS-Ready-000000?style=flat-square&logo=apple)

---

## Tổng quan

Ứng dụng Flutter cross-platform cho sinh viên, cung cấp các tính năng:

- **Đăng nhập**: Email/Password + Google Sign-In
- **Xem sơ đồ thư viện**: Sơ đồ tầng với trạng thái ghế realtime
- **Đặt chỗ**: Chọn ngày, giờ, ghế và đặt chỗ
- **Check-in/Check-out**: Quét QR hoặc nhập mã
- **Lịch sử đặt chỗ**: Xem lịch sử và hủy booking
- **Chat AI**: Hỏi đáp với AI Assistant
- **Tin tức**: Xem thông báo từ thư viện
- **Push Notifications**: Nhắc nhở check-in, thông báo

---

## Cấu trúc dự án

```
mobile/
├── android/                     # Android native code
├── ios/                         # iOS native code
├── lib/
│   ├── assets/                  # Asset helpers
│   ├── core/                    # Core utilities
│   ├── models/                  # Data models (14 files)
│   │   ├── user.dart
│   │   ├── booking.dart
│   │   ├── seat.dart
│   │   ├── area.dart
│   │   └── ...
│   ├── services/                # API & Business logic (9 files)
│   │   ├── api_service.dart     # HTTP client
│   │   ├── auth_service.dart    # Authentication
│   │   ├── booking_service.dart # Booking APIs
│   │   ├── websocket_service.dart # Real-time updates
│   │   └── ...
│   ├── views/                   # UI Screens (43 files)
│   │   ├── auth/                # Login, Register screens
│   │   ├── home/                # Home, Dashboard
│   │   ├── booking/             # Floor plan, Seat selection
│   │   ├── chat/                # AI Chat screen
│   │   ├── news/                # News list & detail
│   │   └── profile/             # User profile
│   ├── main.dart                # App entry point
│   ├── main_screen.dart         # Main navigation
│   └── firebase_options.dart    # Firebase config
├── assets/
│   ├── images/                  # App images
│   └── fonts/                   # Custom fonts
├── pubspec.yaml                 # Dependencies
├── firebase.json                # Firebase config
└── test/                        # Unit tests
```

---

## Tech Stack

| Thành phần | Công nghệ |
|------------|-----------|
| **Framework** | Flutter 3.x |
| **Language** | Dart 3.9 |
| **State Management** | Provider |
| **HTTP Client** | http package |
| **Auth** | Google Sign-In + Flutter Secure Storage |
| **Push Notification** | Firebase Messaging |
| **WebSocket** | STOMP Dart Client |
| **QR Code** | Mobile Scanner, QR Flutter |
| **HTML Rendering** | flutter_widget_from_html |
| **Image Caching** | Cached Network Image |
| **Barcode** | barcode_widget |

---

## Cài đặt và Chạy

### Yêu cầu
- **Flutter SDK 3.x** ([Cài đặt Flutter](https://flutter.dev/docs/get-started/install))
- **Android Studio** hoặc **Xcode**
- **JDK 11+** (cho Android)

### Cài đặt

```bash
# Clone và lấy dependencies
cd mobile
flutter pub get
```

### Chạy ứng dụng

```bash
# Xem thiết bị đang kết nối
flutter devices

# Chạy trên Android
flutter run -d android

# Chạy trên iOS
flutter run -d ios

# Chạy trên web (debug)
flutter run -d chrome
```

### Build Release

```bash
# Android APK
flutter build apk --release

# Android App Bundle (Google Play)
flutter build appbundle --release

# iOS
flutter build ios --release
```

---

## Configuration

### API Configuration

Cập nhật trong `lib/services/api_service.dart`:

```dart
// Production
static const String baseUrl = 'https://api.slib.com';

// Development
static const String baseUrl = 'http://localhost:8080';
```

### Firebase Setup

1. Tạo project trên [Firebase Console](https://console.firebase.google.com)
2. Thêm ứng dụng Android/iOS
3. Download `google-services.json` (Android) và `GoogleService-Info.plist` (iOS)
4. Đặt vào thư mục tương ứng

---

## Screens chính

| Screen | Mô tả |
|--------|-------|
| **LoginScreen** | Đăng nhập Email/Google |
| **RegisterScreen** | Đăng ký tài khoản |
| **HomeScreen** | Dashboard, thông tin nhanh |
| **FloorPlanScreen** | Sơ đồ thư viện, chọn ghế |
| **BookingHistoryScreen** | Lịch sử đặt chỗ |
| **BookingDetailScreen** | Chi tiết booking, QR code |
| **CheckInScreen** | Quét QR check-in |
| **ChatScreen** | Chat với AI Assistant |
| **NewsListScreen** | Danh sách tin tức |
| **NewsDetailScreen** | Chi tiết bài viết |
| **ProfileScreen** | Thông tin cá nhân |
| **SettingsScreen** | Cài đặt ứng dụng |

---

## Custom Fonts

Ứng dụng sử dụng custom fonts:

- **Sekuya** - Display font
- **Geom** - Variable font

Cấu hình trong `pubspec.yaml` và sử dụng:

```dart
Text(
  'SLIB',
  style: TextStyle(fontFamily: 'Sekuya'),
)
```

---

## Push Notifications

Firebase Cloud Messaging được cấu hình cho:

- Nhắc nhở check-in trước giờ booking
- Thông báo khi booking sắp hết hạn
- Tin tức mới từ thư viện
- Cập nhật reputation points

---

## Testing

```bash
# Chạy unit tests
flutter test

# Chạy với coverage
flutter test --coverage

# Xem coverage report
genhtml coverage/lcov.info -o coverage/html
open coverage/html/index.html
```

---

## Checklist trước khi release

- [ ] Cập nhật version trong `pubspec.yaml`
- [ ] Test trên nhiều devices
- [ ] Kiểm tra Firebase Analytics
- [ ] Test push notifications
- [ ] Optimize app size
- [ ] Update screenshots cho stores

---

## Related Packages

```yaml
dependencies:
  http: ^1.2.0                    # HTTP requests
  flutter_secure_storage: ^9.0.0  # Secure token storage
  firebase_messaging: ^15.0.0     # Push notifications
  firebase_core: ^3.0.0           # Firebase core
  provider: ^6.0.5                # State management
  google_sign_in: ^6.1.4          # Google OAuth
  cached_network_image: ^3.4.1    # Image caching
  qr_flutter: ^4.1.0              # QR code generation
  mobile_scanner: ^7.1.4          # QR code scanning
  web_socket_channel: ^2.4.0      # WebSocket
  stomp_dart_client: ^2.0.0       # STOMP protocol
```

---

## License

© 2024 SLIB Team. All rights reserved.

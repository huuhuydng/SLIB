# Views Structure Documentation

Cấu trúc thư mục views theo feature-based architecture.

## Nguyên tắc tổ chức

Mỗi feature có **thư mục riêng** với cấu trúc:
```
feature_name/
├── README.md              # Documentation của feature
├── feature_screen.dart    # Màn hình chính
└── widgets/              # Widgets riêng của feature
    ├── widget_1.dart
    ├── widget_2.dart
    └── ...
```

## Danh sách Features

### 🏠 Home (`home/`)
Màn hình chính với dashboard, tin tức, và quick actions.
- **Screen**: `home_screen.dart`
- **Widgets**: 8 widgets (appbar, dashboard, booking card, quick actions, AI card, news slider, compact header, section title)

### 📅 Booking (`booking/`)
Tính năng đặt chỗ ngồi trong thư viện.
- **Screen**: `booking_screen.dart`
- **Widgets**: room_selector, floor_selector, seat_grid, time_picker, booking_summary (sẽ phát triển)

### ✅ Check-in (`checkin/`)
Tính năng check-in bằng QR code.
- **Screen**: `checkin_screen.dart`
- **Widgets**: qr_scanner, checkin_success, checkin_history (sẽ phát triển)

### 🗺️ Map (`map/`)
Xem sơ đồ thư viện và trạng thái ghế.
- **Screen**: `map_screen.dart`
- **Widgets**: floor_map, room_info, seat_status, legend (sẽ phát triển)

### 📜 History (`history/`)
Lịch sử hoạt động và thống kê.
- **Screen**: `history_screen.dart`
- **Widgets**: history_filter, history_item, history_timeline, stats_card (sẽ phát triển)

### 📰 News (`news/`)
Tin tức và thông báo thư viện.
- **Screen**: `news_screen.dart`, `news_detail_screen.dart`
- **Widgets**: `news_item.dart`

### 💬 Chat (`chat/`)
Chat hỗ trợ với quản trị viên.
- **Screen**: `chat_screen.dart`
- **Widgets**: chat_message, chat_input, chat_header (sẽ phát triển)

### ⚙️ Menu (`menu/`)
Menu cài đặt và profile.
- **Screen**: `menu_screen.dart`
- **Widgets**: menu_item, profile_header, settings_section (sẽ phát triển)

### 🔐 Authentication (`authentication/`)
Đăng nhập, đăng ký, quên mật khẩu.

### 💳 Card (`card/`)
Thẻ thư viện điện tử.

### 🧩 Widgets (`widgets/`)
Shared widgets dùng chung cho nhiều feature.

## Quy tắc đặt tên

1. **Screen files**: `{feature}_screen.dart`
   - Ví dụ: `booking_screen.dart`, `home_screen.dart`

2. **Widget files**: `{widget_name}.dart`
   - Ví dụ: `seat_grid.dart`, `room_selector.dart`

3. **Class names**: PascalCase
   - Ví dụ: `BookingScreen`, `SeatGrid`, `RoomSelector`

## Import paths

```dart
// Màn hình
import 'package:slib/views/booking/booking_screen.dart';

// Widget của feature
import 'package:slib/views/booking/widgets/seat_grid.dart';

// Shared widget
import 'package:slib/views/widgets/custom_button.dart';
```

## Lợi ích

✅ **Dễ tìm kiếm**: Biết feature thì biết ngay folder
✅ **Dễ bảo trì**: Code liên quan nhau ở cùng 1 chỗ
✅ **Tái sử dụng**: Widgets trong `widgets/` của feature có thể share
✅ **Scale tốt**: Thêm feature mới = tạo folder mới
✅ **Team work**: Mỗi người có thể làm 1 feature riêng
✅ **Clean code**: Không còn file `others/` chứa đủ thứ

## Khi nào tạo feature mới?

Tạo folder feature mới khi:
- Có màn hình riêng biệt
- Có logic nghiệp vụ riêng
- Có ít nhất 3-4 widgets riêng
- Có thể phát triển độc lập

## Navigation

Khi navigate giữa các feature:
```dart
// Từ Home -> Booking
Navigator.push(
  context,
  MaterialPageRoute(
    builder: (context) => const BookingScreen(),
  ),
);
```

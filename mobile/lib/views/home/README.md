# Home Feature

Màn hình chính của ứng dụng, hiển thị tổng quan về thư viện và các tính năng chính.

## Files
- `home_screen.dart` - Màn hình chính với scroll animation

## Widgets
- `home_appbar.dart` - AppBar với thông tin user và notification
- `live_status_dashboard.dart` - Dashboard hiển thị trạng thái thư viện (độ đông đúc, điểm uy tín)
- `upcoming_booking_card.dart` - Card hiển thị lịch đặt chỗ sắp tới
- `quick_action_grid.dart` - Grid 4 nút tiện ích nhanh
- `ai_suggestion_card.dart` - Card gợi ý từ AI về thời gian đặt chỗ
- `news_slider.dart` - Slider tin tức ngang
- `compact_header.dart` - Header thu gọn khi scroll xuống
- `section_title.dart` - Component tiêu đề section

## Dependencies
- Sử dụng `UserProfile` model
- Sử dụng `AppColors` từ assets

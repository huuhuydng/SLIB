# SLIB System Features Reference

Tài liệu cấu trúc đầy đủ các chức năng hệ thống SLIB Smart Library, phục vụ agent khi cần hiểu và code theo đúng nghiệp vụ.

## System Actors

| Actor | Platform | Mô tả |
|-------|----------|-------|
| **Admin** | Web | Quản lý hạ tầng kỹ thuật, cấu hình hệ thống, quản lý người dùng, thiết kế bản đồ thư viện |
| **Librarian** | Web | Giám sát hoạt động hàng ngày, quản lý đặt chỗ, xử lý phản hồi/khiếu nại, thống kê |
| **Student** | Mobile | Đặt chỗ, check-in/out, chat AI, xem tin tức, gửi phản hồi |

## Modules Overview

| # | Module | Số chức năng | Actors |
|---|--------|-------------|--------|
| 1 | Authentication | 3 | All |
| 2 | Account Management | 9 | All |
| 3 | User Management | 7 | Admin |
| 4 | System Configuration | 39 | Admin |
| 5 | Booking Seat | 14 | Student, Librarian |
| 6 | Library Access | 4 | Student, Admin, Librarian |
| 7 | Reputation & Violation | 10 | Student, Librarian |
| 8 | Feedback | 8 | Student, Admin, Librarian |
| 9 | Notification | 4 | Student, Librarian |
| 10 | News & Announcement | 10 | Student, Librarian |
| 11 | Chat & Support | 7 | Student, Librarian |
| 12 | Statistics & Report | 8 | Librarian |

---

## Module 1: Authentication

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-01 | Log in via Google Account | All | Web + Mobile | Web: "Đăng nhập với Google", Mobile: "Tiếp tục với Google" |
| FE-02 | Log in via SLIB Account | Student | Mobile | "Email FPT hoặc MSSV", "Mật khẩu", "Ghi nhớ đăng nhập", "Đăng nhập" |
| FE-03 | Log out | All | Web + Mobile | Web: "Đăng xuất" (Header dropdown), Mobile: Tab "Thêm" -> "Đăng xuất" |

**Business Logic:**
- Web login: Chỉ Admin/Librarian qua Google (@fpt.edu.vn staff email)
- Mobile login: Chỉ Student (K18 trở về trước: @fe.edu.vn, K19 trở đi: FEID/Personal email)
- SLIB Account login: Chỉ Student K18 trở đi mới được dùng
- Logout: Invalidate token, clear session, redirect về Login Screen

---

## Module 2: Account Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-04 | View profile | All | Web + Mobile | "Hồ sơ cá nhân" |
| FE-05 | Change basic profile | All | Web + Mobile | "Họ và tên", "Số điện thoại", "Ngày sinh" |
| FE-06 | Change password | All | Web + Mobile | "Mật khẩu cũ", "Mật khẩu mới", "Xác nhận mật khẩu" |
| FE-07 | View Barcode | Student | Mobile | Tab "Card", "Mở rộng mã Barcode" |
| FE-08 | View history of activities | Student | Mobile | "Lịch sử hoạt động" |
| FE-09 | View account setting | Student | Mobile | "Cài đặt" |
| FE-10 | Turn on/off notification | Student | Mobile | Toggle "Thông báo" |
| FE-11 | Turn on/off AI suggestion | Student | Mobile | Toggle "Gợi ý AI" |
| FE-12 | Turn on/off HCE feature | Student | Mobile | Toggle "HCE" |

---

## Module 3: User Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-13 | View list of users | Admin | Web | "Người dùng", "Email", "Vai trò", "Trạng thái", "Hoạt động gần nhất", "Thao tác" |
| FE-14 | Import Student via file | Admin | Web | "Import CSV", "Kéo thả file CSV/Excel vào đây", "Chọn file" |
| FE-15 | Download template | Admin | Web | "Tải template mẫu (.xlsx)" |
| FE-16 | Add Librarian | Admin | Web | "Thêm thủ thư", "Họ và tên", "Email", "Mật khẩu tạm thời", "Tạo tài khoản" |
| FE-17 | View user details | Admin | Web | Chi tiết người dùng |
| FE-18 | Change user status | Admin | Web | "Khóa tài khoản", "Lý do khóa tài khoản" |
| FE-19 | Delete user account | Admin | Web | "Xóa tài khoản" |

**Business Logic:**
- Import: Chỉ .csv hoặc .xlsx, check duplicate Student ID & Email
- Add Librarian: Email phải @fpt.edu.vn, unique, tạo với role LIBRARIAN, status ACTIVE
- Lock account: Bắt buộc nhập lý do, Admin không thể tự khóa chính mình, terminate active sessions

---

## Module 4: System Configuration

### 4.1 Area Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-20 | View area map | Admin | Web | "Bản đồ thư viện", "Xem trước", "Tên phòng thư viện", "Trạng thái" |
| FE-21 | CRUD area | Admin | Web | "Chỉnh sửa", "+Phòng", "Lưu", "Xóa phòng" |
| FE-22 | Change area status | Admin | Web | "Hoạt động" / "Đóng cửa" |
| FE-23 | Lock area movement | Admin | Web | "Mở khóa" / "Đã khóa" |

### 4.2 Zone Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-24 | View zone map | Admin | Web | "Khu vực ghế", "Tên khu vực", "Mô tả / Quy định" |
| FE-25 | CRUD zone | Admin | Web | "+Khu vực", "Xóa khu vực" |
| FE-26 | CRUD zone attribute | Admin | Web | "Tiện ích khu vực", "+Thêm", "Tên tiện ích..." |
| FE-27 | View zone details | Admin | Web | "Cài đặt", "Tiện ích khu vực" |
| FE-28 | Lock zone movement | Admin | Web | "Đã khóa vị trí" toggle |

### 4.3 Seat Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-29 | View seat map | Admin | Web | Seat IDs (A1, A2...) trên canvas |
| FE-30 | CRUD seat | Admin | Web | "+Ghế", kéo thả trên canvas |
| FE-31 | Change seat status | Admin | Web | Toggle trạng thái ghế |

### 4.4 Reputation Rule Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-32 | View list of reputation rules | Admin | Web | "Quy tắc uy tín" |
| FE-33 | CRUD reputation rule | Admin | Web | Tạo/Sửa/Xóa quy tắc |
| FE-34 | Set deducted point | Admin | Web | "Điểm trừ" cho mỗi quy tắc |

### 4.5 Library Configuration Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-35 | Set operating hours | Admin | Web | "Giờ mở cửa", "Giờ đóng cửa" |
| FE-36 | Configure booking rules | Admin | Web | Cấu hình quy tắc đặt chỗ |
| FE-37 | Auto check-out toggle | Admin | Web | "Tự động check-out khi hết giờ" |
| FE-38 | Enable/Disable library | Admin | Web | Bật/Tắt thư viện |

### 4.6 HCE Device Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-39 | View HCE devices | Admin | Web | Danh sách thiết bị HCE |
| FE-40 | CRUD HCE device | Admin | Web | Tạo/Sửa/Xóa thiết bị |
| FE-41 | View HCE device details | Admin | Web | Chi tiết thiết bị HCE |

### 4.7 AI Configuration Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-42 | CRUD material | Admin | Web | Quản lý tài liệu AI |
| FE-43 | View list of materials | Admin | Web | Danh sách tài liệu |
| FE-44 | CRUD knowledge store | Admin | Web | Quản lý kho kiến thức |
| FE-45 | View knowledge stores | Admin | Web | Danh sách kho kiến thức |
| FE-46 | Test AI chat | Admin | Web | "Thử nghiệm AI" |

### 4.8 NFC Device Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-47 | CRUD NFC device | Admin | Web | Quản lý thiết bị NFC |
| FE-48 | View NFC devices | Admin | Web | Danh sách thiết bị NFC |
| FE-49 | View NFC device details | Admin | Web | Chi tiết thiết bị NFC |

### 4.9 Kiosk Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-50 | View Kiosk images | Admin | Web | Danh sách ảnh Kiosk |
| FE-51 | CRUD Kiosk image | Admin | Web | Quản lý ảnh Kiosk |
| FE-52 | Change image status | Admin | Web | Thay đổi trạng thái ảnh |
| FE-53 | Preview Kiosk display | Admin | Web | "Xem trước" Kiosk |

### 4.10 Others

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-54 | Config system notification | Admin | Web | Cấu hình thông báo hệ thống |
| FE-55 | View system overview | Admin | Web | "Tổng quan hệ thống" |
| FE-56 | View system log | Admin | Web | "Nhật ký hệ thống" |
| FE-57 | Backup data manually | Admin | Web | "Sao lưu dữ liệu" |
| FE-58 | Auto backup schedule | Admin | Web | "Lịch sao lưu tự động" |

---

## Module 5: Booking Seat

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-59 | View real-time seat map | Student, Librarian | Mobile + Web | Bản đồ chỗ ngồi thời gian thực |
| FE-60 | Filter seat map | Student, Librarian | Mobile + Web | Lọc theo tiện ích, trạng thái |
| FE-61 | View map density | Student, Librarian | Mobile + Web | Bản đồ mật độ (heat map) |
| FE-62 | Booking seat | Student | Mobile | "Đặt chỗ" |
| FE-63 | Preview booking info | Student | Mobile | Xem trước thông tin đặt chỗ |
| FE-64 | Confirm booking via NFC | Student | Mobile | Xác nhận qua NFC tại ghế |
| FE-65 | View booking history | Student | Mobile | "Lịch sử đặt chỗ" |
| FE-66 | Cancel booking | Student | Mobile | "Hủy đặt chỗ" |
| FE-67 | AI seat recommendation | Student | Mobile | "Gợi ý chỗ ngồi AI" |
| FE-68 | Request seat extension | Student | Mobile | "Gia hạn thời gian" |
| FE-69 | View Student bookings list | Librarian | Web | Danh sách đặt chỗ sinh viên |
| FE-70 | Search/Filter bookings | Librarian | Web | Tìm kiếm và lọc đặt chỗ |
| FE-71 | View booking details | Librarian | Web | Chi tiết đặt chỗ |
| FE-72 | Cancel invalid booking | Librarian | Web | Hủy đặt chỗ không hợp lệ |

**Business Logic - Booking Flow:**
1. Student mở seat map -> chọn Area -> chọn Zone -> chọn Seat trống
2. Chọn thời gian (time slot) -> Xem preview -> Xác nhận đặt
3. Reservation status: `PROCESSING` -> `BOOKED`
4. Student đến thư viện, check-in (HCE/QR) -> `CONFIRMED`
5. Hết giờ hoặc student check-out -> kết thúc
6. Nếu không check-in đúng giờ -> `EXPIRED` + trừ điểm uy tín

---

## Module 6: Library Access

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-73 | Check-in/out via HCE | Student | Mobile | Tap NFC tại cổng thư viện |
| FE-74 | Check-in/out via QR | Student | Mobile | Quét QR code tại cổng |
| FE-75 | View check-in/out history | Student | Mobile | "Lịch sử ra vào" |
| FE-76 | View Students access list | Admin, Librarian | Web | Danh sách sinh viên ra vào |

**Business Logic:**
- HCE (Host Card Emulation): Điện thoại giả lập thẻ NFC, tap vào thiết bị tại cổng
- QR code: Quét mã QR động tại cổng thư viện
- Check-in tạo Access Log record, liên kết với Reservation hiện tại
- Auto check-out: Cron job tự động check-out khi hết thời gian booking

---

## Module 7: Reputation & Violation

### 7.1 Reputation Score Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-77 | View reputation score | Student | Mobile | "Điểm uy tín" |
| FE-78 | View reputation history | Student | Mobile | "Lịch sử thay đổi điểm" |
| FE-79 | View deduction reason | Student | Mobile | "Lý do trừ điểm" |
| FE-80 | View violations list | Librarian | Web | "Danh sách vi phạm" |
| FE-81 | View violation details | Librarian | Web | "Chi tiết vi phạm" |

### 7.2 Complaint Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-82 | Create complaint | Student | Mobile | "Gửi khiếu nại" |
| FE-83 | View complaint history | Student | Mobile | "Lịch sử khiếu nại" |
| FE-84 | View complaints list | Librarian | Web | "Danh sách khiếu nại" |
| FE-85 | View complaint details | Librarian | Web | "Chi tiết khiếu nại" |
| FE-86 | Verify complaint | Librarian | Web | "Chấp nhận" / "Từ chối" |

**Business Logic:**
- Reputation Score mặc định 100 điểm, bị trừ khi vi phạm (no-show, quá giờ, etc.)
- Mỗi Reputation Rule định nghĩa số điểm trừ cho từng loại vi phạm
- Student có thể khiếu nại nếu bị trừ sai -> Librarian xác nhận/từ chối

---

## Module 8: Feedback

### 8.1 Feedback System

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-87 | Create feedback after check-out | Student | Mobile | "Đánh giá sau khi rời" |
| FE-88 | View feedbacks list | Librarian | Web | "Danh sách phản hồi" |
| FE-89 | View feedback details | Librarian | Web | "Chi tiết phản hồi" |

### 8.2 Seat Status Report

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-90 | Create seat status report | Student | Mobile | "Báo cáo tình trạng ghế" |
| FE-91 | View report history | Student | Mobile | "Lịch sử báo cáo" |
| FE-92 | View reports list | Librarian | Web | "Danh sách báo cáo tình trạng" |
| FE-93 | View report details | Librarian | Web | "Chi tiết báo cáo" |
| FE-94 | Verify seat status report | Librarian | Web | "Xác nhận báo cáo" |

### 8.3 Report Seat Violation

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-95 | Create seat violation report | Student | Mobile | "Báo cáo vi phạm ghế" |
| FE-96 | View violation report history | Student | Mobile | "Lịch sử báo cáo vi phạm" |
| FE-97 | View violation reports list | Librarian | Web | "Danh sách vi phạm ghế" |
| FE-98 | View violation report details | Librarian | Web | "Chi tiết vi phạm" |
| FE-99 | Verify violation report | Librarian | Web | "Xác nhận vi phạm" |

---

## Module 9: Notification

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-100 | View/delete notifications | Student, Librarian | Mobile + Web | "Thông báo" |
| FE-101 | View notification details | Student, Librarian | Mobile + Web | "Chi tiết thông báo" |
| FE-102 | Filter notification | Student, Librarian | Mobile + Web | Lọc thông báo |
| FE-103 | Mark as read | Student, Librarian | Mobile + Web | "Đánh dấu đã đọc" |

**Non-Screen Function:** Notification Dispatcher - Background service gửi alerts, booking reminders, violation notices qua push notification.

---

## Module 10: News & Announcement

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-104 | View news list | Student, Librarian | Mobile + Web | "Tin tức & Thông báo" |
| FE-105 | View news details | Student, Librarian | Mobile + Web | "Chi tiết tin tức" |
| FE-106 | View categories | Librarian | Web | "Danh mục tin tức" |
| FE-107 | View new books | Student, Librarian | Mobile + Web | "Sách mới" |
| FE-108 | View book info | Student, Librarian | Mobile + Web | "Thông tin sách" |
| FE-109 | CRUD new book | Librarian | Web | Quản lý sách mới |
| FE-110 | CRUD news | Librarian | Web | Quản lý tin tức |
| FE-111 | CRUD category | Librarian | Web | Quản lý danh mục |
| FE-112 | Schedule post | Librarian | Web | "Hẹn giờ đăng" |
| FE-113 | Save draft | Librarian | Web | "Lưu bản nháp" |

---

## Module 11: Chat & Support

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-114 | Chat with AI assistant | Student | Mobile | "Trợ lý AI" |
| FE-115 | Chat with Librarian | Student | Mobile | "Chat với Thủ thư" |
| FE-116 | View chat history | Student | Mobile | "Lịch sử trò chuyện" |
| FE-117 | View chats list | Librarian | Web | "Danh sách hội thoại" |
| FE-118 | View chat details | Librarian | Web | "Chi tiết hội thoại" |
| FE-119 | Manual response | Librarian | Web | Trả lời thủ công |
| FE-120 | AI-suggested response | Librarian | Web | "Gợi ý AI" để trả lời |

**Business Logic:**
- AI Chat: Sử dụng RAG với knowledge store (Admin cấu hình)
- Librarian Chat: Real-time messaging qua WebSocket
- Librarian có thể dùng AI suggestion để soạn câu trả lời

---

## Module 12: Statistics & Report

### 12.1 Statistics Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-121 | General analytics dashboard | Librarian | Web | "Tổng quan phân tích" |
| FE-122 | Violation statistics | Librarian | Web | "Thống kê vi phạm" |
| FE-123 | AI density forecast | Librarian | Web | "Dự báo mật độ bằng AI" |
| FE-124 | Check-in/out statistics | Librarian | Web | "Thống kê ra vào" (Ngày/Tuần/Tháng) |
| FE-125 | Booking statistics | Librarian | Web | "Thống kê đặt chỗ" |
| FE-126 | Feedback analysis | Librarian | Web | "Phân tích phản hồi" |

### 12.2 Report Management

| ID | Chức năng | Actor | Platform | Vietnamese UI |
|----|-----------|-------|----------|---------------|
| FE-127 | Export seat & maintenance report | Librarian | Web | "Xuất báo cáo ghế" |
| FE-128 | Export general analytical report | Librarian | Web | "Xuất báo cáo phân tích" |

---

## Non-Screen Functions (Background Services)

| # | Service | Mô tả |
|---|---------|-------|
| 1 | Automatic Check-out Service | Cron job phát hiện booking hết hạn và tự động check-out |
| 2 | Automated Backup Job | Sao lưu dữ liệu tự động theo lịch Admin cấu hình |
| 3 | AI Sentiment Analysis | Gemini API phân tích và phân loại cảm xúc feedback sinh viên |
| 4 | AI Recommendation Engine | Phân tích mật độ ghế real-time + sở thích cá nhân để gợi ý chỗ ngồi |
| 5 | Notification Dispatcher | Service gửi push notifications, booking reminders, violation notices |
| 6 | Google OAuth API | Xác thực và quản lý token với Google |

---

## Core Business Flows

### Flow 1: Student Booking Journey
```
Mở app -> Login -> Chọn Area -> Chọn Zone -> Chọn Seat 
-> Chọn time slot -> Confirm -> [BOOKED]
-> Đến thư viện -> Check-in (HCE/QR) -> [CONFIRMED]  
-> Học tập -> Check-out (HCE/QR) -> Feedback (optional)
```

### Flow 2: Violation & Reputation
```
Student không check-in đúng giờ -> Auto cancel [EXPIRED]
-> System áp dụng Reputation Rule -> Trừ điểm uy tín
-> Student nhận notification -> Có thể gửi khiếu nại
-> Librarian xem xét khiếu nại -> Chấp nhận/Từ chối
```

### Flow 3: Library Setup (Admin)
```
Tạo Area (Phòng thư viện) -> Tạo Zone (Khu vực ghế) trong Area
-> Thêm tiện ích cho Zone -> Tạo Seat trong Zone
-> Gán NFC tag cho Seat -> Cấu hình booking rules
-> Cấu hình reputation rules -> Enable library
```

### Flow 4: Content Management (Librarian)
```
Tạo tin tức/thông báo -> Chọn danh mục -> Soạn nội dung
-> Lưu nháp hoặc hẹn giờ đăng -> Xuất bản
-> Student xem trên Mobile app
```

## Screen Authorization Summary

| Screen Group | Admin | Librarian | Student |
|-------------|-------|-----------|---------|
| Login (Web) | X | X | |
| Login (Mobile) | | | X |
| Dashboard (Admin) | X | | |
| Dashboard (Librarian) | | X | |
| Home (Mobile) | | | X |
| User Management | X | | |
| Infrastructure Canvas (Map Editor) | X | | |
| Library Configuration | X | | |
| Real-time Map / Monitoring | X | X | X |
| Booking (Student) | | | X |
| Booking Management (Librarian) | | X | |
| Check-in/Check-out | | | X |
| Access Management | X | X | |
| Violation & Complaints | | X | |
| Violation History (Student) | | | X |
| Reputation Points | | | X |
| Feedback & Reports | X | X | |
| Feedback (Student) | | | X |
| Notifications | X | X | X |
| News/Content Management | | X | |
| News (Read) | | | X |
| Chat (Librarian) | | X | |
| Chat (Student) | | | X |
| Analytics Dashboard | X | X | |
| Export Reports | | X | |
| System Logs & Backup | X | | |

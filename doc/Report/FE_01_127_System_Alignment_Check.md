# Đối chiếu FE-01 đến FE-127 với hệ thống hiện tại

Ngày rà soát: 08/04/2026  
Phạm vi rà soát: `backend/`, `frontend/`, `mobile/`, `ai-service/`, `backend/src/test/java/slib/com/example/controller`, `doc/Report/UnitTestReport/UnitTestHtml`

## Mục đích

Tài liệu này dùng để chốt với team rằng bộ mã chức năng `FE-01` đến `FE-127` vẫn là khung chuẩn đang follow trong dự án.

## Quy ước trạng thái

- `Đã có`: Chức năng đã có trong hệ thống và có mapping tương ứng.
- `Đã có, cần hiểu theo scope hiện tại`: Chức năng có trong hệ thống, nhưng wording hoặc phạm vi thực tế đang khác nhẹ so với tên FE ban đầu.
- `Cần bổ sung mã FE nếu muốn quản lý riêng`: Chức năng đã có trong hệ thống nhưng hiện chưa có mã FE riêng trong danh sách chuẩn.

## Kết luận tổng quát

- Bộ `FE-01` đến `FE-127` hiện **đủ khung** trong hệ thống.
- Có thể tiếp tục dùng bộ mã này làm chuẩn thống nhất cho team.
- Một số mã FE cần hiểu theo scope hiện tại, nổi bật là:
  - `FE-73`
  - `FE-81` đến `FE-85`
  - `FE-120`
  - `FE-121`
- Ngoài ra hệ thống hiện có thêm một số chức năng mới chưa được tách mã FE riêng, ví dụ luồng `rời ghế / trả chỗ`.

---

## 1. Authentication Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-01 | Log in via Google Account | Đã có | Đang dùng trong hệ thống, hiện hỗ trợ đăng nhập Google theo rule backend hiện tại |
| FE-02 | Log in via SLIB Account | Đã có | Có trên web và mobile |
| FE-03 | Forgot password | Đã có | Có trả lỗi chi tiết hơn ở backend/mobile |
| FE-04 | Log out | Đã có | Có trên web và mobile |

## 2. Account Management Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-05 | View profile | Đã có |  |
| FE-06 | Change basic profile | Đã có |  |
| FE-07 | Change password | Đã có |  |
| FE-08 | View Barcode | Đã có |  |
| FE-09 | View history of activities | Đã có |  |
| FE-10 | View account setting | Đã có |  |
| FE-11 | Turn on/Turn off notification | Đã có |  |
| FE-12 | Turn on/Turn off AI suggestion | Đã có |  |
| FE-13 | Turn on/Turn off HCE feature | Đã có |  |

## 3. User Management Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-14 | View list of users in the system | Đã có |  |
| FE-15 | Import Student and Teacher via file | Đã có | Đã đồng bộ wording trong report |
| FE-16 | Download template of the file upload | Đã có | Đã đồng bộ wording trong report |
| FE-17 | Add Librarian to the system | Đã có |  |
| FE-18 | View user details | Đã có |  |
| FE-19 | Change user status | Đã có |  |
| FE-20 | Delete user account | Đã có |  |

## 4. System Configuration Module

### 4.1 Area Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-21 | View area map | Đã có |  |
| FE-22 | CRUD area | Đã có |  |
| FE-23 | Change area status | Đã có |  |
| FE-24 | Lock area movement | Đã có |  |

### 4.2 Zone Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-25 | View zone map | Đã có |  |
| FE-26 | CRUD zone | Đã có |  |
| FE-27 | CRUD zone attribute | Đã có |  |
| FE-28 | View zone details | Đã có |  |
| FE-29 | Lock zone movement | Đã có |  |

### 4.3 Seat Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-30 | View seat map | Đã có |  |
| FE-31 | CRUD seat | Đã có |  |
| FE-32 | Change seat status | Đã có |  |

### 4.4 Reputation Rule Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-33 | View list of reputation rules | Đã có |  |
| FE-34 | CRUD reputation rule | Đã có |  |
| FE-35 | Set the deducted point for each reputation rule | Đã có |  |

### 4.5 Library Configuration Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-36 | Set library operating hours | Đã có |  |
| FE-37 | Configure booking rules | Đã có |  |
| FE-38 | Setting time for automatic check-out when time exceeds | Đã có | Đã đồng bộ wording trong report |
| FE-39 | Enable/Disable library | Đã có |  |

### 4.6 HCE Scan Station Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-40 | View HCE scan stations | Đã có |  |
| FE-41 | View HCE scan stations details | Đã có |  |
| FE-42 | Manage HCE station registration | Đã có |  |

### 4.7 AI Configuration Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-43 | CRUD material | Đã có |  |
| FE-44 | View list of materials | Đã có |  |
| FE-45 | CRUD knowledge store | Đã có |  |
| FE-46 | View list of knowledge stores | Đã có |  |
| FE-47 | Test AI chat | Đã có |  |

### 4.8 NFC Tag Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-48 | Manage NFC Tag UID mapping | Đã có |  |
| FE-49 | View NFC Tag mapping list | Đã có |  |
| FE-50 | View NFC Tag mapping details | Đã có |  |

### 4.9 Kiosk Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-51 | View list of Kiosk images | Đã có |  |
| FE-52 | CRUD Kiosk image | Đã có |  |
| FE-53 | Change image status | Đã có |  |
| FE-54 | Preview Kiosk display | Đã có |  |

### 4.10 Others

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-55 | Config system notification | Đã có |  |
| FE-56 | View system overview information | Đã có |  |
| FE-57 | View system log | Đã có |  |
| FE-58 | Backup data manually | Đã có |  |
| FE-59 | Set automatic backup schedule | Đã có |  |

## 5. Booking Seat Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-60 | View real time seat map | Đã có |  |
| FE-61 | Filter seat map | Đã có |  |
| FE-62 | View map density | Đã có |  |
| FE-63 | Booking seat | Đã có |  |
| FE-64 | Preview booking information | Đã có |  |
| FE-65 | Confirm booking via NFC | Đã có |  |
| FE-66 | View history of booking | Đã có |  |
| FE-67 | Cancel booking | Đã có |  |
| FE-68 | Ask AI for recommending seat | Đã có |  |
| FE-69 | View list of user bookings | Đã có |  |
| FE-70 | Search and Filter user booking | Đã có |  |
| FE-71 | View booking details and status | Đã có |  |

## 6. Library Access Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-72 | Check-in/Check-out library via HCE | Đã có |  |
| FE-73 | Check-in/Check-out library via QR code | Đã có, cần hiểu theo scope hiện tại | Hiện thực tế đang theo luồng QR của kiosk |
| FE-74 | View history of check-ins/check-outs | Đã có |  |
| FE-75 | View list of users access to library | Đã có |  |

## 7. Reputation & Violation Module

### 7.1 Reputation Score Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-76 | View reputation score | Đã có |  |
| FE-77 | View history of changed reputation points | Đã có |  |
| FE-78 | View detailed reason for deducting point | Đã có |  |
| FE-79 | View list of users violation | Đã có |  |
| FE-80 | View user violation details | Đã có |  |

### 7.2 Complaint Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-81 | Create complaint | Đã có, cần hiểu theo scope hiện tại | Hiện thực tế đang nghiêng về khiếu nại/kháng cáo vi phạm |
| FE-82 | View history of sending complaint | Đã có, cần hiểu theo scope hiện tại |  |
| FE-83 | View list of complaints | Đã có, cần hiểu theo scope hiện tại |  |
| FE-84 | View complaint details | Đã có, cần hiểu theo scope hiện tại |  |
| FE-85 | Verify complaint | Đã có, cần hiểu theo scope hiện tại |  |

## 8. Feedback Module

### 8.1 Feedback System Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-86 | Create feedback after check-out | Đã có |  |
| FE-87 | View list of feedbacks | Đã có |  |
| FE-88 | View feedback details | Đã có |  |

### 8.2 Seat Status Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-89 | Create seat status report | Đã có |  |
| FE-90 | View history of sending seat status report | Đã có |  |
| FE-91 | View list of seat status reports | Đã có |  |
| FE-92 | View seat status report details | Đã có |  |
| FE-93 | Verify seat status report | Đã có |  |

### 8.3 Report Seat Violation Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-94 | Create report seat violation | Đã có |  |
| FE-95 | View history of sending report seat violation | Đã có |  |
| FE-96 | View list of seat violation reports | Đã có |  |
| FE-97 | View report seat violation details | Đã có |  |
| FE-98 | Verify seat violation report | Đã có |  |

## 9. Notification Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-99 | View and delete list of notifications | Đã có | Web và mobile đều có, cách trình bày khác nhau theo nền tảng |
| FE-100 | View notification details | Đã có | Mobile có xem chi tiết rõ, web hiện thiên về xử lý nhanh hơn |
| FE-101 | Filter notification | Đã có |  |
| FE-102 | Mark notification as read | Đã có |  |

## 10. News & Announcement Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-103 | View list of news & announcements | Đã có |  |
| FE-104 | View news & announcement details | Đã có |  |
| FE-105 | View list of news & announcement categories | Đã có |  |
| FE-106 | View list of new books | Đã có |  |
| FE-107 | View basic information of new book | Đã có |  |
| FE-108 | CRUD new book | Đã có |  |
| FE-109 | CRUD news & announcement | Đã có |  |
| FE-110 | Create news & announcement category | Đã có |  |
| FE-111 | Set time to post news & announcement | Đã có |  |
| FE-112 | Save news & announcement draft | Đã có |  |

## 11. Chat & Support Module

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-113 | Chat with AI virtual assistant | Đã có |  |
| FE-114 | Chat with Librarian | Đã có |  |
| FE-115 | Send request for support | Đã có |  |
| FE-116 | View list of support requests | Đã có |  |
| FE-117 | View history of chat | Đã có |  |
| FE-118 | View list of chats | Đã có |  |
| FE-119 | View chat details | Đã có |  |
| FE-120 | Response to user manually | Đã có, cần hiểu theo scope hiện tại | Hiện đang bám vào phản hồi yêu cầu hỗ trợ thủ công |

## 12. Statistics & Report Module

### 12.1 Statistics Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-121 | View general analytics dashboard | Đã có, cần hiểu theo scope hiện tại | Dashboard hiện bao gồm cả phần AI analytics và panel mở rộng hơn tên cũ |
| FE-122 | View violation statistics | Đã có |  |
| FE-123 | View statistics of density forecast by using AI | Đã có |  |
| FE-124 | View check-in/check-out statistics (Daily/Weekly/Monthly) | Đã có |  |
| FE-125 | View seat booking statistics | Đã có |  |

### 12.2 Report Management

| Mã FE | Chức năng | Trạng thái | Ghi chú |
|---|---|---|---|
| FE-126 | Export seat & maintenance report | Đã có |  |
| FE-127 | Export general analytical report | Đã có |  |

---

## 13. Chức năng hiện có nhưng chưa có mã FE riêng trong danh sách chuẩn

Các chức năng dưới đây **đã có trong hệ thống**, nhưng hiện chưa được tách thành mã FE riêng trong bộ `FE-01..127`:

1. Luồng `rời ghế / trả chỗ ngồi`
   - Thủ thư trả chỗ từ web khi ghế đang `CONFIRMED`
   - Sinh viên quét NFC lần 2 để rời ghế
   - Hệ thống có `actualEndTime` để phân biệt rời ghế sớm với kết thúc tự động

2. Cảnh báo hành vi bằng AI ở dashboard thủ thư
   - `Sinh viên cần lưu ý`
   - xếp hạng `risk score`
   - gửi nhắc nhở trực tiếp từ panel

3. Các cải tiến chi tiết trên mobile
   - lịch sử báo cáo tách tab
   - lịch sử khiếu nại
   - chi tiết thông báo
   - trạng thái hạn chế đặt chỗ và thời gian còn lại

## 14. Đề xuất chốt với team

Nên chốt với team theo 3 ý:

1. Bộ `FE-01..127` vẫn là bộ mã chuẩn dùng xuyên suốt tài liệu, report và unit test.
2. Những mã có ghi `cần hiểu theo scope hiện tại` vẫn được xem là `implemented`, chỉ khác ở cách gọi tên hoặc phạm vi thực tế.
3. Nếu team muốn quản lý chặt các tính năng mới như `rời ghế / trả chỗ`, nên mở thêm mã FE mới ở đợt cập nhật tài liệu kế tiếp thay vì nhét vào FE cũ.

# SLIB Data Model Reference

Tài liệu mô tả data model hệ thống SLIB, bao gồm entities, quan hệ, status codes, và business rules.

## Entities Overview

| # | Entity | Mô tả | Liên kết chính |
|---|--------|-------|----------------|
| 1 | **User** | Tất cả người dùng (Student, Librarian, Admin) | Profile, Token, Setting, Reservation |
| 2 | **Profile** | Thông tin cá nhân chi tiết | User |
| 3 | **Token** | Session/authentication tokens | User |
| 4 | **Setting** | Preferences cá nhân và cấu hình app | User |
| 5 | **Schedule** | Lịch hoạt động thư viện | Library Config |
| 6 | **Activity Log** | Lịch sử hành động người dùng | User |
| 7 | **Point Transaction** | Thay đổi điểm uy tín (+/-) | User, Reputation Rule |
| 8 | **Reputation Rule** | Quy tắc trừ/cộng điểm | Point Transaction |
| 9 | **Reservation** | Đặt chỗ ngồi (booking) | User, Seat, Access Log |
| 10 | **Access Log** | Check-in/check-out timestamps | Reservation, User |
| 11 | **Feedback** | Đánh giá sau check-out | User, Reservation |
| 12 | **Notification** | Thông báo hệ thống | User |
| 13 | **Complaint** | Khiếu nại trừ điểm | User, Point Transaction |
| 14 | **News** | Tin tức, thông báo | Category |
| 15 | **Category** | Phân loại News | News |
| 16 | **New Book** | Sách mới nhập | - |
| 17 | **Area** | Phòng thư viện (tầng/khu lớn) | Zone |
| 18 | **Zone** | Khu vực ghế trong Area | Area, Seat, Amenity |
| 19 | **Seat** | Ghế ngồi cụ thể | Zone, Reservation |
| 20 | **HCE Device** | Thiết bị NFC tại cổng thư viện | - |
| 21 | **Amenity** | Tiện ích Zone (WiFi, ổ cắm, etc.) | Zone |
| 22 | **Factory** | Vật cản/nội thất cố định trên bản đồ | Area |
| 23 | **Conversation** | Thread chat Student-Librarian | User (Student + Librarian) |
| 24 | **Message** | Tin nhắn trong Conversation | Conversation |
| 25 | **AI Chat** | Session chat Student-AI | User (Student) |
| 26 | **AI Message** | Prompt/response trong AI Chat | AI Chat |

## Entity Relationships

```
User 1──N Profile
User 1──N Token
User 1──1 Setting
User 1──N Activity Log
User 1──N Point Transaction
User 1──N Reservation
User 1──N Access Log
User 1──N Feedback
User 1──N Notification
User 1──N Complaint
User 1──N Conversation (as Student or Librarian)
User 1──N AI Chat

Reputation Rule 1──N Point Transaction
Point Transaction 1──0..1 Complaint

Area 1──N Zone
Area 1──N Factory
Zone 1──N Seat
Zone N──N Amenity

Seat 1──N Reservation
Reservation 1──N Access Log
Reservation 1──0..1 Feedback

Category 1──N News

Conversation 1──N Message
AI Chat 1──N AI Message
```

## Status Codes

### User Status
| Status | Vietnamese | Mô tả |
|--------|-----------|-------|
| `ACTIVE` | Hoạt động | Tài khoản đang hoạt động bình thường |
| `LOCKED` | Đã khóa | Bị Admin khóa (có lý do) |

### User Roles
| Role | Vietnamese | Platform |
|------|-----------|----------|
| `ADMIN` | Quản trị viên | Web |
| `LIBRARIAN` | Thủ thư | Web |
| `STUDENT` | Sinh viên | Mobile |

### Reservation Status
| Status | Vietnamese | Mô tả |
|--------|-----------|-------|
| `PROCESSING` | Đang xử lý | Đang giữ ghế (holding) |
| `BOOKED` | Đã đặt | Chờ check-in |
| `CONFIRMED` | Đã xác nhận | Đã check-in tại ghế |
| `EXPIRED` | Hết hạn | Không check-in đúng giờ |
| `CANCELLED` | Đã hủy | Student hoặc Librarian hủy |

### Seat Status
| Status | Vietnamese | Mô tả |
|--------|-----------|-------|
| `AVAILABLE` | Trống | Có thể đặt |
| `HOLDING` | Đang giữ chỗ | Đang trong quá trình đặt |
| `BOOKED` | Đã đặt | Có reservation |
| `MAINTENANCE` | Bảo trì | Không thể đặt |

### Area/Zone Status
| Status | Vietnamese | Mô tả |
|--------|-----------|-------|
| `ACTIVE` | Hoạt động | Đang mở |
| `CLOSED` | Đóng cửa | Đang đóng |
| `LOCKED` | Đã khóa | Khóa di chuyển trên canvas |
| `UNLOCKED` | Mở khóa | Có thể di chuyển trên canvas |

### Complaint Status
| Status | Vietnamese | Mô tả |
|--------|-----------|-------|
| `PENDING` | Chờ xử lý | Mới gửi |
| `ACCEPTED` | Chấp nhận | Librarian chấp nhận, hoàn điểm |
| `DENIED` | Từ chối | Librarian từ chối |

### News Status
| Status | Vietnamese | Mô tả |
|--------|-----------|-------|
| `DRAFT` | Bản nháp | Chưa xuất bản |
| `PUBLISHED` | Đã đăng | Đang hiển thị |
| `SCHEDULED` | Hẹn giờ | Chờ đến giờ đăng |

## Business Rules

### Authentication Rules
- Staff (Admin, Librarian): Chỉ login qua Web bằng Google @fpt.edu.vn
- Student K18 trở về trước: Login bằng @fe.edu.vn qua Google
- Student K19 trở đi: Login bằng FEID (Personal email) hoặc SLIB Account
- Account phải ở trạng thái `ACTIVE` mới được login

### Booking Rules
- Student phải check-in thư viện trước khi đặt chỗ có hiệu lực
- Mỗi Student chỉ có thể có 1 reservation `BOOKED` hoặc `CONFIRMED` tại một thời điểm
- Xác nhận booking bằng NFC: Tap NFC tag gắn tại ghế
- Auto cancel nếu không check-in trong thời gian quy định (Admin cấu hình)
- Gia hạn chỉ được nếu không có booking tiếp theo cho ghế đó

### Reputation Rules
- Điểm mặc định: 100
- Vi phạm no-show (không check-in sau khi đặt): Trừ điểm theo Reputation Rule
- Student có thể khiếu nại trong thời hạn nhất định
- Librarian xác nhận khiếu nại -> hoàn trả điểm nếu chấp nhận
- Admin cấu hình số điểm trừ cho từng loại vi phạm

### Map Editor Rules
- Cấu trúc phân cấp: Area > Zone > Seat
- Area = Phòng thư viện (chứa nhiều Zone và Factory)
- Zone = Khu vực ghế (chứa nhiều Seat, có Amenity)
- Seat = Ghế ngồi cụ thể (có Seat ID unique trong room)
- Factory = Vật cản/nội thất cố định (bàn, tường, cột)
- Tất cả đều có tọa độ X, Y, Width, Height trên canvas
- Lock movement: Ngăn kéo thả trên canvas khi đã sắp xếp xong

### Notification Rules
- Push notification qua Firebase Cloud Messaging
- Trigger events: Booking confirmed, booking expired, reputation change, new news
- Student có thể bật/tắt notification trong Settings

## Technology Mapping

| Layer | Technology | Mô tả |
|-------|-----------|-------|
| Mobile App | Flutter (Dart) | Student-facing app |
| Frontend Web | React 19 | Admin + Librarian dashboard |
| Backend API | Spring Boot 3.4 (Java 21) | REST API, business logic |
| AI Service | FastAPI (Python 3.11) | RAG chatbot, AI recommendations |
| Database | PostgreSQL | Primary data store |
| Cache | Redis | Session, real-time data |
| File Storage | Cloudinary | Images, avatars |
| Auth | Google OAuth 2.0 | SSO authentication |
| Push Notification | Firebase Cloud Messaging | Mobile push |
| NFC/HCE | Android HCE API | Contactless check-in/out |
| Migration | Flyway | Database versioning |
| Container | Docker + Docker Compose | Development & deployment |
| CI/CD | GitHub Actions | Automated build & test |
| Deployment | AWS | Production server |

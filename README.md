# SLIB

SLIB (Smart Library) là hệ thống quản lý thư viện thông minh gồm 4 thành phần chính:

- `backend/`: Spring Boot API, WebSocket, nghiệp vụ đặt chỗ, check-in, reputation, notification
- `frontend/`: web portal cho `ADMIN` và `LIBRARIAN`
- `mobile/`: ứng dụng Flutter cho sinh viên
- `ai-service/`: FastAPI service cho AI chat, RAG và escalation

## 1. Kiến trúc tổng quát

### Thành phần

- `backend`
  Xử lý toàn bộ nghiệp vụ chính: đặt chỗ, xác nhận ghế, check-in/check-out thư viện, cấu hình hệ thống, sơ đồ thư viện, notification, analytics.

- `frontend`
  Giao diện quản trị cho thủ thư và quản trị viên: cấu hình thư viện, quản lý sơ đồ, chatbot test, dashboard, booking, user management.

- `mobile`
  Giao diện cho sinh viên: xem sơ đồ ghế, đặt chỗ, chat AI, xem booking sắp tới, check notification, xác nhận ghế qua NFC.

- `ai-service`
  Xử lý AI chatbot, knowledge base, vector search, debug RAG, escalation sang thủ thư.

### Hạ tầng chính

- PostgreSQL: dữ liệu nghiệp vụ chính
- MongoDB: lịch sử chat AI
- Qdrant: vector database cho RAG
- Redis: cache và hỗ trợ realtime
- WebSocket/STOMP: đồng bộ trạng thái ghế và dashboard realtime

### Domain production

- Frontend: `https://slibsystem.site`
- Backend: `https://api.slibsystem.site`

## 2. Công nghệ sử dụng

### Backend

- Java 21
- Spring Boot 3.4
- Spring Security
- JPA / Hibernate
- Flyway
- STOMP WebSocket

### Frontend

- React 19
- Vite 7
- CSS thuần
- Axios

### Mobile

- Flutter
- Provider
- HTTP
- Firebase Messaging
- NFC / QR integration

### AI Service

- Python
- FastAPI
- Qdrant
- MongoDB

## 3. Cấu trúc thư mục

```text
slib/
├── backend/
├── frontend/
├── mobile/
├── ai-service/
├── docs/
├── docker-compose.yml
└── README.md
```

## 4. Tính năng chính

- Đăng nhập bằng Google hoặc tài khoản SLIB
- Đặt chỗ theo ngày, khung giờ và khu vực
- Xem sơ đồ ghế theo thời gian thực
- Check-in/check-out thư viện bằng HCE hoặc QR
- Xác nhận ghế bằng NFC sau khi đã check-in thư viện
- AI chatbot hỗ trợ và escalation sang thủ thư
- Quản lý sơ đồ thư viện với draft, publish và schedule publish
- Gửi cảnh báo khi booking bị ảnh hưởng bởi thay đổi sơ đồ
- Reputation, complaint, feedback và seat violation
- Dashboard thống kê và giám sát vận hành

## 5. Yêu cầu môi trường

### Bắt buộc

- Java 21
- Node.js 20+
- npm
- Flutter SDK
- Python 3.11+
- Docker và Docker Compose

### Khuyến nghị

- Android Studio
- VS Code hoặc IntelliJ IDEA
- Postman hoặc Bruno để test API

## 6. Chạy local

### 6.1. Chạy hạ tầng phụ trợ

Từ root project:

```bash
docker-compose up -d
```

Lệnh này thường dùng để khởi động các service như database, Redis, MongoDB, Qdrant tùy theo cấu hình hiện tại.

### 6.2. Chạy backend

```bash
cd backend
./mvnw spring-boot:run
```

Hoặc build:

```bash
cd backend
./mvnw clean package -DskipTests
```

### 6.3. Chạy frontend

```bash
cd frontend
npm install
npm run dev
```

Build production:

```bash
cd frontend
npm run build
```

### 6.4. Chạy mobile

```bash
cd mobile
flutter pub get
flutter run
```

Build Android APK:

```bash
cd mobile
flutter build apk --release
```

### 6.5. Chạy AI service

```bash
cd ai-service
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8001
```

## 7. Cấu hình môi trường

Mỗi module có file cấu hình riêng:

- `backend/src/main/resources/application.properties`
- `frontend/` dùng cấu hình theo Vite
- `mobile/` dùng các hằng số API trong source
- `ai-service/app/config/settings.py`

Khi setup local, cần kiểm tra:

- URL backend
- kết nối PostgreSQL
- kết nối MongoDB
- kết nối Redis
- kết nối Qdrant
- cấu hình Firebase nếu dùng push notification
- cấu hình Google login nếu test OAuth

## 8. Cách sử dụng hệ thống

### Sinh viên

1. Đăng nhập vào ứng dụng mobile
2. Chọn ngày và khung giờ
3. Xem sơ đồ ghế và đặt chỗ
4. Check-in thư viện bằng HCE hoặc QR
5. Xác nhận ghế bằng NFC
6. Nhận cảnh báo nếu booking bị ảnh hưởng bởi thay đổi sơ đồ
7. Có thể đổi ghế hoặc hủy booking nếu hệ thống cho phép

### Thủ thư

1. Đăng nhập web portal
2. Theo dõi danh sách người đang trong thư viện
3. Kiểm tra booking, hỗ trợ chat và xử lý escalation
4. Xác nhận hoặc xử lý ngoại lệ theo nghiệp vụ
5. Theo dõi dashboard và notification

### Quản trị viên

1. Cấu hình giờ mở cửa, khung giờ, ngày phục vụ
2. Cấu hình rule booking, cancellation, reminder, warning
3. Quản lý sơ đồ thư viện
4. Quản lý kiosk, NFC tag, HCE station
5. Quản lý AI materials và knowledge stores
6. Quản lý reputation rules và các cấu hình vận hành khác

## 9. Luồng nghiệp vụ nổi bật

### Đặt chỗ

1. Sinh viên chọn ngày và khung giờ
2. Hệ thống trả sơ đồ ghế theo slot đó
3. Sinh viên chọn ghế và tạo booking
4. Booking có thể chuyển qua các trạng thái như `PROCESSING`, `BOOKED`, `CONFIRMED`, `COMPLETED`, `CANCELLED`, `EXPIRED`

### Check-in và xác nhận ghế

1. Sinh viên check-in vào thư viện bằng HCE hoặc QR
2. Sau đó mới được xác nhận ghế bằng NFC
3. Khi rời ghế cũng dùng luồng NFC tương ứng

### Quản lý sơ đồ thư viện

1. Admin chỉnh sửa trên draft
2. Validate xung đột trước khi publish
3. Có thể publish ngay hoặc schedule publish
4. Nếu ghế đang có người ngồi hoặc booking active bị ảnh hưởng thì publish bị chặn
5. Nếu booking tương lai bị ảnh hưởng, sinh viên nhận warning và có thể đổi ghế hoặc hủy

## 10. Lệnh hữu ích

### Backend

```bash
cd backend
./mvnw test
```

### Frontend

```bash
cd frontend
npm run build
npm run lint
```

### Mobile

```bash
cd mobile
flutter analyze
flutter test
```

### AI service

```bash
cd ai-service
python -m uvicorn app.main:app --reload --port 8001
```

## 11. Ghi chú

- Tất cả text hiển thị cho người dùng nên dùng tiếng Việt có dấu.
- API chính dùng prefix `/slib/`.
- WebSocket endpoint là `/ws`.
- AI service dùng prefix `/api/ai/`.
- Khi thay đổi DB schema, dùng Flyway migration trong `backend/src/main/resources/db/migration/`.

## 12. Thành viên phát triển

Nhóm phát triển SLIB có thể cập nhật thêm thông tin thành viên, vai trò và hướng dẫn đóng góp tại đây nếu cần.

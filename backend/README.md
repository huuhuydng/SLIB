# SLIB Backend Service

Backend API service cho hệ thống **SLIB Smart Library** - Hệ thống quản lý thư viện thông minh.

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.0-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue?style=flat-square&logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-Ready-2496ED?style=flat-square&logo=docker)

---

## Tổng quan

Backend service được xây dựng bằng **Spring Boot 3.4.0** với **Java 21**, cung cấp RESTful API cho toàn bộ hệ thống SLIB bao gồm:

- **Authentication**: JWT-based với Google OAuth
- **User Management**: Quản lý sinh viên, admin, thủ thư
- **Area Management**: Quản lý khu vực, zone, ghế ngồi
- **Booking System**: Đặt chỗ, check-in/out
- **Chat & AI**: Tích hợp với AI Service, WebSocket realtime
- **News/Notification**: Hệ thống tin tức, thông báo
- **System Config**: Cấu hình hệ thống linh hoạt.

---

## Kiến trúc dự án

```
backend/
├── src/
│   ├── main/
│   │   ├── java/slib/com/example/
│   │   │   ├── config/          # Cấu hình (CORS, Security, WebSocket...)
│   │   │   ├── controller/      # REST Controllers (25 controllers)
│   │   │   ├── dto/             # Data Transfer Objects (28 DTOs)
│   │   │   ├── entity/          # JPA Entities (32 entities)
│   │   │   ├── repository/      # Spring Data JPA Repositories (28 repos)
│   │   │   ├── service/         # Business Logic Services (30 services)
│   │   │   ├── security/        # JWT, Auth filters
│   │   │   ├── scheduler/       # Scheduled tasks
│   │   │   └── exception/       # Exception handlers
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── application-docker.properties
│   │       └── db/migration/    # Flyway migrations
│   └── test/                    # Unit & Integration tests
├── Dockerfile                   # Docker build config
├── pom.xml                      # Maven dependencies
└── uploads/                     # File uploads directory
```

---

## Thông tin nhánh `feature/user-reputation-v2`

Nhánh này dùng để phát triển phiên bản 2 của cơ chế **điểm uy tín người dùng** với mục tiêu chuẩn hóa rule, tăng khả năng mở rộng và hỗ trợ báo cáo/giám sát tốt hơn.

### Mục tiêu chính

- Thiết kế lại logic cộng/trừ điểm uy tín theo rule cấu hình.
- Tách rõ layer nghiệp vụ để dễ test và bảo trì.
- Chuẩn hóa dữ liệu phục vụ analytics và cảnh báo hành vi.
- Đồng bộ API với frontend/mobile cho các màn hình lịch sử vi phạm và điểm uy tín.

### Phạm vi kỹ thuật dự kiến

- **Database/Flyway**:
  - Bổ sung hoặc điều chỉnh bảng/ràng buộc liên quan đến rule uy tín, transaction điểm, và lịch sử vi phạm.
  - Thêm index cho các truy vấn thống kê và tra cứu theo người dùng/thời gian.
- **Backend Service**:
  - Cập nhật service tính điểm uy tín theo rule động.
  - Bổ sung validate để tránh trừ/cộng điểm trùng lặp.
  - Chuẩn hóa response DTO cho dữ liệu điểm uy tín.
- **API/Controller**:
  - Mở rộng endpoint xem rule, lịch sử điểm, và trạng thái uy tín hiện tại.
  - Bảo đảm phân quyền rõ giữa `STUDENT`, `LIBRARIAN`, `ADMIN`.
- **Test**:
  - Bổ sung test cho luồng cộng điểm, trừ điểm, rollback khi lỗi, và kiểm tra idempotency.

### Quy ước làm việc trên nhánh

- Mọi thay đổi schema bắt buộc đi qua Flyway migration mới, không sửa migration đã chạy ở môi trường dùng chung.
- Ưu tiên backward-compatible cho API nếu frontend/mobile chưa cập nhật đồng thời.
- Mỗi PR nên mô tả rõ:
  - Rule nào thay đổi.
  - Ảnh hưởng dữ liệu cũ (nếu có).
  - Cách test lại thủ công và tự động.

### Tiêu chí hoàn thành

- Tính điểm uy tín chính xác theo rule cấu hình.
- API trả về dữ liệu nhất quán giữa các module sử dụng.
- Flyway migrate sạch trên môi trường mới.
- Các test cốt lõi cho reputation pass ổn định.

---

## Tech Stack

| Thành phần | Công nghệ |
|------------|-----------|
| **Framework** | Spring Boot 3.4.0 |
| **Language** | Java 21 |
| **Database** | PostgreSQL + JPA/Hibernate |
| **Migration** | Flyway |
| **Security** | Spring Security + JWT |
| **WebSocket** | Spring WebSocket + STOMP |
| **Email** | Spring Mail |
| **File Upload** | Cloudinary |
| **AI Integration** | Google Vertex AI, WebFlux |
| **Build Tool** | Maven |

---

## Cài đặt và Chạy

### Yêu cầu
- **Java 21** hoặc cao hơn
- **Maven 3.8+**
- **PostgreSQL 15+**
- **Docker** (optional)

### Chạy Local

1. **Clone repository và cấu hình `.env`:**

```bash
cp .env.example .env
# Điền secret/key thật vào file .env
```

2. **Chạy với Maven:**

```bash
./mvnw spring-boot:run
```

`backend/.env` sẽ được nạp tự động khi ứng dụng khởi động.

3. **Hoặc build JAR:**

```bash
./mvnw clean package -DskipTests
java -jar target/demo-0.0.1-SNAPSHOT.jar
```

### Chạy với Docker

```bash
# Build image
docker build -t slib-backend .

# Run container
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=docker \
  -e DATABASE_URL=jdbc:postgresql://host:5432/slib_db \
  slib-backend
```

---

## API Endpoints chính

| Module | Base Path | Mô tả |
|--------|-----------|-------|
| **Auth** | `/slib/auth/*` | Đăng ký, đăng nhập, Google OAuth |
| **Users** | `/slib/users/*` | CRUD users, import Excel |
| **Areas** | `/slib/areas/*` | Quản lý khu vực thư viện |
| **Zones** | `/slib/zones/*` | Quản lý zone trong area |
| **Seats** | `/slib/seats/*` | Quản lý ghế ngồi |
| **Bookings** | `/slib/bookings/*` | Đặt chỗ, check-in/out |
| **News** | `/slib/news/*` | Tin tức, thông báo |
| **Chat** | `/slib/conversations/*` | Chat với AI/Thủ thư |
| **Config** | `/slib/config/*` | Cấu hình hệ thống |
| **Health** | `/actuator/health` | Health check |

**API Documentation:** Truy cập `/swagger-ui.html` khi server đang chạy.

---

## Environment Variables

```properties
# Database
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_password

# JWT
JWT_SECRET=your-256-bit-secret-key

# Cloudinary (File Upload)
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret

# Google OAuth
GOOGLE_CLIENT_ID=your_google_client_id

# HCE / Internal
GATE_SECRET=your_gate_secret
INTERNAL_API_KEY=your_internal_api_key

# Kiosk
KIOSK_AUTH_ENABLED=true
KIOSK_SECRET_KEY=your_kiosk_secret

# AI Service
MAIL_PASSWORD=your_mail_app_password
```

---

## Testing

```bash
# Chạy tất cả tests
./mvnw test

# Chạy với coverage
./mvnw test jacoco:report

# Test một class cụ thể
./mvnw test -Dtest=SeatControllerUnitTest
```

---
## License

© 2024 SLIB Team. All rights reserved.

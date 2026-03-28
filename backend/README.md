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

1. **Clone repository và cấu hình database:**

```bash
# Tạo database
createdb slib_db

# Cấu hình application.properties
cp src/main/resources/application.properties.example src/main/resources/application.properties
# Chỉnh sửa DB connection, JWT secret, etc.
```

2. **Chạy với Maven:**

```bash
./mvnw spring-boot:run
```

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
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/slib_db
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

# AI Service
AI_SERVICE_URL=http://localhost:8001
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

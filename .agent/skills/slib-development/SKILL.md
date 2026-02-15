---
name: slib-development
description: Hướng dẫn phát triển toàn diện cho hệ thống SLIB Smart Library. Sử dụng skill này khi cần làm việc với bất kỳ component nào của SLIB (backend, frontend, mobile, ai-service). Bao gồm conventions, API patterns, và best practices riêng cho dự án.
compatibility: Java 21, Spring Boot 3.4, React 19, Flutter 3.x, Python 3.11, PostgreSQL, Docker
metadata:
  author: SLIB Team
  version: 1.0.0
  project: SLIB Smart Library
---

# SLIB Development Guide

Skill này cung cấp hướng dẫn phát triển cho hệ thống SLIB Smart Library.

## System Architecture

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Mobile    │────>│   Backend   │<────│  Frontend   │
│  (Flutter)  │     │(Spring Boot)│     │   (React)   │
└─────────────┘     └──────┬──────┘     └─────────────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼─────┐ ┌────▼────┐ ┌─────▼─────┐
        │ PostgreSQL│ │  Redis  │ │AI Service │
        └───────────┘ └─────────┘ └───────────┘
```

## Decision Tree: Component Selection

```
Task -> Which component?
  ├─ Backend API/Database
  │   └─ See [Backend Reference](references/BACKEND.md)
  ├─ Frontend Web (Admin/Librarian)
  │   └─ See [Frontend Reference](references/FRONTEND.md)
  ├─ Mobile App (Student)
  │   └─ See [Mobile Reference](references/MOBILE.md)
  └─ AI Chatbot/RAG
      └─ See [AI Service Reference](references/AI_SERVICE.md)
```

## API Conventions

All backend endpoints use base path `/slib/`:

| Module | Endpoint | Description |
|--------|----------|-------------|
| Auth | `/slib/auth/*` | Login, register, OAuth |
| Users | `/slib/users/*` | User management |
| Areas | `/slib/areas/*` | Library areas |
| Zones | `/slib/zones/*` | Zones within areas |
| Seats | `/slib/seats/*` | Seat management |
| Bookings | `/slib/bookings/*` | Reservations |
| Chat | `/slib/conversations/*` | AI/Human chat |
| News | `/slib/news/*` | Announcements |
| Config | `/slib/config/*` | System settings |

## Naming Conventions

| Component | Convention | Example |
|-----------|------------|---------|
| Entity | PascalCase, singular | `StudentProfile` |
| Controller | *Controller suffix | `BookingController` |
| Service | *Service suffix | `BookingService` |
| Repository | *Repository suffix | `BookingRepository` |
| DTO Request | *Request suffix | `CreateBookingRequest` |
| DTO Response | *Response suffix | `BookingResponse` |
| React Component | PascalCase | `LibraryMapEditor` |
| Flutter Widget | PascalCase | `FloorPlanScreen` |
| CSS Class | kebab-case | `booking-card` |
| DB Table | snake_case, plural | `student_profiles` |
| DB Column | snake_case | `created_at` |

## Common Workflows

### Adding New Feature (Backend)

1. Create Flyway migration: `V{n}__description.sql`
2. Create Entity in `entity/`
3. Create Repository in `repository/`
4. Create Request/Response DTOs in `dto/`
5. Create Service in `service/`
6. Create Controller in `controller/`
7. Write unit tests

### Adding New Screen (Frontend)

1. Create page component in `pages/`
2. Add route in appropriate Routes file
3. Create/update services in `services/`
4. Add styles in `styles/`
5. Update navigation if needed

### Adding New Screen (Mobile)

1. Create screen widget in `views/`
2. Create/update models in `models/`
3. Update services in `services/`
4. Add navigation route

## Status Codes

### Reservation Status
- `PROCESSING` - Đang xử lý (holding seat)
- `BOOKED` - Đã đặt (chờ check-in)
- `CONFIRMED` - Đã xác nhận (checked-in)
- `EXPIRED` - Hết hạn
- `CANCELLED` - Đã hủy

### Seat Status
- `AVAILABLE` - Trống
- `HOLDING` - Đang giữ chỗ
- `BOOKED` - Đã đặt
- `MAINTENANCE` - Bảo trì

## Best Practices

- **Vietnamese in UI**: Sử dụng tiếng Việt có dấu đầy đủ
- **No Emoji**: Không sử dụng emoji/icon trong code và documentation
- **API Response**: Luôn wrap trong ResponseEntity
- **Transactions**: Sử dụng `@Transactional` cho write operations
- **Lazy Loading**: Default `FetchType.LAZY` cho relationships
- **Error Handling**: Throw custom exceptions, handle in GlobalExceptionHandler

## Quick Commands

```bash
# Start all services
docker-compose up -d

# Backend only
cd backend && ./mvnw spring-boot:run

# Frontend only
cd frontend && npm run dev

# Mobile
cd mobile && flutter run

# AI Service
cd ai-service && uvicorn app.main:app --reload --port 8001
```

## Reference Files

- [Backend Patterns](references/BACKEND.md) - Controllers, Services, Entities
- [Frontend Patterns](references/FRONTEND.md) - React components, styling
- [Mobile Patterns](references/MOBILE.md) - Flutter widgets, state management
- [AI Service Patterns](references/AI_SERVICE.md) - RAG, LangChain, endpoints

# SLIB - Hướng dẫn khởi động hệ thống

## Yêu cầu
- Docker Desktop đang chạy
- Ollama đang chạy (cho AI features)
- Node.js (cho Frontend)
- Flutter (cho Mobile)

---

## Khởi động nhanh

### Bước 1: Start Docker containers (Database, Redis, Backend, AI)

```bash
cd /Users/hadi/Desktop/slib
./docker-start.sh
```

Hoặc thủ công:
```bash
docker-compose up -d
```

### Bước 2: Đợi services khởi động (~30-60 giây)

Kiểm tra status:
```bash
docker-compose ps
```

Tất cả containers phải có status `(healthy)`.

### Bước 3: Start Frontend (Terminal riêng)

```bash
cd /Users/hadi/Desktop/slib/frontend
npm run dev
```

Frontend chạy tại: http://localhost:5173

### Bước 4: Start Mobile (Terminal riêng - nếu cần)

```bash
cd /Users/hadi/Desktop/slib/mobile
flutter run
```

---

## Kiến trúc hệ thống

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Containers                     │
├────────────────┬────────────────┬────────────┬──────────┤
│  slib-postgres │   slib-redis   │slib-backend│slib-ai   │
│  (PostgreSQL)  │    (Redis)     │(SpringBoot)│(FastAPI) │
│    :5432       │     :6379      │   :8080    │  :8001   │
└────────────────┴────────────────┴────────────┴──────────┘
                              ▲
                              │
        ┌─────────────────────┼─────────────────────┐
        │                     │                     │
        ▼                     ▼                     ▼
   ┌─────────┐         ┌───────────┐         ┌──────────┐
   │Frontend │         │  Mobile   │         │  Ollama  │
   │ (Vite)  │         │ (Flutter) │         │  (Host)  │
   │ :5173   │         │           │         │  :11434  │
   └─────────┘         └───────────┘         └──────────┘
```

---

## Chi tiết từng service

| Service | URL | Mô tả |
|---------|-----|-------|
| Backend API | http://localhost:8080 | SpringBoot REST API |
| Backend Health | http://localhost:8080/actuator/health | Health check |
| AI Service | http://localhost:8001 | Python FastAPI |
| AI Docs | http://localhost:8001/docs | Swagger UI |
| Database | localhost:5432 | PostgreSQL (user: postgres, pass: Slib@123) |
| Redis | localhost:6379 | Redis cache |
| Frontend | http://localhost:5173 | React Vite |

---

## Lệnh thường dùng

### Xem logs
```bash
# Tất cả
docker-compose logs -f

# Chỉ backend
docker-compose logs -f slib-backend

# Chỉ AI
docker-compose logs -f slib-ai-service
```

### Restart một service
```bash
docker-compose restart slib-backend
```

### Rebuild sau khi sửa code
```bash
docker-compose up --build -d
```

### Dừng hệ thống
```bash
./docker-stop.sh
# hoặc
docker-compose down
```

### Xóa sạch (bao gồm data)
```bash
docker-compose down -v
```

---

## Troubleshooting

### Backend không start
```bash
docker-compose logs slib-backend
```

### Database connection error
Đảm bảo postgres healthy:
```bash
docker exec slib-postgres pg_isready -U postgres
```

### AI không hoạt động
Đảm bảo Ollama đang chạy:
```bash
curl http://localhost:11434/api/version
```

---

## Thứ tự phụ thuộc

```
1. slib-postgres ─┐
2. slib-redis ────┼──► 3. slib-backend
                  └──► 4. slib-ai-service
                                │
5. Frontend (localhost) ────────┘
6. Mobile (localhost) ──────────┘
```

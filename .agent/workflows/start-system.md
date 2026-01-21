---
description: Khởi động đầy đủ hệ thống SLIB (Database, Backend, AI Service, Frontend, Mobile)
---

# Khởi động hệ thống SLIB

---

## 🚀 Cách nhanh nhất (Khuyến nghị)

// turbo
```bash
cd /Users/hadi/Desktop/slib
./start-all.sh
```

Script này sẽ tự động khởi động: Database → Backend → AI Service → Frontend

---

## 📋 Khởi động từng service riêng lẻ

### 1. Khởi động PostgreSQL Database

```bash
cd /Users/hadi/Desktop/slib
docker-compose up -d
```

Kiểm tra database đang chạy:
```bash
docker ps | grep slib-postgres
```

---

### 2. Khởi động Backend (Spring Boot)

// turbo
```bash
cd /Users/hadi/Desktop/slib/backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn spring-boot:run -Dmaven.test.skip=true
```

Kiểm tra backend đang chạy:
```bash
curl -s http://localhost:8080/slib/users/getall | head -50
```

---

### 3. Khởi động AI Service (Python FastAPI)

// turbo
```bash
cd /Users/hadi/Desktop/slib/ai-service
./start.sh
```

Hoặc thủ công:
```bash
cd /Users/hadi/Desktop/slib/ai-service
./venv/bin/uvicorn app.main:app --reload --port 8001
```

Kiểm tra AI service đang chạy:
```bash
curl -s http://localhost:8001/health
```

Swagger Docs: http://localhost:8001/docs

---

### 4. Khởi động Frontend (React)

// turbo
```bash
cd /Users/hadi/Desktop/slib/frontend
npm run dev
```

Frontend: http://localhost:5173

---

### 5. Khởi động Mobile App (Flutter) - Optional

```bash
cd /Users/hadi/Desktop/slib/mobile
flutter run
```

---

## 🛑 Dừng hệ thống

### Cách nhanh
```bash
cd /Users/hadi/Desktop/slib
./stop-all.sh
```

### Dừng từng service

```bash
# Dừng Frontend/Backend: Ctrl+C trong terminal

# Dừng AI Service
pkill -f "uvicorn app.main"

# Dừng port 8080
kill $(lsof -t -i:8080)

# Dừng Database
docker-compose down
```

---

## 📍 Tổng hợp URLs

| Service | URL |
|---------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080/slib |
| AI Service | http://localhost:8001/api/ai |
| AI Swagger | http://localhost:8001/docs |
| Database | localhost:5432 |

---

## 🔧 Troubleshooting

### Port đã bị chiếm
```bash
lsof -i :8080   # hoặc :8001, :5173
kill -9 <PID>
```

### Database connection failed
```bash
docker-compose down
docker-compose up -d
```

### AI Service không start
```bash
cd /Users/hadi/Desktop/slib/ai-service
./venv/bin/pip install -r requirements.txt
```

### JWT/Lombok error khi build
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
java -version
```
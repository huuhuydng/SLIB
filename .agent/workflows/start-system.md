---
description: Khởi động đầy đủ hệ thống SLIB (Database, Backend, AI Service, Frontend, Mobile)
---

# Khởi động hệ thống SLIB

---

## 🐳 Mode 1: Docker Full (Khuyến nghị cho Demo/Test)

Khởi động toàn bộ hệ thống trong Docker containers:

// turbo
```bash
cd /Users/hadi/Desktop/slib
./docker-start.sh
```

Sau đó start Frontend (chạy ngoài Docker):

// turbo
```bash
cd /Users/hadi/Desktop/slib/frontend
npm run dev
```

**Services:**
| Service | URL |
|---------|-----|
| Backend | http://localhost:8080 |
| AI Service | http://localhost:8001 |
| AI Docs | http://localhost:8001/docs |
| Frontend | http://localhost:5173 |
| Database | localhost:5432 |
| Redis | localhost:6379 |

---

## 💻 Mode 2: Dev Mode (Nhanh hơn khi coding)

Chỉ chạy Database + Redis trong Docker, còn lại chạy trực tiếp:

### Bước 1: Start Database & Redis

// turbo
```bash
cd /Users/hadi/Desktop/slib
docker-compose up -d slib-postgres slib-redis
```

### Bước 2: Start Backend (Terminal 1)

// turbo
```bash
cd /Users/hadi/Desktop/slib/backend
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
mvn spring-boot:run -Dmaven.test.skip=true
```

### Bước 3: Start AI Service (Terminal 2)

// turbo
```bash
cd /Users/hadi/Desktop/slib/ai-service
./venv/bin/uvicorn app.main:app --reload --port 8001
```

### Bước 4: Start Frontend (Terminal 3)

// turbo
```bash
cd /Users/hadi/Desktop/slib/frontend
npm run dev
```

### Bước 5: Start Mobile (Optional - Terminal 4)

```bash
cd /Users/hadi/Desktop/slib/mobile
flutter run
```

---

## ⚡ Start Ollama (Cho AI Features)

Trước khi dùng AI features, đảm bảo Ollama đang chạy:

```bash
ollama serve
```

Kiểm tra:
```bash
curl http://localhost:11434/api/version
```

---

## 🛑 Dừng hệ thống

### Docker Full Mode
```bash
cd /Users/hadi/Desktop/slib
./docker-stop.sh
```

### Dev Mode
```bash
# Dừng Frontend/Backend: Ctrl+C trong terminal

# Dừng AI Service
pkill -f "uvicorn app.main"

# Dừng Docker services
docker-compose down
```

---

## 📋 Docker Commands Thường Dùng

```bash
# Xem status containers
docker-compose ps

# Xem logs
docker-compose logs -f                    # Tất cả
docker-compose logs -f slib-backend       # Chỉ backend
docker-compose logs -f slib-ai-service    # Chỉ AI

# Rebuild sau khi sửa code
docker-compose up --build -d slib-backend      # Rebuild backend
docker-compose up --build -d slib-ai-service   # Rebuild AI

# Restart một service
docker-compose restart slib-backend

# Xóa tất cả (bao gồm data)
docker-compose down -v
```

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
docker-compose up -d slib-postgres slib-redis
```

### AI Service không start
```bash
cd /Users/hadi/Desktop/slib/ai-service
./venv/bin/pip install -r requirements.txt
```

### Backend không build được
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
java -version
```

### Container conflict
```bash
docker-compose down
docker rm -f slib-postgres slib-redis slib-backend slib-ai-service
docker-compose up -d
```
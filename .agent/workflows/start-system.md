---
description: Khởi động đầy đủ hệ thống SLIB (Database, Backend, Mobile)
---

# Khởi động hệ thống SLIB

---

## 1. Khởi động PostgreSQL Database

```bash
cd /Users/hadi/Desktop/slib
docker-compose up -d
```

Kiểm tra database đang chạy:
```bash
docker ps | grep slib-postgres
```

---

## 2. Khởi động Backend (Spring Boot)

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

## 3. Khởi động Mobile App (Flutter)

```bash
cd /Users/hadi/Desktop/slib/mobile
flutter run
```

---

## Dừng hệ thống

### Dừng Backend
```bash
# Ctrl+C trong terminal đang chạy backend
# Hoặc:
kill $(lsof -t -i:8080)
```

### Dừng Database
```bash
cd /Users/hadi/Desktop/slib
docker-compose down
```

---

## Troubleshooting

### Port 8080 đã bị chiếm
```bash
lsof -i :8080
kill -9 <PID>
```

### Database connection failed
```bash
docker-compose down
docker-compose up -d
```

### JWT/Lombok error khi build
Đảm bảo đang dùng Java 21:
```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
java -version
```
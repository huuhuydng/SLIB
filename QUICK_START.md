# 🚀 SLIB - Quick Start Guide

## Khởi động nhanh sau khi merge

### 1️⃣ Setup Database (Chỉ chạy 1 lần)

```bash
# Kết nối PostgreSQL và chạy script
cd backend
psql -U postgres -d slib_db < init_sample_data.sql
```

**Dữ liệu được tạo:**
- ✅ User Librarian: phucnhde170706@fpt.edu.vn (MSSV: DE170706)
- ✅ 75 ghế: A1-A25, B1-B25, C1-C25
- ✅ Ghế A6: BOOKED, reservation 15:00-17:00
- ✅ 3 Areas, 3 Zones

### 2️⃣ Chạy Backend

```bash
cd backend
./mvnw spring-boot:run
```

→ Server: http://localhost:8081

### 3️⃣ Chạy Frontend

```bash
cd frontend
npm install
npm run dev
```

→ Web: http://localhost:5173

### 4️⃣ Đăng nhập

1. Mở http://localhost:5173
2. Nhấn **"Sign in with Google"**
3. Hệ thống tự động:
   - LIBRARIAN → Dashboard Librarian
   - ADMIN → Dashboard Admin  
   - STUDENT → Bị chặn ❌

---

## 📂 Cấu trúc mới

```
routes/
  ├── AppRoutes.jsx        # Main (role-based routing)
  ├── AdminRoutes.jsx      # Admin routes
  └── LibrarianRoutes.jsx  # Librarian routes

layouts/
  ├── admin/MainLayout.jsx
  └── librarian/MainLayout.jsx

pages/
  ├── admin/...
  └── librarian/...
```

---

## ✅ Hoàn thành

- ✅ Role-based routing (ADMIN/LIBRARIAN)
- ✅ Block STUDENT login
- ✅ Tách riêng layouts, routes, styles
- ✅ Sample data: Users, Seats, Reservations
- ✅ Google OAuth với role checking

---

## 🐛 Nếu có lỗi

1. **Routing 404:** Xóa cache browser (Ctrl+Shift+R)
2. **CSS lỗi:** Kiểm tra import paths
3. **Login lỗi:** Xem console (F12) và backend logs
4. **DB empty:** Chạy lại `init_sample_data.sql`

---

**Chi tiết đầy đủ:** Xem file [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md)

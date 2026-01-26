# SLIB - Hướng Dẫn Cập Nhật Sau Merge Code

## 📋 Tổng Quan Thay Đổi

Hệ thống đã được tái cấu trúc hoàn toàn với các cải tiến chính:

### ✅ 1. Luồng Authentication Mới

**Luồng đăng nhập:**
```
Khởi động → Trang Login → Google Sign-In → Kiểm tra Role
                                              ↓
                          ┌───────────────────┴───────────────────┐
                          ↓                                       ↓
                    LIBRARIAN                                  ADMIN
                          ↓                                       ↓
              Dashboard Librarian                       Dashboard Admin
            (/dashboard - Librarian)                (/dashboard - Admin)
```

**Quy tắc phân quyền:**
- ✅ **LIBRARIAN** → Truy cập đầy đủ chức năng quản lý thư viện
- ✅ **ADMIN** → Truy cập chức năng quản trị hệ thống
- ❌ **STUDENT** → KHÔNG được phép đăng nhập vào web (chỉ dùng mobile app)

### ✅ 2. Cấu Trúc Thư Mục Mới

```
frontend/src/
├── routes/
│   ├── AppRoutes.jsx          # Main router với role-based routing
│   ├── AdminRoutes.jsx        # Routes cho Admin
│   └── LibrarianRoutes.jsx    # Routes cho Librarian
│
├── layouts/
│   ├── admin/
│   │   └── MainLayout.jsx     # Layout cho Admin (sidebar + content)
│   └── librarian/
│       └── MainLayout.jsx     # Layout cho Librarian
│
├── pages/
│   ├── admin/
│   │   ├── Dashboard/         # Dashboard Admin
│   │   ├── UserManagement/    # Quản lý người dùng
│   │   ├── DeviceManagement/  # Quản lý thiết bị
│   │   ├── SystemConfig/      # Cấu hình hệ thống
│   │   ├── SystemHealth/      # Sức khỏe hệ thống
│   │   └── AIConfig/          # Cấu hình AI
│   │
│   └── librarian/
│       ├── Dashboard/         # Dashboard Librarian
│       ├── CheckInOut/        # Check in/out
│       ├── HeatMap/           # Sơ đồ nhiệt
│       ├── SeatManage/        # Quản lý chỗ ngồi
│       ├── StudentsManage/    # Quản lý sinh viên
│       ├── ViolationManage/   # Quản lý vi phạm
│       ├── ChatManage/        # Trò chuyện
│       ├── Statistic/         # Thống kê
│       └── NotificationManage/ # Thông báo
│
├── components/
│   ├── sidebar_admin/         # Sidebar riêng cho Admin
│   └── sidebar_librarian/     # Sidebar riêng cho Librarian
│
└── styles/
    ├── admin/                 # CSS cho Admin
    └── librarian/             # CSS cho Librarian
```

### ✅ 3. Database Schema & Sample Data

File SQL: `backend/init_sample_data.sql`

**Dữ liệu mẫu được tạo:**

1. **User - Librarian:**
   - Họ tên: Nguyễn Hoàng Phúc
   - Email: phucnhde170706@fpt.edu.vn
   - MSSV: DE170706
   - Role: LIBRARIAN
   - Reputation: 100

2. **Areas (3 khu vực):**
   - Khu vực A - Khu tự học
   - Khu vực B - Khu yên tĩnh
   - Khu vực C - Khu thảo luận

3. **Zones (3 zones tương ứng):**
   - Zone A (trong Area A)
   - Zone B (trong Area B)
   - Zone C (trong Area C)

4. **Seats (75 chỗ ngồi):**
   - A1 → A25 (Zone A) 
   - B1 → B25 (Zone B)
   - C1 → C25 (Zone C)
   - **Ghế A6**: Status = BOOKED

5. **Reservation:**
   - Ghế: A6
   - User: Nguyễn Hoàng Phúc
   - Thời gian: 15:00 - 17:00 (hôm nay)
   - Status: CONFIRMED

---

## 🚀 Hướng Dẫn Chạy

### Bước 1: Khởi động Database

```bash
cd backend
# Nếu dùng PostgreSQL local
psql -U postgres -d slib < init_sample_data.sql

# Hoặc nếu dùng Supabase, chạy script trong Supabase SQL Editor
```

### Bước 2: Khởi động Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend sẽ chạy tại: `http://localhost:8081`

### Bước 3: Khởi động Frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend sẽ chạy tại: `http://localhost:5173`

### Bước 4: Đăng nhập

1. Mở trình duyệt: `http://localhost:5173`
2. Trang Login sẽ hiển thị
3. Nhấn **"Sign in with Google"**
4. Chọn tài khoản Google của bạn
5. Hệ thống sẽ:
   - Kiểm tra role từ backend
   - Nếu LIBRARIAN → Chuyển đến Dashboard Librarian
   - Nếu ADMIN → Chuyển đến Dashboard Admin
   - Nếu STUDENT → Hiển thị cảnh báo và không cho login

---

## 🎨 Giao Diện

### Dashboard Librarian
- Sidebar bên trái với menu:
  - Tổng quan
  - Kiểm tra ra/vào
  - Sơ đồ thư viện
  - Quản lý chỗ ngồi
  - Sinh viên
  - Vi phạm
  - Trò chuyện
  - Thống kê
  - Thông báo
  
### Dashboard Admin
- Sidebar bên trái với menu:
  - Tổng quan
  - Bản đồ thư viện
  - Quản lý người dùng
  - Quản lý thiết bị
  - Cấu hình hệ thống
  - Sức khỏe hệ thống
  - Cấu hình AI

---

## 🔧 Các File Quan Trọng

### Frontend
- [routes/AppRoutes.jsx](frontend/src/routes/AppRoutes.jsx) - Main routing logic
- [components/Login.jsx](frontend/src/components/Login.jsx) - Google Login integration
- [layouts/admin/MainLayout.jsx](frontend/src/layouts/admin/MainLayout.jsx) - Admin layout
- [layouts/librarian/MainLayout.jsx](frontend/src/layouts/librarian/MainLayout.jsx) - Librarian layout

### Backend
- [init_sample_data.sql](backend/init_sample_data.sql) - SQL script tạo dữ liệu mẫu
- [controller/users/UserController.java](backend/src/main/java/slib/com/example/controller/users/UserController.java) - Google login API
- [entity/users/Role.java](backend/src/main/java/slib/com/example/entity/users/Role.java) - Enum định nghĩa roles

---

## 📝 Lưu Ý

1. **Google OAuth:**
   - Đảm bảo Google Client ID đã được cấu hình đúng
   - File: `frontend/src/components/Login.jsx`
   - Client ID: `1071538292660-pf2ma4esd8lt1d2rclm27ipe1n3ch098.apps.googleusercontent.com`

2. **Database:**
   - Script SQL tự động tạo UUIDs
   - Tất cả dữ liệu mẫu có thể chạy nhiều lần (ON CONFLICT DO NOTHING)
   - Verify data bằng queries ở cuối script

3. **CORS:**
   - Backend đã enable CORS cho `http://localhost:5173`
   - Nếu deploy production, cần update CORS origins

4. **LocalStorage:**
   - Token lưu trong: `librarian_token`
   - User info lưu trong: `librarian_user`
   - Logout sẽ xóa cả hai

---

## 🐛 Troubleshooting

### Lỗi "Cannot read properties of undefined (reading 'role')"
- Kiểm tra backend có trả về đúng structure:
  ```json
  {
    "access_token": "...",
    "user": {
      "id": "...",
      "email": "...",
      "role": "LIBRARIAN" // Phải có field này
    }
  }
  ```

### Lỗi 404 khi routing
- Đảm bảo `BrowserRouter` chỉ có trong `AppRoutes.jsx`
- Các routes con dùng `<Routes>` và `<Route>` không cần `BrowserRouter` nữa

### UI không hiển thị đúng
- Kiểm tra CSS đã được copy đúng vào `styles/admin/` hoặc `styles/librarian/`
- Verify import paths trong component
- Xóa cache browser: Ctrl + Shift + R

### Database không có dữ liệu
- Chạy lại script: `psql -U postgres -d slib < init_sample_data.sql`
- Kiểm tra migrations đã chạy chưa
- Verify với queries cuối script

---

## ✨ Tính Năng Hoàn Thành

- ✅ Routing role-based (ADMIN/LIBRARIAN)
- ✅ Block STUDENT login vào web
- ✅ Tách riêng layout và styles cho từng role
- ✅ Google OAuth integration với role checking
- ✅ Sample data: User, Areas, Zones, Seats, Reservations
- ✅ Sidebar navigation cho Admin và Librarian

---

## 📞 Hỗ Trợ

Nếu gặp vấn đề, kiểm tra:
1. Console log trong browser (F12)
2. Backend logs trong terminal
3. Network tab để xem API calls
4. Database data bằng SQL queries

**Chúc bạn làm việc hiệu quả! 🎉**

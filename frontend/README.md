# SLIB Frontend

Giao diện người dùng cho hệ thống Smart Library Information System (SLIB) – quản lý thư viện thông minh, đa nền tảng, realtime, hỗ trợ nhiều vai trò (Admin, Librarian, Student).

---

## 🛠️ Công nghệ sử dụng

- ReactJS (Vite)
- React Router DOM
- Axios
- Zustand (quản lý state)
- React Testing Library, Jest
- ESLint, Prettier
- TailwindCSS (nếu có)

---

## 📁 Cấu trúc dự án

```text
frontend/
│
├── public/                  # Tài nguyên tĩnh (ảnh, favicon, logout.html...)
│
├── src/
│   ├── assets/              # Ảnh, icon, font, file tĩnh dùng trong app
│   ├── components/          # Các thành phần UI tái sử dụng
│   │   ├── common/          # Button, Modal, Loader, ...
│   │   ├── admin/           # UI riêng cho admin
│   │   ├── auth/            # Đăng nhập, đăng ký, quên mật khẩu
│   │   └── ...
│   ├── context/             # React context (nếu dùng)
│   ├── contexts/            # Các context quản lý state toàn cục
│   ├── hooks/               # Custom React hooks
│   ├── layouts/             # Layout tổng thể (AdminLayout, MainLayout...)
│   ├── pages/               # Các trang chính (Home, Dashboard, ...)
│   ├── routes/              # Định nghĩa route, bảo vệ route
│   ├── services/            # Giao tiếp API backend (auth, booking, ...)
│   ├── styles/              # File CSS, theme
│   ├── utils/               # Hàm tiện ích chung
│   ├── App.jsx              # Gốc ứng dụng React
│   ├── main.jsx             # Điểm khởi động React
│   └── ...
│
├── package.json             # Thông tin dependencies, scripts
├── vite.config.js           # Cấu hình Vite
├── README.md                # Tài liệu này
└── ...
```

---

## ⚡ Hướng dẫn cài đặt & chạy dự án

1. **Cài đặt Node.js >= 18**
2. **Cài dependencies:**
	```bash
	npm install
	```
3. **Chạy ở chế độ phát triển:**
	```bash
	npm run dev
	```
4. **Build production:**
	```bash
	npm run build
	```
5. **Chạy test:**
	```bash
	npm test
	```

---

## 📌 Lưu ý
- Đảm bảo backend đã chạy trước khi login/test API.
- Sử dụng file .env để cấu hình endpoint nếu cần.

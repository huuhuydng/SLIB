# SLIB Frontend

Giao diện web cho hệ thống **SLIB Smart Library** - Hệ thống quản lý thư viện thông minh.

![React](https://img.shields.io/badge/React-19.2-61DAFB?style=flat-square&logo=react)
![Vite](https://img.shields.io/badge/Vite-7.3-646CFF?style=flat-square&logo=vite)
![TailwindCSS](https://img.shields.io/badge/Tailwind-4.x-06B6D4?style=flat-square&logo=tailwindcss)

---

## Tổng quan

Frontend được xây dựng với **React 19** và **Vite**, cung cấp giao diện cho:

- **Dashboard Admin**: Quản lý toàn bộ hệ thống
- **Dashboard Librarian**: Quản lý thư viện, chat với sinh viên
- **Authentication**: Đăng nhập Admin/Librarian
- **Library Map Editor**: Vẽ và quản lý sơ đồ thư viện
- **Chat Management**: Quản lý hội thoại AI/Human
- **News Management**: Quản lý tin tức với Rich Text Editor
- **User Management**: Import/Export users qua Excel
- **System Config**: Cấu hình AI, giờ hoạt động, reputation

---

## Cấu trúc dự án

```
frontend/
├── public/                      # Static assets
├── src/
│   ├── assets/                  # Images, fonts, icons
│   ├── components/              # Reusable UI components (27 files)
│   │   ├── LibraryMapEditor.jsx # Sophisticated map editor
│   │   ├── ChatManagement.jsx   # WebSocket chat interface
│   │   ├── RichTextEditor.jsx   # TipTap-based editor
│   │   └── ...
│   ├── context/                 # React Context providers
│   │   └── AuthContext.jsx      # Authentication state
│   ├── hooks/                   # Custom React hooks
│   ├── layouts/                 # Page layouts
│   │   ├── AdminLayout.jsx
│   │   └── LibrarianLayout.jsx
│   ├── pages/                   # Route pages (25 pages)
│   │   ├── admin/               # Admin pages
│   │   └── librarian/           # Librarian pages
│   ├── routes/                  # React Router config
│   │   ├── AdminRoutes.jsx
│   │   └── LibrarianRoutes.jsx
│   ├── services/                # API service layer (11 files)
│   │   ├── api.js               # Axios config
│   │   ├── userService.jsx
│   │   ├── bookingService.js
│   │   └── ...
│   ├── styles/                  # CSS files (43 files)
│   ├── utils/                   # Helper functions
│   ├── App.jsx                  # Root component
│   └── main.jsx                 # Entry point
├── index.html
├── package.json
├── vite.config.js
└── vitest.config.js
```

---

## Tech Stack

| Thành phần | Công nghệ |
|------------|-----------|
| **Framework** | React 19.2 |
| **Build Tool** | Vite 7.3 |
| **Routing** | React Router DOM 7.11 |
| **Styling** | TailwindCSS 4.x + Vanilla CSS |
| **HTTP Client** | Axios 1.13 |
| **Rich Text** | TipTap (full suite) |
| **Icons** | Lucide React |
| **WebSocket** | STOMP.js + SockJS |
| **Auth** | @react-oauth/google |
| **File Processing** | XLSX, JSZip |
| **Testing** | Vitest + Testing Library |

---

## Cài đặt và Chạy

### Yêu cầu
- **Node.js 18+**
- **npm** hoặc **yarn**

### Cài đặt

```bash
# Clone và cài dependencies
cd frontend
npm install
```

### Development

```bash
# Chạy dev server (port 5173)
npm run dev
```

### Production Build

```bash
# Build
npm run build

# Preview build
npm run preview
```

---

## Environment Variables

Tạo file `.env` trong thư mục `frontend/`:

```env
# Backend API
VITE_API_URL=http://localhost:8080

# AI Service
VITE_AI_SERVICE_URL=http://localhost:8001

# Google OAuth
VITE_GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com

# WebSocket
VITE_WS_URL=ws://localhost:8080/ws
```

---

## Scripts

| Script | Mô tả |
|--------|-------|
| `npm run dev` | Chạy development server |
| `npm run build` | Build production |
| `npm run preview` | Preview production build |
| `npm run lint` | Chạy ESLint |
| `npm run test` | Chạy unit tests |
| `npm run test:coverage` | Test với coverage report |

---

## Tính năng chính

### Library Map Editor
- Drag-and-drop zone/seat editor
- Real-time preview
- Save/load floor plans
- Seat factory grid creation

### Chat Management
- Real-time WebSocket chat
- AI escalation to librarian
- Conversation history
- Typing indicators

### Rich Text Editor
- TipTap-based editor
- Image upload
- Tables, headings, lists
- YouTube embeds

### User Import
- Excel template download
- Bulk import with avatar ZIP
- Progress tracking
- Error preview

---

## Testing

```bash
# Chạy tests
npm run test

# Watch mode
npm run test -- --watch

# Coverage report
npm run test:coverage
```

---

## Routes Structure

### Admin Routes (`/admin/*`)
- `/admin` - Dashboard
- `/admin/areas` - Quản lý khu vực
- `/admin/users` - Quản lý người dùng
- `/admin/devices` - Quản lý thiết bị
- `/admin/config` - Cấu hình hệ thống
- `/admin/ai-config` - Cấu hình AI
- `/admin/health` - System health

### Librarian Routes (`/librarian/*`)
- `/librarian` - Dashboard
- `/librarian/zones` - Quản lý zone
- `/librarian/bookings` - Quản lý đặt chỗ
- `/librarian/news` - Quản lý tin tức
- `/librarian/chat` - Chat với sinh viên
- `/librarian/reports` - Báo cáo

---

## License

© 2024 SLIB Team. All rights reserved.

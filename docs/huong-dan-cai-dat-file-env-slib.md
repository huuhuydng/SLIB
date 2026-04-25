# Hướng dẫn cài đặt file ENV cho hệ thống SLIB

Tài liệu này hướng dẫn cấu hình các file `.env` để chạy hệ thống SLIB ở môi trường local.

Các module chính được đề cập trong tài liệu:
- `frontend/`: giao diện web cho thủ thư và quản trị viên
- `backend/`: Spring Boot REST API
- `ai-service/`: FastAPI service cho AI chat và RAG

Nếu làm đúng theo tài liệu này, môi trường local chuẩn sẽ dùng các địa chỉ sau:
- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- AI Service: `http://localhost:8001`
- Qdrant: `http://localhost:6333`
- Ollama: `http://localhost:11434`

---

## I. Chuẩn bị trước khi cấu hình ENV

Trước khi tạo file `.env`, cần chuẩn bị sẵn các thành phần sau:
- Java 21
- Node.js và npm
- Python 3
- PostgreSQL
- Qdrant
- Ollama hoặc Gemini API Key
- Tài khoản Google Cloud để lấy `GOOGLE_CLIENT_ID`
- Tài khoản Cloudinary để lấy thông tin upload ảnh
- Gmail App Password để gửi email

(Hình ảnh tạo: sơ đồ tổng quan các service local của hệ thống SLIB gồm frontend, backend, ai-service, PostgreSQL, Qdrant và Ollama)

---

## II. Cài đặt file ENV của Frontend

### 1. Cấu trúc file `frontend/.env`

Tạo file `.env` trong thư mục `frontend/` và điền nội dung như sau:

```env
VITE_API_BASE_URL=http://localhost:8080
VITE_API_URL=http://localhost:8080
VITE_AI_URL=http://localhost:8001
VITE_NFC_BRIDGE_URL=http://127.0.0.1:5050
```

### 2. Giải thích các biến

- `VITE_API_BASE_URL`: địa chỉ backend local
- `VITE_API_URL`: địa chỉ backend local dùng cho một số flow cũ
- `VITE_AI_URL`: địa chỉ AI service local
- `VITE_NFC_BRIDGE_URL`: địa chỉ service bridge NFC local

### 3. Lưu ý

- Nếu backend chạy cổng khác `8080` thì phải sửa lại `VITE_API_BASE_URL` và `VITE_API_URL`
- Nếu AI service chạy cổng khác `8001` thì phải sửa lại `VITE_AI_URL`
- Nếu không dùng NFC bridge trong quá trình phát triển, vẫn nên giữ giá trị mặc định để tránh lỗi một số màn hình quản trị

(Hình ảnh tạo: thư mục `frontend/` với file `.env.example` và file `.env` được tạo mới)

---

## III. Cài đặt file ENV của Backend

### 1. Cấu trúc file `backend/.env`

Tạo file `.env` trong thư mục `backend/` và điền nội dung như sau:

```env
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_database_password

JWT_SECRET=your_jwt_secret_at_least_32_bytes

GOOGLE_CLIENT_ID=your_google_client_id

GATE_SECRET=your_gate_secret

INTERNAL_API_KEY=your_internal_service_key

CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret

KIOSK_AUTH_ENABLED=true
KIOSK_SECRET_KEY=your_kiosk_secret_key

MAIL_PASSWORD=your_mail_app_password
```

### 2. Giải thích các biến

- `SPRING_DATASOURCE_USERNAME`: username PostgreSQL
- `SPRING_DATASOURCE_PASSWORD`: password PostgreSQL
- `JWT_SECRET`: secret dùng để ký JWT, nên dài ít nhất 32 ký tự
- `GOOGLE_CLIENT_ID`: client ID của Google OAuth
- `GATE_SECRET`: secret cho flow HCE/Gate
- `INTERNAL_API_KEY`: key dùng để backend và AI service xác thực nội bộ
- `CLOUDINARY_CLOUD_NAME`: tên cloud trên Cloudinary
- `CLOUDINARY_API_KEY`: API key của Cloudinary
- `CLOUDINARY_API_SECRET`: API secret của Cloudinary
- `KIOSK_AUTH_ENABLED`: bật hoặc tắt xác thực kiosk
- `KIOSK_SECRET_KEY`: secret key cho kiosk
- `MAIL_PASSWORD`: Gmail App Password dùng để gửi mail

### 3. Giá trị nào bắt buộc phải có

Bắt buộc để backend chạy ổn định:
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `JWT_SECRET`
- `INTERNAL_API_KEY`

Bắt buộc nếu dùng tính năng tương ứng:
- `GOOGLE_CLIENT_ID`: đăng nhập Google
- `CLOUDINARY_*`: upload ảnh
- `MAIL_PASSWORD`: gửi email
- `KIOSK_SECRET_KEY`: xác thực kiosk khi dùng secret key

(Hình ảnh tạo: thư mục `backend/` với file `.env` mẫu chứa các nhóm DATABASE, JWT, GOOGLE OAUTH, CLOUDINARY, EMAIL)

---

## IV. Cài đặt file ENV của AI Service

### 1. Cấu trúc file `ai-service/.env`

Tạo file `.env` trong thư mục `ai-service/` và điền nội dung như sau:

```env
AI_PROVIDER=ollama

OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
OLLAMA_EMBEDDING_MODEL=nomic-embed-text

GEMINI_API_KEY=
GEMINI_MODEL=gemini-2.0-flash

DATABASE_URL=postgresql://postgres:your_database_password@localhost:5434/slib

QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=slib_knowledge

JAVA_BACKEND_URL=http://localhost:8080/slib
INTERNAL_API_KEY=your_internal_service_key

SIMILARITY_THRESHOLD=0.5
CHUNK_SIZE=500
CHUNK_OVERLAP=100
MAX_RETRIEVED_CHUNKS=5

DEBUG=false
```

### 2. Giải thích các biến

- `AI_PROVIDER`: chọn provider AI, hiện hỗ trợ `ollama` hoặc `gemini`
- `OLLAMA_URL`: địa chỉ server Ollama local
- `OLLAMA_MODEL`: model chat dùng với Ollama
- `OLLAMA_EMBEDDING_MODEL`: model embedding dùng cho RAG
- `GEMINI_API_KEY`: API key Gemini nếu dùng Gemini
- `GEMINI_MODEL`: model Gemini sử dụng
- `DATABASE_URL`: chuỗi kết nối PostgreSQL
- `QDRANT_URL`: địa chỉ Qdrant local
- `QDRANT_COLLECTION`: tên collection vector
- `JAVA_BACKEND_URL`: endpoint backend mà AI service sẽ gọi nội bộ
- `INTERNAL_API_KEY`: phải giống với `INTERNAL_API_KEY` của backend
- `SIMILARITY_THRESHOLD`: ngưỡng so khớp cho RAG
- `CHUNK_SIZE`: kích thước chia đoạn tài liệu
- `CHUNK_OVERLAP`: độ chồng lấn giữa các đoạn
- `MAX_RETRIEVED_CHUNKS`: số lượng đoạn lấy ra tối đa
- `DEBUG`: bật hoặc tắt debug mode

### 3. Lưu ý quan trọng

- Nếu chọn `AI_PROVIDER=ollama` thì có thể để trống `GEMINI_API_KEY`
- Nếu chọn `AI_PROVIDER=gemini` thì phải điền `GEMINI_API_KEY`
- `INTERNAL_API_KEY` của AI service phải giống hệt backend
- `DATABASE_URL` phải đúng cổng PostgreSQL đang chạy thực tế

(Hình ảnh tạo: thư mục `ai-service/` với file `.env` có các nhóm OLLAMA, GEMINI, DATABASE, QDRANT, BACKEND INTEGRATION)

---

## V. Hướng dẫn lấy `GOOGLE_CLIENT_ID`

### Bước 1. Truy cập Google Cloud Console

Mở trang:

`https://console.cloud.google.com/`

(Hình ảnh tạo: trang chủ Google Cloud Console sau khi đăng nhập)

### Bước 2. Tạo hoặc chọn project

- Chọn project đang dùng cho SLIB hoặc tạo project mới
- Đặt tên project dễ nhận biết, ví dụ `SLIB Local Auth`

(Hình ảnh tạo: nút chọn project và popup tạo project mới trên Google Cloud Console)

### Bước 3. Vào mục API & Services

- Chọn `APIs & Services`
- Chọn `Credentials`

(Hình ảnh tạo: menu điều hướng đến API & Services > Credentials)

### Bước 4. Tạo OAuth Client ID

- Chọn `Create Credentials`
- Chọn `OAuth client ID`
- Chọn loại ứng dụng phù hợp, thường là `Web application`

(Hình ảnh tạo: form tạo OAuth Client ID với lựa chọn Web application)

### Bước 5. Cấu hình Authorized JavaScript origins

Thêm các origin cần thiết, ví dụ:

- `http://localhost:5173`
- `https://slibsystem.site`

(Hình ảnh tạo: vùng nhập Authorized JavaScript origins trong màn hình tạo OAuth Client ID)

### Bước 6. Sao chép Client ID

Sau khi tạo xong:
- sao chép giá trị `Client ID`
- dán vào biến `GOOGLE_CLIENT_ID` trong `backend/.env`

(Hình ảnh tạo: màn hình hiển thị Client ID sau khi tạo thành công)

---

## VI. Hướng dẫn lấy Cloudinary Credentials

### Bước 1. Truy cập Cloudinary Console

Mở trang:

`https://console.cloudinary.com/`

(Hình ảnh tạo: trang dashboard Cloudinary sau khi đăng nhập)

### Bước 2. Mở Dashboard

Tại dashboard sẽ thấy các thông tin:
- Cloud name
- API Key
- API Secret

(Hình ảnh tạo: khu vực Dashboard của Cloudinary hiển thị Cloud name, API Key, API Secret)

### Bước 3. Điền vào file ENV

Điền các giá trị này vào:

```env
CLOUDINARY_CLOUD_NAME=your_cloudinary_cloud_name
CLOUDINARY_API_KEY=your_cloudinary_api_key
CLOUDINARY_API_SECRET=your_cloudinary_api_secret
```

(Hình ảnh tạo: ví dụ copy thông tin từ Cloudinary sang file `backend/.env`)

---

## VII. Hướng dẫn tạo Gmail App Password

### Bước 1. Truy cập tài khoản Google

Mở trang:

`https://myaccount.google.com/`

(Hình ảnh tạo: trang quản lý tài khoản Google)

### Bước 2. Bật xác minh 2 bước

- Vào `Security`
- Bật `2-Step Verification`

(Hình ảnh tạo: mục Security của tài khoản Google với trạng thái 2-Step Verification)

### Bước 3. Tạo App Password

- Tìm mục `App passwords`
- Tạo mật khẩu ứng dụng mới cho mail

(Hình ảnh tạo: màn hình tạo App Password của Google)

### Bước 4. Điền vào file ENV

Sao chép mật khẩu vừa tạo và điền vào:

```env
MAIL_PASSWORD=your_mail_app_password
```

(Hình ảnh tạo: ví dụ điền `MAIL_PASSWORD` vào file `backend/.env`)

---

## VIII. Hướng dẫn cấu hình Ollama

### Bước 1. Kiểm tra Ollama đã cài chưa

Chạy lệnh:

```bash
ollama --version
```

(Hình ảnh tạo: terminal hiển thị phiên bản Ollama)

### Bước 2. Khởi động Ollama

Chạy lệnh:

```bash
ollama serve
```

(Hình ảnh tạo: terminal hiển thị Ollama đang chạy tại cổng 11434)

### Bước 3. Tải model chat

Chạy lệnh:

```bash
ollama pull llama3.2
```

(Hình ảnh tạo: terminal đang pull model `llama3.2`)

### Bước 4. Tải model embedding

Chạy lệnh:

```bash
ollama pull nomic-embed-text
```

(Hình ảnh tạo: terminal đang pull model `nomic-embed-text`)

### Bước 5. Cập nhật file ENV

```env
AI_PROVIDER=ollama
OLLAMA_URL=http://localhost:11434
OLLAMA_MODEL=llama3.2
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
```

(Hình ảnh tạo: file `ai-service/.env` sau khi cấu hình xong phần Ollama)

---

## IX. Hướng dẫn cấu hình Gemini

Mục này chỉ cần nếu không dùng Ollama.

### Bước 1. Lấy Gemini API Key

Mở trang:

`https://aistudio.google.com/app/apikey`

(Hình ảnh tạo: màn hình tạo API key tại Google AI Studio)

### Bước 2. Tạo API Key mới

- Chọn `Create API key`
- Sao chép key vừa tạo

(Hình ảnh tạo: popup hoặc màn hình hiển thị API key vừa tạo)

### Bước 3. Điền vào file ENV

```env
AI_PROVIDER=gemini
GEMINI_API_KEY=your_gemini_api_key
GEMINI_MODEL=gemini-2.0-flash
```

(Hình ảnh tạo: file `ai-service/.env` sau khi cấu hình Gemini)

---

## X. Hướng dẫn cấu hình PostgreSQL và Qdrant

### 1. PostgreSQL

Backend và AI service đều sử dụng PostgreSQL.

Thông tin mặc định đang dùng:

- Database name: `slib`
- Username: `postgres`
- Backend datasource: `localhost:5434`
- AI service datasource: `localhost:5434`

Ví dụ:

```env
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=your_database_password

DATABASE_URL=postgresql://postgres:your_database_password@localhost:5434/slib
```

(Hình ảnh tạo: công cụ quản trị PostgreSQL hiển thị database `slib`)

### 2. Qdrant

AI service dùng Qdrant để lưu vector embedding.

Giá trị mặc định:

```env
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=slib_knowledge
```

(Hình ảnh tạo: giao diện hoặc terminal xác nhận Qdrant đang chạy ở cổng 6333)

---

## XI. Thứ tự chạy hệ thống sau khi cấu hình ENV

### 1. Chạy Backend

```bash
cd backend
./mvnw spring-boot:run
```

### 2. Chạy AI Service

```bash
cd ai-service
uvicorn app.main:app --reload --port 8001
```

### 3. Chạy Frontend

```bash
cd frontend
npm run dev
```

### 4. Nếu cần thì chạy thêm Docker services

```bash
docker-compose up -d
```

(Hình ảnh tạo: ba cửa sổ terminal đang chạy backend, ai-service và frontend thành công)

---

## XII. Checklist kiểm tra cấu hình thành công

Sau khi cài đặt xong, kiểm tra các điều kiện sau:

- Frontend mở được tại `http://localhost:5173`
- Backend phản hồi ở `http://localhost:8080`
- AI service phản hồi ở `http://localhost:8001/health` hoặc endpoint kiểm tra tương ứng
- Đăng nhập Google không báo sai `Client ID`
- Upload ảnh không lỗi Cloudinary
- Gửi email không lỗi SMTP
- AI chat hoạt động nếu Ollama hoặc Gemini đã cấu hình đúng
- Backend và AI service dùng cùng `INTERNAL_API_KEY`

(Hình ảnh tạo: giao diện frontend SLIB mở thành công trên local)

---

## XIII. Một số lỗi thường gặp

### 1. Lỗi đăng nhập Google thất bại

Nguyên nhân thường gặp:
- sai `GOOGLE_CLIENT_ID`
- thiếu `Authorized JavaScript origins`
- origin local chưa được thêm vào Google Cloud Console

### 2. Lỗi AI service không gọi được backend

Nguyên nhân thường gặp:
- sai `JAVA_BACKEND_URL`
- sai `INTERNAL_API_KEY`
- backend chưa chạy

### 3. Lỗi upload ảnh

Nguyên nhân thường gặp:
- sai `CLOUDINARY_CLOUD_NAME`
- sai `CLOUDINARY_API_KEY`
- sai `CLOUDINARY_API_SECRET`

### 4. Lỗi gửi email

Nguyên nhân thường gặp:
- chưa bật xác minh 2 bước
- dùng sai `MAIL_PASSWORD`
- dùng mật khẩu đăng nhập Gmail thay vì App Password

### 5. Lỗi AI chat không trả lời

Nguyên nhân thường gặp:
- Ollama chưa chạy
- chưa pull model
- `GEMINI_API_KEY` không hợp lệ
- Qdrant chưa chạy

---

## XIV. Kết luận

Để chạy SLIB local ổn định, cần cấu hình đúng 3 file chính:
- `frontend/.env`
- `backend/.env`
- `ai-service/.env`

Ngoài ra cần chuẩn bị đúng các dịch vụ ngoài:
- Google OAuth
- Cloudinary
- Gmail App Password
- PostgreSQL
- Qdrant
- Ollama hoặc Gemini

Sau khi hoàn thiện tài liệu này, có thể xuất sang PDF để gửi cho thành viên mới trong nhóm sử dụng trực tiếp.

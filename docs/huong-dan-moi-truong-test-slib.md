# Hướng dẫn môi trường test cho hệ thống SLIB

Tài liệu này được viết theo form của file `1138_SE_08_Test_Environment.pdf`, nhưng đã điều chỉnh để phù hợp với kiến trúc hiện tại của hệ thống SLIB.

---

## 1. MÔI TRƯỜNG

### Link truy cập

- Link WEB Portal:
  - Production hiện tại: `https://slibsystem.site`
  - Nếu chưa có UAT riêng: dùng local `http://localhost:5173`
  - Nếu có UAT riêng: điền domain test thật, ví dụ `https://uat-ten-he-thong.example.com`

- Link Backend API:
  - Production hiện tại: `https://api.slibsystem.site`
  - Nếu chưa có UAT riêng: dùng local `http://localhost:8080`
  - Nếu có UAT riêng: điền domain API test thật

- Link AI Service:
  - Môi trường local thường dùng: `http://localhost:8001`
  - Nếu chưa có UAT riêng: giữ `http://localhost:8001`
  - Nếu có UAT riêng: điền link AI service test thật

- Mobile App:
  - SLIB không dùng Zalo Mini App như tài liệu mẫu.
  - Thay vào đó nên ghi:
    - `Bản APK Android test: (Điền link phát hành nội bộ)`
    - `Bản iOS test/TestFlight: (Điền link hoặc ghi chú phân phối nội bộ)`

### Hạ tầng dữ liệu

Khác với tài liệu mẫu dùng MySQL, SLIB hiện dùng nhiều thành phần dữ liệu:

#### PostgreSQL

- Nếu chạy bằng `docker-compose` của repo:
  - Host: `localhost`
  - Port: `5432`
- Nếu backend đang trỏ vào PostgreSQL local cấu hình kiểu cũ trong `application.properties`:
  - Host: `localhost`
  - Port: `5434`
- Nếu chạy giữa các container Docker với nhau:
  - Host: `slib-postgres`
  - Port: `5432`
- Database name: `slib`
- Database username: thường là `postgres`
- Database password: lấy từ file root `.env`, không nên ghi công khai vào PDF nếu tài liệu được chia sẻ rộng

#### MongoDB

- Nếu chạy bằng `docker-compose` và truy cập từ máy của bạn:
  - Host: `localhost`
  - Port: `27017`
- Nếu service khác truy cập từ trong Docker network:
  - Host: `slib-mongo`
  - Port: `27017`
- Database name: `slib_chat`
- Username: lấy từ file root `.env`
- Password: lấy từ file root `.env`, không nên ghi công khai vào PDF nếu tài liệu được chia sẻ rộng

#### Qdrant

- Nếu truy cập từ máy local:
  - Host: `localhost`
  - Port: `6333`
- Nếu truy cập giữa các container:
  - Host: `slib-qdrant`
  - Port: `6333`
- Collection: `slib_knowledge`

#### Redis

- Nếu truy cập từ máy local:
  - Host: `localhost`
  - Port: `6379`
- Nếu truy cập giữa các container:
  - Host: `slib-redis`
  - Port: `6379`

### Cấu hình local đang bám theo repo hiện tại

Nếu chạy local theo repo hiện tại thì thông số đang là:

- Frontend: `http://localhost:5173`
- Backend: `http://localhost:8080`
- AI Service: `http://localhost:8001`
- PostgreSQL:
  - `localhost:5432` nếu chạy bằng `docker-compose`
  - `localhost:5434` nếu bạn dùng cấu hình local cũ trong [application.properties](/Users/hadi/Desktop/slib/backend/src/main/resources/application.properties:9)
- MongoDB: `localhost:27017`
- Qdrant: `http://localhost:6333`
- Redis: `localhost:6379`

### Cách tự xác định giá trị cần điền

- Nếu tài liệu dành cho tester/dev trong máy local của bạn:
  - điền toàn bộ bằng `localhost` như phần trên
- Nếu tài liệu dành cho môi trường UAT thật:
  - hỏi người đang deploy hệ thống để lấy domain và host thật
- Nếu chưa có server UAT riêng:
  - đừng ghi `(Điền ...)`, hãy ghi rõ `Chưa có UAT riêng, đang dùng môi trường local`

(Hình ảnh tạo: sơ đồ môi trường test SLIB gồm frontend, backend, mobile, ai-service, PostgreSQL, MongoDB, Redis và Qdrant)

---

## 2. ACCOUNT

Khác với tài liệu mẫu đang liệt kê account theo hệ thống NextExam, SLIB nên chia theo role thực tế:

- `ADMIN`
- `LIBRARIAN`
- `STUDENT`
- `TEACHER`

Nếu cần một bảng account theo đúng form PDF mẫu, có thể dùng mẫu sau:

| Tên hiển thị | User name | Mật khẩu khởi tạo | Vai trò | Ghi chú |
|---|---|---|---|---|
| Quản trị viên SLIB | `admin@...` hoặc `admin` | `(Điền mật khẩu test)` | `ADMIN` | Dùng để quản trị hệ thống |
| Thủ thư SLIB | `librarian@...` hoặc `librarian` | `(Điền mật khẩu test)` | `LIBRARIAN` | Dùng để test portal thủ thư |
| Sinh viên test 01 | `(Điền tài khoản)` | `(Điền mật khẩu test)` | `STUDENT` | Dùng để test mobile và booking |
| Sinh viên test 02 | `(Điền tài khoản)` | `(Điền mật khẩu test)` | `STUDENT` | Dùng để test chat, complaint, feedback |
| Giảng viên test 01 | `(Điền tài khoản)` | `(Điền mật khẩu test)` | `TEACHER` | Dùng để test luồng patron mở rộng |

### Các account seed đã thấy trong repo

Từ migration hiện tại của hệ thống, đang có các mã user mẫu liên quan đến staff:

| Mã user | Email | Họ tên | Vai trò |
|---|---|---|---|
| `DE170706` | `phucnhde170706@fpt.edu.vn` | Nguyễn Hoàng Phúc | `LIBRARIAN` |
| `DE180893` | `uyenlpde180893@fpt.edu.vn` | Lê Phương Uyên | `ADMIN` |
| `ADMIN001` | `admin@fpt.edu.vn` | Admin User | `ADMIN` |
| `LIB001` | `librarian@fpt.edu.vn` | Thủ thư | `LIBRARIAN` |

Lưu ý:
- Không nên công bố password thật trong tài liệu chia sẻ rộng.
- Nếu cần phát tài khoản test cho nhóm, nên reset password riêng cho môi trường test rồi điền vào bản PDF nội bộ.

(Hình ảnh tạo: bảng account test của SLIB với các cột Tên hiển thị, User name, Mật khẩu khởi tạo, Vai trò, Ghi chú)

---

## 3. HƯỚNG DẪN TRUY CẬP HỆ THỐNG

### 3.1. Truy cập web portal

- Bước 1: Truy cập link web test của SLIB
- Bước 2: Đăng nhập bằng tài khoản `ADMIN` hoặc `LIBRARIAN`
- Bước 3: Kiểm tra các phân hệ cần test:
  - Dashboard
  - Quản lý người dùng
  - Quản lý sơ đồ thư viện
  - Booking
  - AI chat hỗ trợ
  - Notification

(Hình ảnh tạo: màn hình đăng nhập web portal SLIB)

### 3.2. Truy cập mobile app

Vì SLIB là ứng dụng Flutter mobile, nên phần này phải thay hoàn toàn cho đoạn Zalo Mini App trong tài liệu mẫu.

- Bước 1: Cài bản APK test hoặc mở bản TestFlight
- Bước 2: Đăng nhập bằng tài khoản `STUDENT` hoặc `TEACHER`
- Bước 3: Kiểm tra các chức năng:
  - Xem sơ đồ ghế
  - Đặt chỗ
  - Xem booking
  - Nhận notification
  - Chat với AI

(Hình ảnh tạo: màn hình mobile app SLIB ở trang đăng nhập hoặc trang chủ)

### 3.3. Truy cập AI chat

- Nếu test trên mobile: dùng tài khoản patron để chat
- Nếu test trên web portal: dùng màn hình test AI hoặc màn hình hỗ trợ chat tương ứng
- Đảm bảo AI service đang hoạt động và Qdrant có dữ liệu

(Hình ảnh tạo: màn hình chat AI của SLIB)

---

## 4. GHI CHÚ QUAN TRỌNG KHI VIẾT BẢN PDF CHÍNH THỨC

Khi chuyển tài liệu này thành PDF chính thức, nên chỉnh các điểm sau để phù hợp với SLIB:

### Nên giữ

- Mục `1. MÔI TRƯỜNG`
- Mục `2. Account`
- Cách trình bày ngắn gọn, dễ copy thông tin
- Bảng account để tester dùng nhanh

### Cần đổi so với file mẫu

- Đổi `Zalo Mini App` thành `Mobile App`
- Đổi `MySQL` thành `PostgreSQL`
- Bổ sung `MongoDB`, `Redis`, `Qdrant` vì SLIB có AI và realtime
- Đổi tên account theo role thật của SLIB
- Thêm link API hoặc AI service nếu tài liệu dùng cho dev/tester nội bộ

### Không nên đưa trực tiếp vào bản gửi rộng

- Password production thật
- API key thật
- Secret key thật
- Tài khoản cá nhân của developer nếu không bắt buộc

---

## 5. MẪU NỘI DUNG RÚT GỌN ĐỂ ĐƯA THẲNG VÀO PDF

Bạn có thể dùng nguyên mẫu rút gọn sau:

```text
1. MÔI TRƯỜNG

Link WEB Portal: (Điền link web test SLIB)
Link Backend API: (Điền link API test SLIB)
Link Mobile App: (Điền link APK/TestFlight)

PostgreSQL:
Host: (Điền host)
Port: 5432
Database name: slib
Database username: (Điền username)
Database password: (Điền password)

MongoDB:
Host: (Điền host)
Port: 27017
Database name: slib_chat

Qdrant:
Host: (Điền host)
Port: 6333
Collection: slib_knowledge

2. Account

Tên hiển thị | User name | Mật khẩu khởi tạo | Vai trò | Ghi chú
Quản trị viên SLIB | ... | ... | ADMIN | Test cấu hình hệ thống
Thủ thư SLIB | ... | ... | LIBRARIAN | Test portal thủ thư
Sinh viên test 01 | ... | ... | STUDENT | Test mobile, booking, chat
Giảng viên test 01 | ... | ... | TEACHER | Test luồng patron mở rộng
```

---

## 6. TÀI LIỆU LIÊN QUAN TRONG REPO

- File hướng dẫn ENV: [huong-dan-cai-dat-file-env-slib.md](/Users/hadi/Desktop/slib/docs/huong-dan-cai-dat-file-env-slib.md)
- File seed user SQL: [slib_users_only_seed.sql](/Users/hadi/Desktop/slib/docs/sql/slib_users_only_seed.sql)
- File schema SQL tổng hợp: [slib_schema_full_from_flyway.sql](/Users/hadi/Desktop/slib/docs/sql/slib_schema_full_from_flyway.sql)

---

## 7. KẾT LUẬN

Nếu lấy đúng form của `1138_SE_08_Test_Environment.pdf` để áp dụng cho SLIB, thì chỉnh trọng tâm như sau:

- Giữ cấu trúc ngắn gọn theo kiểu “Môi trường + Account”
- Thay Zalo Mini App bằng mobile app SLIB
- Thay MySQL bằng PostgreSQL
- Bổ sung MongoDB, Redis, Qdrant nếu tài liệu dành cho dev/tester
- Dùng account theo role `ADMIN`, `LIBRARIAN`, `STUDENT`, `TEACHER`

Tài liệu này là bản trung gian để bạn tiếp tục đổ thông tin thật vào trước khi xuất PDF chính thức.

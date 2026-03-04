# Hệ Thống Khiếu Nại Vi Phạm (Violation Appeal System)

## Tổng Quan

Hệ thống cho phép:
1. **Thủ thư** tạo vi phạm cho sinh viên
2. **Sinh viên** xem vi phạm và khiếu nại
3. **Thủ thư** xem và xử lý khiếu nại (chấp nhận/từ chối)

## Luồng Hoạt Động

### 1. Thủ Thư Tạo Vi Phạm
- Thủ thư chọn sinh viên → Tạo vi phạm mới
- Hệ thống tự động trừ điểm reputation của sinh viên
- Vi phạm có trạng thái: `ACTIVE`

### 2. Sinh Viên Khiếu Nại
- Sinh viên xem danh sách vi phạm của mình
- Chọn vi phạm và gửi khiếu nại với lý do
- Trạng thái vi phạm chuyển thành: `APPEALED`
- Khiếu nại có trạng thái: `PENDING`

### 3. Thủ Thư Xử Lý Khiếu Nại
- Thủ thư xem danh sách khiếu nại đang chờ xử lý
- Có 2 lựa chọn:
  - **Chấp nhận**: Hoàn lại điểm cho sinh viên, vi phạm chuyển thành `CANCELLED`
  - **Từ chối**: Vi phạm vẫn giữ nguyên, chuyển thành `DISMISSED`

## API Endpoints

### Violation APIs

#### 1. Tạo Vi Phạm (Staff Only)
```http
POST /slib/violations
Authorization: Bearer <token>
Content-Type: application/json

{
  "studentId": "uuid",
  "violationReason": "Gây mất trật tự",
  "penaltyPoints": 10,
  "notes": "Vi phạm lần đầu"
}
```

#### 2. Xem Vi Phạm Của Sinh Viên
```http
GET /slib/violations/student/{studentId}
Authorization: Bearer <token>
```

#### 3. Sinh Viên Xem Vi Phạm Của Mình
```http
GET /slib/violations/my-violations
Authorization: Bearer <token>
```

### Appeal APIs

#### 4. Sinh Viên Tạo Khiếu Nại
```http
POST /slib/appeals
Authorization: Bearer <token>
Content-Type: application/json

{
  "violationId": "uuid",
  "appealReason": "Tôi không gây mất trật tự, chỉ nói chuyện nhỏ"
}
```

#### 5. Xem Tất Cả Khiếu Nại (Staff Only)
```http
GET /slib/appeals
Authorization: Bearer <token>
```

#### 6. Xem Khiếu Nại Đang Chờ (Staff Only)
```http
GET /slib/appeals/pending
Authorization: Bearer <token>
```

#### 7. Sinh Viên Xem Khiếu Nại Của Mình
```http
GET /slib/appeals/my-appeals
Authorization: Bearer <token>
```

#### 8. Thủ Thư Xử Lý Khiếu Nại (Staff Only)
```http
PUT /slib/appeals/{appealId}/review
Authorization: Bearer <token>
Content-Type: application/json

{
  "approved": true,  // true = chấp nhận, false = từ chối
  "reviewNotes": "Khiếu nại hợp lý"
}
```

## Database Schema

### Table: violation_records
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| student_id | UUID | FK to users |
| created_by | UUID | FK to users (thủ thư tạo) |
| rule_id | INT | FK to reputation_rules |
| violation_reason | TEXT | Lý do vi phạm |
| penalty_points | INT | Số điểm bị trừ |
| status | VARCHAR | ACTIVE, APPEALED, DISMISSED, CANCELLED |
| notes | TEXT | Ghi chú thêm |
| created_at | TIMESTAMP | Thời gian tạo |
| updated_at | TIMESTAMP | Thời gian cập nhật |

### Table: violation_appeals
| Column | Type | Description |
|--------|------|-------------|
| id | UUID | Primary key |
| violation_id | UUID | FK to violation_records |
| student_id | UUID | FK to users |
| appeal_reason | TEXT | Lý do khiếu nại |
| status | VARCHAR | PENDING, APPROVED, REJECTED |
| reviewed_by | UUID | FK to users (thủ thư xử lý) |
| review_notes | TEXT | Ghi chú khi xử lý |
| reviewed_at | TIMESTAMP | Thời gian xử lý |
| created_at | TIMESTAMP | Thời gian tạo |
| updated_at | TIMESTAMP | Thời gian cập nhật |

## Frontend Components

### 1. Trang Danh Sách Sinh Viên (List View)
- Hiển thị tất cả sinh viên với điểm đánh giá
- Filter: Gương mẫu / Khá / Trung bình
- Click vào sinh viên để xem chi tiết

### 2. Trang Chi Tiết Sinh Viên (Detail View)
#### Trái: Danh Sách Vi Phạm
- Bảng hiển thị:
  - Thời gian
  - Lỗi vi phạm
  - Số điểm trừ
  - Trạng thái khiếu nại (nếu có)

#### Phải: Danh Sách Khiếu Nại Chờ Xử Lý
- Hiển thị các khiếu nại có status = PENDING
- Mỗi khiếu nại hiển thị:
  - Thông tin vi phạm (ngày, lỗi, điểm trừ)
  - Lý do khiếu nại của sinh viên
  - 2 nút: Chấp nhận / Từ chối

## Cách Chạy

### 1. Chạy Migration Database
```bash
# Migration sẽ tự động chạy khi start Spring Boot app
# Hoặc chạy manual SQL script:
psql -U postgres -d slib_db -f backend/src/main/resources/db/migration/V004__add_violation_and_appeal_tables.sql
```

### 2. Start Backend
```bash
cd backend
./mvnw spring-boot:run
```

### 3. Start Frontend
```bash
cd frontend
npm install
npm run dev
```

### 4. Test Workflow
1. Login với tài khoản thủ thư
2. Vào "Quản lý vi phạm"
3. Chọn sinh viên → Tạo vi phạm (chưa có UI, dùng Postman)
4. Sinh viên login → Xem vi phạm → Tạo khiếu nại (chưa có UI)
5. Thủ thư xem khiếu nại ở trang chi tiết sinh viên
6. Chấp nhận/Từ chối khiếu nại

## TODO - Tính Năng Bổ Sung

- [ ] UI để thủ thư tạo vi phạm trực tiếp từ web
- [ ] UI để sinh viên xem và tạo khiếu nại
- [ ] Notification khi có khiếu nại mới
- [ ] Notification khi khiếu nại được xử lý
- [ ] Xuất báo cáo vi phạm theo sinh viên/thời gian
- [ ] Thống kê số lượng vi phạm/khiếu nại
- [ ] Mobile app integration

## Lưu Ý Kỹ Thuật

1. **Transaction Management**: Sử dụng `@Transactional` để đảm bảo tính nhất quán
2. **Authorization**: Kiểm tra quyền truy cập với `@PreAuthorize`
3. **Validation**: Validate input với `@Valid` và các annotation
4. **Error Handling**: Sử dụng RuntimeException, cần thêm custom exception handler
5. **Frontend State**: Mock data hiện tại, cần kết nối API thật

## Troubleshooting

### Lỗi: Table không tồn tại
```bash
# Chạy lại migration
./mvnw flyway:migrate
```

### Lỗi: Foreign key constraint
```bash
# Kiểm tra users table có dữ liệu
# Kiểm tra reputation_rules table tồn tại
```

### Lỗi: Frontend không hiển thị khiếu nại
```javascript
// Kiểm tra violations có dữ liệu
console.log(violations);

// Kiểm tra filter
console.log(violations.filter(v => v.hasAppeal && v.appealStatus === 'PENDING'));
```

## Contact & Support
- Developer: [Your Name]
- Email: youremail@example.com

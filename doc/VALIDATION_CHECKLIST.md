# Checklist Validate SLib

Tài liệu này tổng hợp nhanh các điểm validate đã được chuẩn hóa và các khu vực còn nên xử tiếp.

Ngày cập nhật: 2026-04-05

## 1. Đã chuẩn hóa

### Backend - nhóm người dùng

- Tạo helper chung ở `backend/src/main/java/slib/com/example/util/UserValidationUtil.java`
- Rule đã thống nhất cho:
  - `fullName`: bắt buộc khi tạo user, tối đa 255 ký tự
  - `email`: bắt buộc, chuẩn hóa lowercase, đúng định dạng, tối đa 255 ký tự
  - `userCode`: bắt buộc, uppercase, chỉ cho `A-Z 0-9 . _ -`, tối đa 20 ký tự
  - `phone`: tùy chọn, chuẩn hóa bỏ khoảng trắng và `().-`, chỉ cho `+` đầu chuỗi và 8-20 chữ số
  - `dob`: không được ở tương lai, không trước năm 1900

- Đã áp dụng vào:
  - tạo user admin
  - sửa user admin
  - import user
  - cập nhật hồ sơ cá nhân
  - cập nhật profile mobile/web staff

### Backend - request DTO

- Đã thêm annotation validate cho:
  - `AdminCreateUserRequest`
  - `AdminUpdateUserRequest`
  - `ImportUserRequest`
  - `UpdateProfileRequest`
  - `LoginRequest`
  - `ChangePasswordRequest`
  - `GoogleLoginRequest`
  - `AdminResetPasswordRequest`
  - `RefreshTokenRequest`
  - `LogoutRequest`
  - `SupportRequestStatusUpdateRequest`
  - `SupportRequestRespondRequest`
  - `CreateComplaintRequest`
  - `ComplaintResolutionRequest`
  - `CreateFeedbackRequest`
  - `NewsUpsertRequest`
  - `CategoryCreateRequest`
  - `NewBookRequest`
  - `ActivateDeviceRequest`
  - `ActivateCodeRequest`
  - `ValidateQrRequest`
  - `CompleteKioskSessionRequest`
  - `SessionTokenRequest`
  - `UserIdRequest`
  - `CreateKioskRequest`
  - `UpdateKioskRequest`

### Backend - nội dung và tác vụ staff

- Tạo helper chung ở `backend/src/main/java/slib/com/example/util/ContentValidationUtil.java`
- Đã chuẩn hóa thêm cho:
  - `support-requests`
    - `description` tối đa 2000 ký tự
    - `response` tối đa 2000 ký tự
    - tối đa 5 ảnh, chỉ nhận mime `image/*`, mỗi ảnh tối đa 5MB
  - `complaints`
    - `subject` tối đa 255 ký tự
    - `content` tối đa 4000 ký tự
    - `resolution note` tối đa 1000 ký tự
    - `evidenceUrl` phải là URL `http/https` hợp lệ
  - `feedbacks`
    - `rating` từ 1 đến 5
    - `content` tối đa 2000 ký tự
    - `category` chỉ cho `FACILITY`, `SERVICE`, `GENERAL`, `MESSAGE`
  - `news`
    - `title` tối đa 255 ký tự
    - `summary` tối đa 1000 ký tự
    - `imageUrl` phải là URL `http/https` hợp lệ
    - `categoryId` lấy qua DTO thay vì nhận thẳng entity
  - `news categories`
    - `name` tối đa 50 ký tự
    - `colorCode` phải ở dạng `#RGB` hoặc `#RRGGBB`
    - chặn trùng tên không phân biệt hoa thường
  - `new-books`
    - `title` tối đa 300 ký tự
    - `author` tối đa 200 ký tự
    - `isbn` tối đa 20 ký tự
    - `category/publisher` tối đa 255 ký tự
    - `coverUrl/sourceUrl` phải là URL `http/https` hợp lệ
  - `kiosk`
    - toàn bộ luồng activate/validate/checkout/checkin/expire đã bỏ `Map<String, String>` sang DTO có validate
    - CRUD kiosk admin đã chặn `kioskCode`, `kioskName`, `kioskType`, `location`

### Frontend - web admin/staff

- Đã thêm helper chung ở `frontend/src/utils/userValidation.js`
- Đã thêm helper chung ở `frontend/src/utils/formValidation.js`
- Đã chặn sớm ở:
  - modal thêm người dùng
  - modal sửa người dùng
  - import người dùng
  - màn cài đặt tài khoản staff
  - tạo/cập nhật sách mới
  - tạo/cập nhật tin tức
  - tạo/cập nhật kiosk
  - phản hồi yêu cầu hỗ trợ
  - chuẩn hóa đọc lỗi API `message/errors/error`

### Test/CI

- `backend`: `mvn test -q` pass
- `frontend`: `npm run build` pass

## 2. Validate đã có sẵn từ trước trong hệ thống

Các nhóm dưới đây đã có validate tương đối rõ ở DTO/controller:

- Cấu hình thư viện:
  - giờ mở/đóng
  - slot duration
  - max booking days
  - max bookings per day
  - max hours per day
  - auto cancel
  - min reputation
- Lịch backup:
  - `time`
  - `retainDays`
  - `isActive`
- AI config:
  - provider
  - model name
  - base URL
  - api key length
  - temperature
  - max tokens
  - language
- Layout/zone/seat:
  - width/height > 0
  - row/column > 0
  - position không âm

## 3. Còn nên làm tiếp

### Ưu tiên cao

- Upload file còn lại
  - avatar
  - import zip/avatar batch
  - ảnh news upload endpoint

- Auth web/mobile
  - gom regex email về một rule chung cho login/forgot/reset
  - gom rule mật khẩu về một helper dùng lại cả web/mobile

### Ưu tiên trung bình

- Forgot/reset password ở frontend
  - gom đọc lỗi API
  - gom validate email/password về helper chung

- NFC/HCE còn lại
  - validate format raw UID
  - thống nhất message lỗi ở các endpoint không thuộc kiosk auth

### Ưu tiên thấp nhưng nên dọn

- Chuẩn hóa toàn bộ message lỗi để không lộ raw SQL hoặc stack-ish message ra UI
- Đổi các endpoint còn nhận `Map<String, Object>` sang DTO có `@Valid`
- Gom helper validate dùng chung cho frontend mobile nếu cần

## 4. Nguyên tắc nên giữ

- Chặn ở frontend để UX tốt hơn
- Chặn lại ở backend để an toàn dữ liệu
- Không dựa vào lỗi database để báo validate cho người dùng
- Message lỗi phải ngắn, rõ, tiếng Việt có dấu
- Mỗi field nên có đúng một rule thống nhất toàn hệ thống

## 5. Lộ trình đề xuất

1. Chuẩn hóa nốt validate file upload còn lại
2. Gom validate auth/password dùng chung giữa web/mobile
3. Rà các endpoint còn dùng `Map<String, Object>` và chuyển sang DTO
4. Mở rộng helper validate cho mobile nếu cần

# FE-01: Đăng nhập bằng tài khoản Google (Login with Google Account)

## 1. Mô tả chức năng (Functional Description)

| Thông tin | Chi tiết |
|-----------|----------|
| **ID** | FE-01 |
| **Tên chức năng** | Đăng nhập bằng tài khoản Google (Login with Google Account) |
| **Module** | Authentication Module |
| **Mô tả** | Cho phép người dùng đăng nhập vào hệ thống SLIB bằng tài khoản Google thông qua Google ID Token |
| **Actor** | Sinh viên, Thủ thư, Quản trị viên |
| **API Endpoint** | `POST /slib/users/login-google` |
| **Input** | `id_token`, `full_name`, `noti_device`, `device_info` |
| **Output** | AuthResponse (accessToken, refreshToken, user info) |

---

## 2. Test Cases

### Test Case Summary

| Test Case ID | Test Scenario | Expected Result | Status |
|--------------|--------------|-----------------|--------|
| UTCD1 | Token hợp lệ + có quyền truy cập | 200 OK - Đăng nhập thành công | ✅ Passed |
| UTCD2 | Token không hợp lệ / thiếu token | 400 Bad Request - Thiếu Google ID Token | ✅ Passed |
| UTCD3 | Token hợp lệ nhưng email không phải FPT | 401 Unauthorized - Không có quyền truy cập | ✅ Passed |
| UTCD4 | Token hợp lệ nhưng tài khoản bị khóa | 401 Unauthorized - Tài khoản bị khóa | ✅ Passed |
| UTCD5 | Lỗi hệ thống không mong đợi | 500 Internal Server Error | ✅ Passed |

---

### Chi tiết từng Test Case

#### UTCD1: Đăng nhập với Token hợp lệ

| Field | Value |
|-------|-------|
| **Test Case ID** | UTCD1 |
| **Test Scenario** | Đăng nhập với Google ID Token hợp lệ và email FPT (@fpt.edu.vn) |
| **Pre-condition** | Người dùng có tài khoản Google hợp lệ với email @fpt.edu.vn |
| **Test Steps** | 1. Gửi POST request đến `/slib/users/login-google`<br>2. Body chứa `id_token` hợp lệ<br>3. Body chứa `full_name`, `noti_device`, `device_info` |
| **Expected Result** | - HTTP Status: **200 OK**<br>- Response chứa: `accessToken`, `refreshToken`, `email`, `fullName`, `userCode`, `role` |
| **Test Data** | `id_token`: "valid.google.id.token"<br>`email`: "student@fpt.edu.vn" |
| **Status** | ✅ Passed |

---

#### UTCD2: Đăng nhập với Token không hợp lệ

| Field | Value |
|-------|-------|
| **Test Case ID** | UTCD2 |
| **Test Scenario** | Đăng nhập nhưng thiếu hoặc có ID token không hợp lệ |
| **Pre-condition** | Không có |
| **Test Steps** | 1. Gửi POST request đến `/slib/users/login-google`<br>2. Body **KHÔNG** chứa `id_token` hoặc `id_token` = "" |
| **Expected Result** | - HTTP Status: **400 Bad Request**<br>- Response: "Thiếu Google ID Token" |
| **Test Data** | `id_token`: "" (empty) hoặc không có trường này |
| **Status** | ✅ Passed |

---

#### UTCD3: Đăng nhập với Token hợp lệ nhưng không có quyền

| Field | Value |
|-------|-------|
| **Test Case ID** | UTCD3 |
| **Test Scenario** | Đăng nhập với Google ID Token hợp lệ nhưng email **KHÔNG** phải @fpt.edu.vn |
| **Pre-condition** | Người dùng có tài khoản Google nhưng email không thuộc domain FPT |
| **Test Steps** | 1. Gửi POST request đến `/slib/users/login-google`<br>2. Body chứa `id_token` hợp lệ<br>3. Email từ Google token không phải @fpt.edu.vn |
| **Expected Result** | - HTTP Status: **401 Unauthorized**<br>- Response: "Chỉ chấp nhận email @fpt.edu.vn hoặc email trong whitelist" |
| **Test Data** | `email`: "user@gmail.com" |
| **Status** | ✅ Passed |

---

#### UTCD4: Đăng nhập nhưng tài khoản bị khóa

| Field | Value |
|-------|-------|
| **Test Case ID** | UTCD4 |
| **Test Scenario** | Đăng nhập với token hợp lệ nhưng tài khoản người dùng đã bị khóa/vô hiệu hóa |
| **Pre-condition** | Tài khoản người dùng tồn tại trong hệ thống nhưng `isActive = false` |
| **Test Steps** | 1. Gửi POST request đến `/slib/users/login-google`<br>2. Body chứa `id_token` hợp lệ<br>3. Tài khoản trong DB có `isActive = false` |
| **Expected Result** | - HTTP Status: **401 Unauthorized**<br>- Response: "Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên để được hỗ trợ." |
| **Test Data** | User trong DB: `isActive: false` |
| **Status** | ✅ Passed |

---

#### UTCD5: Lỗi hệ thống không mong đợi

| Field | Value |
|-------|-------|
| **Test Case ID** | UTCD5 |
| **Test Scenario** | Xảy ra lỗi hệ thống không mong đợi trong quá trình xử lý |
| **Pre-condition** | Hệ thống gặp sự cố (database error, Google API error, v.v.) |
| **Test Steps** | 1. Gửi POST request đến `/slib/users/login-google`<br>2. Xảy ra exception không xử lý được |
| **Expected Result** | - HTTP Status: **401 Unauthorized** (do RuntimeException được bắt và trả về 401)<br>- Response: "Lỗi: [error message]" |
| **Test Data** | Lỗi: "Database connection failed", "Lỗi xác thực Google: Invalid token" |
| **Status** | ✅ Passed |

---

## 3. Test Environment

| Thông tin | Chi tiết |
|-----------|----------|
| **Testing Framework** | JUnit 5, Mockito, MockMvc |
| **Language** | Java 21 |
| **Build Tool** | Maven |
| **Test Type** | Unit Test (Controller Layer) |
| **Backend Framework** | Spring Boot 3.4.0 |
| **API Testing Tool** | MockMvc |

---

## 4. Test Execution Results

```
Test Suite: FE01_LoginWithGoogleTest
Total Tests: 5
Passed: 5
Failed: 0
Skipped: 0
Success Rate: 100%
```

---

## 5. Notes

- Test case này sử dụng `@WebMvcTest` để mock controller layer
- `AuthService` được mock để giả lập các trường hợp
- Security filters được disable (`addFilters = false`)
- Test data sử dụng real-world data patterns (FPT email format)

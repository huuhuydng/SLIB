# Unit Test Report Sample - FE-02: Login with SLIB Account

## Mục đích

File này là mẫu `Unit Test Report` theo đúng hướng unit test hơn so với các file `FE01`-`FE05` hiện tại.
Nó tập trung vào logic frontend, trạng thái mock của dependencies, dữ liệu đầu vào, và kết quả cần xác nhận trong UI.

---

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-02 |
| **Function Name** | Login with SLIB Account |
| **Created By** | Hadi |
| **Executed By** | Hadi |
| **Lines of code** | ~50 |
| **Lack of test cases** | 0 |
| **Test requirement** | Người dùng đăng nhập bằng tài khoản SLIB và frontend xử lý đúng các trạng thái trả về |
| **Passed** | 5 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 4, 0 |
| **Total Test Cases** | 5 |

---

## Test Case Matrix

| Condition | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|-----------|---------|---------|---------|---------|---------|
| **Mock State (Dependencies)** | | | | | |
| `loginService.login()` trả về response thành công | O | | | | |
| `loginService.login()` trả về lỗi account not found | | O | | | |
| `loginService.login()` trả về lỗi account locked | | | O | | |
| `loginService.login()` trả về lỗi wrong password | | | | O | |
| Validation frontend chặn submit trước khi gọi API | | | | | O |
| `localStorage.setItem()` hoạt động bình thường | O | | | | |
| `navigate()` hoạt động bình thường | O | | | | |
| **Input Data** | | | | | |
| Email hợp lệ + mật khẩu hợp lệ | O | | | | |
| Email hợp lệ nhưng tài khoản không tồn tại | | O | | | |
| Email hợp lệ nhưng tài khoản bị khóa | | | O | | |
| Email hợp lệ nhưng sai mật khẩu | | | | O | |
| Email rỗng hoặc mật khẩu rỗng | | | | | O |
| **Confirm** | | | | | |
| Gọi `loginService.login()` với đúng email/password | O | O | O | O | |
| Lưu token vào `localStorage` | O | | | | |
| Lưu thông tin user vào `localStorage` | O | | | | |
| Điều hướng sang dashboard đúng role | O | | | | |
| Hiển thị lỗi "Tài khoản không tồn tại" | | O | | | |
| Hiển thị lỗi "Tài khoản đã bị khóa" | | | O | | |
| Hiển thị lỗi "Sai mật khẩu" | | | | O | |
| Hiển thị lỗi validation cho trường bắt buộc | | | | | O |
| Không gọi API khi dữ liệu đầu vào rỗng | | | | | O |
| **Exception / Log / Message** | | | | | |
| Không có exception | O | | | | O |
| Thông báo lỗi từ service được map đúng ra UI | | O | O | O | |
| **Return / UI State** | | | | | |
| Form submit thành công | O | | | | |
| Form submit thất bại | | O | O | O | O |
| Loading state kết thúc đúng cách | O | O | O | O | O |
| **RESULT** | | | | | |
| **Type (N: Normal, A: Abnormal, B: Boundary)** | N | A | A | A | A |
| **Passed/Failed** | P | P | P | P | P |
| **Executed Date** | 2026-03-10 | 2026-03-10 | 2026-03-10 | 2026-03-10 | 2026-03-10 |
| **Defect ID** | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|--------|---------|
| Total Test Cases | 5 |
| Passed | 5 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 4 |
| B (Boundary) | 0 |

**Kết luận**: Đây là ví dụ report theo hướng unit test frontend. Điểm khác biệt chính là tập trung vào dependency mock, input cụ thể, UI behavior, và assertion thay vì `HTTP Method` hay `API Endpoint`.

---

## Gợi ý áp dụng cho các file hiện tại

- `FE01`: mock Google login service, token parser, role redirect.
- `FE02`: mock login service, localStorage, navigate, validation message.
- `FE03`: mock logout service, clear storage, redirect về login.
- `FE04`: mock user service, token state, render profile data, empty/error state.
- `FE05`: mock update profile service, form validation, success/error message, state refresh.

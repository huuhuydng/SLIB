# Test Report - FE-07: View Barcode

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-07 |
| **Function Name** | View Barcode |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~10 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 3 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 2, 1, 0 |
| **Total Test Cases** | 3 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 |
|-------|---------|---------|---------|
| **CONDITION** | | | |
| **Precondition** | | | |
| Authorized | ⚪ | ⚪ | |
| **HTTP Method** | | | |
| GET | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | |
| /slib/users/me (profile chứa userCode) | ⚪ | ⚪ | ⚪ |
| **Input** | | | |
| Valid JWT Token, userCode exists | ⚪ | | |
| Valid JWT Token, userCode = null or empty | | ⚪ | |
| No token (not logged in) | | | ⚪ |
| **CONFIRM** | | | |
| **Return** | | | |
| 200: OK (barcode rendered from userCode) | ⚪ | | |
| 200: OK (barcode rendered empty / hidden) | | ⚪ | |
| 401: Unauthorized | | | ⚪ |
| **Exception** | | | |
| None | ⚪ | ⚪ | |
| RuntimeException | | | ⚪ |
| **Log message** | | | |
| "Barcode rendered with userCode" | ⚪ | | |
| "Barcode rendered empty" | | ⚪ | |
| "No token provided" | | | ⚪ |
| **RESULT** | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | |
| N | ⚪ | ⚪ | |
| A | | | ⚪ |
| **Passed/Failed** | | | |
| P | ⚪ | ⚪ | ⚪ |
| F | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 3 |
| Passed | 3 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 2 |
| A (Abnormal) | 1 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

**Ghi chú**: Chức năng View Barcode là client-side rendering. Mobile app lấy `userCode` từ `AuthService.currentUser` (đã có sẵn sau login qua `GET /slib/users/me`) rồi dùng `BarcodeWidget` (Code128) để render barcode trực tiếp. Nếu không có mã (`userCode` rỗng), mobile sẽ không hiển thị mã giả để tránh nhầm lẫn.

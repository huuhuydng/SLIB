# Test Report - FE-121: View Analytics Dashboard

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-121 |
| **Function Name** | View Analytics Dashboard |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~30 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 5 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 4, 0 |
| **Total Test Cases** | 5 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|-------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | |
| **Precondition** | | | | | |
| Valid JWT + Librarian role | ⚪ | | | | |
| No token provided | | ⚪ | | | |
| Token expired | | | ⚪ | | |
| Invalid token format | | | | ⚪ | |
| Valid JWT + Student role | | | | | ⚪ |
| **HTTP Method** | | | | | |
| GET | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/dashboard | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| **Authorization** | | | | | |
| Valid JWT Token (Librarian) | ⚪ | | | | |
| No Token | | ⚪ | | | |
| Expired Token | | | ⚪ | | |
| Invalid Token | | | | ⚪ | |
| Valid JWT Token (Student) | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | ⚪ | ⚪ | |
| 403: Forbidden | | | | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Dashboard data retrieved successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Expired JWT token" | | | ⚪ | | |
| "Invalid JWT token format" | | | | ⚪ | |
| "Access denied - insufficient permissions" | | | | | ⚪ |
| **RESULT** | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | |
| N | ⚪ | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ |
| B | | | | | |
| **Passed/Failed** | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | |

---

## Chi tiết từng Test Case

### UTCID01 - Xem dashboard thành công (Librarian)

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | View analytics dashboard with valid librarian token |
| **Precondition** | Valid JWT token with LIBRARIAN role |
| **HTTP Method** | GET |
| **API Endpoint** | `/slib/dashboard` |
| **Input** | Authorization: Bearer {librarian_jwt_token} |
| **Expected Return** | 200 OK |
| **Exception** | None |
| **Log Message** | "Dashboard data retrieved successfully for librarian" |
| **Type** | N (Normal) |
| **Passed/Failed** | P (Passed) |
| **Executed Date** | 2026-03-07 |
| **Defect ID** | - |

---

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | View dashboard without authentication token |
| **Precondition** | No token provided |
| **HTTP Method** | GET |
| **API Endpoint** | `/slib/dashboard` |
| **Input** | No Authorization header |
| **Expected Return** | 401 Unauthorized |
| **Exception** | RuntimeException("Unauthorized access") |
| **Log Message** | "No token provided" |
| **Type** | A (Abnormal) |
| **Passed/Failed** | P (Passed) |
| **Executed Date** | 2026-03-07 |
| **Defect ID** | - |

---

### UTCID03 - Token hết hạn

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID03 |
| **Test Scenario** | View dashboard with expired token |
| **Precondition** | Token expired |
| **HTTP Method** | GET |
| **API Endpoint** | `/slib/dashboard` |
| **Input** | Authorization: Bearer {expired_token} |
| **Expected Return** | 401 Unauthorized |
| **Exception** | RuntimeException("Token đã hết hạn") |
| **Log Message** | "Expired JWT token" |
| **Type** | A (Abnormal) |
| **Passed/Failed** | P (Passed) |
| **Executed Date** | 2026-03-07 |
| **Defect ID** | - |

---

### UTCID04 - Token không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | View dashboard with invalid token format |
| **Precondition** | Invalid token format |
| **HTTP Method** | GET |
| **API Endpoint** | `/slib/dashboard` |
| **Input** | Authorization: Bearer {invalid_token} |
| **Expected Return** | 401 Unauthorized |
| **Exception** | RuntimeException("Invalid token format") |
| **Log Message** | "Invalid JWT token format" |
| **Type** | A (Abnormal) |
| **Passed/Failed** | P (Passed) |
| **Executed Date** | 2026-03-07 |
| **Defect ID** | - |

---

### UTCID05 - User không có quyền (Student role)

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | View dashboard with student role - forbidden |
| **Precondition** | Valid JWT token with STUDENT role |
| **HTTP Method** | GET |
| **API Endpoint** | `/slib/dashboard` |
| **Input** | Authorization: Bearer {student_jwt_token} |
| **Expected Return** | 403 Forbidden |
| **Exception** | RuntimeException("Bạn không có quyền truy cập chức năng này") |
| **Log Message** | "Access denied - insufficient permissions" |
| **Type** | A (Abnormal) |
| **Passed/Failed** | P (Passed) |
| **Executed Date** | 2026-03-07 |
| **Defect ID** | - |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 5 |
| Passed | 5 |
| Failed | 0 |
| Untested | 0 |
| N/A | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 4 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass. Chức năng FE-121 hoạt động đúng theo yêu cầu.

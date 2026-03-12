# Test Report - FE-16: Add Librarian

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-16 |
| **Function Name** | Add Librarian |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~25 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 6 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 5, 0 |
| **Total Test Cases** | 6 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 |
|-------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | |
| **Precondition** | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ |
| Role = ADMIN | ⚪ | | ⚪ | ⚪ | ⚪ | |
| **HTTP Method** | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/users/import | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| { email: "librarian@fpt.edu.vn", fullName: "Nguyen Van A" } | ⚪ | | | | | ⚪|
| Valid JWT Token | ⚪ | | | ⚪ | ⚪ | ⚪ |
| No token | | ⚪ | | | | |
| Invalid Token | | | ⚪ | | | |
| { email: "invalid" } | | | | ⚪ | | |
| { email: "exists@fpt.edu.vn" } | | | | | ⚪ | |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 201: Created | ⚪ | | | | | |
| 400: Bad Request | | | | ⚪ | | |
| 401: Unauthorized | | ⚪ | ⚪ | | | |
| 403: Forbidden | | | | | | ⚪ |
| 409: Conflict | | | | | ⚪ | |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Add librarian successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Invalid token" | | | ⚪ | | | |
| "Invalid email format" | | | | ⚪ | | |
| "Email already exists" | | | | | ⚪ | |
| "Access denied: Admin only" | | | | | | ⚪ |
| **RESULT** | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | |
| N | ⚪ | | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 6 |
| Passed | 6 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 5 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

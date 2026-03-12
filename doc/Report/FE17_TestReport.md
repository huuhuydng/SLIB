# Test Report - FE-17: View User Details

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-17 |
| **Function Name** | View User Details |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~15 |
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
| Role = ADMIN/LIBRARIAN | ⚪ | | ⚪ | ⚪ | ⚪ | |
| **HTTP Method** | | | | | | |
| GET | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/users/{userId} | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid JWT Token + userId | ⚪ | | | | | ⚪ |
| No token | | ⚪ | | | | |
| Invalid Token | | | ⚪ | | | |
| Valid JWT Token + userId (deleted) | | | | ⚪ | | |
| Invalid userId format | | | | | ⚪ | |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: Success | ⚪ | | | | | |
| 401: Unauthorized | | ⚪ | ⚪ | | | |
| 403: Forbidden | | | | | | ⚪ |
| 404: Not Found | | | | ⚪ | | |
| 400: Bad Request | | | | | ⚪ | |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Get user details successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Invalid token" | | | ⚪ | | | |
| "User not found" | | | | ⚪ | | |
| "Invalid userId format" | | | | | ⚪ | |
| "Access denied: Admin/Librarian only" | | | | | | ⚪ |
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

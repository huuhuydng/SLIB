# Test Report - FE-38: Enable/Disable Library

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-38 |
| **Function Name** | Enable/Disable Library |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~20 |
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
| Admin logged in | ⚪ | | | | |
| User logged in | | ⚪ | | | |
| User logged in, not admin | | | ⚪ | | |
| Admin logged in, library already in target state | | | | ⚪ | |
| No precondition | | | | | ⚪ |
| **HTTP Method** | | | | | |
| POST/PUT | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/settings/library/status | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid admin JWT Token + valid status | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Valid non-admin JWT Token | | | ⚪ | | |
| Valid admin JWT Token + same status | | | | ⚪ | |
| System error simulation | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 400: Bad Request | | | | ⚪ | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | ⚪ | | |
| 409: Conflict | | | | ⚪ | |
| 500: Internal Server Error | | | | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Library status updated successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Access denied" | | | ⚪ | | |
| "Library already in target state" | | | | ⚪ | |
| "System error" | | | | | ⚪ |
| **RESULT** | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | |
| N | ⚪ | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 5 |
| Passed | 5 |
| Failed | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 4 |

**Kết luận**: Tất cả test cases đã pass.

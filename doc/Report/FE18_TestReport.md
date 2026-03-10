# Test Report - FE-18: Change User Status

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-18 |
| **Function Name** | Change User Status |
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
| Role = ADMIN | ⚪ | | ⚪ | ⚪ | ⚪ | |
| **HTTP Method** | | | | | | |
| PATCH | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/users/{userId}/status | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid JWT Token | ⚪ | |  | ⚪ | ⚪ | ⚪ |
| userId (valid) | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ |
| isActive: true | ⚪ | ⚪ | ⚪| ⚪ | | ⚪ |
| No token | | ⚪ | | | | |
| Invalid Token | | | ⚪ | | | |
| userId (not found) | | | | ⚪ | | |
| isActive: 3 | | | | | ⚪ | |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: Success | ⚪ | | | | | |
| 400: Bad Request | | | | ⚪ | ⚪ | |
| 401: Unauthorized | | ⚪ | ⚪ | | | |
| 403: Forbidden | | | | | | ⚪ |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Change user status successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Invalid token" | | | ⚪ | | | |
| "User not found" | | | | ⚪ | | |
| "Invalid isActive value" | | | | | ⚪ | |
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

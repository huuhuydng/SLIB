# Test Report - FE-02: Login with SLIB Account

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-02 |
| **Function Name** | Login with SLIB Account |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~50 |
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
| Account exists, not locked | ⚪ | | | ⚪ | ⚪ |
| Account does not exist | | ⚪ | | | |
| Account exists, locked | | | ⚪ | | |
| **HTTP Method** | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/auth/login | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid credentials | ⚪ | ⚪ | ⚪ | | |
| Invalid credentials | | | | ⚪ | |
| Empty credentials | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: Success | ⚪ | | | | |
| 400: Bad Request | | | | | ⚪ |
| 401: Unauthorized | | | | ⚪ | |
| 403: Forbidden | | ⚪ | ⚪ | | |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Login successful" | ⚪ | | | | |
| "Failed to log in: Account not found" | | ⚪ | | | |
| "Failed to log in: Account locked" | | | ⚪ | | |
| "Failed to log in: Wrong password" | | | | ⚪ | |
| "Failed to log in: Email and password are required" | | | | | ⚪ |
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
| Untested | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 4 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

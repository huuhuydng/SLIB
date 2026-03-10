# Test Report - FE-01: Login with Google Account

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-01 |
| **Function Name** | Login with Google Account |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~50 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 6 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 2, 4, 0 |
| **Total Test Cases** | 6 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 |
|-------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | |
| **Precondition** | | | | | | |
| Account exists, not locked | ⚪ | | | | | |
| Account does not exist | | ⚪ | | | | |
| Non-FPT email domain | | | ⚪ | | | |
| Account exists, locked | | | | ⚪ | | |
| **HTTP Method** | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/auth/google | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid Google token | ⚪ | ⚪ | | | | |
| Invalid Google token | | | ⚪ | | | |
| No token in request | | | | | ⚪ | |
| Valid Google token | | | | | | ⚪ |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: Success | ⚪ | ⚪ | | | | |
| 400: Bad Request | | | | | ⚪ | |
| 401: Unauthorized | | | ⚪ | | | |
| 403: Forbidden | | | | ⚪ | | |
| 500: Internal Server Error | | | | | | ⚪ |
| **Exception** | | | | | | |
| None | ⚪ | ⚪ | | | | |
| BadRequestException | | | | | ⚪ | |
| UnauthorizedException | | | ⚪ | | | |
| ForbiddenException | | | | ⚪ | | |
| RuntimeException | | | | | | ⚪ |
| **Log message** | | | | | | |
| "Login successful" | ⚪ | | | | | |
| "New user created" | | ⚪ | | | | |
| "Non-FPT email rejected" | | | ⚪ | | | |
| "Account locked" | | | | ⚪ | | |
| "Missing token" | | | | | ⚪ | |
| "Unexpected error from Google" | | | | | | ⚪ |
| **RESULT** | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | |
| N | ⚪ | ⚪ | | | | |
| A | | | ⚪ | ⚪ | ⚪ | ⚪ |
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
| N (Normal) | 2 |
| A (Abnormal) | 4 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

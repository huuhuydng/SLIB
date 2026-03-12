# Test Report - FE-37: Auto Checkout Setting

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-37 |
| **Function Name** | Auto Checkout Setting |
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
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ |
| Role = ADMIN | ⚪ | | | ⚪ | |
| **HTTP Method** | | | | | |
| POST/PUT | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/settings/auto-checkout | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Valid non-admin JWT Token | | | ⚪ | | |
| Valid JWT Token + invalid settings | | | | ⚪ | |
| Non-admin role | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 400: Bad Request | | | | ⚪ | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | ⚪ | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | ⚪ | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ |
| **Log message** | | | | | |
| "Auto checkout setting updated successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Access denied" | | | ⚪ | | ⚪ |
| "Invalid settings" | | | | ⚪ | |
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

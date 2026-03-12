# Test Report - FE-28: Lock Zone Movement

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-28 |
| **Function Name** | Lock Zone Movement |
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
| Authorized | ⚪ | | ⚪ | | ⚪ |
| Role = ADMIN | ⚪ | | | | |
| Zone exists | ⚪ | | | ⚪ | |
| **HTTP Method** | | | | | |
| POST/PATCH | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/zones/{id}/lock | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Zone not found | | | ⚪ | | |
| Zone already locked | | | | ⚪ | |
| Non-admin role | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | | | ⚪ |
| 404: Not Found | | | ⚪ | | |
| 409: Conflict | | | | ⚪ | |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Zone locked successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Zone not found" | | | ⚪ | | |
| "Zone already locked" | | | | ⚪ | |
| "Access denied" | | | | | ⚪ |
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

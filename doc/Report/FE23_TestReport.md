# Test Report - FE-23: Lock Area Movement

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-23 |
| **Function Name** | Lock Area Movement |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~20 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 4 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 3, 0 |
| **Total Test Cases** | 4 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 |
|-------|---------|---------|---------|---------|
| **CONDITION** | | | | |
| **Precondition** | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ |
| Role = ADMIN | ⚪ | | ⚪ | |
| Area exists | ⚪ | |⚪ | ⚪ |
| **HTTP Method** | | | | |
| PUT | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | |
| /slib/areas/{id}/locked | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | |
| Valid JWT Token | ⚪ | | | |
| No token in request | | ⚪ | | |
| Area not found | | | ⚪ | |
| **CONFIRM** | | | | |
| **Return** | | | | |
| 200: OK | ⚪ | | | |
| 401: Unauthorized | | ⚪ | | |
| 404: Not Found | | | ⚪ | |
| 403: Forbidden | | | | ⚪ |
| **Exception** | | | | |
| None | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | |
| "Area locked/unlocked successfully" | ⚪ | | | |
| "No token provided" | | ⚪ | | |
| "Area not found" | | | ⚪ | |
| "Access denied" | | | | ⚪ |
| **RESULT** | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | |
| N | ⚪ | | | |
| A | | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 4 |
| Passed | 4 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 3 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

# Test Report - FE-22: Change Area Status

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-22 |
| **Function Name** | Change Area Status |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~25 |
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
| Role = ADMIN | ⚪ | | ⚪ | ⚪ |  |
| Area exists | ⚪ | | | ⚪ | ⚪ |
| **HTTP Method** | | | | | |
| PATCH | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/areas/{id}/status | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | ⚪ | ⚪ | ⚪ |
| isActive: true | ⚪ | ⚪ | | | ⚪ |
| No token | | ⚪ | | | |
| Area not found | | | ⚪ | | |
| isActive: 3 | | | | ⚪ | |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 400: Bad Request | | | | ⚪ | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | | | ⚪ |
| 404: Not Found | | | ⚪ | | |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Area status updated successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Area not found" | | | ⚪ | | |
| "Invalid status value" | | | | ⚪ | |
| "Access denied: Admin only" | | | | | ⚪ |
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

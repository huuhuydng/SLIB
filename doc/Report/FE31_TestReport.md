# Test Report - FE-31: Change Seat Status

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-31 |
| **Function Name** | Change Seat Status |
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
| Authorized | ⚪ | | ⚪ | | ⚪ |
| Role = ADMIN | ⚪ | | | | |
| Seat exists | ⚪ | | | ⚪ | |
| **HTTP Method** | | | | | |
| PATCH/PUT | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/seats/{id}/status | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Invalid status value | | | ⚪ | | |
| Seat not found | | | | ⚪ | |
| Non-admin role | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 400: Bad Request | | | ⚪ | | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | | | ⚪ |
| 404: Not Found | | | | ⚪ | |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Seat status updated successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Invalid status value" | | | ⚪ | | |
| "Seat not found" | | | | ⚪ | |
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

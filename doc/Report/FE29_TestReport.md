# Test Report - FE-29: View Seat Map

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-29 |
| **Function Name** | View Seat Map |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~30 |
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
| Zone exists | ⚪ | | | | |
| **HTTP Method** | | | | | |
| GET | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/zones/{zoneId}/seats | ⚪ | ⚪ | ⚪ | ⚪ | |
| /slib/seats/map | ⚪ | ⚪ | ⚪ | | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Zone not found | | | ⚪ | | |
| No permission | | | | ⚪ | |
| Invalid zoneId format | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | | ⚪ | |
| 404: Not Found | | | ⚪ | | |
| 400: Bad Request | | | | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "Seat map retrieved successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Zone not found" | | | ⚪ | | |
| "Access denied" | | | | ⚪ | |
| "Invalid zone ID format" | | | | | ⚪ |
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

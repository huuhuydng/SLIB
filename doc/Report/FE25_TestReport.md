# Test Report - FE-25: CRUD Zone

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-25 |
| **Function Name** | CRUD Zone |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~60 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 17 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 4, 13, 0 |
| **Total Test Cases** | 17 |

---

## Test Case Matrix

| UTCID | UTC01 | UTC02 | UTC03 | UTC04 | UTC05 | UTC06 | UTC07 | UTC08 | UTC09 | UTC10 | UTC11 | UTC12 | UTC13 | UTC14 | UTC15 | UTC16 | UTC17 |
|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|-------|
| **CONDITION** | | | | | | | | | | | | | | | | | |
| **Precondition** | | | | | | | | | | | | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| Role = ADMIN | | | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | | ⚪ | | ⚪ | ⚪ |
| Area exists | | | | ⚪ | | | | | | | | | | | | | |
| Zone exists | | | | | | | | | ⚪ | | | ⚪ | | ⚪ | | | ⚪ |
| **HTTP Method** | | | | | | | | | | | | | | | | | |
| GET | ⚪ | ⚪ | ⚪ | | | | | | | | | | | | | | |
| POST | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | | | | | | | | |
| PATCH | | | | | | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | | | |
| DELETE | | | | | | | | | | | | | | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | | | | | | | | | | | |
| /slib/zones | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | | | | | | | | |
| /slib/zones/{id} | | | ⚪ | | | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | | | | | | | | | | | |
| Valid JWT Token | ⚪ | | | | | | | | | | | | | | | | |
| No token | | ⚪ | | | | ⚪ | | | | ⚪ | | | | | ⚪ | | |
| Valid data | | | | ⚪ | | | | | ⚪ | | | | | | | | |
| Area not found | | | | | ⚪ | | | | | | | | | | | | |
| Non-admin role | | | | | | | ⚪ | | | | ⚪ | | | | | ⚪ | |
| Duplicate zone name | | | | | | | | ⚪ | | | | | | | | | |
| Zone not found | | | ⚪ | | | | | | | | | ⚪ | | | | | |
| Valid data (PATCH) | | | | | | | | | | | | | ⚪ | | | | |
| Invalid data (PATCH) | | | | | | | | | | | | ⚪ | | | | |
| Valid data (DELETE) | | | | | | | | | | | | | | ⚪ | | | |
| **CONFIRM** | | | | | | | | | | | | | | | | | |
| **Return** | | | | | | | | | | | | | | | | | |
| 200: OK | ⚪ | | | | | | | | ⚪ | | | | | ⚪ | | | |
| 201: Created | | | | ⚪ | | | | | | | | | | | | | |
| 400: Bad Request | | | | | | | | | | | | ⚪ | | | | | |
| 401: Unauthorized | | ⚪ | | | | ⚪ | | | | ⚪ | | | | | ⚪ | | |
| 403: Forbidden | | | | | | | ⚪ | | | | ⚪ | | | | | ⚪ | |
| 404: Not Found | | | ⚪ | | ⚪ | | | | | | | ⚪ | | | | | |
| 409: Conflict | | | | | | | | ⚪ | | | | | | | | | |
| **Exception** | | | | | | | | | | | | | | | | | |
| None | ⚪ | | | ⚪ | | | | | ⚪ | | | | | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | | | | | | | | | | | | |
| "Zone retrieved successfully" | ⚪ | | | | | | | | | | | | | | | | |
| "No token provided" | | ⚪ | | | | | | | | | | | | | | | |
| "Zone not found" | | | ⚪ | | | | | | | | | | | | | | |
| "Zone created successfully" | | | | ⚪ | | | | | | | | | | | | | |
| "Area not found" | | | | | ⚪ | | | | | | | | | | | | |
| "No token provided" | | | | | | ⚪ | | | | | | | | | | | |
| "Access denied" | | | | | | | ⚪ | | | | | | | | | | |
| "Zone name already exists" | | | | | | | | ⚪ | | | | | | | | | |
| "Zone updated successfully" | | | | | | | | | ⚪ | | | | | | | | |
| "Invalid request parameters" | | | | | | | | | | | | ⚪ | | | | | |
| "Zone deleted successfully" | | | | | | | | | | | | | | ⚪ | | | |
| **RESULT** | | | | | | | | | | | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | | | | | | | | | | | |
| N | ⚪ | | | ⚪ | | | | | ⚪ | | | | | ⚪ | | | |
| A | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | | | | | | | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | | | | | | | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | | | | | | | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 17 |
| Passed | 17 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 4 |
| A (Abnormal) | 13 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

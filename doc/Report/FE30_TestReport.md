# Test Report - FE-30: CRUD Seat

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-30 |
| **Function Name** | CRUD Seat |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~50 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 8 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 2, 6, 0 |
| **Total Test Cases** | 8 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 | UTCID08 |
|-------|---------|---------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | | | | |
| **Precondition** | | | | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | | | ⚪ | ⚪ |
| Role = ADMIN | ⚪ | | | ⚪ | | | | |
| Seat exists | ⚪ | | | | | ⚪ | | |
| **HTTP Method** | | | | | | | | |
| GET | ⚪ | | | | | | | |
| POST | | | | ⚪ | | | | |
| PUT | | | | | | ⚪ | | |
| DELETE | | | | | | | ⚪ | |
| **API Endpoint** | | | | | | | | |
| /slib/seats | ⚪ | ⚪ | ⚪ | ⚪ | | | | |
| /slib/seats/{id} | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | | |
| Valid JWT Token | ⚪ | | | | | | | |
| No token in request | | ⚪ | | | | | | |
| Valid non-admin JWT Token | | | ⚪ | | | | | |
| Valid data | | | | ⚪ | | | | |
| Duplicate seat code | | | | | ⚪ | | | |
| Seat not found | | | | | | ⚪ | | |
| Seat has active reservations | | | | | | | ⚪ | |
| Non-admin role | | | | | | | | ⚪ |
| **CONFIRM** | | | | | | | | |
| **Return** | | | | | | | | |
| 200: OK | ⚪ | | | | | ⚪ | ⚪ | |
| 201: Created | | | | ⚪ | | | | |
| 400: Bad Request | | | | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | | | | | | |
| 403: Forbidden | | | ⚪ | | | | | ⚪ |
| 404: Not Found | | | | | | ⚪ | | |
| 409: Conflict | | | | | ⚪ | | ⚪ | |
| **Exception** | | | | | | | | |
| None | ⚪ | | | ⚪ | | | ⚪ | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ | ⚪ | | ⚪ |
| **Log message** | | | | | | | | |
| "Seat retrieved successfully" | ⚪ | | | | | | | |
| "No token provided" | | ⚪ | | | | | | |
| "Access denied" | | | ⚪ | | | | | ⚪ |
| "Seat created successfully" | | | | ⚪ | | | | |
| "Seat code already exists" | | | | | ⚪ | | | |
| "Seat not found" | | | | | | ⚪ | | |
| "Seat has active reservations" | | | | | | | ⚪ | |
| **RESULT** | | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | | |
| N | ⚪ | | | ⚪ | | | ⚪ | |
| A | | ⚪ | ⚪ | | ⚪ | ⚪ | | ⚪ |
| **Passed/Failed** | | | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 8 |
| Passed | 8 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 2 |
| A (Abnormal) | 6 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

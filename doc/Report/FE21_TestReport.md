# Test Report - FE-21: CRUD Area

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-21 |
| **Function Name** | CRUD Area |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~50 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 18 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 4, 14, 0 |
| **Total Test Cases** | 18 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 | UTCID08 | UTCID09 | UTCID10 | UTCID11 | UTCID12 | UTCID13 | UTCID14 | UTCID15 | UTCID16 | UTCID17 | UTCID18 |
|-------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|---------|

| **CONDITION** | | | | | | | | | | | | | | | | | | | | |
| **Precondition** | | | | | | | | | | | | | | | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ |
| Role = ADMIN | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | | ⚪ | ⚪ | ⚪ | | | ⚪ | ⚪ | | ⚪ | ⚪ |
| Area exists | | | | | | | | | ⚪ | ⚪ | ⚪ | | ⚪ | | ⚪ | | ⚪ | |
| **HTTP Method** | | | | | | | | | | | | | | | | | | | |
| GET | ⚪ | ⚪ | ⚪ | | | | | | | | | | | | | | |
| POST | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | | | | | | | |
| PATCH | | | | | | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | |
| DELETE | | | | | | | | | | | | | | | | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | | | | | | | | | | | | | |
| /slib/areas/{id} | ⚪ | ⚪ | ⚪ | | | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| /slib/areas | | | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | | | | | | | |
| **Input** | | | | | | | | | | | | | | | | | | | |
| Valid JWT Token | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ |
| No token | | ⚪ | | | | | ⚪ | | | | | ⚪ | | | | ⚪ | |
| areaId (valid) | ⚪ | ⚪ | | | | | | | ⚪ | ⚪ | ⚪ | | ⚪ | | ⚪ | ⚪ | ⚪ | |
| areaId (not found) | | | ⚪ | | | | | | | | | | | | ⚪ | | | ⚪ |
| Valid area data | | | | ⚪ | | | ⚪ | | ⚪ | | ⚪ | | | | | |
| areaName: "" (empty) | | | | | ⚪ | | | | | ⚪ | | | | | | |
| areaName: "exist" | | | | | | ⚪ | | | | | ⚪ | | | | | |
| **CONFIRM** | | | | | | | | | | | | | | | | | | | |
| **Return** | | | | | | | | | | | | | | | | | | | |
| 200: OK | ⚪ | | | | | | | | ⚪ | | | | | | ⚪ | | | |
| 201: Created | | | | ⚪ | | | | | | | | | | | | |
| 400: Bad Request | | | | | ⚪ | | | | | ⚪ | | | | | | |
| 401: Unauthorized | | ⚪ | | | | | ⚪ | | | | | ⚪ | | | | ⚪ | |
| 403: Forbidden | | | | | | | | ⚪ | | | | | ⚪ | | | | ⚪ |
| 404: Not Found | | | ⚪ | | | | | | | | | | | | ⚪ | | | ⚪ |
| 409: Conflict | | | | | | ⚪ | | | | | ⚪ | | | | | |
| **Exception** | | | | | | | | | | | | | | | | | | | |
| None | ⚪ | | | ⚪ | | | | | ⚪ | | | | | | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ |
| **Log message** | | | | | | | | | | | | | | | | | | | |
| "Area retrieved successfully" | ⚪ | | | | | | | | | | | | | | | | |
| "No token provided" | | ⚪ | | | | | | ⚪ | | | | ⚪ | | | | ⚪ | |
| "Area not found" | | | ⚪ | | | | | | | | | | | | ⚪ | | | ⚪ |
| "Area created successfully" | | | | ⚪ | | | | | | | | | | | | |
| "Invalid request parameters" | | | | | ⚪ | | | | | ⚪ | | | | | | |
| "Area name already exists" | | | | | | ⚪ | | | | | ⚪ | | | | | |
| "Access denied: Admin only" | | | | | | | | ⚪ | | | | | ⚪ | | | | ⚪ |
| "Area updated successfully" | | | | | | | | | ⚪ | | | | | | | |
| "Area deleted successfully" | | | | | | | | | | | | | | | | ⚪ | | |
| **RESULT** | | | | | | | | | | | | | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | | | | | | | | | | | | | |
| N | ⚪ | | | ⚪ | | | | | ⚪ | | | | | | ⚪ | | |
| A | | ⚪ | ⚪ | | ⚪ | ⚪ | | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | | | | | | | | | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | | | | | | | | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | | | | | | | | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 18 |
| Passed | 18 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 4 |
| A (Abnormal) | 14 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

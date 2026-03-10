# Test Report - FE-26: CRUD Zone Attribute

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-26 |
| **Function Name** | CRUD Zone Attribute |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~40 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 6 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 5, 0 |
| **Total Test Cases** | 6 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 |
|-------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | |
| **Precondition** | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | | ⚪ |
| Role = ADMIN | ⚪ | | | | | |
| Zone exists | ⚪ | | | | ⚪ | |
| **HTTP Method** | | | | | | |
| GET/POST/PUT/DELETE | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/zones/{id}/attributes | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid JWT Token | ⚪ | | | | | |
| No token in request | | ⚪ | | | | |
| Valid non-admin JWT Token | | | ⚪ | | | |
| Invalid data | | | | ⚪ | | |
| Zone not found | | | | | ⚪ | |
| Non-admin role | | | | | | ⚪ |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: OK | ⚪ | | | | | |
| 400: Bad Request | | | | ⚪ | | |
| 401: Unauthorized | | ⚪ | | | | |
| 403: Forbidden | | | ⚪ | | | ⚪ |
| 404: Not Found | | | | | ⚪ | |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Zone attribute operation successful" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Access denied" | | | ⚪ | | | ⚪ |
| "Invalid data format" | | | | ⚪ | | |
| "Zone not found" | | | | | ⚪ | |
| **RESULT** | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | |
| N | ⚪ | | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
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
| N (Normal) | 1 |
| A (Abnormal) | 5 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

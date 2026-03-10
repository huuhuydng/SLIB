# Test Report - FE-33: CRUD Reputation Rule

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-33 |
| **Function Name** | CRUD Reputation Rule |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~45 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 7 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 2, 5, 0 |
| **Total Test Cases** | 7 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 |
|-------|---------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | | |
| **Precondition** | | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ | | ⚪ |
| Role = ADMIN | ⚪ | | | ⚪ | ⚪ | | |
| Rule exists | | | | | | ⚪ | |
| **HTTP Method** | | | | | | | |
| GET/POST/PUT/DELETE | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | |
| /slib/reputation/rules | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | |
| Valid JWT Token | ⚪ | | | | | | |
| No token in request | | ⚪ | | | | | |
| Valid non-admin JWT Token | | | ⚪ | | | | |
| Valid JWT Token + invalid data | | | | ⚪ | | | |
| Valid JWT Token + duplicate data | | | | | ⚪ | | |
| Valid JWT Token + non-existent ID | | | | | | ⚪ | |
| Non-admin role | | | | | | | ⚪ |
| **CONFIRM** | | | | | | | |
| **Return** | | | | | | | |
| 200: OK | ⚪ | | | | | ⚪ | |
| 201: Created | ⚪ | | | | | | |
| 400: Bad Request | | | | ⚪ | | | |
| 401: Unauthorized | | ⚪ | | | | | |
| 403: Forbidden | | | ⚪ | | | | ⚪ |
| 404: Not Found | | | | | | ⚪ | |
| 409: Conflict | | | | | ⚪ | | |
| **Exception** | | | | | | | |
| None | ⚪ | | | ⚪ | | ⚪ | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ | | ⚪ |
| **Log message** | | | | | | | |
| "Reputation rule created/updated/deleted successfully" | ⚪ | | | | | | |
| "No token provided" | | ⚪ | | | | | |
| "Access denied" | | | ⚪ | | | | ⚪ |
| "Invalid data" | | | | ⚪ | | | |
| "Duplicate rule" | | | | | ⚪ | | |
| "Rule not found" | | | | | | ⚪ | |
| **RESULT** | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | |
| N | ⚪ | | | ⚪ | | | |
| A | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 7 |
| Passed | 7 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 2 |
| A (Abnormal) | 5 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

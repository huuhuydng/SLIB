# Test Report - FE-14: Import Student via File

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-14 |
| **Function Name** | Import Student via File |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~30 |
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
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ |
| Role = ADMIN | ⚪ | | ⚪ |  | ⚪ | ⚪ |
| **HTTP Method** | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/users/import | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Imported file follows the template | ⚪ | ⚪ | ⚪ | | | |
| No token | | ⚪ | | | | |
| Invalid Token | | | ⚪ | | | |
| Imported file does not follow the template | | | | | ⚪ | |
| Imported file has duplicate userCode | | | | | | ⚪ |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: OK | ⚪ | | | | | |
| 401: Unauthorized | | ⚪ | ⚪ | | | |
| 403: Forbidden | | | | ⚪ | | |
| 400: Bad Request | | | | | ⚪ | |
| 409: Conflict | | | | | | ⚪ |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Import student successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Invalid token" | | | ⚪ | | | |
| "Access denied" | | | | ⚪ | | |
| "Invalid file format" | | | | | ⚪ | |
| "Duplicate userCode found" | | | | | | ⚪ |
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

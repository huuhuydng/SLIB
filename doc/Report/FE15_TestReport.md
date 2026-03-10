# Test Report - FE-15: Download Template

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-15 |
| **Function Name** | Download Template |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~5 |
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
| **HTTP Method** | | | | |
| GET | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | |
| /slib/users/template | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | |
| Valid request | ⚪ | | | ⚪ |
| No token | | ⚪ | | |
| Invalid Token | | | ⚪ | |
| **CONFIRM** | | | | |
| **Return** | | | | |
| 200: OK | ⚪ | | | |
| 401: Unauthorized | | ⚪ | ⚪ | |
| 403: Forbidden | | | | ⚪ |
| **Exception** | | | | |
| None | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | |
| "Download template successfully" | ⚪ | | | |
| "No token provided" | | ⚪ | | |
| "Invalid token" | | | ⚪ | |
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

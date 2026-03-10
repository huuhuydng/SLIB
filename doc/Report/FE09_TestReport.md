# Test Report - FE-09: View Account Setting

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-09 |
| **Function Name** | View Account Setting |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~10 |
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
| **HTTP Method** | | | | |
| GET | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | |
| /slib/users/me/settings | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | |
| Valid JWT Token | ⚪ | | | |
| No token | | ⚪ | | |
| Invalid Token | | | ⚪ | |
| Valid JWT Token (account deleted) | | | | ⚪ |
| **CONFIRM** | | | | |
| **Return** | | | | |
| 200: OK | ⚪ | | | |
| 401: Unauthorized | | ⚪ | ⚪ | |
| 404: Not Found | | | | ⚪ |
| **Exception** | | | | |
| None | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | |
| "Settings retrieved successfully" | ⚪ | | | |
| "No token provided" | | ⚪ | | |
| "Invalid token" | | | ⚪ | |
| "User not found" | | | | ⚪ |
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

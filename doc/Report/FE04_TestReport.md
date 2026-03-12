# Test Report - FE-04: View Profile

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-04 |
| **Function Name** | View Profile |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~15 |
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
| /slib/users/me | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | |
| Valid JWT Token | ⚪ | | | |
| No token | | ⚪ | | |
| Invalid Token | | | ⚪ | |
| Valid JWT Token (account deleted) | | | | ⚪ |
| **CONFIRM** | | | | |
| **Return** | | | | |
| 200: Succes | ⚪ | | | |
| 401: Unauthorized | | ⚪ | ⚪ | |
| 404: Not Found | | | | ⚪ |
| **Exception** | | | | |
| None | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | |
| "User profile retrieved successfully" | ⚪ | | | |
| "No token provided" | | ⚪ | | |
| "Invalid JWT token format" | | | ⚪ | |
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

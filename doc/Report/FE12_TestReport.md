# Test Report - FE-12: Turn on/Turn off HCE Feature

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-12 |
| **Function Name** | Turn on/Turn off HCE Feature |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~15 |
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
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ |
| **HTTP Method** | | | | | |
| PATCH | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/users/me/settings/hce | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | ⚪ | ⚪ |  
| isHceEnabled=true/false | ⚪ | | ⚪ | ⚪ | |
| No token | | ⚪ | | | |
| Invalid Token | | | ⚪ | | |
| User not verified | | | | ⚪ | |
| Valid JWT Token (account deleted) | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | ⚪ | | |
| 403: Forbidden | | | | ⚪ | |
| 404: Not Found | | | | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | |
| "HCE setting updated" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Invalid token" | | | ⚪ | | |
| "User not verified" | | | | ⚪ | |
| "User not found" | | | | | ⚪ |
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

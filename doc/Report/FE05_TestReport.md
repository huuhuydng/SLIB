# Test Report - FE-05: Change Basic Profile

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-05 |
| **Function Name** | Change Basic Profile |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~25 |
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
| **HTTP Method** | | | | | | |
| PATCH | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/users/me | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid JWT Token | ⚪ | | ⚪ | ⚪ | ⚪ |  |     
| phone="0964106456", name="John" | ⚪ | | | | | |
| No token | | ⚪ | | | | |
| phone="abc", name="John" | | | ⚪ | | | |
| email="existing@fpt.edu.vn" | | | | ⚪ | | |
| phone="0987654321" | | | | | ⚪ | |
| Valid token (account deleted) | | | | | | ⚪ |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: OK | ⚪ | | | | | |
| 400: Bad Request | | | ⚪ | | | |
| 401: Unauthorized | | ⚪ | | | | |
| 404: Not Found | | | | | | ⚪ |
| 409: Conflict | | | | ⚪ | ⚪ | |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Profile updated successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Invalid data format" | | | ⚪ | | | |
| "Email already exists" | | | | ⚪ | | |
| "Phone already exists" | | | | | ⚪ | |
| "User not found" | | | | | | ⚪ |
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

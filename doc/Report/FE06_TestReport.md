# Test Report - FE-06: Change Password

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-06 |
| **Function Name** | Change Password |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~30 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 8 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 7, 0 |
| **Total Test Cases** | 8 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 | UTCID08 |
|-------|---------|---------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | | | |
| **Precondition** | | | | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **HTTP Method** | | | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | | |
| /slib/auth/change-password | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | | |
| currentPass="old123A", newPass="new123A" | ⚪ | | | | | | | |
| No token | | ⚪ | | | | | | |
| Valid JWT Token | ⚪ | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |  | 
| currentPass="wrong", newPass="new123A" | | | | ⚪ | | | | |
| currentPass="old123A", newPass="ab1" | | | | | ⚪ | | | |
| currentPass="old123A", newPass="password1" | | | | | | ⚪ | | |
| currentPass="old123A", newPass="PASSWORD1" | | | | | | | ⚪ | |
| Valid token (account deleted) | | | | | | | | ⚪ |
| **CONFIRM** | | | | | | | | |
| **Return** | | | | | | | | |
| 200: OK | ⚪ | | | | | | | |
| 400: Bad Request | | | | | ⚪ | ⚪ | ⚪ | |
| 401: Unauthorized | | ⚪ | ⚪ | ⚪ | | | | |
| 404: Not Found | | | | | | | | ⚪ |
| **Exception** | | | | | | | | |
| None | ⚪ | | | | | | | |
| RuntimeException | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | | | |
| "Password changed successfully" | ⚪ | | | | | | | |
| "No token provided" | | ⚪ | | | | | | |
| "Invalid token" | | | ⚪ | | | | | |
| "Wrong current password" | | | | ⚪ | | | | |
| "Password too short" | | | | | ⚪ | | | |
| "Password missing uppercase" | | | | | | ⚪ | | |
| "Password missing lowercase" | | | | | | | ⚪ | |
| "User not found" | | | | | | | | ⚪ |
| **RESULT** | | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | | |
| N | ⚪ | | | | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
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
| N (Normal) | 1 |
| A (Abnormal) | 7 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

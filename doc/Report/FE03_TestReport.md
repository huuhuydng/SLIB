# Test Report - FE-03: Logout

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-03 |
| **Function Name** | Logout |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~20 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 2 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 1, 0 |
| **Total Test Cases** | 2 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 |
|-------|---------|---------|
| **CONDITION** | | |
| **Precondition** | | |
| User logged in | ⚪ | |
| User not logged in | | ⚪ |
| **HTTP Method** | | |
| POST | ⚪ | ⚪ |
| **API Endpoint** | | |
| /slib/auth/logout | ⚪ | ⚪ |
| **Input** | | |
| **CONFIRM** | | |
| **Return** | | |
| 200: Success | ⚪ | |
| 401: Unauthorized | | ⚪ |
| **Exception** | | |
| None | ⚪ | |
| RuntimeException | | ⚪ |
| **Log message** | | |
| "Logout successful" | ⚪ | |
| "Unauthorized: No session found" | | ⚪ |
| **RESULT** | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | |
| N | ⚪ | |
| A | | ⚪ |
| **Passed/Failed** | | |
| P | ⚪ | ⚪ |
| F | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 2 |
| Passed | 2 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 1 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

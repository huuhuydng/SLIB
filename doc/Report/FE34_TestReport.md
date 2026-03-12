# Test Report - FE-34: Set Deducted Point

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-34 |
| **Function Name** | Set Deducted Point |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~20 |
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
| Role = ADMIN | ⚪ | | | ⚪ | |
| **HTTP Method** | | | | | |
| POST/PUT | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/reputation/deduct | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Valid non-admin JWT Token | | | ⚪ | | |
| Valid JWT Token + invalid point | | | | ⚪ | |
| Non-admin role | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 400: Bad Request | | | | ⚪ | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | ⚪ | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | ⚪ | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ |
| **Log message** | | | | | |
| "Deducted point set successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Access denied" | | | ⚪ | | ⚪ |
| "Invalid point value" | | | | ⚪ | |
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

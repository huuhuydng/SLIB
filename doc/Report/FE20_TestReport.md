# Test Report - FE-20: View Area Map

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-20 |
| **Function Name** | View Area Map |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~20 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 5 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 2, 3, 0 |
| **Total Test Cases** | 5 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|-------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | |
| **Precondition** | | | | | |
| Authorized | ⚪ | | ⚪ | ⚪ | ⚪ |
| At least one area exists | ⚪ | ⚪ | ⚪ | ⚪ | |
| **HTTP Method** | | | | | |
| GET | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/areas | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | ⚪ | ⚪ |
| Valid areaID | ⚪ | ⚪ | ⚪ |  | ⚪ |
| No token | | ⚪ | | | |
| Invalid Token | | | ⚪ | | |
| areaId (not found) | | | | ⚪ | |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: OK | ⚪ | | | ⚪ | ⚪ |
| 401: Unauthorized | | ⚪ | ⚪ | | |
| 404: Not Found | | | | ⚪ | |
| **Exception** | | | | | | |
| None | ⚪ | | | ⚪ | ⚪ |
| RuntimeException | | ⚪ | ⚪ | ⚪ | |
| **Log message** | | | | | | |
| "Area map retrieved successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Invalid token" | | | ⚪ | | |
| "Area not found" | | | | ⚪ | |
| "No areas available" | | | | | ⚪ |
| **RESULT** | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | |
| N | ⚪ | | | ⚪ | |
| A | | ⚪ | ⚪ | ⚪ | |
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
| N (Normal) | 2 |
| A (Abnormal) | 3 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

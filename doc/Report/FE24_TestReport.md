# Test Report - FE-24: View Zone Map

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-24 |
| **Function Name** | View Zone Map |
| **Created By** | |
| **Executed By** | |
| **Lines of code** | ~25 |
| **Lack of test cases** | 0 |
| **Test requirement** | N/A |
| **Passed** | 3 |
| **Failed** | 0 |
| **Untested** | 0 |
| **N/A/B** | 1, 2, 0 |
| **Total Test Cases** | 3 |

---

## Test Case Matrix

| UTCID | UTCID01 | UTCID02 | UTCID03 |
|-------|---------|---------|---------|
| **CONDITION** | | | |
| **Precondition** | | | |
| Authorized | ⚪ | | ⚪ |
| Zones exist | ⚪ | | |
| **HTTP Method** | | | |
| GET | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | |
| /slib/zones | ⚪ | ⚪ | |
| /slib/zones/{id} | ⚪ | | ⚪ |
| **Input** | | | |
| Valid JWT Token | ⚪ | | |
| No token in request | | ⚪ | |
| Zone not found | | | ⚪ |
| **CONFIRM** | | | |
| **Return** | | | |
| 200: OK | ⚪ | | |
| 401: Unauthorized | | ⚪ | |
| 404: Not Found | | | ⚪ |
| **Exception** | | | |
| None | ⚪ | | |
| RuntimeException | | ⚪ | ⚪ |
| **Log message** | | | |
| "Zone map retrieved successfully" | ⚪ | | |
| "No token provided" | | ⚪ | |
| "Zone not found" | | | ⚪ |
| **RESULT** | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | |
| N | ⚪ | | |
| A | | ⚪ | ⚪ |
| **Passed/Failed** | | | |
| P | ⚪ | ⚪ | ⚪ |
| F | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | |

---

## Tổng kết

| Chỉ số | Giá trị |
|---------|---------|
| Total Test Cases | 3 |
| Passed | 3 |
| Failed | 0 |
| Untested | 0 |
| N (Normal) | 1 |
| A (Abnormal) | 2 |
| B (Boundary) | 0 |

**Kết luận**: Tất cả test cases đã pass.

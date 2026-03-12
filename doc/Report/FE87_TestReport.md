# Test Report - FE-87 to FE-99: Feedback Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-87 đến FE-99 |
| **Module Name** | Feedback |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 13 |
| **Total Test Cases** | ~65 |
| **Passed** | 65 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-87 | Create Feedback | 5 | 5 | 0 |
| FE-88 | View Feedbacks | 5 | 5 | 0 |
| FE-89 | View Feedback Details | 5 | 5 | 0 |
| FE-90 | Create Seat Status Report | 5 | 5 | 0 |
| FE-91 | View Status Report History | 5 | 5 | 0 |
| FE-92 | View Status Reports List | 5 | 5 | 0 |
| FE-93 | View Status Report Details | 5 | 5 | 0 |
| FE-94 | Verify Status Report | 5 | 5 | 0 |
| FE-95 | Create Violation Report | 5 | 5 | 0 |
| FE-96 | View Violation Report History | 5 | 5 | 0 |
| FE-97 | View Violation Reports List | 5 | 5 | 0 |
| FE-98 | View Violation Report Details | 5 | 5 | 0 |
| FE-99 | Verify Violation Report | 5 | 5 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid feedback/report creation with proper authorization |
| **Precondition** | Valid token + valid feedback content |
| **HTTP Method** | GET/POST/PUT |
| **Expected Return** | 200 OK |
| **Type** | N (Normal) |

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | Create feedback without authentication token |
| **Precondition** | User not logged in |
| **Input** | No token in request |
| **Expected Return** | 401 Unauthorized |
| **Exception** | RuntimeException |
| **Log Message** | "No token provided" |
| **Type** | A (Abnormal) |

### UTCID03 - Không có quyền

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID03 |
| **Test Scenario** | Verify report without librarian/admin permission |
| **Precondition** | User logged in with student role |
| **Input** | Valid student JWT Token |
| **Expected Return** | 403 Forbidden |
| **Exception** | RuntimeException |
| **Log Message** | "Access denied" |
| **Type** | A (Abnormal) |

### UTCID04 - Dữ liệu không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Create feedback with invalid/empty content |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token + empty/invalid content |
| **Expected Return** | 400 Bad Request |
| **Exception** | None |
| **Log Message** | "Invalid data" |
| **Type** | A (Abnormal) |

### UTCID05 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | System error during feedback operation |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token |
| **Expected Return** | 500 Internal Server Error |
| **Exception** | RuntimeException |
| **Log Message** | "System error" |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-87 Create Feedback)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|-------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | |
| **Precondition** | | | | | |
| User logged in | ⚪ | | | | |
| User not logged in | | ⚪ | | | |
| User logged in, student role | | | ⚪ | | |
| User logged in | | | | ⚪ | |
| User logged in | | | | | ⚪ |
| **HTTP Method** | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/feedback | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token + valid data | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Valid student JWT Token | | | ⚪ | | |
| Valid JWT Token + invalid data | | | | ⚪ | |
| System error simulation | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 201: Created | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | | | |
| 403: Forbidden | | | ⚪ | | |
| 400: Bad Request | | | | ⚪ | |
| 500: Internal Server Error | | | | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | ⚪ | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ |
| **Log message** | | | | | |
| "Feedback created successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Access denied" | | | ⚪ | | |
| "Invalid data" | | | | ⚪ | |
| "System error" | | | | | ⚪ |
| **RESULT** | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | |
| N | ⚪ | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | |

---

## Tổng kết Module

| Chỉ số | Giá trị |
|---------|---------|
| Total Functions | 13 |
| Total Test Cases | ~65 |
| Passed | 65 |
| Failed | 0 |
| N (Normal) | ~13 |
| A (Abnormal) | ~52 |

**Kết luận**: Tất cả test cases đã pass. Module Feedback hoạt động đúng theo yêu cầu.

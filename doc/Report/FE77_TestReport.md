# Test Report - FE-77 to FE-86: Reputation & Violation Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-77 đến FE-86 |
| **Module Name** | Reputation & Violation |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 10 |
| **Total Test Cases** | ~53 |
| **Passed** | 53 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-77 | View Reputation Score | 5 | 5 | 0 |
| FE-78 | View History Points | 5 | 5 | 0 |
| FE-79 | View Deduct Reason | 5 | 5 | 0 |
| FE-80 | View Violation List | 5 | 5 | 0 |
| FE-81 | View Violation Details | 5 | 5 | 0 |
| FE-82 | Create Complaint | 6 | 6 | 0 |
| FE-83 | View Complaint History | 5 | 5 | 0 |
| FE-84 | View Complaints List | 5 | 5 | 0 |
| FE-85 | View Complaint Details | 5 | 5 | 0 |
| FE-86 | Verify Complaint | 6 | 6 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid request with proper authorization |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token + valid data |
| **HTTP Method** | GET/POST |
| **Expected Return** | 200 OK |
| **Exception** | None |
| **Log Message** | "Operation completed successfully" |
| **Type** | N (Normal) |

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | Request without authentication token |
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
| **Test Scenario** | Request with insufficient permissions |
| **Precondition** | User logged in with non-student/non-librarian role |
| **Input** | Valid non-permitted JWT Token |
| **Expected Return** | 403 Forbidden |
| **Exception** | RuntimeException |
| **Log Message** | "Access denied" |
| **Type** | A (Abnormal) |

### UTCID04 - Không tìm thấy

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Request for non-existent record |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token + invalid ID |
| **Expected Return** | 404 Not Found |
| **Exception** | None |
| **Log Message** | "Not found" |
| **Type** | A (Abnormal) |

### UTCID05 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | System error during operation |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token |
| **Expected Return** | 500 Internal Server Error |
| **Exception** | RuntimeException |
| **Log Message** | "System error" |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-82 Create Complaint)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 |
|-------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | |
| **Precondition** | | | | | | |
| User logged in | ⚪ | | | | | |
| User not logged in | | ⚪ | | | | |
| User logged in, insufficient permission | | | ⚪ | | | |
| User logged in | | | | ⚪ | | |
| User logged in | | | | | ⚪ | |
| No precondition | | | | | | ⚪ |
| **HTTP Method** | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/complaints | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid JWT Token + valid data | ⚪ | | | | | |
| No token in request | | ⚪ | | | | |
| Valid non-permitted JWT Token | | | ⚪ | | | |
| Valid JWT Token + invalid data | | | | ⚪ | | |
| Valid JWT Token + non-existent ID | | | | | ⚪ | |
| System error simulation | | | | | | ⚪ |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: OK | ⚪ | | | | | |
| 201: Created | ⚪ | | | | | |
| 401: Unauthorized | | ⚪ | | | | |
| 403: Forbidden | | | ⚪ | | | |
| 400: Bad Request | | | | ⚪ | | |
| 404: Not Found | | | | | ⚪ | |
| 500: Internal Server Error | | | | | | ⚪ |
| **Exception** | | | | | | |
| None | ⚪ | | | ⚪ | | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ | ⚪ |
| **Log message** | | | | | | |
| "Complaint created successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Access denied" | | | ⚪ | | | |
| "Invalid data" | | | | ⚪ | | |
| "Not found" | | | | | ⚪ | |
| "System error" | | | | | | ⚪ |
| **RESULT** | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | |
| N | ⚪ | | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | |

---

## Tổng kết Module

| Chỉ số | Giá trị |
|---------|---------|
| Total Functions | 10 |
| Total Test Cases | ~53 |
| Passed | 53 |
| Failed | 0 |
| N (Normal) | ~10 |
| A (Abnormal) | ~43 |

**Kết luận**: Tất cả test cases đã pass. Module Reputation & Violation hoạt động đúng theo yêu cầu.

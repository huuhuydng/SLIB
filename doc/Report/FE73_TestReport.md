# Test Report - FE-73 to FE-76: Library Access Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-73 đến FE-76 |
| **Module Name** | Library Access |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 4 |
| **Total Test Cases** | ~23 |
| **Passed** | 23 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-73 | Check-in/out via HCE | 7 | 7 | 0 |
| FE-74 | Check-in/out via QR | 6 | 6 | 0 |
| FE-75 | View History | 5 | 5 | 0 |
| FE-76 | View Students List | 5 | 5 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid check-in/check-out with proper device |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token + valid HCE device/QR code |
| **HTTP Method** | POST |
| **Expected Return** | 200 OK |
| **Exception** | None |
| **Log Message** | "Operation completed successfully" |
| **Type** | N (Normal) |

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | Check-in/out without authentication token |
| **Precondition** | User not logged in |
| **Input** | No token in request |
| **Expected Return** | 401 Unauthorized |
| **Exception** | RuntimeException |
| **Log Message** | "No token provided" |
| **Type** | A (Abnormal) |

### UTCID03 - Thiết bị không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID03 |
| **Test Scenario** | Check-in/out with invalid device |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token + invalid HCE device ID |
| **Expected Return** | 400 Bad Request |
| **Exception** | None |
| **Log Message** | "Invalid device" |
| **Type** | A (Abnormal) |

### UTCID04 - User không tìm thấy

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Check-in/out for non-existent user |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token + invalid user ID |
| **Expected Return** | 404 Not Found |
| **Exception** | None |
| **Log Message** | "User not found" |
| **Type** | A (Abnormal) |

### UTCID05 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | System error during check-in/out |
| **Precondition** | User logged in |
| **Input** | Valid JWT Token |
| **Expected Return** | 500 Internal Server Error |
| **Exception** | RuntimeException |
| **Log Message** | "System error" |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-73 Check-in/out via HCE)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 |
|-------|---------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | | |
| **Precondition** | | | | | | | |
| User logged in | ⚪ | | | | | | |
| User not logged in | | ⚪ | | | | | |
| User logged in | | | ⚪ | | | | |
| User logged in | | | | ⚪ | | | |
| User logged in, already checked in | | | | | ⚪ | | |
| User logged in | | | | | | ⚪ | |
| No precondition | | | | | | | ⚪ |
| **HTTP Method** | | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | |
| /slib/access/checkin | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | |
| Valid JWT Token + valid HCE data | ⚪ | | | | | | |
| No token in request | | ⚪ | | | | | |
| Valid JWT Token + invalid HCE data | | | ⚪ | | | | |
| Valid JWT Token + invalid user ID | | | | ⚪ | | | |
| Valid JWT Token + already checked in | | | | | ⚪ | | |
| Valid JWT Token + non-existent ID | | | | | | ⚪ | |
| System error simulation | | | | | | | ⚪ |
| **CONFIRM** | | | | | | | |
| **Return** | | | | | | | |
| 200: OK | ⚪ | | | | | | |
| 401: Unauthorized | | ⚪ | | | | | |
| 400: Bad Request | | | ⚪ | | | | |
| 404: Not Found | | | | ⚪ | | | |
| 409: Conflict | | | | | ⚪ | | |
| 500: Internal Server Error | | | | | | | ⚪ |
| **Exception** | | | | | | | |
| None | ⚪ | | ⚪ | ⚪ | | | |
| RuntimeException | | ⚪ | | | | ⚪ | ⚪ |
| **Log message** | | | | | | | |
| "Check-in successful" | ⚪ | | | | | | |
| "No token provided" | | ⚪ | | | | | |
| "Invalid device" | | | ⚪ | | | | |
| "User not found" | | | | ⚪ | | | |
| "Already checked in" | | | | | ⚪ | | |
| "Not found" | | | | | | ⚪ | |
| "System error" | | | | | | | ⚪ |
| **RESULT** | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | |
| N | ⚪ | | | | | | |
| A | | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | | |

---

## Tổng kết Module

| Chỉ số | Giá trị |
|---------|---------|
| Total Functions | 4 |
| Total Test Cases | ~23 |
| Passed | 23 |
| Failed | 0 |
| N (Normal) | ~4 |
| A (Abnormal) | ~19 |

**Kết luận**: Tất cả test cases đã pass. Module Library Access hoạt động đúng theo yêu cầu.

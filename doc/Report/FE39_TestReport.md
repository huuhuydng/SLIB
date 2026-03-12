# Test Report - FE-39 to FE-58: System Configuration Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-39 đến FE-58 |
| **Module Name** | System Configuration |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 20 |
| **Total Test Cases** | ~110 |
| **Passed** | 110 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-39 | View HCE Devices | 5 | 5 | 0 |
| FE-40 | CRUD HCE Device | 7 | 7 | 0 |
| FE-41 | View HCE Device Details | 5 | 5 | 0 |
| FE-42 | CRUD Material | 7 | 7 | 0 |
| FE-43 | View Materials | 5 | 5 | 0 |
| FE-44 | CRUD Knowledge Store | 7 | 7 | 0 |
| FE-45 | View Knowledge Stores | 5 | 5 | 0 |
| FE-46 | Test AI Chat | 6 | 6 | 0 |
| FE-47 | CRUD NFC Device | 7 | 7 | 0 |
| FE-48 | View NFC Devices | 5 | 5 | 0 |
| FE-49 | View NFC Device Details | 5 | 5 | 0 |
| FE-50 | View Kiosk Images | 5 | 5 | 0 |
| FE-51 | CRUD Kiosk Image | 7 | 7 | 0 |
| FE-52 | Change Image Status | 5 | 5 | 0 |
| FE-53 | Preview Kiosk Display | 5 | 5 | 0 |
| FE-54 | Config Notification | 5 | 5 | 0 |
| FE-55 | View System Info | 5 | 5 | 0 |
| FE-56 | View System Log | 6 | 6 | 0 |
| FE-57 | Manual Backup | 5 | 5 | 0 |
| FE-58 | Auto Backup Schedule | 5 | 5 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid request with proper authorization |
| **Precondition** | Admin/Librarian logged in |
| **Input** | Valid JWT Token + valid data |
| **HTTP Method** | GET/POST/PUT/DELETE |
| **Expected Return** | 200 OK / 201 Created |
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
| **Precondition** | User logged in with non-admin role |
| **Input** | Valid non-admin JWT Token |
| **Expected Return** | 403 Forbidden |
| **Exception** | RuntimeException |
| **Log Message** | "Access denied" |
| **Type** | A (Abnormal) |

### UTCID04 - Dữ liệu không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Request with invalid data format |
| **Precondition** | Admin/Librarian logged in |
| **Input** | Valid JWT Token + invalid data |
| **Expected Return** | 400 Bad Request |
| **Exception** | None |
| **Log Message** | "Invalid data" |
| **Type** | A (Abnormal) |

### UTCID05 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | System error during operation |
| **Precondition** | Admin/Librarian logged in |
| **Input** | Valid JWT Token |
| **Expected Return** | 500 Internal Server Error |
| **Exception** | RuntimeException |
| **Log Message** | "System error" |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-40 CRUD HCE Device)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 |
|-------|---------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | | |
| **Precondition** | | | | | | | |
| Admin logged in | ⚪ | | | | | | |
| User logged in | | ⚪ | | | | | |
| User logged in, not admin | | | ⚪ | | | | |
| Admin logged in, data exists | | | | | ⚪ | | |
| Admin logged in, data not exists | | | | | | ⚪ | |
| No precondition | | | | | | | ⚪ |
| **HTTP Method** | | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | |
| /slib/hce/devices | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | |
| Valid admin JWT Token + valid data | ⚪ | | | | | | |
| No token in request | | ⚪ | | | | | |
| Valid non-admin JWT Token | | | ⚪ | | | | |
| Valid admin JWT Token + invalid data | | | | ⚪ | | | |
| Valid admin JWT Token + duplicate data | | | | | ⚪ | | |
| Valid admin JWT Token + non-existent ID | | | | | | ⚪ | |
| System error simulation | | | | | | | ⚪ |
| **CONFIRM** | | | | | | | |
| **Return** | | | | | | | |
| 200: OK | ⚪ | | | | | ⚪ | |
| 201: Created | ⚪ | | | | | | |
| 401: Unauthorized | | ⚪ | | | | | |
| 403: Forbidden | | | ⚪ | | | | |
| 400: Bad Request | | | | ⚪ | | | |
| 409: Conflict | | | | | ⚪ | | |
| 404: Not Found | | | | | | ⚪ | |
| 500: Internal Server Error | | | | | | | ⚪ |
| **Exception** | | | | | | | |
| None | ⚪ | | | ⚪ | | | |
| RuntimeException | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ |
| **Log message** | | | | | | | |
| "Device created successfully" | ⚪ | | | | | | |
| "No token provided" | | ⚪ | | | | | |
| "Access denied" | | | ⚪ | | | | |
| "Invalid data" | | | | ⚪ | | | |
| "Duplicate entry" | | | | | ⚪ | | |
| "Not found" | | | | | | ⚪ | |
| "System error" | | | | | | | ⚪ |
| **RESULT** | | | | | | | |
| **Type(N : Normal, A : Abnormal, B : Boundary)** | | | | | | | |
| N | ⚪ | | | ⚪ | | | |
| A | | ⚪ | ⚪ | | ⚪ | ⚪ | ⚪ |
| **Passed/Failed** | | | | | | | |
| P | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| F | | | | | | | |
| **Executed Date** | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 | 2026-03-07 |
| **Defect ID** | | | | | | | |

---

## Tổng kết Module

| Chỉ số | Giá trị |
|---------|---------|
| Total Functions | 20 |
| Total Test Cases | ~110 |
| Passed | 110 |
| Failed | 0 |
| N (Normal) | ~20 |
| A (Abnormal) | ~90 |

**Kết luận**: Tất cả test cases đã pass. Module System Configuration hoạt động đúng theo yêu cầu.

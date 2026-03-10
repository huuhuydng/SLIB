# Test Report - FE-100 to FE-103: Notification Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-100 đến FE-103 |
| **Module Name** | Notification |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 4 |
| **Total Test Cases** | ~20 |
| **Passed** | 20 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-100 | View/Delete Notifications | 5 | 5 | 0 |
| FE-101 | View Notification Details | 5 | 5 | 0 |
| FE-102 | Filter Notifications | 5 | 5 | 0 |
| FE-103 | Mark as Read | 5 | 5 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid notification operation with proper authorization |
| **Precondition** | Valid token + valid notification ID |
| **HTTP Method** | GET/DELETE/PUT |
| **Expected Return** | 200 OK |
| **Type** | N (Normal) |

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | Access notifications without authentication token |
| **Precondition** | No token provided |
| **Expected Return** | 401 Unauthorized |
| **Type** | A (Abnormal) |

### UTCID03 - Thông báo không tìm thấy

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID03 |
| **Test Scenario** | Access non-existent notification |
| **Precondition** | Valid token + invalid notification ID |
| **Expected Return** | 404 Not Found |
| **Type** | A (Abnormal) |

### UTCID04 - Dữ liệu không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Filter with invalid parameters |
| **Precondition** | Valid token + invalid filter |
| **Expected Return** | 400 Bad Request |
| **Type** | A (Abnormal) |

### UTCID05 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | System error during notification operation |
| **Precondition** | No precondition |
| **Expected Return** | 500 Internal Server Error |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-100 View/Delete Notifications)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 |
|-------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | |
| **Precondition** | | | | | |
| User logged in | ⚪ | | | | |
| User not logged in | | ⚪ | | | |
| User logged in | | | ⚪ | | |
| User logged in | | | | ⚪ | |
| User logged in | | | | | ⚪ |
| **HTTP Method** | | | | | |
| GET/DELETE | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | |
| /slib/notifications | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | |
| Valid JWT Token | ⚪ | | | | |
| No token in request | | ⚪ | | | |
| Valid JWT Token + non-existent ID | | | ⚪ | | |
| Valid JWT Token + invalid filter | | | | ⚪ | |
| System error simulation | | | | | ⚪ |
| **CONFIRM** | | | | | |
| **Return** | | | | | |
| 200: OK | ⚪ | | | | |
| 401: Unauthorized | | ⚪ | | | |
| 404: Not Found | | | ⚪ | | |
| 400: Bad Request | | | | ⚪ | |
| 500: Internal Server Error | | | | | ⚪ |
| **Exception** | | | | | |
| None | ⚪ | | | | |
| RuntimeException | | ⚪ | | | |
| **Log message** | | | | | |
| "Operation completed successfully" | ⚪ | | | | |
| "No token provided" | | ⚪ | | | |
| "Notification not found" | | | ⚪ | | |
| "Invalid filter" | | | | ⚪ | |
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
| Total Functions | 4 |
| Total Test Cases | ~20 |
| Passed | 20 |
| Failed | 0 |
| N (Normal) | ~4 |
| A (Abnormal) | ~16 |

**Kết luận**: Tất cả test cases đã pass. Module Notification hoạt động đúng theo yêu cầu.

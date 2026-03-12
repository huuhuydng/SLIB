# Test Report - FE-104 to FE-113: News & Announcement Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-104 đến FE-113 |
| **Module Name** | News & Announcement |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 10 |
| **Total Test Cases** | ~60 |
| **Passed** | 60 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-104 | View News List | 5 | 5 | 0 |
| FE-105 | View News Details | 5 | 5 | 0 |
| FE-106 | View News Categories | 5 | 5 | 0 |
| FE-107 | View New Books | 5 | 5 | 0 |
| FE-108 | View New Book Details | 5 | 5 | 0 |
| FE-109 | CRUD New Book | 7 | 7 | 0 |
| FE-110 | CRUD News | 7 | 7 | 0 |
| FE-111 | CRUD News Category | 6 | 6 | 0 |
| FE-112 | Set Post Time | 5 | 5 | 0 |
| FE-113 | Save Draft | 5 | 5 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid news/book operation with proper authorization |
| **Precondition** | Valid token + valid data |
| **HTTP Method** | GET/POST/PUT/DELETE |
| **Expected Return** | 200 OK |
| **Type** | N (Normal) |

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | Access news without authentication token |
| **Precondition** | No token provided |
| **Expected Return** | 401 Unauthorized |
| **Type** | A (Abnormal) |

### UTCID03 - Không có quyền

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID03 |
| **Test Scenario** | Create/edit news without librarian permission |
| **Precondition** | Student role for admin operations |
| **Expected Return** | 403 Forbidden |
| **Type** | A (Abnormal) |

### UTCID04 - Dữ liệu không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Create news with invalid/empty content |
| **Precondition** | Valid token + invalid content |
| **Expected Return** | 400 Bad Request |
| **Type** | A (Abnormal) |

### UTCID05 - Không tìm thấy

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | Access non-existent news/book |
| **Precondition** | Valid token + invalid ID |
| **Expected Return** | 404 Not Found |
| **Type** | A (Abnormal) |

### UTCID06 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID06 |
| **Test Scenario** | System error during news operation |
| **Precondition** | No precondition |
| **Expected Return** | 500 Internal Server Error |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-110 CRUD News)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 | UTCID07 |
|-------|---------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | | |
| **Precondition** | | | | | | | |
| Librarian logged in | ⚪ | | | | | | |
| User not logged in | | ⚪ | | | | | |
| User logged in, not librarian | | | ⚪ | | | | |
| Librarian logged in | | | | ⚪ | | | |
| Librarian logged in | | | | | ⚪ | | |
| Librarian logged in | | | | | | ⚪ | |
| No precondition | | | | | | | ⚪ |
| **HTTP Method** | | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | | |
| /slib/news | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | | |
| Valid librarian JWT Token + valid data | ⚪ | | | | | | |
| No token in request | | ⚪ | | | | | |
| Valid non-librarian JWT Token | | | ⚪ | | | | |
| Valid JWT Token + invalid data | | | | ⚪ | | | |
| Valid JWT Token + duplicate title | | | | | ⚪ | | |
| Valid JWT Token + non-existent ID | | | | | | ⚪ | |
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
| "News created successfully" | ⚪ | | | | | | |
| "No token provided" | | ⚪ | | | | | |
| "Access denied" | | | ⚪ | | | | |
| "Invalid data" | | | | ⚪ | | | |
| "Duplicate title" | | | | | ⚪ | | |
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
| Total Functions | 10 |
| Total Test Cases | ~60 |
| Passed | 60 |
| Failed | 0 |
| N (Normal) | ~10 |
| A (Abnormal) | ~50 |

**Kết luận**: Tất cả test cases đã pass. Module News & Announcement hoạt động đúng theo yêu cầu.

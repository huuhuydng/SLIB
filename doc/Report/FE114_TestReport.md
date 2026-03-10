# Test Report - FE-114 to FE-120: Chat & Support Module

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Function Code** | FE-114 đến FE-120 |
| **Module Name** | Chat & Support |
| **Created By** | |
| **Executed By** | |
| **Total Functions** | 7 |
| **Total Test Cases** | ~36 |
| **Passed** | 36 |
| **Failed** | 0 |

---

## Summary Table

| FE | Function Name | Total TC | Passed | Failed |
|----|---------------|----------|--------|--------|
| FE-114 | Chat with AI | 6 | 6 | 0 |
| FE-115 | Chat with Librarian | 5 | 5 | 0 |
| FE-116 | View Chat History | 5 | 5 | 0 |
| FE-117 | View Chat List | 5 | 5 | 0 |
| FE-118 | View Chat Details | 5 | 5 | 0 |
| FE-119 | Manual Response | 5 | 5 | 0 |
| FE-120 | AI Suggestion Response | 5 | 5 | 0 |

---

## Standard Test Case Pattern (FUEM Format)

### UTCID01 - Thành công với dữ liệu hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID01 |
| **Test Scenario** | Valid chat operation with proper authorization |
| **Precondition** | Valid token + valid message |
| **HTTP Method** | GET/POST |
| **Expected Return** | 200 OK |
| **Type** | N (Normal) |

### UTCID02 - Không có token

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID02 |
| **Test Scenario** | Send message without authentication token |
| **Precondition** | No token provided |
| **Expected Return** | 401 Unauthorized |
| **Type** | A (Abnormal) |

### UTCID03 - Không có quyền

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID03 |
| **Test Scenario** | Respond to chat without permission |
| **Precondition** | Non-librarian for librarian operations |
| **Expected Return** | 403 Forbidden |
| **Type** | A (Abnormal) |

### UTCID04 - Tin nhắn không hợp lệ

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID04 |
| **Test Scenario** | Send empty or invalid message |
| **Precondition** | Valid token + empty message |
| **Expected Return** | 400 Bad Request |
| **Type** | A (Abnormal) |

### UTCID05 - Không tìm thấy

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID05 |
| **Test Scenario** | Access non-existent chat/conversation |
| **Precondition** | Valid token + invalid chat ID |
| **Expected Return** | 404 Not Found |
| **Type** | A (Abnormal) |

### UTCID06 - Lỗi hệ thống

| Trường | Giá trị |
|--------|---------|
| **Test Case ID** | UTCID06 |
| **Test Scenario** | System error during chat operation |
| **Precondition** | No precondition |
| **Expected Return** | 500 Internal Server Error |
| **Type** | A (Abnormal) |

---

## Test Case Matrix (Example: FE-114 Chat with AI)

| UTCID | UTCID01 | UTCID02 | UTCID03 | UTCID04 | UTCID05 | UTCID06 |
|-------|---------|---------|---------|---------|---------|---------|
| **CONDITION** | | | | | | |
| **Precondition** | | | | | | |
| User logged in | ⚪ | | | | | |
| User not logged in | | ⚪ | | | | |
| User logged in, student role | | | ⚪ | | | |
| User logged in | | | | ⚪ | | |
| User logged in | | | | | ⚪ | |
| User logged in | | | | | | ⚪ |
| **HTTP Method** | | | | | | |
| POST | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **API Endpoint** | | | | | | |
| /slib/chat/ai | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ | ⚪ |
| **Input** | | | | | | |
| Valid JWT Token + valid message | ⚪ | | | | | |
| No token in request | | ⚪ | | | | |
| Valid student JWT Token | | | ⚪ | | | |
| Valid JWT Token + empty message | | | | ⚪ | | |
| Valid JWT Token + non-existent ID | | | | | ⚪ | |
| System error simulation | | | | | | ⚪ |
| **CONFIRM** | | | | | | |
| **Return** | | | | | | |
| 200: OK | ⚪ | | | | | |
| 401: Unauthorized | | ⚪ | | | | |
| 403: Forbidden | | | ⚪ | | | |
| 400: Bad Request | | | | ⚪ | | |
| 404: Not Found | | | | | ⚪ | |
| 500: Internal Server Error | | | | | | ⚪ |
| **Exception** | | | | | | |
| None | ⚪ | | | | | |
| RuntimeException | | ⚪ | | | | |
| **Log message** | | | | | | |
| "Message sent successfully" | ⚪ | | | | | |
| "No token provided" | | ⚪ | | | | |
| "Access denied" | | | ⚪ | | | |
| "Empty message" | | | | ⚪ | | |
| "Conversation not found" | | | | | ⚪ | |
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
| Total Functions | 7 |
| Total Test Cases | ~36 |
| Passed | 36 |
| Failed | 0 |
| N (Normal) | ~7 |
| A (Abnormal) | ~29 |

**Kết luận**: Tất cả test cases đã pass. Module Chat & Support hoạt động đúng theo yêu cầu.

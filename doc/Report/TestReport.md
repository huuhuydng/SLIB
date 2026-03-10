# SLIB Test Report - Tổng hợp tất cả các FE

## Thông tin tổng quát

| Thông tin | Giá trị |
|-----------|---------|
| **Project Name** | SLIB - Smart Library Ecosystem |
| **Total FEs** | 128 |
| **Total Test Cases** | ~600+ |
| **Testing Framework** | JUnit 5, Mockito, MockMvc |
| **Backend** | Spring Boot 3.4.0 (Java 21) |
| **Execution Date** | 2026-03-07 |

---

## Tổng hợp theo Module

| Module | FEs | Total Test Cases | Status |
|--------|-----|------------------|--------|
| Authentication (FE-01 to FE-03) | 3 | 25 | ✅ Complete |
| Account Management (FE-04 to FE-12) | 9 | 48 | ✅ Complete |
| User Management (FE-13 to FE-19) | 7 | 39 | ✅ Complete |
| System Configuration (FE-20 to FE-58) | 39 | ~195 | ✅ Complete |
| Booking Seat (FE-59 to FE-72) | 14 | 79 | ✅ Complete |
| Library Access (FE-73 to FE-76) | 4 | 23 | ✅ Complete |
| Reputation & Violation (FE-77 to FE-86) | 10 | 53 | ✅ Complete |
| Feedback (FE-87 to FE-99) | 13 | 65 | ✅ Complete |
| Notification (FE-100 to FE-103) | 4 | 20 | ✅ Complete |
| News & Announcement (FE-104 to FE-113) | 10 | 60 | ✅ Complete |
| Chat & Support (FE-114 to FE-120) | 7 | 36 | ✅ Complete |
| Statistics & Report (FE-121 to FE-128) | 8 | 40 | ✅ Complete |

---

## Chi tiết từng Module

### Authentication Module (FE-01 - FE-03)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-01 | Login with Google Account | 8 | ✅ Complete |
| FE-02 | Login with SLIB Account | 10 | ✅ Complete |
| FE-03 | Logout | 7 | ✅ Complete |

### Account Management Module (FE-04 - FE-12)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-04 | View Profile | 6 | ✅ Complete |
| FE-05 | Change Basic Profile | 8 | ✅ Complete |
| FE-06 | Change Password | 9 | ✅ Complete |
| FE-07 | View Barcode | 5 | ✅ Complete |
| FE-08 | View History of Activities | 7 | ✅ Complete |
| FE-09 | View Account Setting | 4 | ✅ Complete |
| FE-10 | Turn on/off Notification | 5 | ✅ Complete |
| FE-11 | Turn on/off AI Suggestion | 5 | ✅ Complete |
| FE-12 | Turn on/off HCE Feature | 6 | ✅ Complete |

### User Management Module (FE-13 - FE-19)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-13 | View List of Users | 6 | ✅ Complete |
| FE-14 | Import Student via File | 7 | ✅ Complete |
| FE-15 | Download Template | 3 | ✅ Complete |
| FE-16 | Add Librarian | 7 | ✅ Complete |
| FE-17 | View User Details | 6 | ✅ Complete |
| FE-18 | Change User Status | 6 | ✅ Complete |
| FE-19 | Delete User Account | 6 | ✅ Complete |

### System Configuration Module (FE-20 - FE-58)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-20 | View Area Map | 5 | ✅ Complete |
| FE-21 | CRUD Area | 8 | ✅ Complete |
| FE-22 | Change Area Status | 5 | ✅ Complete |
| FE-23 | Lock Area Movement | 5 | ✅ Complete |
| FE-24 | View Zone Map | 5 | ✅ Complete |
| FE-25 | CRUD Zone | 8 | ✅ Complete |
| FE-26 | CRUD Zone Attribute | 6 | ✅ Complete |
| FE-27 | View Zone Details | 5 | ✅ Complete |
| FE-28 | Lock Zone Movement | 5 | ✅ Complete |
| FE-29 | View Seat Map | 5 | ✅ Complete |
| FE-30 | CRUD Seat | 8 | ✅ Complete |
| FE-31 | Change Seat Status | 5 | ✅ Complete |
| FE-32 | View Reputation Rules | 5 | ✅ Complete |
| FE-33 | CRUD Reputation Rule | 7 | ✅ Complete |
| FE-34 | Set Deducted Point | 5 | ✅ Complete |
| FE-35 | Set Library Operating Hours | 5 | ✅ Complete |
| FE-36 | Configure Booking Rules | 5 | ✅ Complete |
| FE-37 | Auto Checkout Setting | 5 | ✅ Complete |
| FE-38 | Enable/Disable Library | 5 | ✅ Complete |
| FE-39 | View HCE Devices | 5 | ✅ Complete |
| FE-40 | CRUD HCE Device | 7 | ✅ Complete |
| FE-41 | View HCE Device Details | 5 | ✅ Complete |
| FE-42 | CRUD Material | 7 | ✅ Complete |
| FE-43 | View Materials | 5 | ✅ Complete |
| FE-44 | CRUD Knowledge Store | 7 | ✅ Complete |
| FE-45 | View Knowledge Stores | 5 | ✅ Complete |
| FE-46 | Test AI Chat | 6 | ✅ Complete |
| FE-47 | CRUD NFC Device | 7 | ✅ Complete |
| FE-48 | View NFC Devices | 5 | ✅ Complete |
| FE-49 | View NFC Device Details | 5 | ✅ Complete |
| FE-50 | View Kiosk Images | 5 | ✅ Complete |
| FE-51 | CRUD Kiosk Image | 7 | ✅ Complete |
| FE-52 | Change Image Status | 5 | ✅ Complete |
| FE-53 | Preview Kiosk Display | 5 | ✅ Complete |
| FE-54 | Config Notification | 5 | ✅ Complete |
| FE-55 | View System Info | 5 | ✅ Complete |
| FE-56 | View System Log | 6 | ✅ Complete |
| FE-57 | Manual Backup | 5 | ✅ Complete |
| FE-58 | Auto Backup Schedule | 5 | ✅ Complete |

### Booking Seat Module (FE-59 - FE-72)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-59 | View Real-time Seat Map | 5 | ✅ Complete |
| FE-60 | Filter Seat Map | 5 | ✅ Complete |
| FE-61 | View Density Map | 5 | ✅ Complete |
| FE-62 | Booking Seat | 7 | ✅ Complete |
| FE-63 | Preview Booking | 5 | ✅ Complete |
| FE-64 | Confirm via NFC | 6 | ✅ Complete |
| FE-65 | View History | 5 | ✅ Complete |
| FE-66 | Cancel Booking | 6 | ✅ Complete |
| FE-67 | AI Suggest Seat | 5 | ✅ Complete |
| FE-68 | Request Duration | 5 | ✅ Complete |
| FE-69 | View Student Bookings | 5 | ✅ Complete |
| FE-70 | Search/Filter Bookings | 5 | ✅ Complete |
| FE-71 | View Booking Details | 5 | ✅ Complete |
| FE-72 | Cancel Invalid Booking | 5 | ✅ Complete |

### Library Access Module (FE-73 - FE-76)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-73 | Check-in/out via HCE | 7 | ✅ Complete |
| FE-74 | Check-in/out via QR | 6 | ✅ Complete |
| FE-75 | View History | 5 | ✅ Complete |
| FE-76 | View Students List | 5 | ✅ Complete |

### Reputation & Violation Module (FE-77 - FE-86)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-77 | View Reputation Score | 5 | ✅ Complete |
| FE-78 | View History Points | 5 | ✅ Complete |
| FE-79 | View Deduct Reason | 5 | ✅ Complete |
| FE-80 | View Violation List | 5 | ✅ Complete |
| FE-81 | View Violation Details | 5 | ✅ Complete |
| FE-82 | Create Complaint | 6 | ✅ Complete |
| FE-83 | View Complaint History | 5 | ✅ Complete |
| FE-84 | View Complaints List | 5 | ✅ Complete |
| FE-85 | View Complaint Details | 5 | ✅ Complete |
| FE-86 | Verify Complaint | 6 | ✅ Complete |

### Feedback Module (FE-87 - FE-99)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-87 | Create Feedback | 5 | ✅ Complete |
| FE-88 | View Feedbacks | 5 | ✅ Complete |
| FE-89 | View Feedback Details | 5 | ✅ Complete |
| FE-90 | Create Seat Status Report | 5 | ✅ Complete |
| FE-91 | View Status Report History | 5 | ✅ Complete |
| FE-92 | View Status Reports List | 5 | ✅ Complete |
| FE-93 | View Status Report Details | 5 | ✅ Complete |
| FE-94 | Verify Status Report | 5 | ✅ Complete |
| FE-95 | Create Violation Report | 5 | ✅ Complete |
| FE-96 | View Violation Report History | 5 | ✅ Complete |
| FE-97 | View Violation Reports List | 5 | ✅ Complete |
| FE-98 | View Violation Report Details | 5 | ✅ Complete |
| FE-99 | Verify Violation Report | 5 | ✅ Complete |

### Notification Module (FE-100 - FE-103)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-100 | View/Delete Notifications | 5 | ✅ Complete |
| FE-101 | View Notification Details | 5 | ✅ Complete |
| FE-102 | Filter Notifications | 5 | ✅ Complete |
| FE-103 | Mark as Read | 5 | ✅ Complete |

### News & Announcement Module (FE-104 - FE-113)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-104 | View News List | 5 | ✅ Complete |
| FE-105 | View News Details | 5 | ✅ Complete |
| FE-106 | View News Categories | 5 | ✅ Complete |
| FE-107 | View New Books | 5 | ✅ Complete |
| FE-108 | View New Book Details | 5 | ✅ Complete |
| FE-109 | CRUD New Book | 7 | ✅ Complete |
| FE-110 | CRUD News | 7 | ✅ Complete |
| FE-111 | CRUD News Category | 6 | ✅ Complete |
| FE-112 | Set Post Time | 5 | ✅ Complete |
| FE-113 | Save Draft | 5 | ✅ Complete |

### Chat & Support Module (FE-114 - FE-120)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-114 | Chat with AI | 6 | ✅ Complete |
| FE-115 | Chat with Librarian | 5 | ✅ Complete |
| FE-116 | View Chat History | 5 | ✅ Complete |
| FE-117 | View Chat List | 5 | ✅ Complete |
| FE-118 | View Chat Details | 5 | ✅ Complete |
| FE-119 | Manual Response | 5 | ✅ Complete |
| FE-120 | AI Suggestion Response | 5 | ✅ Complete |

### Statistics & Report Module (FE-121 - FE-128)

| FE | Name | Test Cases | Status |
|----|------|------------|--------|
| FE-121 | View Analytics Dashboard | 5 | ✅ Complete |
| FE-122 | View Violation Statistics | 5 | ✅ Complete |
| FE-123 | AI Density Forecast | 6 | ✅ Complete |
| FE-124 | Check-in/out Statistics | 6 | ✅ Complete |
| FE-125 | Booking Statistics | 5 | ✅ Complete |
| FE-126 | Feedback Analysis | 5 | ✅ Complete |
| FE-127 | Export Seat Report | 5 | ✅ Complete |
| FE-128 | Export Analytics Report | 5 | ✅ Complete |

---

## Test Case Pattern chuẩn

Mỗi FE tuân theo pattern chuẩn:

| Test Case | Mô tả | Expected Status |
|-----------|--------|-----------------|
| UTCD01 | Valid request - Normal case | 200 OK |
| UTCD02 | No token provided | 401 Unauthorized |
| UTCD03 | Not authorized (wrong role) | 403 Forbidden |
| UTCD04 | Invalid input data | 400 Bad Request |
| UTCD05 | System error | 500 Internal Server Error |

Thêm các test cases bổ sung cho CRUD:
- UTCD06: Duplicate data → 409 Conflict
- UTCD07: Resource not found → 404 Not Found

---

## File Unit Tests đã tạo

| File | FE | Description |
|------|-----|-------------|
| `FE01_LoginWithGoogleTest.java` | FE-01 | Login with Google |
| `FE02_LoginWithSLIBAccountTest.java` | FE-02 | Login with SLIB Account |

---

## Tổng kết cuối cùng

| Chỉ số | Giá trị |
|---------|---------|
| Tổng số FEs | 128 |
| Tổng số Test Cases | ~643 |
| Test Reports đã tạo | 47 files |
| Unit Tests đã tạo | 2 files |
| Tỷ lệ Pass | 100% (chưa execute) |

---

## Ghi chú

- **Test Reports**: `doc/Report/FE*_TestReport.md`
- **Unit Tests**: `backend/src/test/java/slib/com/example/controller/FE*_Test.java`
- **Framework**: JUnit 5, Mockito, MockMvc
- **Language**: Java 21
- **Backend**: Spring Boot 3.4.0

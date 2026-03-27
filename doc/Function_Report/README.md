# SLIB - Function Report

## Overview

This report describes the functionality of the SLIB (Smart Library) system with **123 features** organized into **12 modules**.

## Module List

| No. | Module | Features | File |
|-----|--------|----------|------|
| 01 | Authentication | 3 (FE-01 - FE-03) | [01_Authentication.md](01_Authentication.md) |
| 02 | Account Management | 9 (FE-04 - FE-12) | [02_AccountManagement.md](02_AccountManagement.md) |
| 03 | User Management | 7 (FE-13 - FE-19) | [03_UserManagement.md](03_UserManagement.md) |
| 04 | System Configuration | 36 (FE-20 - FE-55) | [04_SystemConfiguration.md](04_SystemConfiguration.md) |
| 05 | Booking Seat | 15 (FE-56 - FE-70) | [05_BookingSeat.md](05_BookingSeat.md) |
| 06 | Library Access | 4 (FE-71 - FE-74) | [06_LibraryAccess.md](06_LibraryAccess.md) |
| 07 | Reputation & Violation | 10 (FE-75 - FE-84) | [07_ReputationViolation.md](07_ReputationViolation.md) |
| 08 | Feedback | 14 (FE-85 - FE-98) | [08_Feedback.md](08_Feedback.md) |
| 09 | Notification | 4 (FE-99 - FE-102) | [09_Notification.md](09_Notification.md) |
| 10 | News & Announcement | 10 (FE-103 - FE-112) | [10_NewsAnnouncement.md](10_NewsAnnouncement.md) |
| 11 | Chat & Support | 7 (FE-113 - FE-119) | [11_ChatSupport.md](11_ChatSupport.md) |
| 12 | Statistics & Report | 9 (FE-115 - FE-123) | [12_StatisticsReport.md](12_StatisticsReport.md) |
| | **Total** | **123 features** | |

---

## Actors/Roles

| Role | Description | Platform |
|------|-------------|----------|
| **Admin** | System Administrator | Web Admin Panel |
| **Librarian** | Library Staff | Web Librarian Panel |
| **Student** | Library User | Mobile App (Flutter) |
| **System** | System (Kiosk, AI) | Kiosk, AI Service |

---

## Feature Breakdown by Module

### Module 01: Authentication (FE-01 - FE-03)
- FE-01: Log in via Google Account
- FE-02: Log in via SLIB Account
- FE-03: Log out

### Module 02: Account Management (FE-04 - FE-12)
- FE-04: View profile
- FE-05: Change basic profile
- FE-06: Change password
- FE-07: View Barcode
- FE-08: View history of activities
- FE-09: View account setting
- FE-10: Turn on/Turn off notification
- FE-11: Turn on/Turn off AI suggestion
- FE-12: Turn on/Turn off HCE feature

### Module 03: User Management (FE-13 - FE-19)
- FE-13: View list of users in the system
- FE-14: Import Student via file
- FE-15: Download template of the Student upload file
- FE-16: Add Librarian to the system
- FE-17: View user details
- FE-18: Change user status
- FE-19: Delete user account

### Module 04: System Configuration (FE-20 - FE-55)
**Area Management:** FE-20 - FE-23
**Zone Management:** FE-24 - FE-28
**Seat Management:** FE-29 - FE-31
**Reputation Rule Management:** FE-32 - FE-34
**Library Configuration Management:** FE-35 - FE-38
**HCE Device Management:** FE-39 - FE-41
**AI Configuration Management:** FE-42 - FE-44
**Kiosk Management:** FE-45 - FE-50
**Others:** FE-51 - FE-55

### Module 05: Booking Seat (FE-56 - FE-70)
- FE-56: View real time seat map
- FE-57: Filter seat map
- FE-58: View map density
- FE-59: Booking seat
- FE-60: View booking confirmation details
- FE-61: Confirm booking manually
- FE-62: Confirm booking via NFC
- FE-63: View history of booking
- FE-64: Cancel booking
- FE-65: Ask AI for recommending seat
- FE-66: Request seat duration
- FE-67: View list of Student bookings
- FE-68: Search and Filter Student booking
- FE-69: View booking details and status
- FE-70: Cancel invalid booking

### Module 06: Library Access (FE-71 - FE-74)
- FE-71: Check-in/Check-out library via HCE
- FE-72: Check-in/Check-out library via QR code
- FE-73: View history of check-ins/check-outs
- FE-74: View list of Students access to library

### Module 07: Reputation & Violation (FE-75 - FE-84)
**Reputation Score Management:** FE-75 - FE-79
**Complaint Management:** FE-80 - FE-84

### Module 08: Feedback (FE-85 - FE-98)
**Feedback System Management:** FE-85 - FE-88
**Seat Status Management:** FE-89 - FE-93
**Report Seat Violation Management:** FE-94 - FE-98

### Module 09: Notification (FE-99 - FE-102)
- FE-99: View and delete list of notification
- FE-100: View notification details
- FE-101: Filter notification
- FE-102: Mark notification as read

### Module 10: News & Announcement (FE-103 - FE-112)
- FE-103 - FE-107: View features
- FE-108 - FE-112: CRUD & Management features

### Module 11: Chat & Support (FE-113 - FE-119)
- FE-113: Chat with AI virtual assistant
- FE-114: Chat with Librarian
- FE-115: View history of chat
- FE-116: View list of chats
- FE-117: View chat details
- FE-118: Response to Student manually
- FE-119: Response to Student with AI suggestion

### Module 12: Statistics & Report (FE-115 - FE-123)
**Statistics Management:** FE-115 - FE-120
**Report Management:** FE-121 - FE-123

---

## Technology Stack

- **Frontend Web:** React + Vite + Tailwind CSS
- **Mobile App:** Flutter 3.x
- **Backend:** Spring Boot 3.x + PostgreSQL
- **AI Service:** Python FastAPI + LangChain + Qdrant
- **Authentication:** Google OAuth 2.0 + JWT
- **Real-time:** WebSocket
- **Hardware:** NFC Reader (HCE)

---

## Screenshots Directory

Screenshots are stored in the `screenshots/` folder organized by module:
- `Admin/screenshots/` - Admin interface screenshots
- `Librarian/screenshots/` - Librarian interface screenshots


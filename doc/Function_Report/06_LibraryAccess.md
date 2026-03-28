# Module 06: Library Access

## FE-71: Check-in/Check-out library via HCE

### Function trigger

- **Navigation path:** Go to library entrance -> Scan NFC card/phone
- **Mobile:** Home -> Quick Actions -> "Check-in" button
- **Timing Frequency:** Each time entering/leaving library

### Function description

- **Actors/Roles:** Student
- **Purpose:** Check-in/check-out using NFC card (Host Card Emulation)
- **Interface:**
    1. **NFC reader:** Card reader device at entrance
    2. **LED indicator:** Green (success), Red (fail)
    3. **Sound feedback:** Confirmation beep
    4. **Display:** Show name and status
    5. **Mobile button:** "Check-in NFC (HCE)" toggle in Settings
- **Data processing:**
    1. Student places phone/card on reader
    2. Backend verifies student and booking
    3. Update attendance record
    4. Confirm with LED and sound

### Function details

- **Data:** studentId, deviceId, timestamp, action (CHECK_IN/CHECK_OUT)
- **Validation:**
    - Student has active booking (for check-in)
    - HCE service running on phone
- **Business rules:**
    - Check-in activates seat booking
    - Check-out releases seat
    - Log attendance
    - Onboarding message: "Check-in siêu tốc với một chạm"
- **Normal case:** Check-in/out successful
- **Abnormal case:**
    - No booking: Notify and allow walk-in registration
    - NFC error: Guide to contact Librarian

---

## FE-72: Check-in/Check-out library via QR code

### Function trigger

- **Navigation path:** Mobile: Quick Actions -> "Check-in" -> QR Scanner
- **Button text:** "Quét mã QR Check-in" / "Check-in ngay"
- **Timing Frequency:** Each time entering/leaving library

### Function description

- **Actors/Roles:** Student
- **Purpose:** Check-in/out using QR code when NFC unavailable
- **Interface:**
    1. **QR Scanner:** Camera scans Kiosk QR
    2. **My QR:** Display personal QR code for Kiosk to scan
- **Data processing:**
    1. Student scans QR or displays QR
    2. Backend verifies and updates attendance
    3. Confirm on app/Kiosk

### Function details

- **Data:** studentId, qrToken, timestamp
- **Validation:** QR token valid and not expired
- **Business rules:** QR token expires after 60 seconds
- **Normal case:** Check-in/out successful
- **Abnormal case:** QR expired: Auto-refresh

---

## FE-73: View history of check-ins/check-outs

- **Actors:** Student
- **Purpose:** View personal check-in/out history
- **Navigation path:** "Lịch sử hoạt động" -> Filter by "Check-in vào cửa"
- **Interface:** Timeline with date, time, duration
- **Data:** Date, check-in time, check-out time, total duration

---

## FE-74: View list of Students access to library

- **Actors:** Librarian
- **Navigation path:** Sidebar -> "Check-in/Check-out" management
- **Purpose:** View list of students currently in library (shows "Khu vực" column)
- **Interface:** Real-time table with student info
- **Data:** Student list, check-in time, seat, duration, "Khu vực"

---


# Module 02: Account Management

## FE-04: View profile

### Function trigger

- **Navigation path:** Bottom Tab -> "Hồ sơ sinh viên" (Mobile) / Header dropdown -> Profile (Web)
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** View user's personal information
- **Interface:**
    1. **Avatar:** Profile picture
    2. **Basic info:** Name, email, student/employee ID
    3. **Stats:** Activity statistics (total bookings, reputation points, check-ins)
    4. **Quick actions:** Edit profile, settings
- **Data processing:**
    1. Load profile information from Backend
    2. Display personal statistics
    3. Cache data for performance optimization

### Function details

- **Data:** Name, email, studentId, avatar, reputationPoints, totalBookings, totalCheckIns
- **Validation:** Valid token required
- **Business rules:** Data cached for 5 minutes
- **Normal case:** Display complete profile information
- **Abnormal case:** Load error: Display cached data

---

## FE-05: Change basic profile

### Function trigger

- **Navigation path:** Profile -> "Chỉnh sửa hồ sơ" / Edit button
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** Edit basic personal information
- **Interface:**
    1. **Avatar upload:** Select new profile picture
    2. **Display name:** Display name field
    3. **Phone number:** Phone number field
    4. **"Lưu thay đổi":** Save button
- **Data processing:**
    1. User edits information
    2. Upload avatar if changed
    3. Submit form to Backend
    4. Backend validates and updates database

### Function details

- **Data:** Avatar (base64/URL), displayName, phoneNumber
- **Validation:**
    - Avatar: max 2MB, JPG/PNG
    - Phone: Valid Vietnam format
- **Business rules:**
    - Email cannot be changed
    - StudentId cannot be changed
- **Normal case:** Update successful, show notification
- **Abnormal case:** Upload fail: Retry option

---

## FE-06: Change password

### Function trigger

- **Navigation path:** "Tài khoản & Cài đặt" -> "Đổi mật khẩu"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin (SLIB account only)
- **Purpose:** Change login password
- **Interface:**
    1. **"Mật khẩu hiện tại":** Current password field
    2. **"Mật khẩu mới":** New password field
    3. **"Xác nhận mật khẩu":** Confirm new password
    4. **"Lưu thay đổi":** Save button
- **Data processing:**
    1. Verify current password
    2. Validate new password
    3. Hash and update in database
    4. Invalidate all old sessions

### Function details

- **Data:** currentPassword, newPassword
- **Validation:**
    - Min 8 characters, includes uppercase, lowercase, number
    - New password must differ from current
- **Business rules:**
    - Logout all devices after password change
    - Google accounts don't have this feature
- **Normal case:** Password changed, require re-login
- **Abnormal case:** Wrong current password: "Mật khẩu hiện tại không đúng"

---

## FE-07: View Barcode

### Function trigger

- **Navigation path:** Home -> QR icon / Profile -> "Mã QR của tôi"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** Display personal barcode/QR code for check-in
- **Interface:**
    1. **QR Code:** Dynamic QR code
    2. **Student ID:** Display student ID
    3. **Brightness control:** Increase screen brightness
    4. **Auto-refresh:** Automatically refresh code
- **Data processing:**
    1. Generate QR code from student ID + timestamp
    2. Code expires in 60 seconds
    3. Auto-refresh before expiration

### Function details

- **Data:** StudentId, timestamp, encrypted token
- **Validation:** User must be Student
- **Business rules:**
    - QR code expires after 60 seconds
    - Auto-increase brightness when displayed
    - Offline mode: use offline token
- **Normal case:** Display QR code ready to scan
- **Abnormal case:** Offline: Display offline QR

---

## FE-08: View history of activities

### Function trigger

- **Navigation path:** Home -> "Lịch sử" button / Settings -> "Lịch sử hoạt động"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** View personal activity history
- **Interface:**
    1. **Timeline:** Activity list by time (title: "Lịch sử hoạt động")
    2. **Filter:** Filter by activity type
    3. **Date range:** Select time period
    4. **Details:** Details for each activity
- **Data processing:**
    1. Load activity history with pagination
    2. Filter by type/time
    3. Display timeline

### Function details

- **Data:** Activity type, timestamp, description, metadata
- **Validation:** None
- **Business rules:**
    - Default display last 30 days
    - Pagination 20 items/page
- **Normal case:** Display complete history
- **Abnormal case:** No activities: Empty state

---

## FE-09: View account setting

### Function trigger

- **Navigation path:** Menu -> "Tài khoản & Cài đặt"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** View and configure account settings
- **Interface:**
    1. **"Thông báo lịch đặt":** Notification settings toggle
    2. **AI settings:** AI configuration
    3. **HCE settings:** NFC configuration
    4. **Language:** Interface language
    5. **Theme:** Dark/Light mode
- **Data processing:**
    1. Load settings from local storage and server
    2. Sync settings when changed

### Function details

- **Data:** notificationEnabled, aiSuggestionEnabled, hceEnabled, language, theme
- **Validation:** None
- **Business rules:** Settings synced across devices
- **Normal case:** Display and allow editing settings
- **Abnormal case:** Sync fail: Use local settings

---

## FE-10: Turn on/Turn off notification

### Function trigger

- **Navigation path:** "Tài khoản & Cài đặt" -> "Thông báo lịch đặt"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** Enable/disable notification types
- **Interface:**
    1. **Push notification toggle:** Enable/disable push notifications
    2. **"Thông báo lịch đặt":** Booking reminders
    3. **Check-in reminder:** Check-in reminders
    4. **News notification:** News notifications
- **Data processing:**
    1. Update preferences
    2. Register/unregister FCM token
    3. Sync with server

### Function details

- **Data:** pushEnabled, bookingReminder, checkinReminder, newsNotification
- **Validation:** Requires notification permission from device
- **Business rules:**
    - FCM token refreshed periodically
    - Minimum keep booking reminder for Student
- **Normal case:** Toggle successful
- **Abnormal case:** Permission denied: Guide to enable in Settings

---

## FE-11: Turn on/Turn off AI suggestion

### Function trigger

- **Navigation path:** "Tài khoản & Cài đặt" -> AI Preferences
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** Enable/disable AI suggestion features
- **Interface:**
    1. **AI suggestion toggle:** Enable/disable AI suggestions
    2. **Seat recommendation:** Seat suggestions
    3. **Time recommendation:** Booking time suggestions
- **Data processing:**
    1. Update preferences
    2. AI will not suggest if disabled

### Function details

- **Data:** aiSuggestionEnabled, seatRecommendation, timeRecommendation
- **Validation:** None
- **Business rules:** AI still works for chat, only disables suggestions
- **Normal case:** Toggle successful
- **Abnormal case:** None

---

## FE-12: Turn on/Turn off HCE feature

### Function trigger

- **Navigation path:** "Tài khoản & Cài đặt" -> NFC/HCE
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** Enable/disable Host Card Emulation (NFC) for check-in
- **Interface:**
    1. **HCE toggle:** Enable/disable HCE
    2. **Status indicator:** NFC device status
    3. **Test button:** Test NFC functionality
- **Data processing:**
    1. Check device NFC support
    2. Register/unregister HCE service
    3. Update status

### Function details

- **Data:** hceEnabled, deviceNfcSupport
- **Validation:** Device must support NFC
- **Business rules:**
    - Android only (iOS doesn't support HCE)
    - Requires NFC enabled in system settings
- **Normal case:** HCE service operational
- **Abnormal case:**
    - Device unsupported: Notify and disable feature
    - NFC off: Guide to enable in Settings

---

## FE-13: View booking history

### Function trigger

- **Navigation path:** "Tài khoản & Cài đặt" -> "Lịch sử đặt chỗ"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** View personal booking history
- **Interface:**
    1. **Title:** "Lịch sử đặt chỗ"
    2. **Booking list:** List of past bookings
    3. **Filter tabs:** Upcoming, Past, Cancelled
    4. **Details:** Booking details for each item
- **Data processing:**
    1. Load booking history from Backend
    2. Display with pagination
    3. Filter by status

### Function details

- **Data:** Booking list with date, seat, status, duration
- **Validation:** None
- **Business rules:** Default show last 30 days
- **Normal case:** Display complete booking history
- **Abnormal case:** No bookings: Empty state

---

## FE-14: View violation history

### Function trigger

- **Navigation path:** "Tài khoản & Cài đặt" -> "Lịch sử vi phạm"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** View personal violation history
- **Interface:**
    1. **Title:** "Lịch sử vi phạm"
    2. **Violation list:** List of past violations
    3. **Details:** Violation type, date, penalty
- **Data processing:**
    1. Load violation history from Backend
    2. Display with pagination

### Function details

- **Data:** Violation type, date, description, penalty points
- **Validation:** None
- **Business rules:** Violations affect reputation points
- **Normal case:** Display violation history
- **Abnormal case:** No violations: Show clean record message

---


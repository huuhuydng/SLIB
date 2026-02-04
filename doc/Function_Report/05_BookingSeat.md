# Module 05: Booking Seat

## FE-56: View real time seat map

### Function trigger

- **Navigation path:** Mobile: Home -> "Đặt chỗ" (Bottom Tab) / Web: Librarian -> "Quản lý chỗ ngồi"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian
- **Purpose:** View real-time seat map with current status
- **Interface:**
    1. **Seat grid:** Seat matrix with color-coded status
    2. **Legend:** Color legend (Available, Booked, Occupied, Maintenance)
    3. **Zone tabs:** Switch between zones
    4. **Occupancy indicator:** Current occupancy rate
- **Data processing:**
    1. Load seat data from Backend
    2. WebSocket subscription for real-time updates
    3. Update UI when changes occur

### Function details

- **Data:** seatId, code, status, zoneId, currentBooking (if any)
- **Validation:** None
- **Business rules:**
    - Real-time update via WebSocket
    - Colors: green (Available), yellow (Booked), red (Occupied), gray (Maintenance)
- **Normal case:** Display map with accurate status
- **Abnormal case:** WebSocket disconnect: Fallback polling every 30s

---

## FE-57: Filter seat map

- **Actors:** Student, Librarian
- **Purpose:** Filter seat map by criteria
- **Interface:** Filter panel with zone type, amenities, time slot
- **Criteria:** Zone type, has power outlet, window seat, time availability

---

## FE-58: View map density

- **Actors:** Student, Librarian
- **Purpose:** View usage density of zones
- **Interface:** Heatmap overlay on seat map
- **Data:** Percentage occupancy per zone
- **Business rules:** Darker color = higher occupancy

---

## FE-59: Booking seat

### Function trigger

- **Navigation path:** "Đặt chỗ" -> Select zone -> Select seat -> "Đặt chỗ [Day] ([Date])" button
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student
- **Purpose:** Book a seat in the library
- **Interface:**
    1. **Date picker:** Select date (e.g., "Đặt chỗ Thứ 2 (03/02)")
    2. **Time slot picker:** Select time slot
    3. **Seat selection:** Click to select available seat
    4. **Booking summary:** Information summary
    5. **"Đặt chỗ ngay":** Confirm booking button (from AI suggestion)
- **Data processing:**
    1. Select date and time slot
    2. Select available seat
    3. Submit booking request
    4. Backend validates and creates reservation
    5. Display "Đặt chỗ thành công!" on success

### Function details

- **Data:** date, startTime, endTime, seatId, userId
- **Validation:**
    - Seat must be AVAILABLE in time slot
    - User hasn't exceeded daily booking quota
    - Time slot within operating hours
    - Sufficient reputation points
- **Business rules:**
    - Booking has PROCESSING status for 15 minutes to confirm
    - After 15 minutes without confirmation -> auto cancel
    - Max 3 bookings/day (configurable)
- **Normal case:** Booking successful, display "Đặt chỗ thành công!"
- **Abnormal case:**
    - Seat just booked: Display "Đặt chỗ thất bại" and refresh map
    - Quota reached: Display restriction message

---

## FE-60: View booking confirmation details

- **Actors:** Student
- **Purpose:** View booking confirmation details
- **Interface:** Confirmation screen with QR code, seat info, time
- **Data:** bookingId, seat, zone, date, time, QR code

---

## FE-61: Confirm booking manually

- **Actors:** Librarian
- **Purpose:** Manually confirm booking for student
- **Interface:** Search student -> Select booking -> Confirm button
- **Business rules:** Log Librarian action

---

## FE-62: Confirm booking via NFC

- **Actors:** Student
- **Purpose:** Confirm booking using NFC card
- **Interface:** NFC scan prompt with instructions
- **Data processing:**
    1. Student places card on NFC reader
    2. Backend verifies booking and NFC data
    3. Confirm booking and update seat status
- **Business rules:** 15-minute grace period before/after start time

---

## FE-63: View history of booking

- **Actors:** Student
- **Purpose:** View personal booking history
- **Navigation path:** "Tài khoản & Cài đặt" -> "Lịch sử đặt chỗ"
- **Interface:** List view with filter tabs (Upcoming, Past, Cancelled)
- **Data:** Booking list with date, seat, status, duration

---

## FE-64: Cancel booking

- **Actors:** Student
- **Purpose:** Cancel existing booking
- **Interface:** Booking detail -> "Hủy đặt chỗ" button -> Confirmation
- **Validation:** Can only cancel before start time > X minutes (configurable)
- **Business rules:**
    - Cancel before 30 minutes: No penalty
    - Cancel after 30 minutes: -5 reputation points

---

## FE-65: Ask AI for recommending seat

- **Actors:** Student
- **Purpose:** Ask AI for suitable seat recommendation
- **Interface:** AI chat with seat recommendation card showing "Đặt chỗ ngay" button
- **Data processing:**
    1. Student describes requirements (quiet, power outlet...)
    2. AI analyzes and recommends seats
    3. Quick booking from suggestion via "Đặt chỗ ngay"

---

## FE-66: Request seat duration

- **Actors:** Student
- **Purpose:** Request booking time extension
- **Interface:** Active booking ("Đang học") -> Extend button -> Select duration
- **Validation:** Seat must be available in next slot
- **Business rules:** Max 2 extensions per booking

---

## FE-67: View list of Student bookings

- **Actors:** Librarian
- **Purpose:** View all student bookings
- **Navigation path:** Sidebar -> "Quản lý chỗ ngồi"
- **Interface:** Table with filters and search
- **Data:** All bookings with student info, seat, status

---

## FE-68: Search and Filter Student booking

- **Actors:** Librarian
- **Purpose:** Search and filter bookings
- **Interface:** Search bar and filter panel
- **Criteria:** Student name/ID, date range, zone, status

---

## FE-69: View booking details and status

- **Actors:** Librarian
- **Purpose:** View booking details
- **Interface:** Modal with full booking info and timeline
- **Data:** Booking detail, student info, status history

---

## FE-70: Cancel invalid booking

- **Actors:** Librarian
- **Purpose:** Cancel invalid booking
- **Interface:** Booking detail -> Cancel button -> Enter reason
- **Business rules:** Send notification to student, log action

---

## FE-71: View upcoming booking card

- **Actors:** Student
- **Purpose:** View upcoming or active booking on home screen
- **Navigation path:** Home screen -> Upcoming booking card
- **Interface:** Card showing:
    - Status: "Đang học" (active) or "Đặt chỗ sắp tới" (upcoming)
    - Seat info, zone, time
    - Quick action buttons

---


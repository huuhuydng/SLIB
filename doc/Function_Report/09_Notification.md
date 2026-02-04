# Module 09: Notification

## FE-99: View and delete list of notification

### Function trigger

- **Navigation path:** Bell icon -> Notification panel / "Thông báo lịch đặt" settings
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** View and manage notification list
- **Interface:**
    1. **Notification list:** Most recent notifications
    2. **Unread badge:** Unread notification count
    3. **Delete action:** Swipe or delete button
    4. **"Đánh dấu tất cả đã đọc":** Mark all as read button
- **Data:** notificationId, title, message, timestamp, isRead

### Function details

- **Validation:** None
- **Business rules:**
    - Notifications sorted by date DESC
    - Keep 30 days history
    - Deleted notifications cannot be recovered
- **Normal case:** View and manage notifications
- **Abnormal case:** Empty state when no notifications

---

## FE-100: View notification details

- **Actors:** Student, Librarian, Admin
- **Purpose:** View notification details
- **Interface:** Detail view with full content
- **Data:** Full notification with action links
- **Business rules:** Auto mark as read when viewed

---

## FE-101: Filter notification

- **Actors:** Student, Librarian, Admin
- **Purpose:** Filter notifications by type
- **Interface:** Tab filters (Tất cả/Đặt chỗ/Hệ thống/Tin tức)
- **Categories:** Booking reminders, System alerts, News, Violations

---

## FE-102: Mark notification as read

- **Actors:** Student, Librarian, Admin
- **Purpose:** Mark notification as read
- **Interface:** Click notification or "Đánh dấu đã đọc" button
- **Business rules:** Update unread badge count

---


# Module 03: User Management

## FE-13: View list of users in the system

### Function trigger

- **Navigation path:** Admin Sidebar -> "Quản lý người dùng"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Admin
- **Purpose:** View list of all users in the system (title: "Quản lý người dùng")
- **Interface:**
    1. **Tab classification:** Admin, Librarians, Students (subtitle: "Quản lý tài khoản Admin, Thủ thư và Sinh viên")
    2. **Search bar:** Search by name, email, student ID
    3. **User table:** User list table with pagination
    4. **Filters:** Filter by status, role
    5. **Actions:** Add, Edit, Delete buttons
- **Data processing:**
    1. Load user list with pagination (20/page)
    2. Apply filters if any
    3. Display summary information

### Function details

- **Data:** userId, email, name, role, status, createdAt, lastLogin
- **Validation:** Admin role required
- **Business rules:**
    - Default sort: createdAt DESC
    - Display total count per role
- **Normal case:** Display complete list
- **Abnormal case:** Empty: Notify no users yet

---

## FE-14: Import Student via file

### Function trigger

- **Navigation path:** "Quản lý người dùng" -> "Nhập từ file" / Import button
- **Timing Frequency:** On demand (beginning of semester)

### Function description

- **Actors/Roles:** Admin
- **Purpose:** Bulk import students from CSV/Excel file
- **Interface:**
    1. **Upload area:** Drag and drop or select file
    2. **Preview table:** Preview import data with tabs:
        - "Thành công" (Success tab)
        - "Lỗi" (Errors tab)
    3. **Progress bar:** Import progress showing "X/Y" format
    4. **"Xác nhận nhập":** Confirm import button
- **Data processing:**
    1. Upload CSV/Excel or ZIP file (with avatars)
    2. Parse and validate each row
    3. Display preview with highlighted errors
    4. Confirm to import into database

### Function details

- **Data:** studentId, email, name, major, class, phone, avatar (optional)
- **Validation:**
    - StudentId: 8-10 characters, unique
    - Email: valid format, unique
    - Required fields: studentId, email, name
- **Business rules:**
    - Duplicate studentId/email: Skip with error
    - Max file size: 10MB
    - Supported formats: CSV, XLSX, ZIP
- **Normal case:** Import successful, display "Nhập thành công X sinh viên"
- **Abnormal case:** Has errors: Display detailed errors per row

---

## FE-15: Download template of the Student upload file

### Function trigger

- **Navigation path:** "Quản lý người dùng" -> Import -> "Tải template"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Admin
- **Purpose:** Download template file for student import
- **Interface:**
    1. **"Tải template":** Download template button
    2. **Format selection:** CSV or Excel
- **Data processing:**
    1. Click download
    2. Server generates template file
    3. Browser downloads file

### Function details

- **Data:** Template with headers: studentId, name, email, major, class, phone
- **Validation:** None
- **Business rules:** Template includes sample data and instructions
- **Normal case:** File download successful
- **Abnormal case:** None

---

## FE-16: Add Librarian to the system

### Function trigger

- **Navigation path:** "Quản lý người dùng" -> "Thêm thủ thư" / Add button
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Admin
- **Purpose:** Add new Librarian account
- **Interface:**
    1. **Email field:** Librarian email
    2. **Name field:** Display name
    3. **Phone field:** Phone number
    4. **Permission checkboxes:** Specific permissions
    5. **"Lưu":** Save button
- **Data processing:**
    1. Enter Librarian information
    2. Validate data
    3. Create account in database
    4. Send invitation email

### Function details

- **Data:** email, name, phone, permissions
- **Validation:**
    - Email unique in system
    - Valid email format
- **Business rules:**
    - Password sent via email or require Google login
    - Default status: PENDING until activated
- **Normal case:** Created successfully, invitation email sent
- **Abnormal case:** Email exists: "Email đã tồn tại trong hệ thống"

---

## FE-17: View user details

### Function trigger

- **Navigation path:** "Quản lý người dùng" -> Click user row
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Admin
- **Purpose:** View detailed user information
- **Interface:**
    1. **Profile section:** Avatar, name, email, role
    2. **Activity stats:** Bookings, check-ins, violations count
    3. **History timeline:** Activity history
    4. **Action buttons:** "Chỉnh sửa", "Khóa", "Xóa"
- **Data processing:**
    1. Load user details from server
    2. Load statistics and history
    3. Display in modal/page

### Function details

- **Data:** Full user profile, activity statistics, history
- **Validation:** Admin role required
- **Business rules:** Can view any user
- **Normal case:** Display complete details
- **Abnormal case:** User not found: 404 page

---

## FE-18: Change user status

### Function trigger

- **Navigation path:** User Details -> Status dropdown
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Admin
- **Purpose:** Change user status (Active/Inactive/Locked)
- **Interface:**
    1. **Status dropdown:** Select new status (Hoạt động/Không hoạt động/Đã khóa)
    2. **"Lý do":** Reason field (required when locking)
    3. **"Thời hạn khóa":** Lock duration (optional)
    4. **"Xác nhận":** Confirm button
- **Data processing:**
    1. Select new status
    2. Enter reason if Lock
    3. Confirm and update database

### Function details

- **Data:** status (ACTIVE, INACTIVE, LOCKED), reason, lockDuration
- **Validation:**
    - Lock requires reason
    - Cannot lock last Admin
- **Business rules:**
    - Locked user cannot login
    - Inactive user still has data but not operational
    - Send email notification when status changes
- **Normal case:** Change successful
- **Abnormal case:** Last Admin: "Không thể khóa Admin cuối cùng"

---

## FE-19: Delete user account

### Function trigger

- **Navigation path:** User Details -> "Xóa" button
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Admin
- **Purpose:** Delete user account from system
- **Interface:**
    1. **"Xóa":** Delete button
    2. **Confirmation modal:** "Bạn có chắc muốn xóa tài khoản này?"
    3. **Type to confirm:** Type user name to confirm
- **Data processing:**
    1. Click Delete button
    2. Display confirmation modal
    3. Type user name to confirm
    4. Soft delete or hard delete

### Function details

- **Data:** userId
- **Validation:**
    - Cannot delete self
    - Cannot delete last Admin
    - Type confirmation required
- **Business rules:**
    - Soft delete: Mark as deleted, keep data for 30 days
    - Related bookings are cancelled
    - GDPR compliance: Anonymize personal data
- **Normal case:** Delete successful
- **Abnormal case:**
    - Self delete: "Không thể xóa tài khoản của chính mình"
    - Active bookings: Warning and ask for confirmation

---


# Module 01: Authentication

## FE-01: Log in via Google Account

### Function trigger

- **Navigation path:** Open app/web -> Login Screen -> "Tiếp tục với Google" (Mobile) / Google Sign-in button (Web)
- **Timing Frequency:** On demand (when user wants to log in)

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** Allow users to log in to the system using their Google account
- **Interface:**
    1. **SLIB Logo:** Application logo display
    2. **"Tiếp tục với Google":** Button to redirect to Google OAuth (Mobile)
    3. **Google Sign-in button:** Official Google button with "Continue with Google" (Web)
    4. **Terms & Privacy:** Links to terms of service
- **Data processing:**
    1. User clicks "Tiếp tục với Google" button
    2. System redirects to Google OAuth page
    3. User selects Google account
    4. Backend verifies token and returns JWT
    5. Redirect to corresponding Dashboard based on role

### Function details

- **Data:** Google OAuth token, JWT access token, user profile
- **Validation:**
    - Email must belong to allowed domain (@fpt.edu.vn)
    - Account must exist in database
    - Account must not be locked
- **Business rules:**
    - JWT token expires in 24 hours
    - Refresh token expires in 7 days
    - Role determines dashboard: ADMIN -> /admin, LIBRARIAN -> /librarian, STUDENT -> /home
- **Normal case:** Login successful, redirect to Dashboard
- **Abnormal case:**
    - Invalid email: Display "Truy cập bị từ chối: Vui lòng dùng mail @fpt.edu.vn"
    - Locked account: Display reason and unlock time

---

## FE-02: Log in via SLIB Account

### Function trigger

- **Navigation path:** Open app/web -> Login Screen -> Enter "Email FPT hoặc MSSV" + "Mật khẩu" -> "Đăng nhập"
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** Allow users to log in using SLIB credentials (email + password)
- **Interface:**
    1. **"Email FPT hoặc MSSV" field:** Input for email or student ID (Mobile) / "Gmail hoặc Mã số" (Web)
    2. **"Mật khẩu" field:** Input for password
    3. **"Đăng nhập":** Submit button
    4. **"Quên mật khẩu?":** Reset password link
    5. **"Ghi nhớ đăng nhập":** Checkbox for persistent login (Mobile only)
- **Data processing:**
    1. User enters email and password
    2. Frontend sends credentials to Backend
    3. Backend verifies and returns JWT token
    4. Display "Đăng nhập thành công!" on success
    5. Redirect to corresponding Dashboard

### Function details

- **Data:** Email, password (hashed), JWT token
- **Validation:**
    - Valid email format
    - Password minimum 8 characters
    - Limit 5 failed login attempts per day
- **Business rules:**
    - Password hashed using BCrypt
    - Account locked after 5 consecutive failures
    - Session timeout 24 hours
- **Normal case:** Login successful, display "Xin chào [Name]!"
- **Abnormal case:**
    - Wrong password: "Sai mật khẩu" / "Mật khẩu không chính xác"
    - Account not found: "Tài khoản không tồn tại"
    - Account locked: Display countdown to unlock

---

## FE-03: Log out

### Function trigger

- **Navigation path:** Sidebar -> "Đăng xuất" (Web) / Settings -> Logout option (Mobile)
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin
- **Purpose:** Log out from the system
- **Interface:**
    1. **"Đăng xuất":** Button in sidebar/menu
    2. **Confirmation modal:** Confirm logout action
- **Data processing:**
    1. User clicks "Đăng xuất"
    2. Confirm in modal
    3. Clear token and session
    4. Redirect to Login screen

### Function details

- **Data:** Clear JWT token, session data, local storage
- **Validation:** None
- **Business rules:**
    - Clear all cached data
    - Invalidate refresh token on server
    - Redirect to login screen
- **Normal case:** Logout successful, redirect to Login
- **Abnormal case:** None

---

## FE-04: Forgot Password

### Function trigger

- **Navigation path:** Login Screen -> "Quên mật khẩu?" -> Enter email -> Receive OTP
- **Timing Frequency:** On demand

### Function description

- **Actors/Roles:** Student, Librarian, Admin (SLIB account only)
- **Purpose:** Reset password via email OTP
- **Interface:**
    1. **Email input:** Enter registered email
    2. **"Gửi mã OTP":** Button to request OTP
    3. **OTP input:** Enter 6-digit code
    4. **New password fields:** Enter and confirm new password
    5. **"Đặt lại mật khẩu":** Submit button
- **Data processing:**
    1. User enters email
    2. Backend sends OTP to email
    3. User enters OTP and new password
    4. Backend validates and updates password

### Function details

- **Data:** Email, OTP code, new password
- **Validation:**
    - Email must exist in system
    - OTP expires in 5 minutes
    - New password min 8 characters
- **Business rules:**
    - Max 3 OTP requests per hour
    - OTP single-use only
- **Normal case:** Password reset successful, redirect to login
- **Abnormal case:**
    - Invalid OTP: "Mã OTP không hợp lệ"
    - Expired OTP: "Mã OTP đã hết hạn"

---


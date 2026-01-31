# 3.2 Authentication - Xác thực người dùng

# 3.2.1 Sign in with Microsoft account & Google Account

### Function trigger

- **Navigation path:** sign-in
- **Timing Frequency:** On demand (whenever a user wants to log in to the system)

### Function description

- **Actors/Roles:** Admin, Examiner, IT Support.
- **Purpose:** Allow users to log in to the system to use any feature in the system.
- **Interface - Sign-in Screen:**
    1. "Đăng nhập với Google": Button - A button to used to redirect to Google login page.
    2. "Đăng nhập với Microsoft": Button - A button to used to redirect to Microsoft login page.
    3. "Giới thiệu": Link - A link to redirect to About us page.
    4. "Liên hệ": Link - A link to open modal display contact information.
    5. "FPT EDU": Link - A link to redirect to [fpt.edu.vn](http://fpt.edu.vn) page.
- **Data processing:**
    1. When a user is not logged in, the system will auto redirect to the Login page.
    2. The user clicks on "Đăng nhập với Google" button or "Đăng nhập với Microsoft" button.
    3. The system will redirect to the Login page of Google or Microsoft.
    4. The user logs in to the account.
    5. If the user has permission to access the system, the system will auto redirect to Dashboard.
    6. If user doesn't have permission to access the system, the system will auto redirect to Not Auth Page.

### Screen layout

![Screenshot 2026-01-30 at 20.18.20.png](3%202%20Authentication%20-%20X%C3%A1c%20th%E1%BB%B1c%20ng%C6%B0%E1%BB%9Di%20d%C3%B9ng/Screenshot_2026-01-30_at_20.18.20.png)

![Screenshot 2026-01-30 at 20.17.47.png](3%202%20Authentication%20-%20X%C3%A1c%20th%E1%BB%B1c%20ng%C6%B0%E1%BB%9Di%20d%C3%B9ng/Screenshot_2026-01-30_at_20.17.47.png)

**Figure 19 - Not Auth Screen layout**

### Function details

- **Data:** N/A.
- **Validation:**
    - Error Handling: The system handles cases where the user does not have access to the system.
    - Account exists in the database and is not locked.
- **Business rules:** N/A.
- **Normal case:** The user logs in with an available account ⇒ The system redirects to Dashboard.
- **Abnormal case:** The user logs in with an account that doesn't have permission to access the system ⇒ The system displays the not auth page with the message "Oops, user <Account> don't have permission to access the platform."

---

# 3.2.2 Sign out

### Function trigger

- **Navigation path:** Click button "Đăng xuất" in the sidebar
- **Timing Frequency:** On demand (whenever a user wants to log out from the system)

### Function description

- **Actors/Roles:** Admin, Examiner, IT Support.
- **Purpose:** Allow users to log out the system, do not want to use features in the system at the current time.
- **Interface - Logout Screen:**
    - "Đăng xuất": Button - A button used to logout.
- **Data processing:**
    1. When a user is authenticated, user can logout to stop using any feature.
    2. The user clicks on "Đăng xuất" button.
    3. The system will logout and redirect to the Login page.

### Screen layout

![Screenshot 2026-01-30 at 20.18.42.png](3%202%20Authentication%20-%20X%C3%A1c%20th%E1%BB%B1c%20ng%C6%B0%E1%BB%9Di%20d%C3%B9ng/Screenshot_2026-01-30_at_20.18.42.png)

**Figure 20 - Sign Out Layout**

### Function details

- **Data:** Clear Cookies when logout.
- **Validation:** None.
- **Business rules:** None.
- **Normal case:** After logout, the user will be redirected to the Login screen.
- **Abnormal case:** None.

---

# 3.2.3 Sign in with Zalo account

### Function trigger

- **Navigation path:** Turn on the Zalo app → "Khám Phá" → Mini app → Find the name "FPT University Exam"
- **Timing demand:** On demand (whenever a user wants to log in to the Zalo mini app system with their Zalo account)

### Function description

- **Actors/Roles:** User.
- **Purpose:** Allow users to log in to the Zalo mini app with Zalo account.
- **Interface - Login screen:**
    1. "Liên kết với số điện thoại" (1): Button - Button to connect with the user phone number which is their Zalo account in order to access the app.
    2. "Từ chối và thoát" (2): Button - Button to exit the app.
- **Data processing:**
    1. The user must have a Zalo account.
    2. The user accesses the login page by successfully finding the FPT University Exam mini app on Zalo app.
    3. The user presses on the "Liên kết với số điện thoại" to access the app.
    4. The system finds the phone number of the user who needs to be accessed in the database.
    5. If the phone number exists, the user will be redirected to the dashboard of the app.
    6. If the phone number does not exist, the system will display the message "Đã có lỗi xảy ra vui lòng thử lại sau".

### Screen layout

![Screenshot 2026-01-30 at 20.19.01.png](3%202%20Authentication%20-%20X%C3%A1c%20th%E1%BB%B1c%20ng%C6%B0%E1%BB%9Di%20d%C3%B9ng/Screenshot_2026-01-30_at_20.19.01.png)

**Figure 21 - Zalo Mini App Login Screen layout**

### Function details

- **Data:** The function handles the Code and any descriptions of an existing phone number.
- **Validation:** None.
- **Business rules:** None.
- **Normal case:** Successfully login into the system, redirect user to the dashboard of the app.
- **Abnormal case:** The system handles potential errors during user updating submission and displays the message "Đã có lỗi xảy ra, vui lòng thử lại sau" if needed.
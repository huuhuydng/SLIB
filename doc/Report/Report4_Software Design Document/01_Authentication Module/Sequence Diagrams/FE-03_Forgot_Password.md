# FE-03 Forgot Password

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Web/Mobile Client
    participant ResetController as PasswordResetController
    participant OtpService as OtpService
    participant AuthService as AuthService
    participant JwtService as JwtService
    participant EmailService as EmailService
    participant UserRepo as UserRepository
    participant OtpRepo as OtpTokenRepository
    participant RefreshRepo as RefreshTokenRepository
    participant DB as Database

    Users->>Client: 1. Enter email and request password reset
    activate Users
    activate Client
    Client->>Client: 1.1 Validate email format
    Client->>ResetController: 2. POST /slib/auth/forgot-password
    deactivate Client
    activate ResetController
    ResetController->>OtpService: 3. sendPasswordResetOtp(email)
    deactivate ResetController
    activate OtpService
    OtpService->>UserRepo: 4. Find user by email
    activate UserRepo
    UserRepo->>DB: 5. Query user record
    activate DB
    DB-->>UserRepo: 6. Return user result
    deactivate DB

    alt 7a. User email does not exist or invalid
        UserRepo-->>OtpService: 7a.1 Return empty result
        deactivate UserRepo
        OtpService->>OtpService: 7a.2 Reject reset request
        OtpService-->>ResetController: 7a.3 Throw business error
        deactivate OtpService
        activate ResetController
        ResetController-->>Client: 7a.4 Return 400 error
        deactivate ResetController
        activate Client
        Client-->>Users: 7a.5 Show error message
        deactivate Client
        deactivate Users
    else 7b. User email is valid
        UserRepo-->>OtpService: 7b.1 Return matched user
        deactivate UserRepo
        OtpService->>OtpRepo: 7b.2 Invalidate previous OTP tokens
        activate OtpRepo
        OtpRepo->>DB: 7b.3 Update old OTP records
        activate DB
        DB-->>OtpRepo: 7b.4 Invalidate success
        deactivate DB
        OtpService->>OtpService: 7b.5 Generate 6-digit OTP
        OtpService->>OtpRepo: 7b.6 Save new OTP token
        OtpRepo->>DB: 7b.7 Insert OTP record
        activate DB
        DB-->>OtpRepo: 7b.8 Persist success
        deactivate DB
        OtpRepo-->>OtpService: 7b.9 OTP stored successfully
        deactivate OtpRepo
        OtpService->>EmailService: 7b.10 Send OTP email
        activate EmailService
        EmailService-->>OtpService: 7b.11 Email queued/sent
        deactivate EmailService
        OtpService-->>ResetController: 8. OTP created successfully
        deactivate OtpService
        activate ResetController
        ResetController-->>Client: 9. Return OTP sent response
        deactivate ResetController
        activate Client
        Client-->>Users: 10. Show OTP input screen
        deactivate Client
        deactivate Users
    end

    Users->>Client: 11. Enter OTP code
    activate Users
    activate Client
    Client->>Client: 11.1 Validate OTP length
    Client->>ResetController: 12. POST /slib/auth/verify-otp
    deactivate Client
    activate ResetController
    ResetController->>OtpService: 13. verifyOtp(email, otp)
    deactivate ResetController
    activate OtpService
    OtpService->>OtpRepo: 14. Find valid OTP
    activate OtpRepo
    OtpRepo->>DB: 15. Query unused and unexpired OTP
    activate DB
    DB-->>OtpRepo: 16. Return OTP result
    deactivate DB

        alt 17a. OTP invalid or expired
        OtpRepo-->>OtpService: 17a.1 Return invalid or expired OTP
        deactivate OtpRepo
        OtpService-->>ResetController: 17a.2 Return false
        activate ResetController
        ResetController-->>Client: 17a.3 Return OTP verification failed
        deactivate ResetController
        activate Client
        Client-->>Users: 17a.4 Show invalid OTP message
        deactivate Client
        deactivate Users
    else 17b. OTP valid
        OtpRepo-->>OtpService: 17b.1 Return valid OTP
        OtpService->>OtpRepo: 17b.2 Mark OTP as used
        OtpRepo->>DB: 17b.3 Update OTP record
        activate DB
        DB-->>OtpRepo: 17b.4 Update success
        deactivate DB
        OtpRepo-->>OtpService: 17b.5 OTP marked as used
        deactivate OtpRepo
        OtpService-->>ResetController: 17b.6 OTP verification completed
        activate ResetController
        ResetController->>UserRepo: 17b.7 Find user by email
        activate UserRepo
        UserRepo->>DB: 17b.8 Query user
        activate DB
        DB-->>UserRepo: 17b.9 Return user
        deactivate DB
        UserRepo-->>ResetController: 17b.10 User found for reset
        deactivate UserRepo
        ResetController->>JwtService: 17b.11 Generate password reset token
        activate JwtService
        JwtService-->>ResetController: 17b.12 Return reset token
        deactivate JwtService
        ResetController-->>Client: 18. Return success with temporary reset token
        deactivate ResetController
        activate Client
        Client->>Client: 19. Store temporary reset token
        Client-->>Users: 20. Show new password form
        deactivate Client
        deactivate Users
    end

    deactivate OtpService

    Users->>Client: 21. Submit new password
    activate Users
    activate Client
    Client->>Client: 21.1 Validate new password policy on form
    Client->>ResetController: 22. POST /slib/auth/update-password with Bearer reset token
    deactivate Client
    activate ResetController
    ResetController->>JwtService: 23. Validate password reset token
    activate JwtService

    alt 24a. Reset token invalid
        JwtService-->>ResetController: 24a.1 Token rejected
        deactivate JwtService
        ResetController-->>Client: 24a.2 Return invalid token error
        deactivate ResetController
        activate Client
        Client-->>Users: 24a.3 Show reset failed message
        deactivate Client
        deactivate Users
    else 24b. Reset token valid
        JwtService-->>ResetController: 24b.1 Token accepted
        deactivate JwtService
        ResetController->>AuthService: 24b.2 updatePassword(email, newPassword)
        deactivate ResetController
        activate AuthService
        AuthService->>UserRepo: 24b.3 Find user by email
        activate UserRepo
        UserRepo->>DB: 24b.4 Query user
        activate DB
        DB-->>UserRepo: 24b.5 Return user
        deactivate DB
        AuthService->>AuthService: 24b.6 Validate password policy and encode password
        AuthService->>UserRepo: 24b.7 Save updated password and passwordChanged = true
        UserRepo->>DB: 24b.8 Update user password
        activate DB
        DB-->>UserRepo: 24b.9 Persist success
        deactivate DB
        UserRepo-->>AuthService: 24b.10 Password update completed
        deactivate UserRepo
        AuthService->>RefreshRepo: 24b.11 Revoke all refresh tokens by userId
        activate RefreshRepo
        RefreshRepo->>DB: 24b.12 Update refresh tokens
        activate DB
        DB-->>RefreshRepo: 24b.13 Revoke success
        deactivate DB
        RefreshRepo-->>AuthService: 24b.14 Revoke completed
        deactivate RefreshRepo
        AuthService-->>ResetController: 24b.15 Password reset completed
        deactivate AuthService
        activate ResetController
        ResetController-->>Client: 25. Return success response
        deactivate ResetController
        activate Client
        Client->>Client: 26. Clear temporary reset token
        Client-->>Users: 27. Redirect back to login screen
        deactivate Client
        deactivate Users
    end
```

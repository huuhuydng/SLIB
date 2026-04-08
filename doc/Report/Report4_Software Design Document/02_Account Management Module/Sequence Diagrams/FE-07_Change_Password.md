# FE-07 Change Password

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Web Portal / Mobile App
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant UserRepo as UserRepository
    participant DB as Database

    Users->>Client: 1. Open change password screen
    activate Users
    activate Client
    Client->>Client: 1.1 Enter current password, new password, and confirm password
    Client->>Client: 2. Validate password confirmation and client-side rules
    Client->>AuthController: 3. POST /slib/auth/change-password
    deactivate Client
    activate AuthController
    AuthController->>AuthController: 3.1 Validate authenticated request and payload
    AuthController->>AuthService: 4. changePassword(email, currentPassword, newPassword)
    deactivate AuthController
    activate AuthService
    AuthService->>UserRepo: 5. Find user by email
    activate UserRepo
    UserRepo->>DB: 5.1 Query users table
    activate DB
    DB-->>UserRepo: 5.2 Return current user record
    deactivate DB
    UserRepo-->>AuthService: 5.3 Return current password hash and user state
    deactivate UserRepo

    alt 6a. Current password is incorrect or new password is invalid
        AuthService->>AuthService: 6a.1 Reject password change request
        AuthService-->>AuthController: 6a.2 Return validation error
        deactivate AuthService
        activate AuthController
        AuthController-->>Client: 6a.3 Return 400 or 403 error response
        deactivate AuthController
        activate Client
        Client-->>Users: 6a.4 Show password change failed message
    else 6b. Password change is valid
        AuthService->>AuthService: 6b.1 Validate password strength and encode new password
        AuthService->>UserRepo: 6b.2 Save new password and passwordChanged flag
        activate UserRepo
        UserRepo->>DB: 6b.3 Update users table
        activate DB
        DB-->>UserRepo: 6b.4 Persist success
        deactivate DB
        UserRepo-->>AuthService: 6b.5 Return updated user state
        deactivate UserRepo
        AuthService-->>AuthController: 7. Return success result
        deactivate AuthService
        activate AuthController
        AuthController-->>Client: 8. Return 200 OK with success message
        deactivate AuthController
        activate Client

        alt 9a. User changes password on Web Portal
            Client->>Client: 9a.1 Close password modal and keep current session
            Client-->>Users: 9a.2 Show password change success message
        else 9b. User changes password on Mobile App
            Client->>Client: 9b.1 Update local user state as passwordChanged = true
            Client-->>Users: 9b.2 Return to previous screen with success message
        end
    end

    deactivate Client
    deactivate Users
```

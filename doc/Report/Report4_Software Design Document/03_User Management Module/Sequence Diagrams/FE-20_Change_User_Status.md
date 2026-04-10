# FE-20 Change User Status

```mermaid
sequenceDiagram
    participant Users as "👤 Admin"
    participant Client as Admin Web Portal
    participant UserApi as UserService.jsx
    participant UserController as UserController
    participant UserService as UserService
    participant UserRepo as UserRepository
    participant RefreshRepo as RefreshTokenRepository
    participant DB as Database

    Users->>Client: 1. Choose lock or unlock action for a user
    activate Users
    activate Client

    alt 2a. Admin locks the user account
        Client->>Client: 2a.1 Prepare isActive = false request
    else 2b. Admin unlocks the user account
        Client->>Client: 2b.1 Prepare isActive = true request
    end

    Client->>UserApi: 3. Send update user status request
    activate UserApi
    UserApi->>UserController: 4. PATCH /slib/users/{userId}/status
    deactivate UserApi
    deactivate Client
    activate UserController
    UserController->>UserService: 5. toggleUserActive(userId, isActive)
    deactivate UserController
    activate UserService
    UserService->>UserRepo: 6. Find target user by userId
    activate UserRepo
    UserRepo->>DB: 6.1 Query users table
    activate DB
    DB-->>UserRepo: 6.2 Return target user
    deactivate DB
    UserRepo-->>UserService: 6.3 Return current user state
    deactivate UserRepo

    alt 7a. Locking or role change would remove the last active admin
        UserService->>UserService: 7a.1 Reject protected admin status change
        UserService-->>UserController: 7a.2 Return business rule error
        deactivate UserService
        activate UserController
        UserController-->>UserApi: 7a.3 Return 400 Bad Request
        deactivate UserController
        activate UserApi
        UserApi-->>Client: 7a.4 Show status change failed message
        deactivate UserApi
        activate Client
        Client-->>Users: 7a.5 Display protected admin warning
    else 7b. Status change is allowed
        UserService->>UserRepo: 7b.1 Save updated isActive state
        activate UserRepo
        UserRepo->>DB: 7b.2 Update users table
        activate DB
        DB-->>UserRepo: 7b.3 Persist success
        deactivate DB
        UserRepo-->>UserService: 7b.4 Return updated user
        deactivate UserRepo

        alt 8a. User account is being locked
            UserService->>RefreshRepo: 8a.1 Revoke all refresh tokens of the user
            activate RefreshRepo
            RefreshRepo->>DB: 8a.2 Update refresh_tokens set revoked = true
            activate DB
            DB-->>RefreshRepo: 8a.3 Tokens revoked
            deactivate DB
            RefreshRepo-->>UserService: 8a.4 Token revocation completed
            deactivate RefreshRepo
        else 8b. User account is being unlocked
            UserService->>UserService: 8b.1 Skip token revocation
        end

        UserService-->>UserController: 9. Return updated user status result
        deactivate UserService
        activate UserController
        UserController-->>UserApi: 10. Return 200 OK with new status
        deactivate UserController
        activate UserApi
        UserApi-->>Client: 11. Return success response
        deactivate UserApi
        activate Client
        Client->>Client: 12. Refresh user list and row badges
        Client-->>Users: 13. Show user status updated successfully
    end

    deactivate Client
    deactivate Users
```


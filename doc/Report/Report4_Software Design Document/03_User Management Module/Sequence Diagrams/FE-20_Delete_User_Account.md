# FE-20 Delete User Account

```mermaid
sequenceDiagram
    participant Users as "👤 Admin"
    participant Client as Admin Web Portal
    participant DeleteModal as DeleteUserModal
    participant UserApi as UserService.jsx
    participant UserController as UserController
    participant UserService as UserService
    participant UserRepo as UserRepository
    participant RefreshRepo as RefreshTokenRepository
    participant DB as Database

    Users->>Client: 1. Choose delete action on a user row
    activate Users
    activate Client
    Client->>DeleteModal: 2. Open delete confirmation modal
    activate DeleteModal
    DeleteModal->>UserApi: 3. Check whether the target user has active bookings
    activate UserApi
    UserApi->>UserController: 4. GET /slib/users/{userId}/active-bookings
    deactivate UserApi
    deactivate Client
    activate UserController
    UserController->>UserService: 5. countActiveOrUpcomingBookings(userId)
    deactivate UserController
    activate UserService
    UserService->>UserRepo: 5.1 Verify target user exists
    activate UserRepo
    UserRepo->>DB: 5.2 Query users table
    activate DB
    DB-->>UserRepo: 5.3 Return target user
    deactivate DB
    UserRepo-->>UserService: 5.4 User exists
    deactivate UserRepo
    UserService-->>UserController: 6. Return active booking count
    deactivate UserService
    activate UserController
    UserController-->>UserApi: 7. Return booking check result
    deactivate UserController
    activate UserApi
    UserApi-->>DeleteModal: 8. Return hasActiveBookings flag and count
    deactivate UserApi
    activate Client
    DeleteModal-->>Client: 9. Show delete warnings and type-to-confirm field
    deactivate DeleteModal

    alt 10a. Admin cancels deletion or confirmation text is invalid
        Client-->>Users: 10a.1 Keep user account unchanged
    else 10b. Admin confirms permanent deletion
        Client->>UserApi: 10b.1 Send delete user request
        activate UserApi
        UserApi->>UserController: 10b.2 DELETE /slib/users/{userId}
        deactivate UserApi
        deactivate Client
        activate UserController
        UserController->>UserService: 10b.3 deleteUserById(userId)
        deactivate UserController
        activate UserService
        UserService->>UserRepo: 10b.4 Find target user and validate deletion rules
        activate UserRepo
        UserRepo->>DB: 10b.5 Query users table
        activate DB
        DB-->>UserRepo: 10b.6 Return target user
        deactivate DB
        UserRepo-->>UserService: 10b.7 Return target user state
        deactivate UserRepo

        alt 11a. Deletion would remove the last active admin
            UserService->>UserService: 11a.1 Reject protected admin deletion
            UserService-->>UserController: 11a.2 Return business rule error
            deactivate UserService
            activate UserController
            UserController-->>UserApi: 11a.3 Return 400 Bad Request
            deactivate UserController
            activate UserApi
            UserApi-->>Client: 11a.4 Show delete failed message
            deactivate UserApi
            activate Client
            Client-->>Users: 11a.5 Display protected admin warning
        else 11b. Deletion is allowed
            UserService->>RefreshRepo: 11b.1 Delete or revoke authentication tokens
            activate RefreshRepo
            RefreshRepo->>DB: 11b.2 Delete refresh token records
            activate DB
            DB-->>RefreshRepo: 11b.3 Token cleanup completed
            deactivate DB
            RefreshRepo-->>UserService: 11b.4 Return cleanup result
            deactivate RefreshRepo
            UserService->>DB: 12. Delete related records in activity, reservations, profiles, settings, chats, and reports
            activate DB
            DB-->>UserService: 13. Related data cleanup completed
            deactivate DB
            UserService->>UserRepo: 14. Delete target user
            activate UserRepo
            UserRepo->>DB: 14.1 Remove user from users table
            activate DB
            DB-->>UserRepo: 14.2 Persist deletion
            deactivate DB
            UserRepo-->>UserService: 14.3 User deleted
            deactivate UserRepo
            UserService-->>UserController: 15. Return permanent deletion success
            deactivate UserService
            activate UserController
            UserController-->>UserApi: 16. Return 200 OK
            deactivate UserController
            activate UserApi
            UserApi-->>Client: 17. Return success response
            deactivate UserApi
            activate Client
            Client->>Client: 18. Close modal and refresh user list
            Client-->>Users: 19. Show user account deleted successfully
        end
    end

    deactivate Client
    deactivate Users
```

# FE-17 Add Librarian to the System

```mermaid
sequenceDiagram
    participant Users as "👤 Admin"
    participant Client as Admin Web Portal
    participant UserApi as UserService.jsx
    participant UserController as UserController
    participant UserService as UserService
    participant AuthService as AuthService
    participant UserRepo as UserRepository
    participant EmailService as EmailService
    participant DB as Database

    Users->>Client: 1. Open add librarian form
    activate Users
    activate Client
    Users->>Client: 2. Enter librarian information and submit
    Client->>Client: 2.1 Validate full name, email, userCode, and phone
    Client->>UserApi: 3. Send create librarian request
    activate UserApi
    UserApi->>UserController: 4. POST /slib/users
    deactivate UserApi
    deactivate Client
    activate UserController
    UserController->>UserController: 4.1 Validate AdminCreateUserRequest payload
    UserController->>UserService: 5. createUser(request)
    deactivate UserController
    activate UserService
    UserService->>UserService: 5.1 Normalize and validate managed user data
    UserService->>UserRepo: 6. Check uniqueness of email, userCode, and phone
    activate UserRepo
    UserRepo->>DB: 6.1 Query existing users
    activate DB
    DB-->>UserRepo: 6.2 Return duplicate check result
    deactivate DB

    alt 7a. User identity fields already exist
        UserRepo-->>UserService: 7a.1 Return duplicate conflict
        deactivate UserRepo
        UserService-->>UserController: 7a.2 Return validation error
        deactivate UserService
        activate UserController
        UserController-->>UserApi: 7a.3 Return 400 Bad Request
        deactivate UserController
        activate UserApi
        UserApi-->>Client: 7a.4 Show duplicate field error
        deactivate UserApi
        activate Client
        Client-->>Users: 7a.5 Display add librarian failed message
    else 7b. Librarian data is valid
        UserRepo-->>UserService: 7b.1 Return no duplicate found
        UserService->>AuthService: 7b.2 Encode default password
        activate AuthService
        AuthService-->>UserService: 7b.3 Return encoded default password
        deactivate AuthService
        UserService->>UserService: 8. Build User and default UserSetting
        UserService->>UserRepo: 9. Save librarian account
        UserRepo->>DB: 9.1 Insert users and user_settings
        activate DB
        DB-->>UserRepo: 9.2 Persist success
        deactivate DB
        UserRepo-->>UserService: 9.3 Return saved librarian
        deactivate UserRepo
        UserService->>EmailService: 10. Send welcome email with default password
        activate EmailService
        EmailService-->>UserService: 10.1 Welcome email queued or sent
        deactivate EmailService
        UserService-->>UserController: 11. Return created librarian response
        deactivate UserService
        activate UserController
        UserController-->>UserApi: 12. Return 200 OK with created user
        deactivate UserController
        activate UserApi
        UserApi-->>Client: 13. Return created librarian data
        deactivate UserApi
        activate Client
        Client->>Client: 14. Close modal and refresh user list
        Client-->>Users: 15. Show add librarian success message
    end

    deactivate Client
    deactivate Users
```

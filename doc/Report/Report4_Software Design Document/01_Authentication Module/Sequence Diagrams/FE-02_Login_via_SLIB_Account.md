# FE-02 Login via SLIB Account

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Web/Mobile Client
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant JwtService as JwtService
    participant UserRepo as UserRepository
    participant RefreshRepo as RefreshTokenRepository
    participant DB as Database

    Users->>Client: 1. Enter identifier and password
    activate Users
    activate Client
    Users->>Client: 2. Press "Login"
    Client->>Client: 2.1 Validate required fields
    Client->>AuthController: 3. POST /slib/auth/login
    deactivate Client
    activate AuthController
    AuthController->>AuthController: 3.1 Validate request format
    AuthController->>AuthService: 4. loginWithPassword(identifier, password, deviceInfo)
    deactivate AuthController
    activate AuthService
    AuthService->>UserRepo: 5. Find user by email or username
    activate UserRepo
    UserRepo->>DB: 6. Query user record
    activate DB
    DB-->>UserRepo: 7. Return user result
    deactivate DB

    alt 8a. User not found
        UserRepo-->>AuthService: 8a.1 Return empty result
        deactivate UserRepo
        AuthService->>AuthService: 8a.2 Mark login as invalid
        AuthService-->>AuthController: 8a.3 Throw invalid credentials error
        activate AuthController
        AuthController-->>Client: 8a.4 Return 4xx error
        deactivate AuthController
        activate Client
        Client-->>Users: 8a.5 Show login failed message
        deactivate Client
        deactivate Users
    else 8b. User found
        UserRepo-->>AuthService: 8b.1 Return matched user
        deactivate UserRepo
        AuthService->>AuthService: 8b.2 Validate active status and password hash

        alt 9a. Account inactive or password invalid
            AuthService-->>AuthController: 9a.1 Throw authentication error
            activate AuthController
            AuthController-->>Client: 9a.2 Return 4xx error
            deactivate AuthController
            activate Client
            Client-->>Users: 9a.3 Show error message
            deactivate Client
            deactivate Users
        else 9b. Credentials valid
            AuthService->>JwtService: 9b.1 Generate access token
            activate JwtService
            JwtService-->>AuthService: 9b.2 Return access token
            deactivate JwtService
            AuthService->>JwtService: 9b.3 Generate refresh token
            activate JwtService
            JwtService-->>AuthService: 9b.4 Return refresh token
            deactivate JwtService
            AuthService->>RefreshRepo: 9b.5 Revoke old refresh tokens by userId
            activate RefreshRepo
            RefreshRepo->>DB: 9b.6 Update revoked tokens
            activate DB
            DB-->>RefreshRepo: 9b.7 Revoke success
            deactivate DB
            AuthService->>RefreshRepo: 9b.8 Save new refresh token hash
            RefreshRepo->>DB: 9b.9 Insert refresh token
            activate DB
            DB-->>RefreshRepo: 9b.10 Persist success
            deactivate DB
            RefreshRepo-->>AuthService: 9b.11 Token persistence completed
            AuthService->>AuthService: 9b.12 Build AuthResponse
            deactivate RefreshRepo
            AuthService-->>AuthController: 10. Return AuthResponse
            activate AuthController
            AuthController-->>Client: 11. Return 200 OK with tokens and user info
            deactivate AuthController
            activate Client

            alt 12a. First login or passwordChanged = false
                Client->>Client: 12a.1 Save session data
                Client-->>Users: 12a.2 Redirect to Change Password screen
                deactivate Users
            else 12b. Normal login
                Client->>Client: 12b.1 Save session data
                Client-->>Users: 12b.2 Redirect to dashboard or main screen
                deactivate Users
            end
            deactivate Client
        end
    end

    deactivate AuthService
```

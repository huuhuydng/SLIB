# FE-01 Login via Google Account

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Web/Mobile Client
    participant Google as Google Identity
    participant AuthController as AuthController
    participant AuthService as AuthService
    participant JwtService as JwtService
    participant UserRepo as UserRepository
    participant RefreshRepo as RefreshTokenRepository
    participant DB as Database

    Users->>Client: 1. Choose "Login with Google"
    activate Users
    activate Client
    Client->>Client: 1.1 Open Google sign-in flow
    Client->>Google: 2. Request Google sign-in
    deactivate Client
    activate Google
    Google-->>Client: 3. Return Google ID token and basic profile
    deactivate Google
    activate Client
    Client->>Client: 3.1 Validate token and prepare login payload
    Client->>AuthController: 4. POST /slib/auth/google with idToken
    deactivate Client
    activate AuthController
    AuthController->>AuthController: 4.1 Validate request format
    AuthController->>AuthService: 5. loginWithGoogle(idToken, fullName, fcmToken, deviceInfo)
    deactivate AuthController
    activate AuthService
    AuthService->>Google: 6. Verify Google ID token
    activate Google
    Google-->>AuthService: 7. Return verified payload
    deactivate Google

    alt 8a. Email is not allowed
        AuthService->>AuthService: 8a.1 Reject non-permitted email domain
        AuthService-->>AuthController: 8a.2 Throw access denied error
        activate AuthController
        AuthController-->>Client: 8a.3 Return 403 Forbidden
        deactivate AuthController
        activate Client
        Client-->>Users: 8a.4 Show login failed message
        deactivate Client
        deactivate Users
    else 8b. Email is allowed
        AuthService->>UserRepo: 8b.1 Find user by email
        activate UserRepo
        UserRepo->>DB: 8b.2 Query user record
        activate DB
        DB-->>UserRepo: 8b.3 Return user
        deactivate DB

        alt 9a. User not found or inactive
            UserRepo-->>AuthService: 9a.1 Return missing/inactive user
            AuthService->>AuthService: 9a.2 Mark login as denied
            AuthService-->>AuthController: 9a.3 Return access denied
            deactivate UserRepo
            activate AuthController
            AuthController-->>Client: 9a.4 Return error response
            deactivate AuthController
            activate Client
            Client-->>Users: 9a.5 Show access denied message
            deactivate Client
            deactivate Users
        else 9b. User is valid
            UserRepo-->>AuthService: 9b.1 Return active user
            AuthService->>JwtService: 9b.2 Generate access token
            activate JwtService
            JwtService-->>AuthService: 9b.3 Return access token
            deactivate JwtService
            AuthService->>JwtService: 9b.4 Generate refresh token
            activate JwtService
            JwtService-->>AuthService: 9b.5 Return refresh token
            deactivate JwtService
            AuthService->>RefreshRepo: 9b.6 Revoke old refresh tokens by userId
            activate RefreshRepo
            RefreshRepo->>DB: 9b.7 Update refresh_tokens set revoked = true
            activate DB
            DB-->>RefreshRepo: 9b.8 Old tokens revoked
            deactivate DB
            AuthService->>RefreshRepo: 9b.9 Save new refresh token hash
            RefreshRepo->>DB: 9b.10 Insert refresh token record
            activate DB
            DB-->>RefreshRepo: 9b.11 Persist success
            deactivate DB
            RefreshRepo-->>AuthService: 9b.12 Token persistence completed
            AuthService->>AuthService: 9b.13 Build AuthResponse
            deactivate RefreshRepo
            deactivate UserRepo
            AuthService-->>AuthController: 10. Return AuthResponse
            activate AuthController
            AuthController-->>Client: 11. Return 200 OK with tokens and user info
            deactivate AuthController
            activate Client
            Client->>Client: 12. Store access token, refresh token, and user profile
            Client-->>Users: 13. Redirect to dashboard or main screen
            deactivate Client
            deactivate Users
        end
    end

    deactivate AuthService
```

# FE-04 Log out

```mermaid
sequenceDiagram
    participant Users as "👤 Admin, Librarian, Student, Teacher"
    participant Client as Frontend (Web/Mobile)
    participant AuthController as Backend API
    participant UserAPI as User Profile API
    participant AuthService as AuthService
    participant RefreshRepo as Token Service
    participant DB as Database

    Users->>Client: 1. Click "Log out"
    activate Users
    activate Client
    Client->>Client: 1.1 Show confirmation modal
    Users->>Client: 1.2 Confirm log out action

    alt 2a. Web logout with refresh token revoke
        Client->>Client: 2a.1 Retrieve current tokens
        Client->>Client: 2a.2 Prepare logout request
        Client->>AuthController: 2a.3 POST /slib/auth/logout with refreshToken
        deactivate Client
        activate AuthController
        AuthController->>AuthService: 2a.4 logout(refreshToken)
        deactivate AuthController
        activate AuthService
        AuthService->>AuthService: 2a.5 Validate logout request
        AuthService->>RefreshRepo: 2a.6 Revoke refresh token by token hash
        activate RefreshRepo
        RefreshRepo->>DB: 2a.7 Update refresh_tokens set revoked = true
        activate DB
        DB-->>RefreshRepo: 2a.8 Token revoked
        deactivate DB
        RefreshRepo-->>AuthService: 2a.9 Token revocation completed
        AuthService->>AuthService: 2a.10 Build logout result
        deactivate RefreshRepo
        AuthService-->>AuthController: 2a.11 Logout successful
        deactivate AuthService
        activate AuthController
        AuthController-->>Client: 2a.12 Return 200 OK
        deactivate AuthController
        activate Client
        Client->>Client: 3. Clear all tokens, session data, and cache
        Client->>Client: 4. Clear navigation history
        Client-->>Users: 5. Redirect to Login screen
        deactivate Client
        deactivate Users
    else 2b. Mobile logout with local token cleanup
        Client->>UserAPI: 2b.1 PATCH /me to clear FCM token
        deactivate Client
        activate UserAPI
        UserAPI->>DB: 2b.2 Update user.notiDevice = null
        activate DB
        DB-->>UserAPI: 2b.3 Update success
        deactivate DB
        UserAPI-->>Client: 2b.4 Profile update acknowledged
        deactivate UserAPI
        activate Client
        Client->>Client: 2b.5 Clear JWT, refresh token, local storage, and HCE context
        Client->>Client: 2b.6 Clear navigation history
        Client-->>Users: 3. Redirect to onboarding or login screen
        deactivate Client
        deactivate Users
    end
```

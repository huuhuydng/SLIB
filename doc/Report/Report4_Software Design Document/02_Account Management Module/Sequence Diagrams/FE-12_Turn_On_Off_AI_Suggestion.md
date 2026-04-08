# FE-12 Turn On/Turn Off AI Suggestion

```mermaid
sequenceDiagram
    participant Users as "👤 Student, Teacher"
    participant Client as Mobile App
    participant LocalStore as LocalStorageService
    participant UserSettingController as UserSettingController
    participant UserSettingService as UserSettingService
    participant UserRepo as UserRepository
    participant UserSettingRepo as UserSettingRepository
    participant DB as Database

    Users->>Client: 1. Toggle the AI suggestion setting
    activate Users
    activate Client

    alt 2a. User turns AI suggestion on
        Client->>Client: 2a.1 Set isAiRecommendEnabled = true in UI state
    else 2b. User turns AI suggestion off
        Client->>Client: 2b.1 Set isAiRecommendEnabled = false in UI state
    end

    Client->>LocalStore: 3. Save updated setting locally
    activate LocalStore
    LocalStore-->>Client: 4. Local cache updated
    deactivate LocalStore
    Client->>UserSettingController: 5. PUT /slib/settings/{userId} with isAiRecommendEnabled
    deactivate Client
    activate UserSettingController
    UserSettingController->>UserRepo: 6. Resolve authorized userId
    activate UserRepo
    UserRepo->>DB: 6.1 Query current user by email
    activate DB
    DB-->>UserRepo: 6.2 Return authenticated user
    deactivate DB
    UserRepo-->>UserSettingController: 6.3 Confirm requested userId
    deactivate UserRepo
    UserSettingController->>UserSettingService: 7. updateSettings(userId, dto)
    deactivate UserSettingController
    activate UserSettingService
    UserSettingService->>UserSettingRepo: 8. Load current settings
    activate UserSettingRepo
    UserSettingRepo->>DB: 8.1 Query user_settings
    activate DB
    DB-->>UserSettingRepo: 8.2 Return existing settings or empty result
    deactivate DB

    alt 9a. Settings record does not exist yet
        UserSettingRepo-->>UserSettingService: 9a.1 Return empty result
        UserSettingService->>UserSettingService: 9a.2 Build default settings first
    else 9b. Settings record already exists
        UserSettingRepo-->>UserSettingService: 9b.1 Return existing settings
    end

    UserSettingService->>UserSettingService: 10. Apply new AI suggestion preference
    UserSettingService->>UserSettingRepo: 11. Save updated settings
    UserSettingRepo->>DB: 11.1 Update user_settings table
    activate DB
    DB-->>UserSettingRepo: 11.2 Persist success
    deactivate DB
    UserSettingRepo-->>UserSettingService: 11.3 Return updated settings
    deactivate UserSettingRepo
    UserSettingService-->>UserSettingController: 12. Return updated settings
    deactivate UserSettingService
    activate UserSettingController

    alt 13a. Server synchronization succeeds
        UserSettingController-->>Client: 13a.1 Return 200 OK
        deactivate UserSettingController
        activate Client
        Client-->>Users: 13a.2 Keep AI suggestion switch in the new state
    else 13b. Server synchronization fails
        UserSettingController-->>Client: 13b.1 Return error response
        deactivate UserSettingController
        activate Client
        Client->>Client: 13b.2 Keep local state and log sync failure
        Client-->>Users: 13b.3 Continue showing the updated switch state locally
    end

    deactivate Client
    deactivate Users
```

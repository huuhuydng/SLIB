# FE-10 View Account Setting

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

    Users->>Client: 1. Open account and settings screen
    activate Users
    activate Client
    Client->>LocalStore: 2. Load cached user settings
    activate LocalStore
    LocalStore-->>Client: 3. Return cached settings or empty state
    deactivate LocalStore

    alt 4a. Cached settings exist
        Client->>Client: 4a.1 Render switches immediately from local cache
    else 4b. Cached settings do not exist
        Client->>Client: 4b.1 Show loading state for settings section
    end

    Client->>UserSettingController: 5. GET /slib/settings/{userId}
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
    UserSettingController->>UserSettingService: 7. getSettings(userId)
    deactivate UserSettingController
    activate UserSettingService
    UserSettingService->>UserSettingRepo: 8. Find user settings by userId
    activate UserSettingRepo
    UserSettingRepo->>DB: 8.1 Query user_settings
    activate DB
    DB-->>UserSettingRepo: 8.2 Return existing settings or empty result
    deactivate DB

    alt 9a. Settings already exist
        UserSettingRepo-->>UserSettingService: 9a.1 Return existing settings
        deactivate UserSettingRepo
    else 9b. Settings do not exist yet
        UserSettingRepo-->>UserSettingService: 9b.1 Return empty result
        UserSettingService->>UserSettingService: 9b.2 Build default settings
        UserSettingService->>UserSettingRepo: 9b.3 Save default settings
        UserSettingRepo->>DB: 9b.4 Insert user_settings record
        activate DB
        DB-->>UserSettingRepo: 9b.5 Persist success
        deactivate DB
        UserSettingRepo-->>UserSettingService: 9b.6 Return saved default settings
        deactivate UserSettingRepo
    end

    UserSettingService-->>UserSettingController: 10. Return UserSetting entity
    deactivate UserSettingService
    activate UserSettingController
    UserSettingController-->>Client: 11. Return 200 OK with user settings
    deactivate UserSettingController
    activate Client
    Client->>LocalStore: 12. Update cached settings
    activate LocalStore
    LocalStore-->>Client: 13. Cache updated
    deactivate LocalStore
    Client-->>Users: 14. Show account setting screen with current toggles
    deactivate Client
    deactivate Users
```

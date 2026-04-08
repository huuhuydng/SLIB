# FE-102 Mark Notification as Read

```mermaid
sequenceDiagram
    participant Users as "Librarian, Student, Teacher"
    participant NotificationScreen as Notification Screen
    participant MobileNotiService as NotificationService (Mobile)
    participant AuthService as AuthService (Mobile)
    participant NotificationController as NotificationController
    participant PushService as PushNotificationService
    participant NotificationRepo as NotificationRepository
    participant DB as Database

    activate Users
    Users->>NotificationScreen: 1. Tap an unread notification
    activate NotificationScreen
    NotificationScreen->>MobileNotiService: 2. openNotificationTarget(notification)
    activate MobileNotiService
    MobileNotiService->>AuthService: 3. Get token and current userId
    activate AuthService
    AuthService-->>MobileNotiService: 4. Return token and user context
    deactivate AuthService
    MobileNotiService->>NotificationController: 5. PUT /slib/notifications/mark-read/{notificationId}?userId={userId}
    activate NotificationController
    NotificationController->>PushService: 6. markAsRead(notificationId, userId)
    activate PushService
    PushService->>NotificationRepo: 7. Load notification by id
    activate NotificationRepo
    NotificationRepo->>DB: 7.1 Query notification row
    activate DB
    DB-->>NotificationRepo: 7.2 Return notification
    deactivate DB

    alt 8a. Notification belongs to another user
        NotificationRepo-->>PushService: 8a.1 Return notification with invalid ownership
        deactivate NotificationRepo
        PushService-->>NotificationController: 8a.2 Throw access error
        deactivate PushService
        NotificationController-->>MobileNotiService: 8a.3 Return error response
        deactivate NotificationController
        MobileNotiService-->>NotificationScreen: 8a.4 Keep current unread state
        deactivate MobileNotiService
        NotificationScreen-->>Users: 8a.5 Show read action failure
    else 8b. Notification belongs to the current user
        NotificationRepo->>DB: 8b.1 Update isRead = true
        activate DB
        DB-->>NotificationRepo: 8b.2 Persist success
        deactivate DB
        NotificationRepo-->>PushService: 8b.3 Return updated notification
        deactivate NotificationRepo
        PushService-->>NotificationController: 9. Return success
        deactivate PushService
        NotificationController-->>MobileNotiService: 10. Return 200 OK
        deactivate NotificationController
        MobileNotiService->>MobileNotiService: 11. Update local item state and decrease unread counter
        MobileNotiService-->>NotificationScreen: 12. Return updated unread state
        deactivate MobileNotiService
        NotificationScreen-->>Users: 13. Show notification as read
    end

    deactivate NotificationScreen
    deactivate Users
```

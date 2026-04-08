# FE-86 Create Feedback After Check-Out

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant HomeScreen as Mobile Home Screen
    participant AuthService as AuthService (Mobile)
    participant FeedbackDialog as Feedback Dialog
    participant FeedbackController as FeedbackController
    participant FeedbackService as FeedbackService
    participant UserRepo as UserRepository
    participant ReservationRepo as ReservationRepository
    participant FeedbackRepo as FeedbackRepository
    participant DB as Database

    activate Users
    Users->>HomeScreen: 1. Reopen app after a completed reservation
    activate HomeScreen
    HomeScreen->>AuthService: 2. Get valid access token
    activate AuthService
    AuthService-->>HomeScreen: 3. Return token
    deactivate AuthService
    HomeScreen->>FeedbackController: 4. GET /slib/feedbacks/check-pending
    activate FeedbackController
    FeedbackController->>UserRepo: 5. Resolve current user from session
    activate UserRepo
    UserRepo->>DB: 5.1 Query user by email
    activate DB
    DB-->>UserRepo: 5.2 Return user
    deactivate DB
    UserRepo-->>FeedbackController: 5.3 Return userId
    deactivate UserRepo
    FeedbackController->>FeedbackService: 6. checkPendingFeedback(userId)
    activate FeedbackService
    FeedbackService->>ReservationRepo: 7. Find completed reservations without feedback
    activate ReservationRepo
    ReservationRepo->>DB: 7.1 Query eligible completed reservations
    activate DB
    DB-->>ReservationRepo: 7.2 Return reservation list
    deactivate DB
    ReservationRepo-->>FeedbackService: 7.3 Return reservations
    deactivate ReservationRepo
    FeedbackService->>FeedbackRepo: 8. Check existing feedback by reservationId
    activate FeedbackRepo
    FeedbackRepo->>DB: 8.1 Query feedback existence
    activate DB
    DB-->>FeedbackRepo: 8.2 Return existence result
    deactivate DB
    FeedbackRepo-->>FeedbackService: 8.3 Return pending decision
    deactivate FeedbackRepo

    alt 9a. No pending feedback
        FeedbackService-->>FeedbackController: 9a.1 Return hasPending = false
        deactivate FeedbackService
        FeedbackController-->>HomeScreen: 9a.2 Return 200 OK without dialog data
        deactivate FeedbackController
        HomeScreen-->>Users: 9a.3 Keep normal home screen flow
    else 9b. Pending feedback exists
        FeedbackService-->>FeedbackController: 9b.1 Return reservation summary
        deactivate FeedbackService
        FeedbackController-->>HomeScreen: 9b.2 Return 200 OK with reservationId, zoneName, seatCode
        deactivate FeedbackController
        HomeScreen->>FeedbackDialog: 10. Open post-check-out feedback dialog
        activate FeedbackDialog
        Users->>FeedbackDialog: 11. Select rating, category, and optional comment
        FeedbackDialog->>AuthService: 12. Get valid access token
        activate AuthService
        AuthService-->>FeedbackDialog: 13. Return token
        deactivate AuthService
        FeedbackDialog->>FeedbackController: 14. POST /slib/feedbacks
        activate FeedbackController
        FeedbackController->>UserRepo: 15. Resolve current user from session
        activate UserRepo
        UserRepo->>DB: 15.1 Query user by email
        activate DB
        DB-->>UserRepo: 15.2 Return user
        deactivate DB
        UserRepo-->>FeedbackController: 15.3 Return studentId
        deactivate UserRepo
        FeedbackController->>FeedbackService: 16. create(studentId, rating, content, category, null, reservationId)
        activate FeedbackService
        FeedbackService->>UserRepo: 17. Load student profile
        activate UserRepo
        UserRepo->>DB: 17.1 Query user by id
        activate DB
        DB-->>UserRepo: 17.2 Return user
        deactivate DB
        UserRepo-->>FeedbackService: 17.3 Return student
        deactivate UserRepo
        FeedbackService->>ReservationRepo: 18. Validate reservation ownership and completion
        activate ReservationRepo
        ReservationRepo->>DB: 18.1 Query reservation by id
        activate DB
        DB-->>ReservationRepo: 18.2 Return reservation
        deactivate DB
        ReservationRepo-->>FeedbackService: 18.3 Return reservation context
        deactivate ReservationRepo
        FeedbackService->>FeedbackRepo: 19. Save new feedback with NEW status
        activate FeedbackRepo
        FeedbackRepo->>DB: 19.1 Insert feedback
        activate DB
        DB-->>FeedbackRepo: 19.2 Persist success
        deactivate DB
        FeedbackRepo-->>FeedbackService: 19.3 Return saved feedback
        deactivate FeedbackRepo
        FeedbackService-->>FeedbackController: 20. Return FeedbackDTO
        deactivate FeedbackService
        FeedbackController-->>FeedbackDialog: 21. Return 201 Created
        deactivate FeedbackController
        FeedbackDialog-->>Users: 22. Show thank-you state
        deactivate FeedbackDialog
    end

    deactivate HomeScreen
    deactivate Users
```

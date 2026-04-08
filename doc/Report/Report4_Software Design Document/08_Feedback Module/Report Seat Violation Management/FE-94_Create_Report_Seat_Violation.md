# FE-94 Create Report Seat Violation

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant ViolationScreen as Violation Report Screen
    participant AuthService as AuthService (Mobile)
    participant BookingService as BookingService (Mobile)
    participant ViolationController as SeatViolationReportController
    participant ViolationService as SeatViolationReportService
    participant UserRepo as UserRepository
    participant SeatRepo as SeatRepository
    participant ReservationRepo as ReservationRepository
    participant Cloudinary as CloudinaryService
    participant ReportRepo as SeatViolationReportRepository
    participant DB as Database

    activate Users
    Users->>ViolationScreen: 1. Open violation reporting screen
    activate ViolationScreen
    ViolationScreen->>AuthService: 2. Get current user session
    activate AuthService
    AuthService-->>ViolationScreen: 3. Return authenticated user
    deactivate AuthService
    ViolationScreen->>BookingService: 4. Get current confirmed booking
    activate BookingService
    BookingService-->>ViolationScreen: 5. Return confirmed seat and area context
    deactivate BookingService
    Users->>ViolationScreen: 6. Tap another occupied seat on the floor plan
    ViolationScreen->>ViolationScreen: 7. Open violation report bottom sheet
    Users->>ViolationScreen: 8. Select violation type, enter description, and optionally attach evidence
    ViolationScreen->>AuthService: 9. Get valid access token
    activate AuthService
    AuthService-->>ViolationScreen: 10. Return token
    deactivate AuthService
    ViolationScreen->>ViolationController: 11. POST /slib/violation-reports (multipart)
    activate ViolationController
    ViolationController->>UserRepo: 12. Resolve current user from session
    activate UserRepo
    UserRepo->>DB: 12.1 Query user by email
    activate DB
    DB-->>UserRepo: 12.2 Return reporter
    deactivate DB
    UserRepo-->>ViolationController: 12.3 Return reporterId
    deactivate UserRepo
    ViolationController->>ViolationService: 13. createReport(reporterId, request, images)
    activate ViolationService
    ViolationService->>UserRepo: 14. Load reporter
    activate UserRepo
    UserRepo->>DB: 14.1 Query user by id
    activate DB
    DB-->>UserRepo: 14.2 Return reporter
    deactivate DB
    UserRepo-->>ViolationService: 14.3 Return reporter
    deactivate UserRepo
    ViolationService->>ReservationRepo: 15. Validate reporter has an active confirmed reservation
    activate ReservationRepo
    ReservationRepo->>DB: 15.1 Query reporter reservations
    activate DB
    DB-->>ReservationRepo: 15.2 Return reservation list
    deactivate DB
    ReservationRepo-->>ViolationService: 15.3 Return active reservation
    deactivate ReservationRepo
    ViolationService->>SeatRepo: 16. Load target seat
    activate SeatRepo
    SeatRepo->>DB: 16.1 Query seat by id
    activate DB
    DB-->>SeatRepo: 16.2 Return seat
    deactivate DB
    SeatRepo-->>ViolationService: 16.3 Return seat
    deactivate SeatRepo
    ViolationService->>ReservationRepo: 17. Find active confirmed violator reservation on the target seat
    activate ReservationRepo
    ReservationRepo->>DB: 17.1 Query overlapping reservations on seat
    activate DB
    DB-->>ReservationRepo: 17.2 Return active reservation rows
    deactivate DB
    ReservationRepo-->>ViolationService: 17.3 Return violator reservation
    deactivate ReservationRepo

    alt 18a. Selected seat is the reporter's own seat
        ViolationService-->>ViolationController: 18a.1 Throw invalid report type error
        deactivate ViolationService
        ViolationController-->>ViolationScreen: 18a.2 Return error response
        deactivate ViolationController
        ViolationScreen-->>Users: 18a.3 Show invalid seat warning
    else 18b. Target seat is a valid occupied seat
        alt 19a. Evidence image is attached
            ViolationService->>Cloudinary: 19a.1 Upload first evidence image
            activate Cloudinary
            Cloudinary-->>ViolationService: 19a.2 Return evidenceUrl
            deactivate Cloudinary
        else 19b. No evidence image is attached
            ViolationService->>ViolationService: 19b.1 Continue with evidenceUrl = null
        end

        ViolationService->>ReportRepo: 20. Save violation report with PENDING status
        activate ReportRepo
        ReportRepo->>DB: 20.1 Insert violation report
        activate DB
        DB-->>ReportRepo: 20.2 Persist success
        deactivate DB
        ReportRepo-->>ViolationService: 20.3 Return saved report
        deactivate ReportRepo
        ViolationService-->>ViolationController: 21. Return ViolationReportResponse
        deactivate ViolationService
        ViolationController-->>ViolationScreen: 22. Return 201 Created
        deactivate ViolationController
        ViolationScreen-->>Users: 23. Show report submission success
    end

    deactivate ViolationScreen
    deactivate Users
```

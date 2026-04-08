# FE-81 Create Complaint

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant MobileApp as Mobile App
    participant AuthService as AuthService (Mobile)
    participant ComplaintController as ComplaintController
    participant ComplaintService as ComplaintService
    participant UserRepo as UserRepository
    participant PointRepo as PointTransactionRepository
    participant ViolationRepo as SeatViolationReportRepository
    participant ComplaintRepo as ComplaintRepository
    participant DB as Database

    Users->>MobileApp: 1. Choose appeal and enter a reason
    activate Users
    activate MobileApp
    MobileApp->>MobileApp: 1.1 Prepare complaint payload
    MobileApp->>AuthService: 1.2 Get current access token
    activate AuthService
    AuthService-->>MobileApp: 1.3 Return token
    deactivate AuthService
    MobileApp->>ComplaintController: 2. POST /slib/complaints
    deactivate MobileApp
    activate ComplaintController
    ComplaintController->>UserRepo: 3. Resolve userId from session
    activate UserRepo
    UserRepo->>DB: 3.1 Query user by email
    activate DB
    DB-->>UserRepo: 3.2 Return user
    deactivate DB
    UserRepo-->>ComplaintController: 3.3 Return userId
    deactivate UserRepo
    ComplaintController->>ComplaintService: 4. create(studentId, subject, content, evidenceUrl, pointTransactionId, violationReportId)
    deactivate ComplaintController
    activate ComplaintService
    ComplaintService->>UserRepo: 5. Load student user info
    activate UserRepo
    UserRepo->>DB: 5.1 Query user by id
    activate DB
    DB-->>UserRepo: 5.2 Return user
    deactivate DB
    UserRepo-->>ComplaintService: 5.3 Return student
    deactivate UserRepo

    alt 6a. Appeal a point transaction (pointTransactionId)
        ComplaintService->>PointRepo: 6a.1 Find point transaction
        activate PointRepo
        PointRepo->>DB: 6a.2 Query point_transactions by id
        activate DB
        DB-->>PointRepo: 6a.3 Return transaction
        deactivate DB
        PointRepo-->>ComplaintService: 6a.4 Return transaction
        deactivate PointRepo
        ComplaintService->>ComplaintRepo: 6a.5 Check pending complaints
        activate ComplaintRepo
        ComplaintRepo->>DB: 6a.6 Query complaints by pointTransactionId
        activate DB
        DB-->>ComplaintRepo: 6a.7 Return result
        deactivate DB
        ComplaintRepo-->>ComplaintService: 6a.8 Return status
        deactivate ComplaintRepo
    else 6b. Appeal a violation report (violationReportId)
        ComplaintService->>ViolationRepo: 6b.1 Find violation report
        activate ViolationRepo
        ViolationRepo->>DB: 6b.2 Query seat_violation_reports by id
        activate DB
        DB-->>ViolationRepo: 6b.3 Return report
        deactivate DB
        ViolationRepo-->>ComplaintService: 6b.4 Return report
        deactivate ViolationRepo
        ComplaintService->>ComplaintRepo: 6b.5 Check pending complaints
        activate ComplaintRepo
        ComplaintRepo->>DB: 6b.6 Query complaints by violationReportId
        activate DB
        DB-->>ComplaintRepo: 6b.7 Return result
        deactivate DB
        ComplaintRepo-->>ComplaintService: 6b.8 Return status
        deactivate ComplaintRepo
    end

    alt 7a. Complaint data is invalid
        ComplaintService-->>ComplaintController: 7a.1 Throw validation error
        deactivate ComplaintService
        activate ComplaintController
        ComplaintController-->>MobileApp: 7a.2 Return 400 Bad Request
        deactivate ComplaintController
        activate MobileApp
        MobileApp-->>Users: 7a.3 Show error message
        deactivate MobileApp
    else 7b. Complaint data is valid
        ComplaintService->>ComplaintRepo: 7b.1 Save complaint with PENDING status
        activate ComplaintRepo
        ComplaintRepo->>DB: 7b.2 Insert complaints
        activate DB
        DB-->>ComplaintRepo: 7b.3 Persist success
        deactivate DB
        ComplaintRepo-->>ComplaintService: 7b.4 Return new complaint
        deactivate ComplaintRepo
        ComplaintService-->>ComplaintController: 8. Return ComplaintDTO
        deactivate ComplaintService
        activate ComplaintController
        ComplaintController-->>MobileApp: 9. Return 201 Created
        deactivate ComplaintController
        activate MobileApp
        MobileApp-->>Users: 10. Show complaint submission success
        deactivate MobileApp
    end

    deactivate Users
```

# FE-102 Verify Seat Violation Report

```mermaid
sequenceDiagram
    participant Librarian as Librarian
    participant ViolationManage as ViolationManage Page
    participant ViolationController as SeatViolationReportController
    participant UserRepo as UserRepository
    participant ViolationService as SeatViolationReportService
    participant ReportRepo as SeatViolationReportRepository
    participant ReservationRepo as ReservationRepository
    participant RuleRepo as ReputationRuleRepository
    participant ProfileRepo as StudentProfileRepository
    participant ActivityRepo as ActivityLogRepository
    participant PointRepo as PointTransactionRepository
    participant PushService as PushNotificationService
    participant DB as Database

    activate Librarian
    Librarian->>ViolationManage: 1. Open a pending violation report detail modal
    activate ViolationManage
    Librarian->>ViolationManage: 2. Click Verify
    ViolationManage->>ViolationController: 3. PUT /slib/violation-reports/{id}/verify
    activate ViolationController
    ViolationController->>UserRepo: 4. Resolve current librarian from session
    activate UserRepo
    UserRepo->>DB: 4.1 Query user by email
    activate DB
    DB-->>UserRepo: 4.2 Return librarian
    deactivate DB
    UserRepo-->>ViolationController: 4.3 Return librarianId
    deactivate UserRepo
    ViolationController->>ViolationService: 5. verifyReport(reportId, librarianId)
    activate ViolationService
    ViolationService->>ReportRepo: 6. Load report by id
    activate ReportRepo
    ReportRepo->>DB: 6.1 Query violation report
    activate DB
    DB-->>ReportRepo: 6.2 Return report
    deactivate DB
    ReportRepo-->>ViolationService: 6.3 Return report entity
    deactivate ReportRepo

    alt 7a. Report is already processed
        ViolationService-->>ViolationController: 7a.1 Throw processing error
        deactivate ViolationService
        ViolationController-->>ViolationManage: 7a.2 Return error response
        deactivate ViolationController
        ViolationManage-->>Librarian: 7a.3 Show verification failed message
    else 7b. Report is still pending
        ViolationService->>UserRepo: 7b.1 Load librarian profile
        activate UserRepo
        UserRepo->>DB: 7b.2 Query user by id
        activate DB
        DB-->>UserRepo: 7b.3 Return librarian
        deactivate DB
        UserRepo-->>ViolationService: 7b.4 Return librarian
        deactivate UserRepo
        ViolationService->>RuleRepo: 8. Find reputation rule by violation type
        activate RuleRepo
        RuleRepo->>DB: 8.1 Query reputation rule
        activate DB
        DB-->>RuleRepo: 8.2 Return rule or null
        deactivate DB
        RuleRepo-->>ViolationService: 8.3 Return rule lookup result
        deactivate RuleRepo
        ViolationService->>ReservationRepo: 9. Recheck violator from reservation if needed
        activate ReservationRepo
        ReservationRepo->>DB: 9.1 Query reservation context
        activate DB
        DB-->>ReservationRepo: 9.2 Return reservation data
        deactivate DB
        ReservationRepo-->>ViolationService: 9.3 Return violator context
        deactivate ReservationRepo
        ViolationService->>ReportRepo: 10. Update report status to VERIFIED and store deducted points
        activate ReportRepo
        ReportRepo->>DB: 10.1 Update violation report
        activate DB
        DB-->>ReportRepo: 10.2 Persist success
        deactivate DB
        ReportRepo-->>ViolationService: 10.3 Return updated report
        deactivate ReportRepo
        ViolationService->>ProfileRepo: 11. Update violator reputation score and violation count
        activate ProfileRepo
        ProfileRepo->>DB: 11.1 Update student profile
        activate DB
        DB-->>ProfileRepo: 11.2 Persist success
        deactivate DB
        ProfileRepo-->>ViolationService: 11.3 Return updated profile
        deactivate ProfileRepo
        ViolationService->>ActivityRepo: 12. Save activity log for the violator
        activate ActivityRepo
        ActivityRepo->>DB: 12.1 Insert activity log
        activate DB
        DB-->>ActivityRepo: 12.2 Persist success
        deactivate DB
        ActivityRepo-->>ViolationService: 12.3 Return activity log
        deactivate ActivityRepo
        ViolationService->>PointRepo: 13. Save penalty point transaction
        activate PointRepo
        PointRepo->>DB: 13.1 Insert point transaction
        activate DB
        DB-->>PointRepo: 13.2 Persist success
        deactivate DB
        PointRepo-->>ViolationService: 13.3 Return point transaction
        deactivate PointRepo
        ViolationService->>PushService: 14. Notify violator and reporter about the verified result
        activate PushService
        PushService-->>ViolationService: 15. Notifications dispatched
        deactivate PushService
        ViolationService-->>ViolationController: 16. Return ViolationReportResponse
        deactivate ViolationService
        ViolationController-->>ViolationManage: 17. Return 200 OK
        deactivate ViolationController
        ViolationManage->>ViolationManage: 18. Refresh violation report list and modal state
        ViolationManage-->>Librarian: 19. Show verification success
    end

    deactivate ViolationManage
    deactivate Librarian
```


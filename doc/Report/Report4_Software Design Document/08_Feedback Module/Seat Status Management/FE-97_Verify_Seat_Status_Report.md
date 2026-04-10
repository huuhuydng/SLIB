# FE-97 Verify Seat Status Report

```mermaid
sequenceDiagram
    participant Librarian as Librarian
    participant SeatStatusManage as SeatStatusReportManage Page
    participant SeatStatusController as SeatStatusReportController
    participant UserRepo as UserRepository
    participant SeatStatusService as SeatStatusReportService
    participant ReportRepo as SeatStatusReportRepository
    participant PushService as PushNotificationService
    participant DB as Database

    activate Librarian
    Librarian->>SeatStatusManage: 1. Open a pending seat status report detail modal
    activate SeatStatusManage
    Librarian->>SeatStatusManage: 2. Click Verify
    SeatStatusManage->>SeatStatusController: 3. PUT /slib/seat-status-reports/{id}/verify
    activate SeatStatusController
    SeatStatusController->>UserRepo: 4. Resolve current staff user from session
    activate UserRepo
    UserRepo->>DB: 4.1 Query user by email
    activate DB
    DB-->>UserRepo: 4.2 Return librarian
    deactivate DB
    UserRepo-->>SeatStatusController: 4.3 Return librarianId
    deactivate UserRepo
    SeatStatusController->>SeatStatusService: 5. verifyReport(reportId, librarianId)
    activate SeatStatusService
    SeatStatusService->>ReportRepo: 6. Load pending report by id
    activate ReportRepo
    ReportRepo->>DB: 6.1 Query report by id
    activate DB
    DB-->>ReportRepo: 6.2 Return report
    deactivate DB
    ReportRepo-->>SeatStatusService: 6.3 Return report entity
    deactivate ReportRepo
    SeatStatusService->>UserRepo: 7. Load librarian profile
    activate UserRepo
    UserRepo->>DB: 7.1 Query user by id
    activate DB
    DB-->>UserRepo: 7.2 Return librarian
    deactivate DB
    UserRepo-->>SeatStatusService: 7.3 Return librarian
    deactivate UserRepo

    alt 8a. Report is not pending anymore
        SeatStatusService-->>SeatStatusController: 8a.1 Throw processing error
        deactivate SeatStatusService
        SeatStatusController-->>SeatStatusManage: 8a.2 Return error response
        deactivate SeatStatusController
        SeatStatusManage-->>Librarian: 8a.3 Show verification failed message
    else 8b. Report is still pending
        SeatStatusService->>ReportRepo: 8b.1 Update status to VERIFIED and save verifier info
        activate ReportRepo
        ReportRepo->>DB: 8b.2 Update seat status report
        activate DB
        DB-->>ReportRepo: 8b.3 Persist success
        deactivate DB
        ReportRepo-->>SeatStatusService: 8b.4 Return updated report
        deactivate ReportRepo
        SeatStatusService->>PushService: 9. Notify reporter that the report was verified
        activate PushService
        PushService-->>SeatStatusService: 10. Notification dispatched
        deactivate PushService
        SeatStatusService-->>SeatStatusController: 11. Return SeatStatusReportResponse
        deactivate SeatStatusService
        SeatStatusController-->>SeatStatusManage: 12. Return 200 OK
        deactivate SeatStatusController
        SeatStatusManage->>SeatStatusManage: 13. Refresh report list and modal state
        SeatStatusManage-->>Librarian: 14. Show verification success
    end

    deactivate SeatStatusManage
    deactivate Librarian
```


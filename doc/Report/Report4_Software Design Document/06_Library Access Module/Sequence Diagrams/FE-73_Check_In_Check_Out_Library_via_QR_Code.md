# FE-73 Check-in/Check-out Library via QR Code

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant MobileApp as Mobile App
    participant QrScreen as QrScanScreen
    participant KioskAuthController as KioskAuthController
    participant KioskQrAuthService as KioskQrAuthService
    participant SessionRepo as KioskQrSessionRepository
    participant AccessLogRepo as AccessLogRepository
    participant DB as Database

    Users->>MobileApp: 1. Open the QR check-in screen
    activate Users
    activate MobileApp
    MobileApp->>QrScreen: 2. Start QrScanScreen and check current status
    activate QrScreen
    QrScreen->>KioskAuthController: 3. GET /slib/kiosk/session/check-status/{userId}
    deactivate QrScreen
    deactivate MobileApp
    activate KioskAuthController
    KioskAuthController->>KioskQrAuthService: 4. isUserCheckedIn(userId)
    deactivate KioskAuthController
    activate KioskQrAuthService
    KioskQrAuthService->>AccessLogRepo: 5. checkInUser(userId)
    activate AccessLogRepo
    AccessLogRepo->>DB: 5.1 Query active access log
    activate DB
    DB-->>AccessLogRepo: 5.2 Return active session or empty result
    deactivate DB
    AccessLogRepo-->>KioskQrAuthService: 5.3 Return current status
    deactivate AccessLogRepo
    KioskQrAuthService-->>KioskAuthController: 6. Return check-in status
    deactivate KioskQrAuthService
    activate KioskAuthController
    KioskAuthController-->>QrScreen: 7. Return isCheckedIn flag
    deactivate KioskAuthController
    activate QrScreen
    activate MobileApp

    alt 8a. User is already checked in and chooses check-out
        QrScreen->>KioskAuthController: 8a.1 POST /slib/kiosk/session/checkout-mobile
        deactivate QrScreen
        deactivate MobileApp
        activate KioskAuthController
        KioskAuthController->>KioskQrAuthService: 8a.2 checkOutByUserId(userId)
        deactivate KioskAuthController
        activate KioskQrAuthService
        KioskQrAuthService->>AccessLogRepo: 8a.3 checkInUser(userId)
        activate AccessLogRepo
        AccessLogRepo->>DB: 8a.4 Query current active access log
        activate DB
        DB-->>AccessLogRepo: 8a.5 Return active access log
        deactivate DB
        AccessLogRepo-->>KioskQrAuthService: 8a.6 Return active log
        KioskQrAuthService->>AccessLogRepo: 8a.7 Update checkOutTime
        AccessLogRepo->>DB: 8a.8 Persist check-out
        activate DB
        DB-->>AccessLogRepo: 8a.9 Persist success
        deactivate DB
        AccessLogRepo-->>KioskQrAuthService: 8a.10 Return closed log
        deactivate AccessLogRepo
        KioskQrAuthService-->>KioskAuthController: 9a. Return mobile check-out success
        deactivate KioskQrAuthService
        activate KioskAuthController
        KioskAuthController-->>QrScreen: 10a. Return 200 OK
        deactivate KioskAuthController
        activate QrScreen
        activate MobileApp
        QrScreen-->>Users: 11a. Show check-out successful message
    else 8b. User scans kiosk QR to check in
        QrScreen->>QrScreen: 8b.1 Scan QR and parse kiosk code
        QrScreen->>KioskAuthController: 8b.2 POST /slib/kiosk/qr/validate
        deactivate QrScreen
        deactivate MobileApp
        activate KioskAuthController
        KioskAuthController->>KioskQrAuthService: 8b.3 validateQr(qrPayload, kioskCode)
        deactivate KioskAuthController
        activate KioskQrAuthService
        KioskQrAuthService->>SessionRepo: 8b.4 Find active QR session by payload
        activate SessionRepo
        SessionRepo->>DB: 8b.5 Query kiosk QR session
        activate DB
        DB-->>SessionRepo: 8b.6 Return active session or empty result
        deactivate DB
        SessionRepo-->>KioskQrAuthService: 8b.7 Return QR session
        deactivate SessionRepo

        alt 9a. QR is invalid or expired
            KioskQrAuthService-->>KioskAuthController: 9a.1 Return QR validation failure
            deactivate KioskQrAuthService
            activate KioskAuthController
            KioskAuthController-->>QrScreen: 9a.2 Return error response
            deactivate KioskAuthController
            activate QrScreen
            activate MobileApp
            QrScreen-->>Users: 9a.3 Show invalid QR message
        else 9b. QR is valid
            KioskQrAuthService-->>KioskAuthController: 9b.1 Return sessionToken and kiosk info
            deactivate KioskQrAuthService
            activate KioskAuthController
            KioskAuthController-->>QrScreen: 9b.2 Return QR validation success
            deactivate KioskAuthController
            activate QrScreen
            activate MobileApp
            QrScreen->>QrScreen: 10. Show confirmation dialog for kiosk check-in
            Users->>MobileApp: 11. Confirm check-in
            MobileApp->>KioskAuthController: 12. POST /slib/kiosk/session/complete
            deactivate QrScreen
            deactivate MobileApp
            activate KioskAuthController
            KioskAuthController->>KioskQrAuthService: 13. completeSession(sessionToken, userId)
            deactivate KioskAuthController
            activate KioskQrAuthService
            KioskQrAuthService->>SessionRepo: 14. Load kiosk QR session
            activate SessionRepo
            SessionRepo->>DB: 14.1 Query session by token
            activate DB
            DB-->>SessionRepo: 14.2 Return session
            deactivate DB
            SessionRepo-->>KioskQrAuthService: 14.3 Return session entity
            deactivate SessionRepo
            KioskQrAuthService->>AccessLogRepo: 15. Check existing active access log
            activate AccessLogRepo
            AccessLogRepo->>DB: 15.1 Query active access session
            activate DB
            DB-->>AccessLogRepo: 15.2 Return active session or empty result
            deactivate DB

            alt 16a. User is not currently checked in
                AccessLogRepo-->>KioskQrAuthService: 16a.1 Return empty session
                KioskQrAuthService->>AccessLogRepo: 16a.2 Save new access log
                AccessLogRepo->>DB: 16a.3 Insert access log
                activate DB
                DB-->>AccessLogRepo: 16a.4 Persist success
                deactivate DB
                AccessLogRepo-->>KioskQrAuthService: 16a.5 Return new access log
            else 16b. User already has an active access log
                AccessLogRepo-->>KioskQrAuthService: 16b.1 Reuse current active access log
            end

            deactivate AccessLogRepo
            KioskQrAuthService->>SessionRepo: 17. Mark kiosk session as USED and attach accessLogId
            activate SessionRepo
            SessionRepo->>DB: 17.1 Update kiosk QR session
            activate DB
            DB-->>SessionRepo: 17.2 Persist success
            deactivate DB
            SessionRepo-->>KioskQrAuthService: 17.3 Return updated session
            deactivate SessionRepo
            KioskQrAuthService-->>KioskAuthController: 18. Return kiosk session response
            deactivate KioskQrAuthService
            activate KioskAuthController
            KioskAuthController-->>MobileApp: 19. Return 200 OK with check-in session info
            deactivate KioskAuthController
            activate MobileApp
            MobileApp-->>Users: 20. Show QR check-in successful message
        end
    end

    deactivate QrScreen
    deactivate MobileApp
    deactivate Users
```

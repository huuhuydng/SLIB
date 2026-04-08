# FE-72 Check-in/Check-out Library via HCE

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant MobileApp as Mobile App
    participant HceBridge as HceBridge
    participant HceService as MyHostApduService
    participant GateDevice as HCE Gate Device
    participant HCEController as HCEController
    participant CheckInService as CheckInService
    participant HceStationService as HceStationService
    participant AccessLogRepo as AccessLogRepository
    participant DB as Database

    Users->>MobileApp: 1. Sign in and keep HCE enabled on the device
    activate Users
    activate MobileApp
    MobileApp->>HceBridge: 2. setUserId(currentUserId)
    activate HceBridge
    HceBridge->>HceService: 2.1 Save HCE user id into Android shared preferences
    activate HceService
    HceService-->>HceBridge: 2.2 User id stored for HCE emulation
    deactivate HceService
    HceBridge-->>MobileApp: 2.3 HCE bridge sync completed
    deactivate HceBridge
    deactivate MobileApp

    Users->>GateDevice: 3. Tap the phone on the HCE gate reader
    activate GateDevice
    GateDevice->>HceService: 4. Send SELECT AID APDU command
    activate HceService
    HceService-->>GateDevice: 5. Return userId token from HCE response
    deactivate HceService
    GateDevice->>HCEController: 6. POST /slib/hce/checkin with token, gateId, and X-API-KEY
    deactivate GateDevice
    activate HCEController
    HCEController->>HCEController: 6.1 Validate gate API key
    HCEController->>CheckInService: 7. processCheckIn(request)
    deactivate HCEController
    activate CheckInService
    CheckInService->>HceStationService: 8. validateStationForCheckIn(gateId)
    activate HceStationService
    HceStationService->>DB: 8.1 Query HCE station and update last heartbeat
    activate DB
    DB-->>HceStationService: 8.2 Return valid station
    deactivate DB
    HceStationService-->>CheckInService: 8.3 Station validation passed
    deactivate HceStationService
    CheckInService->>DB: 9. Load user and user HCE setting
    activate DB
    DB-->>CheckInService: 9.1 Return user and setting data
    deactivate DB

    alt 10a. Station, user, or HCE setting is invalid
        CheckInService-->>HCEController: 10a.1 Throw check-in validation error
        deactivate CheckInService
        activate HCEController
        HCEController-->>GateDevice: 10a.2 Return error response
        deactivate HCEController
        activate GateDevice
        GateDevice-->>Users: 10a.3 Show access denied or system error message
        deactivate GateDevice
    else 10b. User and station are valid
        CheckInService->>AccessLogRepo: 10b.1 checkInUser(userId)
        activate AccessLogRepo
        AccessLogRepo->>DB: 10b.2 Query current active access session
        activate DB
        DB-->>AccessLogRepo: 10b.3 Return active session or empty result
        deactivate DB

        alt 11a. User is already inside the library
            AccessLogRepo-->>CheckInService: 11a.1 Return active access log
            CheckInService->>AccessLogRepo: 11a.2 Update checkOutTime
            AccessLogRepo->>DB: 11a.3 Persist check-out time
            activate DB
            DB-->>AccessLogRepo: 11a.4 Persist success
            deactivate DB
            AccessLogRepo-->>CheckInService: 11a.5 Return closed access log
            deactivate AccessLogRepo
            CheckInService-->>HCEController: 12a. Return CHECK_OUT response
            deactivate CheckInService
            activate HCEController
            HCEController-->>GateDevice: 13a. Return 200 OK with CHECK_OUT result
            deactivate HCEController
            activate GateDevice
            GateDevice-->>Users: 14a. Show check-out success message
            deactivate GateDevice
        else 11b. User is entering the library
            AccessLogRepo-->>CheckInService: 11b.1 Return empty active session
            CheckInService->>AccessLogRepo: 11b.2 Save new access log with checkInTime
            AccessLogRepo->>DB: 11b.3 Insert access log
            activate DB
            DB-->>AccessLogRepo: 11b.4 Persist success
            deactivate DB
            AccessLogRepo-->>CheckInService: 11b.5 Return new access log
            deactivate AccessLogRepo
            CheckInService-->>HCEController: 12b. Return CHECK_IN response
            deactivate CheckInService
            activate HCEController
            HCEController-->>GateDevice: 13b. Return 200 OK with CHECK_IN result
            deactivate HCEController
            activate GateDevice
            GateDevice-->>Users: 14b. Show check-in success message
            deactivate GateDevice
        end
    end

    deactivate Users
```

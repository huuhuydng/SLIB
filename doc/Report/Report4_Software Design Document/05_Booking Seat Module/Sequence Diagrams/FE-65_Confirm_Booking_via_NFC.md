# FE-65 Confirm Booking via NFC

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant Client as Mobile App
    participant NfcUidService as NfcUidService
    participant BookingController as BookingController
    participant BookingService as BookingService
    participant SeatService as SeatService
    participant ReservationRepo as ReservationRepository
    participant DB as Database

    Users->>Client: 1. Choose "Confirm via NFC" from the active booking
    activate Users
    activate Client
    Client->>NfcUidService: 2. Start NFC UID scan
    deactivate Client
    activate NfcUidService

    alt 3a. NFC is not available or scanning fails
        NfcUidService-->>Client: 3a.1 Return NFC unavailable or scan error
        deactivate NfcUidService
        activate Client
        Client-->>Users: 3a.2 Show NFC error and allow retry
    else 3b. NFC UID is detected
        NfcUidService-->>Client: 3b.1 Return raw NFC UID
        deactivate NfcUidService
        activate Client
        Client->>BookingController: 4. POST /slib/bookings/confirm-nfc-uid/{reservationId}
        deactivate Client
        activate BookingController
        BookingController->>BookingService: 5. confirmSeatWithNfcUid(reservationId, rawNfcUid)
        deactivate BookingController
        activate BookingService
        BookingService->>ReservationRepo: 6. Load reservation by id
        activate ReservationRepo
        ReservationRepo->>DB: 6.1 Query reservation
        activate DB
        DB-->>ReservationRepo: 6.2 Return reservation
        deactivate DB
        ReservationRepo-->>BookingService: 6.3 Return reservation entity
        deactivate ReservationRepo
        BookingService->>SeatService: 7. Resolve seat by raw NFC UID
        activate SeatService
        SeatService->>DB: 7.1 Query seat by hashed NFC UID
        activate DB
        DB-->>SeatService: 7.2 Return mapped seat
        deactivate DB
        SeatService-->>BookingService: 7.3 Return seat from NFC tag
        deactivate SeatService

        alt 8a. NFC seat does not match reservation or check-in window is invalid
            BookingService-->>BookingController: 8a.1 Throw NFC confirmation error
            deactivate BookingService
            activate BookingController
            BookingController-->>Client: 8a.2 Return 400 Bad Request
            deactivate BookingController
            activate Client
            Client-->>Users: 8a.3 Show NFC confirmation failed message
        else 8b. NFC seat matches the reservation
            BookingService->>ReservationRepo: 8b.1 Update reservation status to CONFIRMED
            activate ReservationRepo
            ReservationRepo->>DB: 8b.2 Persist confirmedAt and CONFIRMED status
            activate DB
            DB-->>ReservationRepo: 8b.3 Persist success
            deactivate DB
            ReservationRepo-->>BookingService: 8b.4 Return confirmed reservation
            deactivate ReservationRepo
            BookingService-->>BookingController: 9. Return confirmed reservation DTO
            deactivate BookingService
            activate BookingController
            BookingController-->>Client: 10. Return 200 OK with CONFIRMED status
            deactivate BookingController
            activate Client
            Client-->>Users: 11. Show seat check-in successful message
        end
    end

    deactivate Client
    deactivate Users
```

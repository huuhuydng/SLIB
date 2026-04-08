# FE-63 Booking Seat

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant Client as Mobile App
    participant BookingController as BookingController
    participant BookingService as BookingService
    participant UserRepo as UserRepository
    participant SeatRepo as SeatRepository
    participant ReservationRepo as ReservationRepository
    participant LibrarySettingService as LibrarySettingService
    participant Broker as WebSocket Broker
    participant DB as Database

    Users->>Client: 1. Confirm booking from the seat preview
    activate Users
    activate Client
    Client->>BookingController: 2. POST /slib/bookings/create
    deactivate Client
    activate BookingController
    BookingController->>UserRepo: 2.1 Resolve and authorize the current user
    activate UserRepo
    UserRepo->>DB: 2.2 Query user by email or id
    activate DB
    DB-->>UserRepo: 2.3 Return user record
    deactivate DB
    UserRepo-->>BookingController: 2.4 Return authorized user
    deactivate UserRepo
    BookingController->>BookingService: 3. createBooking(userId, seatId, startTime, endTime)
    deactivate BookingController
    activate BookingService
    BookingService->>SeatRepo: 4. Load seat for update
    activate SeatRepo
    SeatRepo->>DB: 4.1 Query seat with lock
    activate DB
    DB-->>SeatRepo: 4.2 Return seat record
    deactivate DB
    SeatRepo-->>BookingService: 4.3 Return seat entity
    deactivate SeatRepo
    BookingService->>LibrarySettingService: 5. Load booking rules and library state
    activate LibrarySettingService
    LibrarySettingService->>DB: 5.1 Query library settings
    activate DB
    DB-->>LibrarySettingService: 5.2 Return booking constraints
    deactivate DB
    LibrarySettingService-->>BookingService: 5.3 Return current settings
    deactivate LibrarySettingService
    BookingService->>ReservationRepo: 6. Check active reservations and user booking limits
    activate ReservationRepo
    ReservationRepo->>DB: 6.1 Query user bookings and overlapping reservations
    activate DB
    DB-->>ReservationRepo: 6.2 Return booking history and overlaps
    deactivate DB
    ReservationRepo-->>BookingService: 6.3 Return validation data

    alt 7a. Seat is conflicted or booking rule is violated
        BookingService-->>BookingController: 7a.1 Throw booking validation error
        deactivate ReservationRepo
        deactivate BookingService
        activate BookingController
        BookingController-->>Client: 7a.2 Return 400 Bad Request
        deactivate BookingController
        activate Client
        Client-->>Users: 7a.3 Show booking failed message
    else 7b. Booking request is valid
        BookingService->>ReservationRepo: 7b.1 Save reservation with PROCESSING status
        ReservationRepo->>DB: 7b.2 Insert reservation
        activate DB
        DB-->>ReservationRepo: 7b.3 Persist success
        deactivate DB
        ReservationRepo-->>BookingService: 7b.4 Return saved reservation
        deactivate ReservationRepo
        BookingService->>Broker: 8. Broadcast HOLDING seat update for the selected slot
        activate Broker
        Broker-->>BookingService: 8.1 Seat update event accepted
        deactivate Broker
        BookingService-->>BookingController: 9. Return ReservationDTO
        deactivate BookingService
        activate BookingController
        BookingController-->>Client: 10. Return 200 OK with reservationId and PROCESSING status
        deactivate BookingController
        activate Client
        Client->>Client: 11. Navigate to the booking confirmation screen
        Client-->>Users: 12. Show reservation held for 2 minutes
    end

    deactivate Client
    deactivate Users
```

# FE-67 Cancel Booking

```mermaid
sequenceDiagram
    participant Users as "Student, Teacher"
    participant Client as Mobile App
    participant BookingController as BookingController
    participant BookingService as BookingService
    participant ReservationRepo as ReservationRepository
    participant Broker as WebSocket Broker
    participant DB as Database

    Users->>Client: 1. Select an active booking and choose Cancel
    activate Users
    activate Client
    Client->>Client: 2. Show cancellation confirmation sheet

    alt 3a. User closes the confirmation sheet
        Client-->>Users: 3a.1 Keep the booking unchanged
    else 3b. User confirms cancellation
        Client->>BookingController: 3b.1 PUT /slib/bookings/cancel/{reservationId}
        deactivate Client
        activate BookingController
        BookingController->>ReservationRepo: 3b.2 Load reservation for authorization
        activate ReservationRepo
        ReservationRepo->>DB: 3b.3 Query reservation by id
        activate DB
        DB-->>ReservationRepo: 3b.4 Return reservation
        deactivate DB
        ReservationRepo-->>BookingController: 3b.5 Return reservation owner
        deactivate ReservationRepo
        BookingController->>BookingService: 4. cancelBooking(reservationId)
        deactivate BookingController
        activate BookingService
        BookingService->>ReservationRepo: 5. Load reservation again for business validation
        activate ReservationRepo
        ReservationRepo->>DB: 5.1 Query reservation status and time
        activate DB
        DB-->>ReservationRepo: 5.2 Return reservation entity
        deactivate DB
        ReservationRepo-->>BookingService: 5.3 Return reservation

        alt 6a. Cancellation is blocked by status or the 12-hour rule
            BookingService-->>BookingController: 6a.1 Throw cancellation error
            deactivate ReservationRepo
            deactivate BookingService
            activate BookingController
            BookingController-->>Client: 6a.2 Return 400 Bad Request
            deactivate BookingController
            activate Client
            Client-->>Users: 6a.3 Show cancellation failed message
        else 6b. Cancellation is allowed
            BookingService->>ReservationRepo: 6b.1 Update reservation status to CANCEL
            ReservationRepo->>DB: 6b.2 Persist cancelled reservation
            activate DB
            DB-->>ReservationRepo: 6b.3 Persist success
            deactivate DB
            ReservationRepo-->>BookingService: 6b.4 Return updated reservation
            deactivate ReservationRepo
            BookingService->>Broker: 7. Broadcast AVAILABLE seat update for the booked slot
            activate Broker
            Broker-->>BookingService: 7.1 Seat release event accepted
            deactivate Broker
            BookingService-->>BookingController: 8. Return cancelled reservation DTO
            deactivate BookingService
            activate BookingController
            BookingController-->>Client: 9. Return 200 OK with cancelled status
            deactivate BookingController
            activate Client
            Client-->>Users: 10. Show booking cancelled successfully
        end
    end

    deactivate Client
    deactivate Users
```

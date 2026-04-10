# Booking Seat Module

This folder contains the Report 4 diagrams for Module 5 - Booking Seat.

## Included Artifacts

- Sequence diagrams for:
  - `FE-61` View real time seat map
  - `FE-62` Filter seat map
  - `FE-63` View map density
  - `FE-64` Booking seat
  - `FE-65` Preview booking information
  - `FE-66` Confirm booking via NFC
  - `FE-67` View history of booking
  - `FE-68` Cancel booking
  - `FE-69` Ask AI for recommending seat
  - `FE-70` View list of user bookings
  - `FE-71` Search and filter user booking
  - `FE-72` View user booking details and status
  - `FE-73` Leave seat via NFC
  - `FE-74` Release occupied seat by Librarian
  - `FE-75` View actual seat end time
- Class diagram for the Booking Seat Module

## Actor Scope

- `FE-61` to `FE-63`: `Librarian`, `Student`, `Teacher`
- `FE-64` to `FE-69`: `Student`, `Teacher`
- `FE-70` to `FE-72`: `Librarian`
- `FE-73`: `Student`, `Teacher`
- `FE-74`: `Librarian`
- `FE-75`: `Librarian`, `Student`, `Teacher`

## Current Working Assumptions

- `FE-61` and `FE-62` are based on the current real-time seat map flows in `FloorPlanScreen` on mobile and `LibrarianAreas.jsx` on web.
- `FE-63` uses the current occupancy flow through `GET /slib/zones/occupancy/{areaId}`.
- `FE-64` creates a reservation in `PROCESSING` status first, then sends the user to the booking confirmation screen.
- `FE-65` reflects the current preview bottom sheet before the booking request is submitted.
- `FE-66` follows the current NFC UID mapping flow through `POST /slib/bookings/confirm-nfc-uid/{reservationId}`.
- `FE-67` and `FE-68` follow the current mobile booking history and cancellation flow.
- `FE-69` follows the current AI analytics proxy and mobile recommendation flow.
- `FE-70` to `FE-72` reflect the current implementation where search, filter, and booking detail display are handled mainly on the loaded dataset inside `BookingManage.jsx`.
- `FE-73` follows `POST /slib/bookings/leave-seat-nfc/{reservationId}`.
- `FE-74` follows the current librarian release-seat flow through `POST /slib/bookings/leave-seat/{reservationId}`.
- `FE-75` follows the current booking history response, where the actual seat end time is exposed from the reservation payload and rendered directly on the client.

## Sequence Diagram Convention

- Step numbering is written explicitly in each message.
- `alt` branches are used for error cases, current implementation variants, or local-data shortcuts.
- The first participant column represents the applicable actors for that feature in a single lane.
- Activation bars are added on the actor lane and on the main processing lanes to stay aligned with the Module 1 convention used for `mermaid.ai`.

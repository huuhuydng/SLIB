# Booking Seat Module

This folder contains the Report 4 diagrams for Module 5 - Booking Seat.

## Included Artifacts

- Sequence diagrams for:
  - `FE-60` View real time seat map
  - `FE-61` Filter seat map
  - `FE-62` View map density
  - `FE-63` Booking seat
  - `FE-64` Preview booking information
  - `FE-65` Confirm booking via NFC
  - `FE-66` View history of booking
  - `FE-67` Cancel booking
  - `FE-68` Ask AI for recommending seat
  - `FE-69` View list of user bookings
  - `FE-70` Search and Filter user booking
  - `FE-71` View booking details and status
- Class diagram for the Booking Seat Module

## Actor Scope

- `FE-60` to `FE-62`: `Librarian, Student, Teacher`
- `FE-63` to `FE-68`: `Student, Teacher`
- `FE-69` to `FE-71`: `Librarian`

## Current Working Assumptions

- `FE-60` and `FE-61` are based on the current real-time seat map flows in `FloorPlanScreen` on mobile and `LibrarianAreas.jsx` on web.
- `FE-62` uses the current occupancy flow through `GET /slib/zones/occupancy/{areaId}`.
- `FE-63` creates a reservation in `PROCESSING` status first, then sends the user to the booking confirmation screen.
- `FE-64` reflects the current preview bottom sheet before the booking request is submitted.
- `FE-65` follows the current NFC UID mapping flow through `POST /slib/bookings/confirm-nfc-uid/{reservationId}`.
- `FE-66` and `FE-67` follow the current mobile booking history and cancellation flow.
- `FE-68` follows the current AI analytics proxy and mobile recommendation flow.
- `FE-70` and `FE-71` reflect the current implementation where search, filter, and booking detail display are handled mainly on the loaded dataset inside `BookingManage.jsx`.

## Sequence Diagram Convention

- Step numbering is written explicitly in each message.
- `alt` branches are used for error cases, current implementation variants, or local-data shortcuts.
- The first participant column represents the applicable actors for that feature in a single lane.
- Activation bars are added on the actor lane and on the main processing lanes to stay aligned with the Module 1 convention used for `mermaid.ai`.

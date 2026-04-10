# Library Access Module

This folder contains the Report 4 diagrams for Module 6 - Library Access.

## Included Artifacts

- Sequence diagrams for:
  - `FE-76` Check-in/Check-out library via HCE
  - `FE-77` Check-in/Check-out library via QR code
  - `FE-78` View history of check-ins/check-outs
  - `FE-79` View list of users access to library
- Class diagram for the Library Access Module

## Actor Scope

- `FE-76` to `FE-78`: `Student, Teacher`
- `FE-79`: `Admin, Librarian`

## Current Working Assumptions

- `FE-76` follows the current Android HCE flow through `HceBridge`, `MyHostApduService`, the gate device request, and `POST /slib/hce/checkin`.
- `FE-77` follows the current QR kiosk flow in `QrScanScreen`, `KioskService`, and `KioskAuthController`.
- `FE-78` is mapped to the current `ActivityHistoryScreen` implementation, because the mobile app currently shows check-in and check-out records through the activity history timeline instead of a dedicated access-log screen.
- `FE-79` is mapped to the current `CheckInOut.jsx` implementation on the Librarian web portal, backed by the HCE access-log APIs and WebSocket updates.

## Sequence Diagram Convention

- Step numbering is written explicitly in each message.
- `alt` branches are used when the current system has multiple outcomes or multiple access methods in the same feature.
- The first participant column represents the applicable actors for that feature in a single lane.
- Diagrams prioritize the current code flow over a purely conceptual flow when the current implementation is more specific.

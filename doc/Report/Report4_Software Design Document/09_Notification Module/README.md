# Module 9 - Notification Management

This folder contains the Report 4 diagrams for Module 9 - Notification Management.

## Included Artifacts

- Sequence diagrams for:
  - `FE-99` View and delete list of notifications
  - `FE-100` View notification details
  - `FE-101` Filter notification
  - `FE-102` Mark notification as read
- Class diagram for the Notification Management module

## Actor Scope

- `FE-99` to `FE-102`: Librarian, Student, Teacher

## Current Working Assumptions

- The current end-user notification screen is implemented in the mobile app via `NotificationScreen` and `NotificationService`.
- Librarian web currently uses notification badges, real-time toasts, and pending counters, but the full list/filter/delete/detail interaction is implemented in the mobile notification screen.
- `FE-99` follows `GET /slib/notifications/user/{userId}` and `DELETE /slib/notifications/{notificationId}`.
- `FE-100` follows the current `openNotificationTarget(...)` behavior, where opening a notification routes the user to the referenced module screen rather than a standalone notification detail page.
- `FE-101` is currently a client-side category filter in `NotificationScreen`; no dedicated backend filter endpoint is used.
- `FE-102` follows `PUT /slib/notifications/mark-read/{notificationId}` and updates local unread counters immediately after success.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

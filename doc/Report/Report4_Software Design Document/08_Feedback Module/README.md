# Module 8 - Feedback Management

This folder contains the Report 4 diagrams for Module 8 - Feedback Management.

## Included Artifacts

- Sequence diagrams for:
  - `FE-86` Create feedback after check-out
  - `FE-87` View list of feedbacks
  - `FE-88` View feedback details
  - `FE-89` Create seat status report
  - `FE-90` View history of sending seat status report
  - `FE-91` View list of seat status reports
  - `FE-92` View seat status report details
  - `FE-93` Verify seat status report
  - `FE-94` Create report seat violation
  - `FE-95` View history of sending report seat violation
  - `FE-96` View list of seat violation reports
  - `FE-97` View report seat violation details
  - `FE-98` Verify seat violation report
- Class diagram for the Feedback Management module

## Actor Scope

- `FE-86`: Student, Teacher
- `FE-87` to `FE-88`: Librarian
- `FE-89` to `FE-90`: Student, Teacher
- `FE-91` to `FE-92`: Admin, Librarian
- `FE-93`: Librarian
- `FE-94` to `FE-95`: Student, Teacher
- `FE-96` to `FE-97`: Admin, Librarian
- `FE-98`: Librarian

## Current Working Assumptions

- `FE-86` follows the mobile feedback prompt flow using `GET /slib/feedbacks/check-pending` and `POST /slib/feedbacks`.
- `FE-87` and `FE-88` follow the Librarian `FeedbackManage` page using `GET /slib/feedbacks` and `PUT /slib/feedbacks/{id}/review`.
- `FE-88` opens feedback details from the already loaded feedback list in the current web implementation.
- `FE-89` follows the mobile seat status report flow using `POST /slib/seat-status-reports`.
- `FE-90` follows the mobile report history flow using `GET /slib/seat-status-reports/my`.
- `FE-91` and `FE-92` follow the staff `SeatStatusReportManage` page using `GET /slib/seat-status-reports`.
- `FE-92` opens seat status report details from the already loaded report list in the current web implementation.
- `FE-93` follows the staff verification action using `PUT /slib/seat-status-reports/{id}/verify`.
- `FE-94` follows the mobile violation reporting flow using `POST /slib/violation-reports`.
- `FE-95` follows the mobile report history flow using `GET /slib/violation-reports/my`.
- `FE-96` and `FE-97` follow the staff `ViolationManage` page using `GET /slib/violation-reports`.
- `FE-97` opens violation report details from the already loaded report list in the current web implementation.
- `FE-98` follows the staff verification action using `PUT /slib/violation-reports/{id}/verify`.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

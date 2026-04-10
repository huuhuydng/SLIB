# Module 8 - Feedback Management

This folder contains the Report 4 diagrams for Module 8 - Feedback Management.

## Included Artifacts

- Sequence diagrams for:
  - `FE-90` Create feedback after check-out
  - `FE-91` View list of feedbacks
  - `FE-92` View feedback details
  - `FE-93` Create seat status report
  - `FE-94` View history of sending seat status report
  - `FE-95` View list of seat status reports
  - `FE-96` View seat status report details
  - `FE-97` Verify seat status report
  - `FE-98` Create report seat violation
  - `FE-99` View history of sending report seat violation
  - `FE-100` View list of seat violation reports
  - `FE-101` View report seat violation details
  - `FE-102` Verify seat violation report
- Class diagram for the Feedback Management module

## Actor Scope

- `FE-90`: Student, Teacher
- `FE-91` to `FE-92`: Librarian
- `FE-93` to `FE-94`: Student, Teacher
- `FE-95` to `FE-96`: Admin, Librarian
- `FE-97`: Librarian
- `FE-98` to `FE-99`: Student, Teacher
- `FE-100` to `FE-101`: Admin, Librarian
- `FE-102`: Librarian

## Current Working Assumptions

- `FE-90` follows the mobile feedback prompt flow using `GET /slib/feedbacks/check-pending` and `POST /slib/feedbacks`.
- `FE-91` and `FE-92` follow the Librarian `FeedbackManage` page using `GET /slib/feedbacks` and `PUT /slib/feedbacks/{id}/review`.
- `FE-92` opens feedback details from the already loaded feedback list in the current web implementation.
- `FE-93` follows the mobile seat status report flow using `POST /slib/seat-status-reports`.
- `FE-94` follows the mobile report history flow using `GET /slib/seat-status-reports/my`.
- `FE-95` and `FE-96` follow the staff `SeatStatusReportManage` page using `GET /slib/seat-status-reports`.
- `FE-96` opens seat status report details from the already loaded report list in the current web implementation.
- `FE-97` follows the staff verification action using `PUT /slib/seat-status-reports/{id}/verify`.
- `FE-98` follows the mobile violation reporting flow using `POST /slib/violation-reports`.
- `FE-99` follows the mobile report history flow using `GET /slib/violation-reports/my`.
- `FE-100` and `FE-101` follow the staff `ViolationManage` page using `GET /slib/violation-reports`.
- `FE-101` opens violation report details from the already loaded report list in the current web implementation.
- `FE-102` follows the staff verification action using `PUT /slib/violation-reports/{id}/verify`.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

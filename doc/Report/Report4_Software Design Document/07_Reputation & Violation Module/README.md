# Module 7 - Reputation and Violation Management

This folder contains the Report 4 diagrams for Module 7 - Reputation and Violation Management.

## Included Artifacts

- Sequence diagrams for:
  - `FE-76` View reputation score
  - `FE-77` View history of changed reputation points
  - `FE-78` View detailed reason for deducting points
  - `FE-79` View list of users violation
  - `FE-80` View user violation details
  - `FE-81` Create complaint
  - `FE-82` View history of sending complaint
  - `FE-83` View list of complaints
  - `FE-84` View complaint details
  - `FE-85` Verify complaint
- Class diagram for the Reputation and Violation Management module

## Actor Scope

- `FE-76` to `FE-78`: Student, Teacher
- `FE-79` to `FE-80`: Librarian
- `FE-81` to `FE-82`: Student, Teacher
- `FE-83` to `FE-85`: Librarian

## Current Working Assumptions

- `FE-76` uses `GET /slib/student-profile/me` to retrieve the reputation score.
- `FE-77` pulls penalty history via `GET /slib/activities/penalties/{userId}` and reported violations via `GET /slib/violation-reports/against-me`.
- `FE-79` and `FE-80` follow the Librarian `ViolationManage` page using `GET /slib/violation-reports`.
- `FE-81` and `FE-82` follow the mobile appeal flow using `POST /slib/complaints` and `GET /slib/complaints/my`.
- `FE-83` to `FE-85` follow the Librarian `ComplaintManage` page using `GET /slib/complaints` and `PUT /slib/complaints/{id}/accept|deny`.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

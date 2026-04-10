# Module 7 - Reputation and Violation Management

This folder contains the Report 4 diagrams for Module 7 - Reputation and Violation Management.

## Included Artifacts

- Sequence diagrams for:
  - `FE-80` View reputation score
  - `FE-81` View history of changed reputation points
  - `FE-82` View detailed reason for deducting points
  - `FE-83` View list of users violation
  - `FE-84` View user violation details
  - `FE-85` Create complaint
  - `FE-86` View history of sending complaint
  - `FE-87` View list of complaints
  - `FE-88` View complaint details
  - `FE-89` Verify complaint
- Class diagram for the Reputation and Violation Management module

## Actor Scope

- `FE-80` to `FE-82`: Student, Teacher
- `FE-83` to `FE-84`: Librarian
- `FE-85` to `FE-86`: Student, Teacher
- `FE-87` to `FE-89`: Librarian

## Current Working Assumptions

- `FE-80` uses `GET /slib/student-profile/me` to retrieve the reputation score.
- `FE-81` pulls penalty history via `GET /slib/activities/penalties/{userId}` and reported violations via `GET /slib/violation-reports/against-me`.
- `FE-83` and `FE-84` follow the Librarian `ViolationManage` page using `GET /slib/violation-reports`.
- `FE-85` and `FE-86` follow the mobile appeal flow using `POST /slib/complaints` and `GET /slib/complaints/my`.
- `FE-87` to `FE-89` follow the Librarian `ComplaintManage` page using `GET /slib/complaints` and `PUT /slib/complaints/{id}/accept|deny`.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

# Module 12 - Statistics & Report Module

This folder contains the Report 4 diagrams for Module 12 - Statistics & Report Module.

## Included Artifacts

- Sequence diagrams for:
  - `FE-129` View general analytics dashboard
  - `FE-130` View violation statistics
  - `FE-131` View statistics of density forecast via AI
  - `FE-132` View check-in/check-out statistics
  - `FE-133` View seat booking statistics
  - `FE-134` View AI prioritized students
  - `FE-135` Send warning to student from AI analytics dashboard
  - `FE-136` Export general analytical report
- Class diagram for the Statistics & Report module

## Actor Scope

- `FE-129` to `FE-136`: Librarian

## Current Working Assumptions

- `FE-129` follows the current librarian dashboard flow in `Dashboard.jsx`, `dashboardService.js`, `DashboardController`, and `DashboardService`.
- `FE-130` follows the current statistics page flow in `Statistic.jsx`, `statisticService.js`, `StatisticController`, and `StatisticService`, specifically the violation analytics section rendered from `/slib/statistics`.
- `FE-131` follows the current AI analytics panel flow in `AIAnalyticsPanel.jsx` and `frontend/src/services/admin/ai/analyticsService.js`, backed by `AIAnalyticsProxyController` and the AI service analytics endpoints.
- `FE-132` follows the current librarian check-in/check-out monitoring and statistics flow in `CheckInOut.jsx`, `librarianService`, `HCEController`, and `CheckInService`, including WebSocket updates from `/topic/access-logs`.
- `FE-133` follows the current seat booking statistics flow rendered in `Statistic.jsx`, using `/slib/statistics` and `/slib/dashboard/chart-stats` together to show booking totals, usage, cancellation, no-show, and period-based chart data.
- `FE-134` follows the current behavior issue cards shown on `Dashboard.jsx`, backed by `GET /slib/ai/analytics/behavior-issues`.
- `FE-135` follows the current behavior warning action in `Dashboard.jsx`, backed by `POST /slib/notifications/staff/behavior-warning`.
- `FE-136` maps to the current analytical report export capability through the statistics and dashboard aggregation already exposed by `StatisticController` and `DashboardController`. The current project does not provide a dedicated backend file export endpoint for this report yet, so this FE is documented as exporting the currently aggregated analytical dataset from the librarian reporting view.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md`.
- Diagrams prioritize the current code flow over a purely conceptual flow.

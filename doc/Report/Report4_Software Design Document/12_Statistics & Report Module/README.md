# Module 12 - Statistics & Report Module

This folder contains the Report 4 diagrams for Module 12 - Statistics & Report Module.

## Included Artifacts

- Sequence diagrams for:
  - `FE-121` View general analytics dashboard
  - `FE-122` View violation statistics
  - `FE-123` View statistics of density forecast via AI
  - `FE-124` View check-in/check-out statistics
  - `FE-125` View seat booking statistics
  - `FE-126` Export seat & maintenance report
  - `FE-127` Export general analytical report
- Class diagram for the Statistics & Report module

## Actor Scope

- `FE-121`: Librarian
- `FE-122`: Librarian
- `FE-123`: Librarian
- `FE-124`: Librarian
- `FE-125`: Librarian
- `FE-126`: Librarian
- `FE-127`: Librarian

## Current Working Assumptions

- This module follows the latest numbering requested for Report 4, where Module 12 is documented as `FE-121` to `FE-127`.
- `FE-121` follows the current librarian dashboard flow in `Dashboard.jsx`, `dashboardService.js`, `DashboardController`, and `DashboardService`.
- `FE-122` follows the current statistics page flow in `Statistic.jsx`, `statisticService.js`, `StatisticController`, and `StatisticService`, specifically the violation analytics section rendered from `/slib/statistics`.
- `FE-123` follows the current AI analytics panel flow in `AIAnalyticsPanel.jsx` and `frontend/src/services/admin/ai/analyticsService.js`, backed by `AIAnalyticsProxyController` and the AI service analytics endpoints.
- `FE-124` follows the current librarian check-in/check-out monitoring and statistics flow in `CheckInOut.jsx`, `librarianService`, `HCEController`, and `CheckInService`, including WebSocket updates from `/topic/access-logs`.
- `FE-125` follows the current seat booking statistics flow rendered in `Statistic.jsx`, using `/slib/statistics` and `/slib/dashboard/chart-stats` together to show booking totals, usage, cancellation, no-show, and period-based chart data.
- `FE-126` maps to the current export flow implemented in `CheckInOut.jsx` and `HCEController.exportAccessLogsToExcel(...)`. The exported file is effectively the current seat usage and library access operational report available in the system.
- `FE-127` maps to the current general analytical report export capability through the statistics/dashboard aggregation already exposed by `StatisticController` and `DashboardController`. The current project does not provide a dedicated backend file export endpoint for this report yet, so this FE is documented as generating/exporting the current analytical dataset from the librarian reporting view.

## Sequence Diagram Convention

- Step numbering is explicit and continuous.
- When using `alt`, numbering continues correctly inside each branch and after the branch.
- The first participant column represents the applicable actors based on `README_Feature_Breakdown.md` and the latest module numbering request.
- Diagrams prioritize the current code flow over a purely conceptual flow.


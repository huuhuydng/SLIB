# Statistics & Report Module Class Diagram

```mermaid
classDiagram
    class DashboardController {
        +getDashboardStats()
        +getLibraryStatus()
        +getChartStats(range)
        +getTopStudents(range)
        +testBroadcast()
    }

    class StatisticController {
        +getStatistics(range)
    }

    class AIAnalyticsProxyController {
        +getDensityPrediction(zone_id, days)
        +getUsageStatistics(period)
        +getRealtimeCapacity()
        +getBehaviorSummary(days)
        +getBehaviorIssues(limit)
        +getStudentBehavior(body)
    }

    class NotificationController {
        +sendBehaviorWarning(body)
    }

    class HCEController {
        +getAllAccessLogs()
        +getTodayStats()
        +getAccessLogsByDateRange(startDate, endDate)
        +exportAccessLogsToExcel(startDate, endDate)
    }

    class DashboardService {
        +getDashboardStats()
        +getLibraryStatus()
        +getChartStats(range)
        +getTopStudents(range)
    }

    class StatisticService {
        +getStatistics(range)
        +buildOverview(startDate)
        +buildBookingAnalysis(startDate)
        +buildViolationsByType(startDate)
        +buildFeedbackSummary(startDate)
        +buildZoneUsage(startDate)
        +buildPeakHours(startDate)
        +buildInsights(...)
    }

    class CheckInService {
        +getAllAccessLogs()
        +getTodayStats()
        +getAccessLogsByDateRange(startDate, endDate)
        +exportAccessLogsToExcel(startDate, endDate)
    }

    class DashboardPage {
        +fetchDashboardData()
        +refreshStatsOnly()
        +fetchAccessLogs()
        +fetchChatOverview()
        +handleOpenStudentProfile(issue)
        +handleSendBehaviorWarning(issue)
    }

    class StatisticPage {
        +fetchData()
        +setRange(range)
        +renderViolationStatistics()
        +renderBookingStatistics()
        +renderCheckInChart()
    }

    class AIAnalyticsPanel {
        +fetchData()
        +renderDensityTab()
        +renderUsageTab()
        +renderBehaviorTab()
        +renderCapacityTab()
    }

    class CheckInOutPage {
        +fetchData()
        +handleExportToExcel()
        +applySearchAndFilters()
        +subscribeAccessLogTopic()
    }

    class DashboardServiceClient {
        +getDashboardStats()
        +getChartStats(range)
        +getTopStudents(range)
    }

    class StatisticServiceClient {
        +getStatistics(range)
    }

    class AnalyticsServiceClient {
        +getDensityPrediction(zoneId, days)
        +getUsageStatistics(period)
        +getRealtimeCapacity()
        +getBehaviorSummary(days)
        +getBehaviorIssues(limit)
        +sendBehaviorWarning(userId, primaryIssue, detail)
    }

    class LibrarianServiceClient {
        +getAllAccessLogs()
        +getAccessLogStats()
        +getAccessLogsByDateRange(startDate, endDate)
    }

    DashboardController --> DashboardService
    StatisticController --> StatisticService
    AIAnalyticsProxyController --> AnalyticsServiceClient
    NotificationController --> AnalyticsServiceClient
    HCEController --> CheckInService
    DashboardPage --> DashboardServiceClient
    DashboardPage --> AnalyticsServiceClient
    DashboardPage --> LibrarianServiceClient
    StatisticPage --> StatisticServiceClient
    StatisticPage --> DashboardServiceClient
    StatisticPage --> AIAnalyticsPanel
    AIAnalyticsPanel --> AnalyticsServiceClient
    CheckInOutPage --> LibrarianServiceClient
    CheckInOutPage --> HCEController
```

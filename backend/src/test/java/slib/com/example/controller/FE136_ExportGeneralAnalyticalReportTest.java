package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.dashboard.StatisticController;
import slib.com.example.dto.dashboard.StatisticDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.dashboard.StatisticService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-136: Export general analytical report
 * Tests StatisticController.getStatistics() which provides data for analytical report export
 * Test Report: doc/Report/UnitTestReport/FE127_TestReport.md
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-136: Export general analytical report - Unit Tests")
class FE136_ExportGeneralAnalyticalReportTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @Test
    @DisplayName("UTCID01: Get full statistics for report export returns all sections")
    void getStatistics_fullData_returnsAllSections() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .overview(StatisticDTO.OverviewDTO.builder()
                        .totalCheckIns(500).totalBookings(300).totalViolations(20)
                        .totalFeedbacks(50).totalComplaints(10).build())
                .bookingAnalysis(StatisticDTO.BookingAnalysisDTO.builder()
                        .totalBookings(300).usedBookings(250).cancelledBookings(30).expiredNoShow(20)
                        .usedPercent(83.33).cancelledPercent(10.0).expiredPercent(6.67).build())
                .violationsByType(Collections.emptyList())
                .feedbackSummary(StatisticDTO.FeedbackSummaryDTO.builder()
                        .averageRating(4.2).totalCount(50)
                        .ratingDistribution(Collections.emptyList())
                        .recentFeedbacks(Collections.emptyList()).build())
                .zoneUsage(Collections.emptyList())
                .peakHours(Collections.emptyList())
                .insights(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("year")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "year"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview").exists())
                .andExpect(jsonPath("$.bookingAnalysis").exists())
                .andExpect(jsonPath("$.feedbackSummary").exists());
    }

    @Test
    @DisplayName("UTCID02: Get statistics with insights for report")
    void getStatistics_withInsights_returnsInsightsData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .insights(List.of(
                        StatisticDTO.InsightDTO.builder()
                                .type("peak").title("Khung gio cao diem")
                                .description("test").tone("orange").build()))
                .build();
        when(statisticService.getStatistics("month")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insights").isArray());
    }
}

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.StatisticController;
import slib.com.example.dto.StatisticDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.StatisticService;

import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for StatisticController
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("StatisticController Unit Tests")
class StatisticControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    // =========================================
    // === GET STATISTICS ===
    // =========================================

    @Test
    @DisplayName("getStatistics_defaultRange_week_returns200WithData")
    void getStatistics_defaultRange_week_returns200WithData() throws Exception {
        StatisticDTO.OverviewDTO overview = StatisticDTO.OverviewDTO.builder()
                .totalCheckIns(100L)
                .totalBookings(50L)
                .totalViolations(10L)
                .totalFeedbacks(20L)
                .totalComplaints(5L)
                .build();

        StatisticDTO dto = StatisticDTO.builder()
                .overview(overview)
                .bookingAnalysis(StatisticDTO.BookingAnalysisDTO.builder().build())
                .violationsByType(Collections.emptyList())
                .feedbackSummary(StatisticDTO.FeedbackSummaryDTO.builder()
                        .averageRating(4.5)
                        .totalCount(20L)
                        .ratingDistribution(Collections.emptyList())
                        .recentFeedbacks(Collections.emptyList())
                        .build())
                .zoneUsage(Collections.emptyList())
                .peakHours(Collections.emptyList())
                .build();

        when(statisticService.getStatistics("week")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalCheckIns").value(100))
                .andExpect(jsonPath("$.overview.totalBookings").value(50))
                .andExpect(jsonPath("$.overview.totalViolations").value(10))
                .andExpect(jsonPath("$.overview.totalFeedbacks").value(20))
                .andExpect(jsonPath("$.overview.totalComplaints").value(5));
    }

    @Test
    @DisplayName("getStatistics_monthRange_returns200WithData")
    void getStatistics_monthRange_returns200WithData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .overview(StatisticDTO.OverviewDTO.builder()
                        .totalCheckIns(500L)
                        .totalBookings(200L)
                        .totalViolations(30L)
                        .totalFeedbacks(50L)
                        .totalComplaints(15L)
                        .build())
                .bookingAnalysis(StatisticDTO.BookingAnalysisDTO.builder().build())
                .violationsByType(Collections.emptyList())
                .feedbackSummary(StatisticDTO.FeedbackSummaryDTO.builder().build())
                .zoneUsage(Collections.emptyList())
                .peakHours(Collections.emptyList())
                .build();

        when(statisticService.getStatistics("month")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics")
                .param("range", "month")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalCheckIns").value(500));
    }

    @Test
    @DisplayName("getStatistics_yearRange_returns200WithData")
    void getStatistics_yearRange_returns200WithData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .overview(StatisticDTO.OverviewDTO.builder()
                        .totalCheckIns(10000L)
                        .totalBookings(5000L)
                        .totalViolations(300L)
                        .totalFeedbacks(1000L)
                        .totalComplaints(200L)
                        .build())
                .bookingAnalysis(StatisticDTO.BookingAnalysisDTO.builder().build())
                .violationsByType(Collections.emptyList())
                .feedbackSummary(StatisticDTO.FeedbackSummaryDTO.builder().build())
                .zoneUsage(Collections.emptyList())
                .peakHours(Collections.emptyList())
                .build();

        when(statisticService.getStatistics("year")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics")
                .param("range", "year")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalCheckIns").value(10000));
    }

    @Test
    @DisplayName("getStatistics_invalidRange_usesDefaultWeek")
    void getStatistics_invalidRange_usesDefaultWeek() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .overview(StatisticDTO.OverviewDTO.builder().build())
                .bookingAnalysis(StatisticDTO.BookingAnalysisDTO.builder().build())
                .violationsByType(Collections.emptyList())
                .feedbackSummary(StatisticDTO.FeedbackSummaryDTO.builder().build())
                .zoneUsage(Collections.emptyList())
                .peakHours(Collections.emptyList())
                .build();

        // Should default to "week" when invalid range is passed
        when(statisticService.getStatistics("invalid")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics")
                .param("range", "invalid")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }
}

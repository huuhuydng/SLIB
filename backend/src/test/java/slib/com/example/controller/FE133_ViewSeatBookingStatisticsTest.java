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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-133: View seat booking statistics
 * Tests StatisticController.getStatistics() with focus on booking analysis and zone usage
 * Test Report: doc/Report/UnitTestReport/FE125_TestReport.md
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-133: View seat booking statistics - Unit Tests")
class FE133_ViewSeatBookingStatisticsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @Test
    @DisplayName("UTCID01: Get statistics returns booking analysis")
    void getStatistics_returnsBookingAnalysis() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .bookingAnalysis(StatisticDTO.BookingAnalysisDTO.builder()
                        .totalBookings(200).usedBookings(150).cancelledBookings(30).expiredNoShow(20)
                        .usedPercent(75.0).cancelledPercent(15.0).expiredPercent(10.0)
                        .build())
                .zoneUsage(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("week")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingAnalysis.totalBookings").value(200));
    }

    @Test
    @DisplayName("UTCID02: Get statistics returns zone usage data")
    void getStatistics_returnsZoneUsage() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .zoneUsage(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("month")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "month"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zoneUsage").isArray());
    }
}

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
 * Unit Tests for FE-139: View violation statistics
 * Tests StatisticController.getStatistics() with focus on violation data
 * Test Report: doc/Report/UnitTestReport/FE122_TestReport.md
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-139: View violation statistics - Unit Tests")
class FE139_ViewViolationStatisticsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @Test
    @DisplayName("UTCID01: Get statistics with default range returns violation data")
    void getStatistics_defaultRange_returnsViolationData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .violationsByType(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("week")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UTCID02: Get statistics with month range returns 200")
    void getStatistics_monthRange_returns200() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .violationsByType(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("month")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "month"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("UTCID03: Get statistics with year range returns 200")
    void getStatistics_yearRange_returns200() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .violationsByType(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("year")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "year"))
                .andExpect(status().isOk());
    }
}

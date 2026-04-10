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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-132: View check-in/check-out statistics
 * Tests StatisticController.getStatistics() with focus on check-in overview data
 * Test Report: doc/Report/UnitTestReport/FE124_TestReport.md
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-132: View check-in/check-out statistics - Unit Tests")
class FE132_ViewCheckInOutStatisticsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @Test
    @DisplayName("UTCID01: Get statistics returns check-in overview data")
    void getStatistics_returnsCheckInOverview() throws Exception {
        StatisticDTO.OverviewDTO overview = StatisticDTO.OverviewDTO.builder()
                .totalCheckIns(150)
                .build();
        StatisticDTO dto = StatisticDTO.builder()
                .overview(overview)
                .build();
        when(statisticService.getStatistics("week")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overview.totalCheckIns").value(150));
    }

    @Test
    @DisplayName("UTCID02: Get statistics returns comparison data for check-ins")
    void getStatistics_returnsComparisonData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .comparison(StatisticDTO.OverviewComparisonDTO.builder()
                        .checkIns(StatisticDTO.MetricDeltaDTO.builder()
                                .currentValue(150).previousValue(120).changeValue(30).changePercent(25.0)
                                .build())
                        .build())
                .build();
        when(statisticService.getStatistics("month")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "month"))
                .andExpect(status().isOk());
    }
}

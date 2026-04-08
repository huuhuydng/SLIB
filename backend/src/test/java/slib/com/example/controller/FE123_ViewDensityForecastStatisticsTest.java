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
 * Unit Tests for FE-123: View statistics of density forecast by using AI
 * Tests StatisticController.getStatistics() with focus on peak hours / density data
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-123: View density forecast statistics - Unit Tests")
class FE123_ViewDensityForecastStatisticsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StatisticService statisticService;

    @Test
    @DisplayName("UTCID01: Get statistics returns peak hours density data")
    void getStatistics_returnsPeakHoursData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .peakHours(List.of(
                        StatisticDTO.PeakHourDTO.builder().hour(9).count(25).build(),
                        StatisticDTO.PeakHourDTO.builder().hour(14).count(40).build()))
                .build();
        when(statisticService.getStatistics("week")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.peakHours").isArray());
    }

    @Test
    @DisplayName("UTCID02: Get statistics with day range returns density data")
    void getStatistics_dayRange_returnsDensityData() throws Exception {
        StatisticDTO dto = StatisticDTO.builder()
                .peakHours(Collections.emptyList())
                .build();
        when(statisticService.getStatistics("day")).thenReturn(dto);

        mockMvc.perform(get("/slib/statistics").param("range", "day"))
                .andExpect(status().isOk());
    }
}

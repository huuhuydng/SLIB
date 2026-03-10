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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.StatisticService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-122: View Violation Statistics
 * Test Report: doc/Report/FE121_TestReport.md
 */
@WebMvcTest(value = StatisticController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-122: View Violation Statistics - Unit Tests")
class FE122_ViolationStatisticsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private StatisticService statisticService;

        @Test
        @DisplayName("UTCD01: View violation statistics returns 200 OK")
        void viewViolationStats_validToken_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/statistics/violations"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: View violation statistics without token returns 401")
        void viewViolationStats_noToken_returns401() throws Exception {
                mockMvc.perform(get("/slib/statistics/violations"))
                        .andExpect(status().isUnauthorized());
        }
}

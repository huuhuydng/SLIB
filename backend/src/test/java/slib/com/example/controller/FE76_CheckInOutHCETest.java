package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.hce.HCEController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.hce.CheckInService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-76: Check-in/Check-out library via HCE
 * Test Report: doc/Report/FE76_TestReport.md
 */
@WebMvcTest(value = HCEController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = "gate.secret=TEST_SECRET_KEY")
@DisplayName("FE-76: Check-in/Check-out library via HCE - Unit Tests")
class FE76_CheckInOutHCETest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CheckInService hceService;

        @Test
        @DisplayName("UTCD01: Check-in via HCE with valid API key returns 200 OK")
        void checkInHCE_validApiKey_returns200OK() throws Exception {
                when(hceService.processCheckIn(any())).thenReturn(Map.of("status", "SUCCESS"));

                mockMvc.perform(post("/slib/hce/checkin")
                                .header("X-API-KEY", "TEST_SECRET_KEY")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"hce-token-123\",\"gateId\":\"gate-1\"}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Check-in without API key returns 403 Forbidden")
        void checkInHCE_noApiKey_returns403() throws Exception {
                mockMvc.perform(post("/slib/hce/checkin")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"hce-token-123\",\"gateId\":\"gate-1\"}"))
                        .andExpect(status().isForbidden());
        }
}

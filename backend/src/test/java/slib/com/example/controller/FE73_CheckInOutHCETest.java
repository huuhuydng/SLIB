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
import slib.com.example.service.HCEService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-73: Check-in/out via HCE
 * Test Report: doc/Report/FE73_TestReport.md
 */
@WebMvcTest(value = HCEController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-73: Check-in/out via HCE - Unit Tests")
class FE73_CheckInOutHCETest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private HCEService hceService;

        @Test
        @DisplayName("UTCD01: Check-in via HCE returns 200 OK")
        void checkInHCE_validToken_returns200OK() throws Exception {
                mockMvc.perform(post("/slib/hce/check-in")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"token\":\"hce-token-123\"}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Check-in without token returns 401")
        void checkInHCE_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/hce/check-in"))
                        .andExpect(status().isUnauthorized());
        }
}

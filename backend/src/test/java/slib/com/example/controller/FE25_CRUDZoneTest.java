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
import slib.com.example.service.ZoneService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-25: CRUD Zone
 * Test Report: doc/Report/FE25_TestReport.md
 */
@WebMvcTest(value = ZoneController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-25: CRUD Zone - Unit Tests")
class FE25_CRUDZoneTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ZoneService zoneService;

        // UTCD01: Create zone - Success
        @Test
        @DisplayName("UTCD01: Create zone with valid data returns 200 OK")
        void createZone_validData_returns200OK() throws Exception {
                mockMvc.perform(post("/slib/zones")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"Zone A\",\"areaId\":\"" + UUID.randomUUID() + "\"}"))
                        .andExpect(status().isOk());
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: Create zone without token returns 401 Unauthorized")
        void createZone_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/zones"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD04: Invalid data - 400
        @Test
        @DisplayName("UTCD04: Create zone with invalid data returns 400 Bad Request")
        void createZone_invalidData_returns400() throws Exception {
                mockMvc.perform(post("/slib/zones")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"\"}"))
                        .andExpect(status().isBadRequest());
        }
}

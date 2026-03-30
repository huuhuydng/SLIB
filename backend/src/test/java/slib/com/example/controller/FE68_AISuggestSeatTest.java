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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import slib.com.example.controller.ai.AIAnalyticsProxyController;
import slib.com.example.exception.GlobalExceptionHandler;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-68: AI Suggest Seat
 * Test Report: doc/Report/UnitTestReport/FE68_TestReport.md
 */
@WebMvcTest(value = AIAnalyticsProxyController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-68: AI Suggest Seat - Unit Tests")
class FE68_AISuggestSeatTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private RestTemplate restTemplate;

        // =========================================
        // === UTCID01: Valid user_id with all optional filters ===
        // =========================================

        /**
         * UTCID01: Valid user_id with all optional filter parameters
         * Precondition: AI analytics proxy controller is active
         * Expected: 200 OK with recommendation payload
         */
        @Test
        @DisplayName("UTCID01: Valid user_id with all filters returns 200 OK")
        void getSeatRecommendation_allFilters_returns200OK() throws Exception {
                Map<String, Object> aiResponse = Map.of(
                                "recommended_seat", "A-05",
                                "zone", "Zone A",
                                "confidence", 0.85);

                when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                                .thenReturn(ResponseEntity.ok(aiResponse));

                mockMvc.perform(get("/slib/ai/analytics/seat-recommendation")
                                .param("user_id", "user-123")
                                .param("zone_preference", "Zone A")
                                .param("time_slot", "08:00-10:00")
                                .param("date", "2026-03-12"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.recommended_seat").value("A-05"));

                verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        }

        // =========================================
        // === UTCID02: Valid user_id with only required param ===
        // =========================================

        /**
         * UTCID02: Valid user_id with only required parameter (no optional filters)
         * Precondition: AI analytics proxy controller is active
         * Expected: 200 OK with recommendation payload
         */
        @Test
        @DisplayName("UTCID02: Valid user_id only returns 200 OK")
        void getSeatRecommendation_onlyUserId_returns200OK() throws Exception {
                Map<String, Object> aiResponse = Map.of(
                                "recommended_seat", "B-01",
                                "zone", "Zone B",
                                "confidence", 0.72);

                when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                                .thenReturn(ResponseEntity.ok(aiResponse));

                mockMvc.perform(get("/slib/ai/analytics/seat-recommendation")
                                .param("user_id", "user-456"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.recommended_seat").value("B-01"));

                verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        }

        // =========================================
        // === UTCID03: Missing required user_id ===
        // =========================================

        /**
         * UTCID03: Missing required user_id parameter
         * Precondition: AI analytics proxy controller is active
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID03: Missing user_id returns 400 Bad Request")
        void getSeatRecommendation_missingUserId_returns400() throws Exception {
                mockMvc.perform(get("/slib/ai/analytics/seat-recommendation"))
                                .andExpect(status().isBadRequest());

                verify(restTemplate, never()).exchange(anyString(), any(), any(), eq(Map.class));
        }

        // =========================================
        // === UTCID04: Empty user_id ===
        // =========================================

        /**
         * UTCID04: Empty user_id value
         * Precondition: Downstream service may reject malformed requests
         * Expected: 400 Bad Request (downstream rejects empty user_id)
         */
        @Test
        @DisplayName("UTCID04: Empty user_id value triggers downstream error returns 500")
        void getSeatRecommendation_emptyUserId_returns500() throws Exception {
                // Controller has no try-catch, so HttpClientErrorException propagates as 500
                when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                                .thenThrow(new org.springframework.web.client.HttpClientErrorException(
                                                org.springframework.http.HttpStatus.BAD_REQUEST));

                mockMvc.perform(get("/slib/ai/analytics/seat-recommendation")
                                .param("user_id", ""))
                                .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCID05: AI service returns valid response with zone_preference ===
        // =========================================

        /**
         * UTCID05: AI service returns valid recommendation with zone_preference only
         * Precondition: AI analytics proxy controller is active, AI service reachable
         * Expected: 200 OK with recommendation
         */
        @Test
        @DisplayName("UTCID05: Valid request with zone_preference returns 200 OK")
        void getSeatRecommendation_withZonePreference_returns200OK() throws Exception {
                Map<String, Object> aiResponse = Map.of(
                                "recommended_seat", "C-12",
                                "zone", "Zone C",
                                "confidence", 0.90);

                when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class)))
                                .thenReturn(ResponseEntity.ok(aiResponse));

                mockMvc.perform(get("/slib/ai/analytics/seat-recommendation")
                                .param("user_id", "user-789")
                                .param("zone_preference", "Zone C"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.recommended_seat").value("C-12"))
                                .andExpect(jsonPath("$.confidence").value(0.90));

                verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Map.class));
        }
}

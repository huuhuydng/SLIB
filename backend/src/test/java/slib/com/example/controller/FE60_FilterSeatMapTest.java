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
import slib.com.example.controller.zone_config.SeatController;
import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.entity.zone_config.SeatStatus;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.BookingService;
import slib.com.example.service.SeatService;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-60: Filter Seat Map
 * Test Report: doc/Report/UnitTestReport/FE60_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-60: Filter Seat Map - Unit Tests")
class FE60_FilterSeatMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private SeatService seatService;

        // =========================================
        // === UTCID01: Filter by zone/date/time - valid ===
        // =========================================

        /**
         * UTCID01: Valid zone/date/time filter combination
         * Precondition: Filter parameters are provided
         * Expected: 200 OK with filtered seat list
         */
        @Test
        @DisplayName("UTCID01: Filter seats by zone/date/time returns 200 OK")
        void getSeats_validFilter_returns200OK() throws Exception {
                SeatResponse seat = new SeatResponse();
                seat.setSeatId(1);
                seat.setSeatStatus(SeatStatus.AVAILABLE);

                when(seatService.getSeatsByTimeRange(anyString(), anyString(), eq(1)))
                                .thenReturn(List.of(seat));

                mockMvc.perform(get("/slib/seats")
                                .param("zoneId", "1")
                                .param("startTime", "2026-03-12T08:00:00")
                                .param("endTime", "2026-03-12T10:00:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].seatId").value(1));

                verify(seatService, times(1)).getSeatsByTimeRange(anyString(), anyString(), eq(1));
        }

        // =========================================
        // === UTCID02: Filter by different zone ===
        // =========================================

        /**
         * UTCID02: Filter by different zone with time range
         * Precondition: Filter parameters are provided
         * Expected: 200 OK
         */
        @Test
        @DisplayName("UTCID02: Filter by different zone returns 200 OK")
        void getSeats_differentZone_returns200OK() throws Exception {
                SeatResponse seat = new SeatResponse();
                seat.setSeatId(10);
                seat.setSeatStatus(SeatStatus.BOOKED);

                when(seatService.getSeatsByTimeRange(anyString(), anyString(), eq(5)))
                                .thenReturn(List.of(seat));

                mockMvc.perform(get("/slib/seats")
                                .param("zoneId", "5")
                                .param("startTime", "2026-03-12T14:00:00")
                                .param("endTime", "2026-03-12T16:00:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].seatId").value(10));

                verify(seatService, times(1)).getSeatsByTimeRange(anyString(), anyString(), eq(5));
        }

        // =========================================
        // === UTCID03: Filter returns empty result ===
        // =========================================

        /**
         * UTCID03: Filter returns empty result for valid parameters
         * Precondition: Filter parameters are provided
         * Expected: 200 OK with empty list
         */
        @Test
        @DisplayName("UTCID03: Filter with valid params but no matches returns 200 OK")
        void getSeats_noMatches_returns200OK() throws Exception {
                when(seatService.getSeatsByTimeRange(anyString(), anyString(), eq(99)))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/seats")
                                .param("zoneId", "99")
                                .param("startTime", "2026-03-12T08:00:00")
                                .param("endTime", "2026-03-12T10:00:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isEmpty());
        }

        // =========================================
        // === UTCID04: Invalid filter values ===
        // =========================================

        /**
         * UTCID04: Invalid or missing filter values break lookup
         * Precondition: Filter parameters are provided
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID04: Invalid filter values returns 400 Bad Request")
        void getSeats_invalidFilter_returns400() throws Exception {
                when(seatService.getSeatsByTimeRange(eq("invalid-date"), anyString(), any()))
                                .thenThrow(new RuntimeException("Invalid date format"));

                mockMvc.perform(get("/slib/seats")
                                .param("startTime", "invalid-date")
                                .param("endTime", "2026-03-12T10:00:00"))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID05: Missing time range parameters ===
        // =========================================

        /**
         * UTCID05: Missing time range parameters - service throws on bad time range
         * Precondition: Filter parameters provided but service fails
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID05: Service exception during time range lookup returns 400 Bad Request")
        void getSeats_serviceException_returns400() throws Exception {
                when(seatService.getSeatsByTimeRange(anyString(), anyString(), any()))
                                .thenThrow(new RuntimeException("End time must be after start time"));

                mockMvc.perform(get("/slib/seats")
                                .param("startTime", "2026-03-12T10:00:00")
                                .param("endTime", "2026-03-12T08:00:00"))
                                .andExpect(status().isBadRequest());
        }
}

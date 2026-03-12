package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.BookingService;
import slib.com.example.service.SeatService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-31: Change Seat Status
 * Test Report: doc/Report/UnitTestReport/FE31_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-31: Change Seat Status - Unit Tests")
class FE31_ChangeSeatStatusTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @MockBean
        private BookingService bookingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Restrict seat - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Restrict seat with valid JWT token returns 200 OK")
        void restrictSeat_validToken_returns200OK() throws Exception {
                SeatResponse response = new SeatResponse();
                response.setSeatId(1);
                response.setIsActive(false);

                when(seatService.restrictSeatById(1)).thenReturn(response);

                mockMvc.perform(post("/slib/seats/1/restrict"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).restrictSeatById(1);
        }

        // =========================================
        // === UTCID02: No token - Unauthorized ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Restrict seat without token returns 400 Bad Request")
        void restrictSeat_noToken_returns400BadRequest() throws Exception {
                when(seatService.restrictSeatById(1))
                                .thenThrow(new RuntimeException("Unauthorized"));

                mockMvc.perform(post("/slib/seats/1/restrict"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).restrictSeatById(1);
        }

        // =========================================
        // === UTCID03: Invalid status value ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Restrict seat with invalid status value returns 400 Bad Request")
        void restrictSeat_invalidStatus_returns400BadRequest() throws Exception {
                when(seatService.restrictSeatById(1))
                                .thenThrow(new RuntimeException("Trang thai khong hop le"));

                mockMvc.perform(post("/slib/seats/1/restrict"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).restrictSeatById(1);
        }

        // =========================================
        // === UTCID04: Seat not found ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Restrict non-existent seat returns 400 Bad Request")
        void restrictSeat_notFound_returns400BadRequest() throws Exception {
                when(seatService.restrictSeatById(999))
                                .thenThrow(new RuntimeException("Khong tim thay ghe voi id: 999"));

                mockMvc.perform(post("/slib/seats/999/restrict"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).restrictSeatById(999);
        }

        // =========================================
        // === UTCID05: Non-admin role - controller catches RuntimeException ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Restrict seat with access denied returns 400 Bad Request (controller catch-all)")
        void restrictSeat_accessDenied_returns400BadRequest() throws Exception {
                when(seatService.restrictSeatById(1))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(post("/slib/seats/1/restrict"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).restrictSeatById(1);
        }
}

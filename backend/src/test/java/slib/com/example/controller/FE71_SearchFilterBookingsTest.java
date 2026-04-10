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
import slib.com.example.controller.booking.BookingController;
import slib.com.example.dto.booking.BookingResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-71: Search and Filter user booking
 * Test Report: doc/Report/UnitTestReport/FE70_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-71: Search and Filter user booking - Unit Tests")
class FE71_SearchFilterBookingsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @MockBean
        private ReservationRepository reservationRepository;

        @MockBean
        private UserRepository userRepository;

        // =========================================
        // === UTCID01: Get all bookings - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all bookings returns 200 OK with booking list")
        void getAllBookings_withData_returns200OK() throws Exception {
                BookingResponse reservation1 = new BookingResponse();
                BookingResponse reservation2 = new BookingResponse();

                when(bookingService.getAllBookings()).thenReturn(List.of(reservation1, reservation2));

                mockMvc.perform(get("/slib/bookings/getall"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(bookingService, times(1)).getAllBookings();
        }

        // =========================================
        // === UTCID02: Get all bookings with single record - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get all bookings with single record returns 200 OK")
        void getAllBookings_singleRecord_returns200OK() throws Exception {
                BookingResponse reservation = new BookingResponse();

                when(bookingService.getAllBookings()).thenReturn(List.of(reservation));

                mockMvc.perform(get("/slib/bookings/getall"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(1));

                verify(bookingService, times(1)).getAllBookings();
        }

        // =========================================
        // === UTCID03: Get all bookings with many records - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get all bookings with multiple records returns 200 OK")
        void getAllBookings_multipleRecords_returns200OK() throws Exception {
                BookingResponse r1 = new BookingResponse();
                BookingResponse r2 = new BookingResponse();
                BookingResponse r3 = new BookingResponse();

                when(bookingService.getAllBookings()).thenReturn(List.of(r1, r2, r3));

                mockMvc.perform(get("/slib/bookings/getall"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(3));

                verify(bookingService, times(1)).getAllBookings();
        }

        // =========================================
        // === UTCID04: Get all bookings returns empty list - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get all bookings with no data returns 200 OK with empty list")
        void getAllBookings_noData_returns200OKEmptyList() throws Exception {
                when(bookingService.getAllBookings()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/bookings/getall"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(bookingService, times(1)).getAllBookings();
        }

        // =========================================
        // === UTCID05: Service throws exception - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get all bookings when service fails returns 500 Internal Server Error")
        void getAllBookings_serviceFails_returns500InternalServerError() throws Exception {
                when(bookingService.getAllBookings()).thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/bookings/getall"))
                                .andExpect(status().isInternalServerError());

                verify(bookingService, times(1)).getAllBookings();
        }
}

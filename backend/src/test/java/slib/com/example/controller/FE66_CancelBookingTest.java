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
import slib.com.example.service.BookingService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-66: Cancel Booking
 * Test Report: doc/Report/FE59_TestReport.md
 */
@WebMvcTest(value = BookingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-66: Cancel Booking - Unit Tests")
class FE66_CancelBookingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BookingService bookingService;

        @Test
        @DisplayName("UTCD01: Cancel booking returns 200 OK")
        void cancelBooking_validRequest_returns200OK() throws Exception {
                mockMvc.perform(delete("/slib/bookings/" + UUID.randomUUID()))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Cancel without token returns 401")
        void cancelBooking_noToken_returns401() throws Exception {
                mockMvc.perform(delete("/slib/bookings/123"))
                        .andExpect(status().isUnauthorized());
        }
}

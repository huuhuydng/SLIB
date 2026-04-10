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
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.SeatService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.zone_config.SeatController;

/**
 * Unit Tests for FE-31: View seat map
 * Test Report: doc/Report/FE31_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-31: View seat map - Unit Tests")
class FE31_ViewSeatMapTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @MockBean
        private BookingService bookingService;

        @Test
        @DisplayName("UTCD01: View seat map returns 200 OK")
        void viewSeatMap_validToken_returns200OK() throws Exception {
                when(seatService.getAllSeats()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/seats"))
                        .andExpect(status().isOk());

                verify(seatService, times(1)).getAllSeats();
        }

        @Test
        @DisplayName("UTCD02: View seat by non-existent ID returns 404 Not Found")
        void viewSeatMap_nonExistentId_returns404() throws Exception {
                when(seatService.getSeatById(999))
                        .thenThrow(new ResourceNotFoundException("Seat not found with id: 999"));

                mockMvc.perform(get("/slib/seats/999"))
                        .andExpect(status().isNotFound());

                verify(seatService, times(1)).getSeatById(999);
        }
}

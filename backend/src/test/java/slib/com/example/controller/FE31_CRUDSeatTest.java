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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.zone_config.SeatController;
import slib.com.example.dto.zone_config.SeatResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.BookingService;
import slib.com.example.service.SeatService;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-31: CRUD Seat
 * Test Report: doc/Report/UnitTestReport/FE31_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-31: CRUD Seat - Unit Tests")
class FE31_CRUDSeatTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @MockBean
        private BookingService bookingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Get all seats - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all seats with valid JWT token returns 200 OK")
        void getAllSeats_validToken_returns200OK() throws Exception {
                SeatResponse seat = new SeatResponse();
                seat.setSeatId(1);
                seat.setSeatCode("A01");

                when(seatService.getAllSeats()).thenReturn(List.of(seat));

                mockMvc.perform(get("/slib/seats"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).getAllSeats();
        }

        // =========================================
        // === UTCID02: Service error - Bad Request (controller catch-all) ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get seats with service error returns 400 Bad Request")
        void getAllSeats_serviceError_returns400BadRequest() throws Exception {
                when(seatService.getAllSeats())
                                .thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/seats"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).getAllSeats();
        }

        // =========================================
        // === UTCID03: Service throws AccessDeniedException - caught by controller ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get seats with access denied returns 400 Bad Request (controller catch-all)")
        void getAllSeats_accessDenied_returns400BadRequest() throws Exception {
                when(seatService.getAllSeats())
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(get("/slib/seats"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).getAllSeats();
        }

        // =========================================
        // === UTCID04: Create seat - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Create seat with valid data returns 200 OK")
        void createSeat_validData_returns200OK() throws Exception {
                SeatResponse request = new SeatResponse();
                request.setSeatCode("B01");
                request.setZoneId(1);
                request.setRowNumber(1);
                request.setColumnNumber(1);

                SeatResponse response = new SeatResponse();
                response.setSeatId(10);
                response.setSeatCode("B01");
                response.setZoneId(1);

                when(seatService.createSeat(any(SeatResponse.class))).thenReturn(response);

                mockMvc.perform(post("/slib/seats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.seatCode").value("B01"));

                verify(seatService, times(1)).createSeat(any(SeatResponse.class));
        }

        // =========================================
        // === UTCID05: Create seat - Duplicate code ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Create seat with duplicate seat code returns 400 Bad Request")
        void createSeat_duplicateCode_returns400BadRequest() throws Exception {
                SeatResponse request = new SeatResponse();
                request.setSeatCode("A01");
                request.setZoneId(1);

                when(seatService.createSeat(any(SeatResponse.class)))
                                .thenThrow(new slib.com.example.exception.BadRequestException(
                                                "Ma ghe da ton tai"));

                mockMvc.perform(post("/slib/seats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).createSeat(any(SeatResponse.class));
        }

        // =========================================
        // === UTCID06: Update seat - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Update seat with valid data returns 200 OK")
        void updateSeat_validData_returnsOK() throws Exception {
                SeatResponse request = new SeatResponse();
                request.setSeatCode("C01");

                SeatResponse response = new SeatResponse();
                response.setSeatId(999);
                response.setSeatCode("C01");

                when(seatService.updateSeat(eq(999), any(SeatResponse.class))).thenReturn(response);

                mockMvc.perform(put("/slib/seats/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).updateSeat(eq(999), any(SeatResponse.class));
        }

        // =========================================
        // === UTCID07: Delete seat - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Delete seat returns 200 OK")
        void deleteSeat_returns200OK() throws Exception {
                doNothing().when(seatService).deleteSeat(1);

                mockMvc.perform(delete("/slib/seats/1"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).deleteSeat(1);
        }

        // =========================================
        // === UTCID08: Service throws AccessDeniedException on create ===
        // =========================================

        @Test
        @DisplayName("UTCID08: Create seat with access denied returns 403 Forbidden")
        void crudSeat_accessDenied_returns403Forbidden() throws Exception {
                SeatResponse request = new SeatResponse();
                request.setSeatCode("D01");

                when(seatService.createSeat(any(SeatResponse.class)))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(post("/slib/seats")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());

                verify(seatService, times(1)).createSeat(any(SeatResponse.class));
        }
}

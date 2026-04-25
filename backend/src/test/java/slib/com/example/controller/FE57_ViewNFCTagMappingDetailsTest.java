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
import slib.com.example.dto.zone_config.NfcInfoResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.SeatService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-57: View NFC Tag mapping details
 * Test Report: doc/Report/UnitTestReport/FE50_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-57: View NFC Tag mapping details - Unit Tests")
class FE57_ViewNFCTagMappingDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @MockBean
        private BookingService bookingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Seat has an NFC mapping ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get NFC info for seat with NFC mapping returns 200 OK")
        void getNfcInfo_seatWithNfc_returns200OK() throws Exception {
                NfcInfoResponse mockInfo = NfcInfoResponse.builder()
                                .seatId(1).seatCode("A-01").zoneId(1).zoneName("Zone A")
                                .nfcMapped(true).nfcUidMasked("****3C91")
                                .lastUpdated(LocalDateTime.now())
                                .build();

                when(seatService.getNfcInfo(1)).thenReturn(mockInfo);

                mockMvc.perform(get("/slib/seats/1/nfc-info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.seatId").value(1))
                                .andExpect(jsonPath("$.nfcMapped").value(true));

                verify(seatService, times(1)).getNfcInfo(1);
        }

        // =========================================
        // === UTCID02: Seat does not have an NFC mapping ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get NFC info for seat without NFC mapping returns 200 OK")
        void getNfcInfo_seatWithoutNfc_returns200OK() throws Exception {
                NfcInfoResponse mockInfo = NfcInfoResponse.builder()
                                .seatId(2).seatCode("A-02").zoneId(1).zoneName("Zone A")
                                .nfcMapped(false)
                                .build();

                when(seatService.getNfcInfo(2)).thenReturn(mockInfo);

                mockMvc.perform(get("/slib/seats/2/nfc-info"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.seatId").value(2))
                                .andExpect(jsonPath("$.nfcMapped").value(false));

                verify(seatService, times(1)).getNfcInfo(2);
        }

        // =========================================
        // === UTCID03: Seat id does not exist ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get NFC info for non-existent seat returns 400 Bad Request")
        void getNfcInfo_nonExistentSeat_returns400() throws Exception {
                when(seatService.getNfcInfo(999))
                                .thenThrow(new RuntimeException("Seat not found with id: 999"));

                mockMvc.perform(get("/slib/seats/999/nfc-info"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(seatService, times(1)).getNfcInfo(999);
        }

        // =========================================
        // === UTCID04: Seat id/path is invalid ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get NFC info with invalid seat id returns 400 Bad Request")
        void getNfcInfo_invalidSeatId_returns400() throws Exception {
                mockMvc.perform(get("/slib/seats/abc/nfc-info"))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID05: Unexpected service failure ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get NFC info when service fails returns 400 Bad Request")
        void getNfcInfo_serviceFailure_returns400() throws Exception {
                when(seatService.getNfcInfo(1))
                                .thenThrow(new RuntimeException("Unexpected database error"));

                mockMvc.perform(get("/slib/seats/1/nfc-info"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(seatService, times(1)).getNfcInfo(1);
        }
}

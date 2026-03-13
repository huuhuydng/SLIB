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

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-48: Manage NFC Tag UID Mapping
 * Test Report: doc/Report/UnitTestReport/FE48_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-48: Manage NFC Tag UID Mapping - Unit Tests")
class FE48_ManageNFCTagUIDMappingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @MockBean
        private BookingService bookingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Assign new UID to unmapped seat ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Assign a new UID to an unmapped seat returns 200 OK")
        void updateSeatNfcUid_newUid_returns200OK() throws Exception {
                SeatResponse mockResponse = new SeatResponse();
                mockResponse.setSeatId(1);
                mockResponse.setSeatCode("A-01");

                when(seatService.updateNfcTagUid(eq(1), eq("04A23C91")))
                                .thenReturn(mockResponse);

                mockMvc.perform(put("/slib/seats/1/nfc-uid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("nfcTagUid", "04A23C91"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.seatId").value(1));

                verify(seatService, times(1)).updateNfcTagUid(eq(1), eq("04A23C91"));
        }

        // =========================================
        // === UTCID02: Replace existing UID mapping ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Replace an existing UID mapping returns 200 OK")
        void updateSeatNfcUid_replaceExisting_returns200OK() throws Exception {
                SeatResponse mockResponse = new SeatResponse();
                mockResponse.setSeatId(1);

                when(seatService.updateNfcTagUid(eq(1), eq("NEWUID123")))
                                .thenReturn(mockResponse);

                mockMvc.perform(put("/slib/seats/1/nfc-uid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("nfcTagUid", "NEWUID123"))))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).updateNfcTagUid(eq(1), eq("NEWUID123"));
        }

        // =========================================
        // === UTCID03: Clear seat NFC UID ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Clear the seat NFC UID returns 200 OK")
        void clearSeatNfcUid_existingSeat_returns200OK() throws Exception {
                SeatResponse mockResponse = new SeatResponse();
                mockResponse.setSeatId(1);

                when(seatService.clearNfcTagUid(1)).thenReturn(mockResponse);

                mockMvc.perform(delete("/slib/seats/1/nfc-uid"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).clearNfcTagUid(1);
        }

        // =========================================
        // === UTCID04: UID already assigned to another seat ===
        // =========================================

        @Test
        @DisplayName("UTCID04: UID already assigned to another seat returns 400 Bad Request")
        void updateSeatNfcUid_duplicateUid_returns400() throws Exception {
                when(seatService.updateNfcTagUid(eq(2), eq("04A23C91")))
                                .thenThrow(new RuntimeException("UID da duoc gan cho ghe khac"));

                mockMvc.perform(put("/slib/seats/2/nfc-uid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("nfcTagUid", "04A23C91"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(seatService, times(1)).updateNfcTagUid(eq(2), eq("04A23C91"));
        }

        // =========================================
        // === UTCID05: Seat id does not exist (PUT) ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Update NFC UID for non-existent seat returns 400 Bad Request")
        void updateSeatNfcUid_nonExistentSeat_returns400() throws Exception {
                when(seatService.updateNfcTagUid(eq(999), eq("SOMEUID")))
                                .thenThrow(new RuntimeException("Seat not found with id: 999"));

                mockMvc.perform(put("/slib/seats/999/nfc-uid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("nfcTagUid", "SOMEUID"))))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(seatService, times(1)).updateNfcTagUid(eq(999), eq("SOMEUID"));
        }

        // =========================================
        // === UTCID06: Seat id does not exist (DELETE) ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Clear NFC UID for non-existent seat returns 400 Bad Request")
        void clearSeatNfcUid_nonExistentSeat_returns400() throws Exception {
                when(seatService.clearNfcTagUid(999))
                                .thenThrow(new RuntimeException("Seat not found with id: 999"));

                mockMvc.perform(delete("/slib/seats/999/nfc-uid"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(seatService, times(1)).clearNfcTagUid(999);
        }

        // =========================================
        // === UTCID07: Missing nfcTagUid in request body ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Request body missing nfcTagUid returns 400 Bad Request")
        void updateSeatNfcUid_missingUid_returns400() throws Exception {
                when(seatService.updateNfcTagUid(eq(1), isNull()))
                                .thenThrow(new RuntimeException("NFC UID khong duoc de trong"));

                mockMvc.perform(put("/slib/seats/1/nfc-uid")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                                .andExpect(status().isBadRequest());

                verify(seatService, times(1)).updateNfcTagUid(eq(1), isNull());
        }
}

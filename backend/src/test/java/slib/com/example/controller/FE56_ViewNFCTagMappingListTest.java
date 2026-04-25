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
import slib.com.example.dto.zone_config.NfcMappingResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.booking.BookingService;
import slib.com.example.service.zone_config.SeatService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-56: View NFC Tag mapping list
 * Test Report: doc/Report/UnitTestReport/FE49_TestReport.md
 */
@WebMvcTest(value = SeatController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-56: View NFC Tag mapping list - Unit Tests")
class FE56_ViewNFCTagMappingListTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SeatService seatService;

        @MockBean
        private BookingService bookingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Request without filters ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get NFC mappings without filters returns 200 OK")
        void getNfcMappings_noFilters_returns200OK() throws Exception {
                List<NfcMappingResponse> mockMappings = List.of(
                                NfcMappingResponse.builder()
                                                .seatId(1).seatCode("A-01").hasNfcTag(true)
                                                .maskedNfcUid("****3C91").updatedAt(LocalDateTime.now())
                                                .build(),
                                NfcMappingResponse.builder()
                                                .seatId(2).seatCode("A-02").hasNfcTag(false)
                                                .build());

                when(seatService.getNfcMappings(isNull(), isNull(), isNull(), isNull()))
                                .thenReturn(mockMappings);

                mockMvc.perform(get("/slib/seats/nfc-mappings"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).getNfcMappings(isNull(), isNull(), isNull(), isNull());
        }

        // =========================================
        // === UTCID02: Request with hasNfc and search filters ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get NFC mappings with hasNfc and search filters returns 200 OK")
        void getNfcMappings_withHasNfcAndSearch_returns200OK() throws Exception {
                List<NfcMappingResponse> mockMappings = List.of(
                                NfcMappingResponse.builder()
                                                .seatId(1).seatCode("A-01").hasNfcTag(true)
                                                .maskedNfcUid("****3C91")
                                                .build());

                when(seatService.getNfcMappings(isNull(), isNull(), eq(true), eq("A-01")))
                                .thenReturn(mockMappings);

                mockMvc.perform(get("/slib/seats/nfc-mappings")
                                .param("hasNfc", "true")
                                .param("search", "A-01"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).getNfcMappings(isNull(), isNull(), eq(true), eq("A-01"));
        }

        // =========================================
        // === UTCID03: Request with zoneId and areaId filters ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get NFC mappings with zoneId and areaId filters returns 200 OK")
        void getNfcMappings_withZoneAndAreaFilter_returns200OK() throws Exception {
                List<NfcMappingResponse> mockMappings = List.of(
                                NfcMappingResponse.builder()
                                                .seatId(3).seatCode("B-01").zoneId(1).areaId(2L)
                                                .hasNfcTag(true)
                                                .build());

                when(seatService.getNfcMappings(eq(1), eq(2), isNull(), isNull()))
                                .thenReturn(mockMappings);

                mockMvc.perform(get("/slib/seats/nfc-mappings")
                                .param("zoneId", "1")
                                .param("areaId", "2"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).getNfcMappings(eq(1), eq(2), isNull(), isNull());
        }

        // =========================================
        // === UTCID04: No seats match filter ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get NFC mappings with no matching seats returns 200 OK with empty list")
        void getNfcMappings_noMatches_returns200OKEmpty() throws Exception {
                when(seatService.getNfcMappings(eq(99), isNull(), isNull(), isNull()))
                                .thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/seats/nfc-mappings")
                                .param("zoneId", "99"))
                                .andExpect(status().isOk());

                verify(seatService, times(1)).getNfcMappings(eq(99), isNull(), isNull(), isNull());
        }

        // =========================================
        // === UTCID05: Service failure ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get NFC mappings when service fails returns 400 Bad Request")
        void getNfcMappings_serviceFailure_returns400() throws Exception {
                when(seatService.getNfcMappings(isNull(), isNull(), isNull(), isNull()))
                                .thenThrow(new RuntimeException("Service failure"));

                mockMvc.perform(get("/slib/seats/nfc-mappings"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").exists());

                verify(seatService, times(1)).getNfcMappings(isNull(), isNull(), isNull(), isNull());
        }
}

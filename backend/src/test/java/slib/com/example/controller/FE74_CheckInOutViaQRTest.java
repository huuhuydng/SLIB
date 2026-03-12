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
import slib.com.example.controller.kiosk.KioskAuthController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.service.kiosk.KioskQrAuthService;
import slib.com.example.service.kiosk.KioskQrDTO;
import slib.com.example.service.kiosk.KioskTokenService;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-74: Check-in/out via QR
 * Test Report: doc/Report/UnitTestReport/FE74_TestReport.md
 */
@WebMvcTest(value = KioskAuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-74: Check-in/out via QR - Unit Tests")
class FE74_CheckInOutViaQRTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskQrAuthService kioskQrAuthService;

        @MockBean
        private KioskTokenService kioskTokenService;

        @MockBean
        private KioskActivationCodeRepository kioskActivationCodeRepository;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Generate QR for valid kiosk - Normal ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Generate QR for valid kioskCode returns 200 OK")
        void generateQr_validKioskCode_returns200OK() throws Exception {
                String kioskCode = "KIOSK-001";
                KioskQrDTO.QrGenerateResponse mockResponse = KioskQrDTO.QrGenerateResponse.builder()
                                .qrPayload("encoded-qr-payload")
                                .kioskCode(kioskCode)
                                .ttlSeconds(300)
                                .build();

                when(kioskQrAuthService.generateQr(eq(kioskCode))).thenReturn(mockResponse);

                mockMvc.perform(get("/slib/kiosk/qr/generate/{kioskCode}", kioskCode))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.qrPayload").value("encoded-qr-payload"))
                                .andExpect(jsonPath("$.kioskCode").value(kioskCode));

                verify(kioskQrAuthService, times(1)).generateQr(eq(kioskCode));
        }

        // =========================================
        // === UTCID02: Generate QR with different kiosk - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Generate QR for another kiosk returns 200 OK")
        void generateQr_anotherKiosk_returns200OK() throws Exception {
                String kioskCode = "KIOSK-002";
                KioskQrDTO.QrGenerateResponse mockResponse = KioskQrDTO.QrGenerateResponse.builder()
                                .qrPayload("another-qr-payload")
                                .kioskCode(kioskCode)
                                .ttlSeconds(300)
                                .build();

                when(kioskQrAuthService.generateQr(eq(kioskCode))).thenReturn(mockResponse);

                mockMvc.perform(get("/slib/kiosk/qr/generate/{kioskCode}", kioskCode))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.qrPayload").exists());

                verify(kioskQrAuthService, times(1)).generateQr(eq(kioskCode));
        }

        // =========================================
        // === UTCID03: Check-in with valid session token - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Check-in with valid session token returns 200 OK")
        void checkIn_validSessionToken_returns200OK() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("sessionToken", "valid-session-token");

                doNothing().when(kioskQrAuthService).checkIn(eq("valid-session-token"));

                mockMvc.perform(post("/slib/kiosk/session/checkin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value("true"));

                verify(kioskQrAuthService, times(1)).checkIn(eq("valid-session-token"));
        }

        // =========================================
        // === UTCID04: QR generate for invalid/non-existent kiosk - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Generate QR for non-existent kioskCode returns 400 Bad Request")
        void generateQr_invalidKiosk_returns400BadRequest() throws Exception {
                String kioskCode = "NON-EXISTENT";

                when(kioskQrAuthService.generateQr(eq(kioskCode)))
                                .thenThrow(new IllegalArgumentException("Kiosk khong ton tai"));

                mockMvc.perform(get("/slib/kiosk/qr/generate/{kioskCode}", kioskCode))
                                .andExpect(status().isBadRequest());

                verify(kioskQrAuthService, times(1)).generateQr(eq(kioskCode));
        }

        // =========================================
        // === UTCID05: QR validate with expired session - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Validate QR with expired session returns 400 Bad Request")
        void validateQr_expiredSession_returns400BadRequest() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("qrPayload", "expired-payload");
                request.put("kioskCode", "KIOSK-001");

                when(kioskQrAuthService.validateQr(anyString(), anyString()))
                                .thenThrow(new IllegalArgumentException("Phien QR da het han"));

                mockMvc.perform(post("/slib/kiosk/qr/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(kioskQrAuthService, times(1)).validateQr(anyString(), anyString());
        }

        // =========================================
        // === UTCID06: Check-in with invalid session token fails - Abnormal ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Check-in with invalid session token returns 500 Internal Server Error")
        void checkIn_invalidSession_returns500InternalServerError() throws Exception {
                Map<String, String> request = new HashMap<>();
                request.put("sessionToken", "invalid-session");

                doThrow(new RuntimeException("Session khong hop le"))
                                .when(kioskQrAuthService).checkIn(eq("invalid-session"));

                mockMvc.perform(post("/slib/kiosk/session/checkin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());

                verify(kioskQrAuthService, times(1)).checkIn(eq("invalid-session"));
        }
}

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.kiosk.KioskAuthController;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.service.kiosk.KioskQrAuthService;
import slib.com.example.service.kiosk.KioskQrDTO;
import slib.com.example.service.kiosk.KioskTokenService;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = KioskAuthController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("KioskAuthController Unit Tests")
class KioskAuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KioskQrAuthService kioskQrAuthService;

    @MockBean
    private KioskTokenService kioskTokenService;

    @MockBean
    private KioskActivationCodeRepository kioskActivationCodeRepository;

    @Test
    @DisplayName("activateDevice_validToken_returns200")
    void activateDevice_validToken_returns200() throws Exception {
        KioskConfigEntity kiosk = KioskConfigEntity.builder()
                .id(1)
                .kioskCode("K001")
                .kioskName("Kiosk Tang 1")
                .kioskType("INTERACTIVE")
                .location("Tang 1")
                .isActive(true)
                .build();

        when(kioskTokenService.validateDeviceToken("valid-token")).thenReturn(kiosk);

        mockMvc.perform(post("/slib/kiosk/session/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kioskCode").value("K001"))
                .andExpect(jsonPath("$.message").value("Kich hoat kiosk thanh cong"));
    }

    @Test
    @DisplayName("activateDevice_invalidToken_returns401")
    void activateDevice_invalidToken_returns401() throws Exception {
        when(kioskTokenService.validateDeviceToken("bad-token")).thenReturn(null);

        mockMvc.perform(post("/slib/kiosk/session/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"bad-token\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("activateDevice_emptyToken_returns401")
    void activateDevice_emptyToken_returns401() throws Exception {
        mockMvc.perform(post("/slib/kiosk/session/activate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("generateQr_returns200")
    void generateQr_returns200() throws Exception {
        KioskQrDTO.QrGenerateResponse response = KioskQrDTO.QrGenerateResponse.builder()
                .qrPayload("payload123")
                .kioskCode("K001")
                .expiresAt(java.time.LocalDateTime.now().plusMinutes(5))
                .build();

        when(kioskQrAuthService.generateQr("K001")).thenReturn(response);

        mockMvc.perform(get("/slib/kiosk/qr/generate/K001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kioskCode").value("K001"))
                .andExpect(jsonPath("$.qrPayload").value("payload123"));
    }

    @Test
    @DisplayName("validateQr_returns200")
    void validateQr_returns200() throws Exception {
        KioskQrDTO.QrValidateResponse response = KioskQrDTO.QrValidateResponse.builder()
                .valid(true)
                .kioskCode("K001")
                .sessionToken("session-123")
                .build();

        when(kioskQrAuthService.validateQr("payload", "K001")).thenReturn(response);

        mockMvc.perform(post("/slib/kiosk/qr/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrPayload\":\"payload\",\"kioskCode\":\"K001\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.sessionToken").value("session-123"));
    }

    @Test
    @DisplayName("checkOut_returns200")
    void checkOut_returns200() throws Exception {
        doNothing().when(kioskQrAuthService).checkOut("session-token");

        mockMvc.perform(post("/slib/kiosk/session/checkout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionToken\":\"session-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @DisplayName("checkIn_returns200")
    void checkIn_returns200() throws Exception {
        doNothing().when(kioskQrAuthService).checkIn("session-token");

        mockMvc.perform(post("/slib/kiosk/session/checkin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionToken\":\"session-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"));
    }

    @Test
    @DisplayName("checkStatus_returns200")
    void checkStatus_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        when(kioskQrAuthService.isUserCheckedIn(userId)).thenReturn(true);

        mockMvc.perform(get("/slib/kiosk/session/check-status/" + userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isCheckedIn").value(true));
    }

    @Test
    @DisplayName("getActiveSession_notFound_returns404")
    void getActiveSession_notFound_returns404() throws Exception {
        when(kioskQrAuthService.getActiveSession("K001")).thenReturn(Optional.empty());

        mockMvc.perform(get("/slib/kiosk/session/K001"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("expireSession_returns200")
    void expireSession_returns200() throws Exception {
        doNothing().when(kioskQrAuthService).expireSession("session-token");

        mockMvc.perform(post("/slib/kiosk/session/expire")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"sessionToken\":\"session-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value("true"));
    }
}

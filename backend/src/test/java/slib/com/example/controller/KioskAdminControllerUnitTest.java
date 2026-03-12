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
import slib.com.example.controller.kiosk.KioskAdminController;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.repository.kiosk.KioskConfigRepository;
import slib.com.example.service.kiosk.KioskTokenService;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = KioskAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("KioskAdminController Unit Tests")
class KioskAdminControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KioskTokenService kioskTokenService;

    @MockBean
    private KioskConfigRepository kioskConfigRepository;

    @MockBean
    private KioskActivationCodeRepository kioskActivationCodeRepository;

    @Test
    @DisplayName("listKioskSessions_returns200")
    void listKioskSessions_returns200() throws Exception {
        when(kioskConfigRepository.findAll()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/kiosk/admin/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("getKioskDetail_found_returns200")
    void getKioskDetail_found_returns200() throws Exception {
        KioskConfigEntity kiosk = KioskConfigEntity.builder()
                .id(1)
                .kioskCode("K001")
                .kioskName("Kiosk Tang 1")
                .kioskType("INTERACTIVE")
                .location("Tang 1")
                .isActive(true)
                .qrSecretKey("secret")
                .build();

        when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(kiosk));

        mockMvc.perform(get("/slib/kiosk/admin/kiosks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kioskCode").value("K001"))
                .andExpect(jsonPath("$.kioskName").value("Kiosk Tang 1"));
    }

    @Test
    @DisplayName("getKioskDetail_notFound_returns404")
    void getKioskDetail_notFound_returns404() throws Exception {
        when(kioskConfigRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/slib/kiosk/admin/kiosks/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("createKiosk_valid_returns200")
    void createKiosk_valid_returns200() throws Exception {
        when(kioskConfigRepository.existsByKioskCode("K002")).thenReturn(false);

        KioskConfigEntity saved = KioskConfigEntity.builder()
                .id(2)
                .kioskCode("K002")
                .kioskName("Kiosk Tang 2")
                .kioskType("MONITORING")
                .location("Tang 2")
                .isActive(true)
                .qrSecretKey("secret2")
                .build();
        when(kioskConfigRepository.save(any())).thenReturn(saved);

        mockMvc.perform(post("/slib/kiosk/admin/kiosks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kioskCode\":\"K002\",\"kioskName\":\"Kiosk Tang 2\",\"kioskType\":\"MONITORING\",\"location\":\"Tang 2\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kioskCode").value("K002"));
    }

    @Test
    @DisplayName("createKiosk_duplicateCode_returns400")
    void createKiosk_duplicateCode_returns400() throws Exception {
        when(kioskConfigRepository.existsByKioskCode("K001")).thenReturn(true);

        mockMvc.perform(post("/slib/kiosk/admin/kiosks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"kioskCode\":\"K001\",\"kioskName\":\"Test\",\"kioskType\":\"INTERACTIVE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("deleteKiosk_found_returns200")
    void deleteKiosk_found_returns200() throws Exception {
        KioskConfigEntity kiosk = KioskConfigEntity.builder()
                .id(1)
                .kioskCode("K001")
                .kioskName("Kiosk Tang 1")
                .build();

        when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(kiosk));

        mockMvc.perform(delete("/slib/kiosk/admin/kiosks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("revokeToken_returns200")
    void revokeToken_returns200() throws Exception {
        doNothing().when(kioskTokenService).revokeDeviceToken(1);

        mockMvc.perform(delete("/slib/kiosk/admin/token/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}

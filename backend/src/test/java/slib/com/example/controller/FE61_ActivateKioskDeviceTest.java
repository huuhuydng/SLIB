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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.kiosk.KioskAdminController;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.repository.kiosk.KioskConfigRepository;
import slib.com.example.service.kiosk.KioskTokenService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-61: Activate Kiosk device
 * Covers token generation, revocation, and error cases.
 */
@WebMvcTest(value = KioskAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-61: Activate Kiosk device - Unit Tests")
class FE61_ActivateKioskDeviceTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskTokenService kioskTokenService;

        @MockBean
        private KioskConfigRepository kioskConfigRepository;

        @MockBean
        private KioskActivationCodeRepository kioskActivationCodeRepository;

        // Note: generateToken requires a real User principal (not @WithMockUser)
        // because extractUserId checks for slib User entity.
        // We test the error paths and revoke endpoint fully.

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: Generate token for non-existent kiosk - returns 400")
        void generateToken_kioskNotFound_returns400() throws Exception {
                when(kioskConfigRepository.findById(999)).thenReturn(Optional.empty());

                mockMvc.perform(post("/slib/kiosk/admin/token/{kioskId}", 999))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: Generate token when valid token exists without force - returns 400")
        void generateToken_existingToken_noForce_returns400() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Gate A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(kiosk));
                when(kioskTokenService.hasValidToken(kiosk)).thenReturn(true);

                mockMvc.perform(post("/slib/kiosk/admin/token/{kioskId}", 1))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: Generate token with @WithMockUser - auth mismatch returns 400")
        void generateToken_mockUserAuthMismatch_returns400() throws Exception {
                // @WithMockUser creates a Spring UserDetails, not entity User
                // Controller's extractUserId cannot resolve it to UUID → 400
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Gate A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(kiosk));
                when(kioskTokenService.hasValidToken(kiosk)).thenReturn(false);

                mockMvc.perform(post("/slib/kiosk/admin/token/{kioskId}", 1))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: Revoke kiosk device token - returns 200")
        void revokeToken_validKiosk_returns200() throws Exception {
                doNothing().when(kioskTokenService).revokeDeviceToken(1);

                mockMvc.perform(delete("/slib/kiosk/admin/token/{kioskId}", 1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").exists());

                verify(kioskTokenService, times(1)).revokeDeviceToken(1);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD05: Revoke token for non-existent kiosk - service handles it")
        void revokeToken_nonExistentKiosk_returns200() throws Exception {
                doNothing().when(kioskTokenService).revokeDeviceToken(999);

                mockMvc.perform(delete("/slib/kiosk/admin/token/{kioskId}", 999))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD06: Revoke token - service error returns 500")
        void revokeToken_serviceError_returns500() throws Exception {
                doThrow(new RuntimeException("DB Error")).when(kioskTokenService).revokeDeviceToken(1);

                mockMvc.perform(delete("/slib/kiosk/admin/token/{kioskId}", 1))
                                .andExpect(status().isInternalServerError());
        }
}

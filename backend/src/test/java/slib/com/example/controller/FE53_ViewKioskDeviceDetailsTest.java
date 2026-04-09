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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-53: View Kiosk device details
 */
@WebMvcTest(value = KioskAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-53: View Kiosk device details - Unit Tests")
class FE53_ViewKioskDeviceDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private KioskTokenService kioskTokenService;

        @MockBean
        private KioskConfigRepository kioskConfigRepository;

        @MockBean
        private KioskActivationCodeRepository kioskActivationCodeRepository;

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: View kiosk detail - valid kiosk returns 200")
        void getKioskDetail_validId_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Gate A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(kiosk));
                when(kioskTokenService.hasValidToken(any())).thenReturn(true);
                when(kioskTokenService.isOnline(any())).thenReturn(true);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("ONLINE");

                mockMvc.perform(get("/slib/kiosk/admin/kiosks/{kioskId}", 1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.kioskCode").value("KIO-001"))
                                .andExpect(jsonPath("$.kioskName").value("Gate A"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: View kiosk detail - non-existent kiosk returns 404")
        void getKioskDetail_notFound_returns404() throws Exception {
                when(kioskConfigRepository.findById(999)).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/kiosk/admin/kiosks/{kioskId}", 999))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: View inactive kiosk detail - returns 200 with inactive status")
        void getKioskDetail_inactive_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(2).kioskCode("KIO-002").kioskName("Info B")
                                .kioskType("MONITORING").isActive(false).build();
                when(kioskConfigRepository.findById(2)).thenReturn(Optional.of(kiosk));
                when(kioskTokenService.hasValidToken(any())).thenReturn(false);
                when(kioskTokenService.isOnline(any())).thenReturn(false);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("OFFLINE");

                mockMvc.perform(get("/slib/kiosk/admin/kiosks/{kioskId}", 2))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isActive").value(false))
                                .andExpect(jsonPath("$.tokenValid").value(false));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: View kiosk detail checks online and runtimeStatus fields")
        void getKioskDetail_onlineFields_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Kiosk A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(kiosk));
                when(kioskTokenService.hasValidToken(kiosk)).thenReturn(true);
                when(kioskTokenService.isOnline(kiosk)).thenReturn(true);
                when(kioskTokenService.getRuntimeStatus(kiosk)).thenReturn("ONLINE");

                mockMvc.perform(get("/slib/kiosk/admin/kiosks/{kioskId}", 1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.online").value(true))
                                .andExpect(jsonPath("$.runtimeStatus").value("ONLINE"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD05: View kiosk detail verifies kioskType field")
        void getKioskDetail_verifyType_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(3).kioskCode("KIO-003").kioskName("Monitor C")
                                .kioskType("MONITORING").isActive(true).build();
                when(kioskConfigRepository.findById(3)).thenReturn(Optional.of(kiosk));
                when(kioskTokenService.hasValidToken(any())).thenReturn(false);
                when(kioskTokenService.isOnline(any())).thenReturn(false);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("OFFLINE");

                mockMvc.perform(get("/slib/kiosk/admin/kiosks/{kioskId}", 3))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.kioskType").value("MONITORING"));
        }
}

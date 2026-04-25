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

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-58: View list of Kiosk devices
 */
@WebMvcTest(value = KioskAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-58: View list of Kiosk devices - Unit Tests")
class FE58_ViewKioskDevicesTest {

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
        @DisplayName("UTCD01: View all kiosk sessions - returns 200 with list")
        void listKiosks_validAdmin_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Kiosk A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findAll()).thenReturn(Arrays.asList(kiosk));
                when(kioskTokenService.hasValidToken(any())).thenReturn(true);
                when(kioskTokenService.isOnline(any())).thenReturn(true);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("ONLINE");

                mockMvc.perform(get("/slib/kiosk/admin/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].kioskCode").value("KIO-001"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: View kiosk sessions - empty list returns 200")
        void listKiosks_empty_returns200() throws Exception {
                when(kioskConfigRepository.findAll()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/kiosk/admin/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: View multiple kiosk devices with mixed statuses")
        void listKiosks_multipleDevices_returns200() throws Exception {
                KioskConfigEntity k1 = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Gate A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                KioskConfigEntity k2 = KioskConfigEntity.builder()
                                .id(2).kioskCode("KIO-002").kioskName("Info B")
                                .kioskType("MONITORING").isActive(false).build();
                when(kioskConfigRepository.findAll()).thenReturn(Arrays.asList(k1, k2));
                when(kioskTokenService.hasValidToken(k1)).thenReturn(true);
                when(kioskTokenService.hasValidToken(k2)).thenReturn(false);
                when(kioskTokenService.isOnline(any())).thenReturn(false);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("OFFLINE");

                mockMvc.perform(get("/slib/kiosk/admin/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: View kiosk sessions verifies online status field")
        void listKiosks_checkOnlineField_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Kiosk A")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findAll()).thenReturn(Arrays.asList(kiosk));
                when(kioskTokenService.hasValidToken(kiosk)).thenReturn(true);
                when(kioskTokenService.isOnline(kiosk)).thenReturn(true);
                when(kioskTokenService.getRuntimeStatus(kiosk)).thenReturn("ONLINE");

                mockMvc.perform(get("/slib/kiosk/admin/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].online").value(true))
                                .andExpect(jsonPath("$[0].tokenValid").value(true))
                                .andExpect(jsonPath("$[0].runtimeStatus").value("ONLINE"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD05: View kiosk sessions verifies isActive and hasDeviceToken fields")
        void listKiosks_checkActiveAndTokenFields_returns200() throws Exception {
                KioskConfigEntity kiosk = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Kiosk A")
                                .kioskType("MONITORING").isActive(false).build();
                when(kioskConfigRepository.findAll()).thenReturn(Arrays.asList(kiosk));
                when(kioskTokenService.hasValidToken(kiosk)).thenReturn(false);
                when(kioskTokenService.isOnline(kiosk)).thenReturn(false);
                when(kioskTokenService.getRuntimeStatus(kiosk)).thenReturn("OFFLINE");

                mockMvc.perform(get("/slib/kiosk/admin/sessions"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].isActive").value(false))
                                .andExpect(jsonPath("$[0].hasDeviceToken").value(false));
        }
}

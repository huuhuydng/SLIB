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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.kiosk.KioskAdminController;
import slib.com.example.dto.kiosk.CreateKioskRequest;
import slib.com.example.dto.kiosk.UpdateKioskRequest;
import slib.com.example.entity.kiosk.KioskConfigEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.kiosk.KioskActivationCodeRepository;
import slib.com.example.repository.kiosk.KioskConfigRepository;
import slib.com.example.service.kiosk.KioskTokenService;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-54: CRUD Kiosk device
 */
@WebMvcTest(value = KioskAdminController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-54: CRUD Kiosk device - Unit Tests")
class FE54_CRUDKioskDeviceTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private KioskTokenService kioskTokenService;

        @MockBean
        private KioskConfigRepository kioskConfigRepository;

        @MockBean
        private KioskActivationCodeRepository kioskActivationCodeRepository;

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: Create kiosk with valid data - returns 200")
        void createKiosk_validData_returns200() throws Exception {
                when(kioskConfigRepository.existsByKioskCode("KIO-NEW")).thenReturn(false);
                KioskConfigEntity saved = KioskConfigEntity.builder()
                                .id(10).kioskCode("KIO-NEW").kioskName("New Kiosk")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.save(any())).thenReturn(saved);
                when(kioskTokenService.hasValidToken(any())).thenReturn(false);
                when(kioskTokenService.isOnline(any())).thenReturn(false);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("OFFLINE");

                CreateKioskRequest req = new CreateKioskRequest();
                req.setKioskCode("KIO-NEW");
                req.setKioskName("New Kiosk");
                req.setKioskType("INTERACTIVE");
                mockMvc.perform(post("/slib/kiosk/admin/kiosks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.kioskCode").value("KIO-NEW"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: Create kiosk with duplicate code - returns 400")
        void createKiosk_duplicateCode_returns400() throws Exception {
                when(kioskConfigRepository.existsByKioskCode("KIO-001")).thenReturn(true);

                CreateKioskRequest req = new CreateKioskRequest();
                req.setKioskCode("KIO-001");
                req.setKioskName("Dup");
                req.setKioskType("INTERACTIVE");
                mockMvc.perform(post("/slib/kiosk/admin/kiosks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: Create kiosk with blank name - validation fails - returns 400")
        void createKiosk_blankName_returns400() throws Exception {
                CreateKioskRequest req = new CreateKioskRequest();
                req.setKioskCode("KIO-003");
                req.setKioskName("");
                req.setKioskType("INTERACTIVE");
                mockMvc.perform(post("/slib/kiosk/admin/kiosks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: Create kiosk with invalid type - validation fails - returns 400")
        void createKiosk_invalidType_returns400() throws Exception {
                CreateKioskRequest req = new CreateKioskRequest();
                req.setKioskCode("KIO-004");
                req.setKioskName("Test");
                req.setKioskType("INVALID_TYPE");
                mockMvc.perform(post("/slib/kiosk/admin/kiosks")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD05: Update kiosk - valid data returns 200")
        void updateKiosk_validData_returns200() throws Exception {
                KioskConfigEntity existing = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Old Name")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(existing));
                when(kioskConfigRepository.save(any())).thenReturn(existing);
                when(kioskTokenService.hasValidToken(any())).thenReturn(false);
                when(kioskTokenService.isOnline(any())).thenReturn(false);
                when(kioskTokenService.getRuntimeStatus(any())).thenReturn("OFFLINE");

                UpdateKioskRequest req = new UpdateKioskRequest();
                req.setKioskName("Updated Name");
                mockMvc.perform(put("/slib/kiosk/admin/kiosks/{kioskId}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD06: Update non-existent kiosk - returns 404")
        void updateKiosk_notFound_returns404() throws Exception {
                when(kioskConfigRepository.findById(999)).thenReturn(Optional.empty());

                UpdateKioskRequest req = new UpdateKioskRequest();
                req.setKioskName("Test");
                mockMvc.perform(put("/slib/kiosk/admin/kiosks/{kioskId}", 999)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isNotFound());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD07: Delete kiosk - valid ID returns 200")
        void deleteKiosk_validId_returns200() throws Exception {
                KioskConfigEntity existing = KioskConfigEntity.builder()
                                .id(1).kioskCode("KIO-001").kioskName("Del")
                                .kioskType("INTERACTIVE").isActive(true).build();
                when(kioskConfigRepository.findById(1)).thenReturn(Optional.of(existing));

                mockMvc.perform(delete("/slib/kiosk/admin/kiosks/{kioskId}", 1))
                                .andExpect(status().isOk());
                verify(kioskConfigRepository, times(1)).delete(existing);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD08: Delete non-existent kiosk - returns 404")
        void deleteKiosk_notFound_returns404() throws Exception {
                when(kioskConfigRepository.findById(999)).thenReturn(Optional.empty());

                mockMvc.perform(delete("/slib/kiosk/admin/kiosks/{kioskId}", 999))
                                .andExpect(status().isNotFound());
        }
}

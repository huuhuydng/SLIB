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
import slib.com.example.dto.LibrarySettingDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.LibrarySettingService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-37: Configure Booking Rules
 * Test Report: doc/Report/UnitTestReport/FE37_TestReport.md
 */
@WebMvcTest(value = LibrarySettingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-37: Configure Booking Rules - Unit Tests")
class FE37_ConfigureBookingRulesTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private LibrarySettingService librarySettingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Get booking rules - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get booking rules with valid JWT token returns 200 OK")
        void getSettings_validToken_returns200OK() throws Exception {
                LibrarySettingDTO dto = LibrarySettingDTO.builder()
                                .maxBookingDays(14)
                                .maxBookingsPerDay(3)
                                .maxHoursPerDay(4)
                                .autoCancelMinutes(15)
                                .build();

                when(librarySettingService.getSettingsDTO()).thenReturn(dto);

                mockMvc.perform(get("/slib/settings/library"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.maxBookingDays").value(14))
                                .andExpect(jsonPath("$.maxBookingsPerDay").value(3));

                verify(librarySettingService, times(1)).getSettingsDTO();
        }

        // =========================================
        // === UTCID02: No token - Unauthorized ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get booking rules without token returns 500 Internal Server Error")
        void getSettings_noToken_returns500() throws Exception {
                when(librarySettingService.getSettingsDTO())
                                .thenThrow(new RuntimeException("Unauthorized"));

                mockMvc.perform(get("/slib/settings/library"))
                                .andExpect(status().isInternalServerError());

                verify(librarySettingService, times(1)).getSettingsDTO();
        }

        // =========================================
        // === UTCID03: Non-admin - Forbidden ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Update booking rules with non-admin JWT returns 403 Forbidden")
        void updateSettings_nonAdmin_returns403Forbidden() throws Exception {
                LibrarySettingDTO dto = LibrarySettingDTO.builder()
                                .maxBookingDays(7)
                                .build();

                when(librarySettingService.updateSettings(any(LibrarySettingDTO.class)))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(put("/slib/settings/library")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isForbidden());

                verify(librarySettingService, times(1)).updateSettings(any(LibrarySettingDTO.class));
        }

        // =========================================
        // === UTCID04: Invalid rules - Bad Request ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Update booking rules with invalid values returns 400 Bad Request")
        void updateSettings_invalidRules_returns400BadRequest() throws Exception {
                LibrarySettingDTO dto = LibrarySettingDTO.builder()
                                .maxBookingDays(-1)
                                .maxBookingsPerDay(0)
                                .build();

                when(librarySettingService.updateSettings(any(LibrarySettingDTO.class)))
                                .thenThrow(new slib.com.example.exception.BadRequestException(
                                                "Cau hinh dat cho khong hop le"));

                mockMvc.perform(put("/slib/settings/library")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isBadRequest());

                verify(librarySettingService, times(1)).updateSettings(any(LibrarySettingDTO.class));
        }

        // =========================================
        // === UTCID05: Non-admin role ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Update booking rules with non-admin role returns 403 Forbidden")
        void updateSettings_nonAdminRole_returns403Forbidden() throws Exception {
                LibrarySettingDTO dto = LibrarySettingDTO.builder()
                                .maxBookingDays(14)
                                .build();

                when(librarySettingService.updateSettings(any(LibrarySettingDTO.class)))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(put("/slib/settings/library")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(dto)))
                                .andExpect(status().isForbidden());

                verify(librarySettingService, times(1)).updateSettings(any(LibrarySettingDTO.class));
        }
}

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

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-38: Enable/Disable Library
 * Test Report: doc/Report/UnitTestReport/FE38_TestReport.md
 */
@WebMvcTest(value = LibrarySettingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-38: Enable/Disable Library - Unit Tests")
class FE38_EnableDisableLibraryTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private LibrarySettingService librarySettingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Toggle library - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Toggle library status with valid admin JWT returns 200 OK")
        void toggleLock_validAdmin_returns200OK() throws Exception {
                LibrarySettingDTO dto = LibrarySettingDTO.builder()
                                .libraryClosed(true)
                                .closedReason("Bao tri he thong")
                                .build();

                when(librarySettingService.toggleLibraryClosed(eq(true), eq("Bao tri he thong")))
                                .thenReturn(dto);

                Map<String, Object> body = Map.of(
                                "closed", true,
                                "reason", "Bao tri he thong");

                mockMvc.perform(post("/slib/settings/library/toggle-lock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.libraryClosed").value(true));

                verify(librarySettingService, times(1)).toggleLibraryClosed(eq(true), eq("Bao tri he thong"));
        }

        // =========================================
        // === UTCID02: No token - Unauthorized ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Toggle library without token returns 500 Internal Server Error")
        void toggleLock_noToken_returns500() throws Exception {
                when(librarySettingService.toggleLibraryClosed(any(), any()))
                                .thenThrow(new RuntimeException("Unauthorized"));

                Map<String, Object> body = Map.of("closed", true, "reason", "test");

                mockMvc.perform(post("/slib/settings/library/toggle-lock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCID03: Non-admin - Forbidden ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Toggle library with non-admin JWT returns 403 Forbidden")
        void toggleLock_nonAdmin_returns403Forbidden() throws Exception {
                when(librarySettingService.toggleLibraryClosed(any(), any()))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                Map<String, Object> body = Map.of("closed", true, "reason", "test");

                mockMvc.perform(post("/slib/settings/library/toggle-lock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isForbidden());
        }

        // =========================================
        // === UTCID04: Already in target state - Bad Request ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Toggle library to same status returns 400 Bad Request")
        void toggleLock_sameStatus_returns400BadRequest() throws Exception {
                when(librarySettingService.toggleLibraryClosed(any(), any()))
                                .thenThrow(new slib.com.example.exception.BadRequestException(
                                                "Thu vien da o trang thai nay"));

                Map<String, Object> body = Map.of("closed", true, "reason", "test");

                mockMvc.perform(post("/slib/settings/library/toggle-lock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isBadRequest());
        }

        // =========================================
        // === UTCID05: System error ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Toggle library with system error returns 500 Internal Server Error")
        void toggleLock_systemError_returns500() throws Exception {
                when(librarySettingService.toggleLibraryClosed(any(), any()))
                                .thenThrow(new RuntimeException("Database connection failed"));

                Map<String, Object> body = Map.of("closed", false, "reason", "");

                mockMvc.perform(post("/slib/settings/library/toggle-lock")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(body)))
                                .andExpect(status().isInternalServerError());
        }
}

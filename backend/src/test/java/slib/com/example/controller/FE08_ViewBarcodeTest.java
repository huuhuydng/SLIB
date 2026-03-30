package slib.com.example.controller;

import org.junit.jupiter.api.BeforeEach;
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
import slib.com.example.controller.users.UserController;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-08: View Barcode
 * Test Report: doc/Report/FE08_TestReport.md
 *
 * Note: UserController does not have a /me/barcode endpoint.
 * The /me endpoint returns the user profile which includes userCode for barcode.
 * Tests are adapted to use /me endpoint.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-08: View Barcode - Unit Tests")
class FE08_ViewBarcodeTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @MockBean
        private AuthService authService;

        @MockBean
        private CloudinaryService cloudinaryService;

        @MockBean
        private AsyncImportService asyncImportService;

        @MockBean
        private StagingImportService stagingImportService;

        @MockBean
        private SystemLogService systemLogService;

        @BeforeEach
        void clearSecurityContext() {
        }

        // UTCD01: Valid token - view profile (which contains barcode/userCode)
        @Test
        @WithMockUser(username = "student@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD01: View profile with barcode data returns 200 OK")
        void viewBarcode_validToken_returns200OK() throws Exception {
                when(userService.getMyProfile(anyString())).thenReturn(
                        UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email("student@fpt.edu.vn")
                                .fullName("Nguyen Van A")
                                .userCode("SE123456")
                                .role("STUDENT")
                                .build()
                );

                mockMvc.perform(get("/slib/users/me"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.userCode").value("SE123456"));
        }

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: View profile without token returns 401 Unauthorized")
        void viewBarcode_noToken_returns401Unauthorized() throws Exception {
                mockMvc.perform(get("/slib/users/me"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD03: User not found - 404
        @Test
        @WithMockUser(username = "unknown@fpt.edu.vn", roles = "STUDENT")
        @DisplayName("UTCD03: User not found returns 404 Not Found")
        void viewBarcode_userNotFound_returns404NotFound() throws Exception {
                when(userService.getMyProfile(anyString()))
                        .thenThrow(new RuntimeException("User not found"));

                mockMvc.perform(get("/slib/users/me"))
                        .andExpect(status().isNotFound());
        }
}

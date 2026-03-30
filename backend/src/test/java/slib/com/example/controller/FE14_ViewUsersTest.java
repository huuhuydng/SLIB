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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-14: View List of Users
 * Test Report: doc/Report/FE14_TestReport.md
 *
 * Note: @PreAuthorize is not enforced because @EnableMethodSecurity is not configured.
 * With addFilters=false, security filters are disabled. So 401/403 tests are not applicable.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-14: View List of Users - Unit Tests")
class FE14_ViewUsersTest {

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

        // UTCD01: Valid admin token - Success
        @Test
        @DisplayName("UTCD01: View users with admin token returns 200 OK")
        void viewUsers_adminToken_returns200OK() throws Exception {
                when(userService.getAllUsers()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$").isArray());

                verify(userService, times(1)).getAllUsers();
        }

        // UTCD02: Empty list - 200 OK
        @Test
        @DisplayName("UTCD02: View users returns empty list with 200 OK")
        void viewUsers_emptyList_returns200OK() throws Exception {
                when(userService.getAllUsers()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.length()").value(0));
        }

        // UTCD03: Service error - 500
        @Test
        @DisplayName("UTCD03: Service error returns 500 Internal Server Error")
        void viewUsers_serviceError_returns500() throws Exception {
                when(userService.getAllUsers()).thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isInternalServerError());
        }
}

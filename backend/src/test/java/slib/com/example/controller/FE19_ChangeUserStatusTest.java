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
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-19: Change User Status
 * Test Report: doc/Report/FE19_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-19: Change User Status - Unit Tests")
class FE19_ChangeUserStatusTest {

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

        @Test
        @DisplayName("UTCD01: Change user status returns 200 OK")
        void changeUserStatus_admin_returns200OK() throws Exception {
                UUID userId = UUID.randomUUID();
                User mockUser = User.builder().isActive(false).build();
                when(userService.toggleUserActive(any(), anyBoolean())).thenReturn(mockUser);

                mockMvc.perform(patch("/slib/users/" + userId + "/status")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":false}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Invalid UUID format returns 400 Bad Request")
        void changeUserStatus_invalidUUID_returns400() throws Exception {
                mockMvc.perform(patch("/slib/users/invalid-uuid/status")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"isActive\":false}"))
                        .andExpect(status().isBadRequest());
        }
}

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
import slib.com.example.controller.users.UserController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-19: View user details
 * Test Report: doc/Report/FE19_TestReport.md
 *
 * Note: There is no dedicated GET /users/{userId} endpoint.
 * User details are retrieved through GET /users/getall.
 * Tests adapted to verify getall endpoint returns user list.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-19: View user details - Unit Tests")
class FE19_ViewUserDetailsTest {

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

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: View users list returns 200 OK")
        void viewUserDetails_validRequest_returns200OK() throws Exception {
                when(userService.getAllUsers(null, null, null)).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isOk());

                verify(userService, times(1)).getAllUsers(null, null, null);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: Service error returns 500")
        void viewUserDetails_serviceError_returns500() throws Exception {
                when(userService.getAllUsers(null, null, null)).thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isInternalServerError());
        }
}

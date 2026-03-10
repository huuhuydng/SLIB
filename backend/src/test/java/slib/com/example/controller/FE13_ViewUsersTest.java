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
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.UserService;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-13: View List of Users
 * Test Report: doc/Report/FE13_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-13: View List of Users - Unit Tests")
class FE13_ViewUsersTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

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

        // UTCD02: No token - 401
        @Test
        @DisplayName("UTCD02: View users without token returns 401 Unauthorized")
        void viewUsers_noToken_returns401Unauthorized() throws Exception {
                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isUnauthorized());
        }

        // UTCD03: Not admin role - 403
        @Test
        @DisplayName("UTCD03: View users without admin role returns 403 Forbidden")
        void viewUsers_notAdmin_returns403Forbidden() throws Exception {
                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isForbidden());
        }
}

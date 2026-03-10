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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.UserService;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-09: View Account Setting
 * Test Report: doc/Report/FE09_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-09: View Account Setting - Unit Tests")
class FE09_ViewAccountSettingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Test
        @DisplayName("UTCD01: View account setting returns 200 OK")
        void viewAccountSetting_validToken_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/users/me/settings"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: No token returns 401 Unauthorized")
        void viewAccountSetting_noToken_returns401() throws Exception {
                mockMvc.perform(get("/slib/users/me/settings"))
                        .andExpect(status().isUnauthorized());
        }
}

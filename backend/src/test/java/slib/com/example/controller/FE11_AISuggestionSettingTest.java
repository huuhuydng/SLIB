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
 * Unit Tests for FE-11: Turn on/Turn off AI Suggestion
 * Test Report: doc/Report/FE11_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-11: Turn on/Turn off AI Suggestion - Unit Tests")
class FE11_AISuggestionSettingTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Test
        @DisplayName("UTCD01: Update AI suggestion setting returns 200 OK")
        void updateAISuggestionSetting_validValue_returns200OK() throws Exception {
                mockMvc.perform(patch("/slib/users/me/settings/ai-suggestion")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":true}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: No token returns 401 Unauthorized")
        void updateAISuggestionSetting_noToken_returns401() throws Exception {
                mockMvc.perform(patch("/slib/users/me/settings/ai-suggestion")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"enabled\":true}"))
                        .andExpect(status().isUnauthorized());
        }
}

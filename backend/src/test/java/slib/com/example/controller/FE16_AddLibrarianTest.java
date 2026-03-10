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

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-16: Add Librarian
 * Test Report: doc/Report/FE16_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-16: Add Librarian - Unit Tests")
class FE16_AddLibrarianTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private UserService userService;

        @Test
        @DisplayName("UTCD01: Add librarian with admin returns 200 OK")
        void addLibrarian_admin_returns200OK() throws Exception {
                mockMvc.perform(post("/slib/users/librarian")
                                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                .content("{\"email\":\"librarian@fpt.edu.vn\",\"fullName\":\"Test Librarian\"}"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: Add librarian without token returns 401")
        void addLibrarian_noToken_returns401() throws Exception {
                mockMvc.perform(post("/slib/users/librarian"))
                        .andExpect(status().isUnauthorized());
        }
}

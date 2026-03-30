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
import org.springframework.http.MediaType;
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
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-17: Add Librarian
 * Test Report: doc/Report/FE17_TestReport.md
 *
 * Note: There is no dedicated /librarian endpoint in UserController.
 * Adding a librarian is done through the /import endpoint with role=LIBRARIAN.
 * Tests adapted to verify the import endpoint accepts librarian data.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-17: Add Librarian - Unit Tests")
class FE17_AddLibrarianTest {

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
        @DisplayName("UTCD01: Import librarian user returns 200 OK")
        void addLibrarian_admin_returns200OK() throws Exception {
                when(userService.importUsers(any())).thenReturn(Map.of(
                        "success", Collections.emptyList(),
                        "failed", Collections.emptyList(),
                        "successCount", 0,
                        "failedCount", 0
                ));

                mockMvc.perform(post("/slib/users/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[{\"email\":\"librarian@fpt.edu.vn\",\"fullName\":\"Test Librarian\",\"userCode\":\"LIB001\",\"role\":\"LIBRARIAN\"}]"))
                        .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: Import with empty list returns 400 Bad Request")
        void addLibrarian_emptyList_returns400() throws Exception {
                mockMvc.perform(post("/slib/users/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                        .andExpect(status().isBadRequest());
        }
}

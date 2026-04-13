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
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-17: Download template of the file upload
 * Test Report: doc/Report/UnitTestReport/FE16_TestReport.md
 *
 * Note: Security filters are disabled (addFilters=false) and @EnableMethodSecurity is not configured.
 * Tests for 401/403 are not applicable in this configuration.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-17: Download template of the file upload - Unit Tests")
class FE17_DownloadTemplateTest {

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

        // =========================================
        // === UTCD01: Valid request - Success ===
        // =========================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: Download template returns 200 OK with Excel content")
        void downloadTemplate_validRequest_returns200OK() throws Exception {
                mockMvc.perform(get("/slib/users/import/template"))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Content-Disposition",
                                "attachment; filename=slib_user_import_template.xlsx"))
                        .andExpect(content().contentType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        // =========================================
        // === UTCD02: Template contains correct headers ===
        // =========================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: Download template returns non-empty content")
        void downloadTemplate_returnsNonEmptyContent() throws Exception {
                mockMvc.perform(get("/slib/users/import/template"))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        // =========================================
        // === UTCD03: Template download is idempotent ===
        // =========================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: Download template multiple times returns consistent results")
        void downloadTemplate_multipleRequests_returnsConsistentResults() throws Exception {
                mockMvc.perform(get("/slib/users/import/template"))
                        .andExpect(status().isOk());

                mockMvc.perform(get("/slib/users/import/template"))
                        .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: Download template returns XLSX binary content")
        void downloadTemplate_returnsXlsxBinaryContent() throws Exception {
                MvcResult result = mockMvc.perform(get("/slib/users/import/template"))
                        .andExpect(status().isOk())
                        .andReturn();

                byte[] content = result.getResponse().getContentAsByteArray();

                org.junit.jupiter.api.Assertions.assertTrue(content.length > 0);
                org.junit.jupiter.api.Assertions.assertEquals('P', content[0]);
                org.junit.jupiter.api.Assertions.assertEquals('K', content[1]);
        }
}

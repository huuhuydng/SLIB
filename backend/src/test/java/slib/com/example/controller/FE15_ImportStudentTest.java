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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AsyncImportService;
import slib.com.example.service.AuthService;
import slib.com.example.service.StagingImportService;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-15: Import Student via File
 * Test Report: doc/Report/FE15_TestReport.md
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-15: Import Student via File - Unit Tests")
class FE15_ImportStudentTest {

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

        // UTCD01: Valid file + admin - Success
        @Test
        @DisplayName("UTCD01: Import students with valid file returns 200 OK")
        void importStudents_validFile_returns200OK() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "students.xlsx",
                        MediaType.APPLICATION_OCTET_STREAM_VALUE,
                        "test data".getBytes()
                );

                when(asyncImportService.startImport(any())).thenReturn(UUID.randomUUID());

                mockMvc.perform(multipart("/slib/users/import/excel").file(file))
                        .andExpect(status().isOk());

                verify(asyncImportService, times(1)).startImport(any());
        }

        // UTCD02: Missing file - 400
        @Test
        @DisplayName("UTCD02: Import without file returns 400 Bad Request")
        void importStudents_noFile_returns400() throws Exception {
                mockMvc.perform(multipart("/slib/users/import/excel"))
                        .andExpect(status().isBadRequest());
        }

        // UTCD04: Invalid file format - 400
        @Test
        @DisplayName("UTCD04: Invalid file format returns 400 Bad Request")
        void importStudents_invalidFormat_returns400() throws Exception {
                MockMultipartFile file = new MockMultipartFile(
                        "file", "students.txt",
                        MediaType.TEXT_PLAIN_VALUE,
                        "test".getBytes()
                );

                mockMvc.perform(multipart("/slib/users/import/excel").file(file))
                        .andExpect(status().isBadRequest());
        }
}

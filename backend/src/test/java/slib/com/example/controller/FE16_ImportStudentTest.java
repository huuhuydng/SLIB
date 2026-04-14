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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.entity.users.ImportJob;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-16: Import Student and Teacher via file - Unit Tests")
class FE16_ImportStudentTest {

    private static final UUID BATCH_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
    @DisplayName("UTCID01: Import users from valid Excel file")
    void importUsers_fromValidExcelFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.xlsx",
                MediaType.APPLICATION_OCTET_STREAM_VALUE,
                "test data".getBytes());

        when(asyncImportService.startImport(any())).thenReturn(BATCH_ID);

        mockMvc.perform(multipart("/slib/users/import/excel").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.status").value("PROCESSING"));

        verify(asyncImportService, times(1)).startImport(any());
        verify(asyncImportService, times(1)).processImportAsync(BATCH_ID);
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Import users without file")
    void importUsers_withoutFile() throws Exception {
        mockMvc.perform(multipart("/slib/users/import/excel"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Import users with invalid file format")
    void importUsers_withInvalidFileFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "test".getBytes());

        mockMvc.perform(multipart("/slib/users/import/excel").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Chỉ hỗ trợ file Excel (.xlsx, .xls)"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: View import status with valid batchId")
    void viewImportStatus_withValidBatchId() throws Exception {
        ImportJob job = ImportJob.builder()
                .batchId(BATCH_ID)
                .fileName("students.xlsx")
                .status(ImportJob.ImportJobStatus.COMPLETED)
                .totalRows(12)
                .validCount(10)
                .invalidCount(2)
                .importedCount(10)
                .avatarCount(0)
                .avatarUploaded(0)
                .createdAt(LocalDateTime.of(2026, 4, 9, 9, 0))
                .completedAt(LocalDateTime.of(2026, 4, 9, 9, 5))
                .errorMessage("")
                .build();

        when(stagingImportService.getJobStatus(BATCH_ID)).thenReturn(job);

        mockMvc.perform(get("/slib/users/import/{batchId}/status", BATCH_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.batchId").value(BATCH_ID.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.totalRows").value(12))
                .andExpect(jsonPath("$.importedCount").value(10));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: View import status with invalid batchId format")
    void viewImportStatus_withInvalidBatchIdFormat() throws Exception {
        mockMvc.perform(get("/slib/users/import/{batchId}/status", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid batchId format"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID06: View import status with non-existent batchId")
    void viewImportStatus_withNonExistentBatchId() throws Exception {
        when(stagingImportService.getJobStatus(BATCH_ID)).thenReturn(null);

        mockMvc.perform(get("/slib/users/import/{batchId}/status", BATCH_ID))
                .andExpect(status().isNotFound());
    }
}

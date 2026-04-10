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
import slib.com.example.controller.system.BackupController;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.system.BackupScheduleRepository;
import slib.com.example.service.system.BackupService;
import slib.com.example.service.system.SystemLogService;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-59: Backup data manually
 * Test Report: doc/Report/UnitTestReport/FE58_TestReport.md
 */
@WebMvcTest(value = BackupController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-59: Backup data manually - Unit Tests")
class FE59_ManualBackupTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BackupService backupService;

        @MockBean
        private BackupScheduleRepository backupScheduleRepository;

        @MockBean
        private SystemLogService systemLogService;

        // =========================================
        // === UTCID01: Trigger a manual backup ===
        // =========================================

        /**
         * UTCID01: Trigger a manual backup
         * Precondition: Admin opens the backup tab
         * Expected: 200 OK with backup result
         */
        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID01: Trigger a manual backup returns 200 OK")
        void triggerBackup_success_returns200OK() throws Exception {
                BackupHistoryEntity result = BackupHistoryEntity.builder()
                                .id(UUID.randomUUID())
                                .status(BackupHistoryEntity.BackupStatus.SUCCESS)
                                .filePath("/backups/slib_backup_20260311.sql")
                                .fileSizeBytes(1024000L)
                                .startedAt(LocalDateTime.now().minusSeconds(5))
                                .completedAt(LocalDateTime.now())
                                .build();

                when(backupService.performBackup()).thenReturn(result);

                mockMvc.perform(post("/slib/system/backup"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("SUCCESS"))
                                .andExpect(jsonPath("$.message").exists());

                verify(backupService, times(1)).performBackup();
        }

        // =========================================
        // === UTCID02: Load backup history ===
        // =========================================

        /**
         * UTCID02: Load backup history
         * Precondition: Admin opens the backup tab
         * Expected: 200 OK with history list
         */
        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID02: Load backup history returns 200 OK")
        void getHistory_success_returns200OK() throws Exception {
                BackupHistoryEntity historyEntry = BackupHistoryEntity.builder()
                                .id(UUID.randomUUID())
                                .status(BackupHistoryEntity.BackupStatus.SUCCESS)
                                .filePath("/backups/slib_backup.sql")
                                .fileSizeBytes(2048000L)
                                .startedAt(LocalDateTime.now().minusMinutes(10))
                                .completedAt(LocalDateTime.now().minusMinutes(9))
                                .build();

                when(backupService.getHistory()).thenReturn(List.of(historyEntry));

                mockMvc.perform(get("/slib/system/backup/history"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$[0].status").value("SUCCESS"))
                                .andExpect(jsonPath("$[0].duration").exists());

                verify(backupService, times(1)).getHistory();
        }

        // =========================================
        // === UTCID03: Download existing backup file ===
        // =========================================

        /**
         * UTCID03: Download existing backup file
         * Precondition: Admin opens the backup tab
         * Expected: 200 OK with file download
         */
        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID03: Download existing backup file returns 200 OK")
        void downloadBackup_existingFile_returns200OK() throws Exception {
                UUID backupId = UUID.randomUUID();
                File tempFile = File.createTempFile("slib_backup_", ".sql");
                tempFile.deleteOnExit();

                when(backupService.getBackupFile(backupId)).thenReturn(tempFile);

                mockMvc.perform(get("/slib/system/backup/download/" + backupId))
                                .andExpect(status().isOk())
                                .andExpect(header().exists("Content-Disposition"));

                verify(backupService, times(1)).getBackupFile(backupId);
        }

        // =========================================
        // === UTCID04: Download unknown backup id ===
        // =========================================

        /**
         * UTCID04: Download unknown backup id
         * Precondition: Admin opens the backup tab
         * Expected: 404 Not Found
         */
        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID04: Download unknown backup id returns 404 Not Found")
        void downloadBackup_unknownId_returns404() throws Exception {
                UUID unknownId = UUID.randomUUID();

                when(backupService.getBackupFile(unknownId))
                                .thenThrow(new RuntimeException("Backup file not found"));

                mockMvc.perform(get("/slib/system/backup/download/" + unknownId))
                                .andExpect(status().isNotFound());

                verify(backupService, times(1)).getBackupFile(unknownId);
        }

        // =========================================
        // === UTCID05: Backup job fails unexpectedly ===
        // =========================================

        /**
         * UTCID05: Backup job fails unexpectedly
         * Precondition: Admin opens the backup tab
         * Expected: 500 Internal Server Error
         */
        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID05: Backup job fails unexpectedly returns 500 Internal Server Error")
        void triggerBackup_failure_returns500() throws Exception {
                when(backupService.performBackup())
                                .thenThrow(new RuntimeException("Disk full"));

                mockMvc.perform(post("/slib/system/backup"))
                                .andExpect(status().isInternalServerError())
                                .andExpect(jsonPath("$.status").value("FAILED"));

                verify(backupService, times(1)).performBackup();
        }
}

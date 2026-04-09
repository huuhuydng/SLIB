package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import slib.com.example.controller.system.BackupController;
import slib.com.example.entity.system.BackupScheduleEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.system.BackupScheduleRepository;
import slib.com.example.service.system.BackupService;
import slib.com.example.service.system.SystemLogService;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-60: Set automatic backup schedule
 * Test Report: doc/Report/UnitTestReport/FE59_TestReport.md
 */
@WebMvcTest(value = BackupController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-60: Set automatic backup schedule - Unit Tests")
class FE60_AutoBackupScheduleTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private BackupService backupService;

        @MockBean
        private BackupScheduleRepository backupScheduleRepository;

        @MockBean
        private SystemLogService systemLogService;

        // =========================================
        // === UTCID01: Load current backup schedule ===
        // =========================================

        /**
         * UTCID01: Load current backup schedule
         * Precondition: Admin opens backup schedule settings
         * Expected: 200 OK with schedule data
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("UTCID01: Load current backup schedule returns 200 OK")
        void getSchedule_loadCurrent_returns200OK() throws Exception {
                BackupScheduleEntity schedule = BackupScheduleEntity.builder()
                                .id(1)
                                .scheduleName("Daily Backup")
                                .cronExpression("03:00")
                                .backupType(BackupScheduleEntity.BackupType.FULL)
                                .retainDays(30)
                                .isActive(true)
                                .nextBackupAt(LocalDateTime.now().plusDays(1))
                                .build();

                when(backupScheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(schedule));

                mockMvc.perform(get("/slib/system/backup/schedule"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.scheduleName").value("Daily Backup"))
                                .andExpect(jsonPath("$.time").value("03:00"))
                                .andExpect(jsonPath("$.retainDays").value(30))
                                .andExpect(jsonPath("$.isActive").value(true));

                verify(backupScheduleRepository, times(1)).findFirstByOrderByIdAsc();
        }

        // =========================================
        // === UTCID02: Save valid time, retainDays, and active flag ===
        // =========================================

        /**
         * UTCID02: Save valid time, retainDays, and active flag
         * Precondition: Admin opens backup schedule settings
         * Expected: 200 OK with updated schedule
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("UTCID02: Save valid schedule settings returns 200 OK")
        void updateSchedule_validSettings_returns200OK() throws Exception {
                BackupScheduleEntity existing = BackupScheduleEntity.builder()
                                .id(1)
                                .scheduleName("Daily Backup")
                                .cronExpression("03:00")
                                .backupType(BackupScheduleEntity.BackupType.FULL)
                                .retainDays(30)
                                .isActive(false)
                                .build();

                when(backupScheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));
                when(backupScheduleRepository.save(any(BackupScheduleEntity.class))).thenReturn(existing);

                Map<String, Object> request = new HashMap<>();
                request.put("time", "04:30");
                request.put("retainDays", 14);
                request.put("isActive", true);

                mockMvc.perform(put("/slib/system/backup/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.scheduleName").exists());

                verify(backupScheduleRepository, times(1)).save(any(BackupScheduleEntity.class));
        }

        // =========================================
        // === UTCID03: Save invalid time format ===
        // =========================================

        /**
         * UTCID03: Save invalid time format and use fallback nextBackupAt
         * Precondition: Admin opens backup schedule settings
         * Expected: 200 OK with fallback nextBackupAt (controller handles parse error gracefully)
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("UTCID03: Save invalid time format returns 400 Bad Request")
        void updateSchedule_invalidTimeFormat_returns400BadRequest() throws Exception {
                Map<String, Object> request = new HashMap<>();
                request.put("time", "invalid-time-format");
                request.put("retainDays", 7);
                request.put("isActive", true);

                mockMvc.perform(put("/slib/system/backup/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Bad Request"))
                                .andExpect(jsonPath("$.errors.time").value("Thời gian sao lưu phải theo định dạng HH:mm"));

                verify(backupScheduleRepository, never()).save(any(BackupScheduleEntity.class));
        }

        // =========================================
        // === UTCID04: Disable automatic backup schedule ===
        // =========================================

        /**
         * UTCID04: Disable automatic backup schedule
         * Precondition: Admin opens backup schedule settings
         * Expected: 200 OK with isActive=false
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("UTCID04: Disable automatic backup schedule returns 200 OK")
        void updateSchedule_disableSchedule_returns200OK() throws Exception {
                BackupScheduleEntity existing = BackupScheduleEntity.builder()
                                .id(1)
                                .scheduleName("Daily Backup")
                                .cronExpression("03:00")
                                .backupType(BackupScheduleEntity.BackupType.FULL)
                                .retainDays(30)
                                .isActive(true)
                                .build();

                when(backupScheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(existing));
                when(backupScheduleRepository.save(any(BackupScheduleEntity.class))).thenAnswer(invocation -> {
                        BackupScheduleEntity saved = invocation.getArgument(0);
                        return saved;
                });

                Map<String, Object> request = new HashMap<>();
                request.put("time", "03:00");
                request.put("retainDays", 30);
                request.put("isActive", false);

                mockMvc.perform(put("/slib/system/backup/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isActive").value(false));

                verify(backupScheduleRepository, times(1)).save(any(BackupScheduleEntity.class));
        }

        // =========================================
        // === UTCID05: Unexpected schedule persistence failure ===
        // =========================================

        /**
         * UTCID05: Unexpected schedule persistence failure
         * Precondition: Admin opens backup schedule settings
         * Expected: 500 Internal Server Error
         */
        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("UTCID05: Unexpected schedule persistence failure returns 500")
        void updateSchedule_persistenceFailure_returns500() throws Exception {
                when(backupScheduleRepository.findFirstByOrderByIdAsc())
                                .thenThrow(new RuntimeException("Database connection lost"));

                Map<String, Object> request = new HashMap<>();
                request.put("time", "03:00");
                request.put("retainDays", 7);
                request.put("isActive", true);

                mockMvc.perform(put("/slib/system/backup/schedule")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }
}

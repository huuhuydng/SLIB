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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.system.BackupController;
import slib.com.example.entity.system.BackupHistoryEntity;
import slib.com.example.entity.system.BackupScheduleEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.BackupScheduleRepository;
import slib.com.example.service.BackupService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = BackupController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("BackupController Unit Tests")
class BackupControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BackupService backupService;

    @MockBean
    private BackupScheduleRepository backupScheduleRepository;

    @Test
    @DisplayName("triggerBackup_success_returns200")
    void triggerBackup_success_returns200() throws Exception {
        BackupHistoryEntity result = BackupHistoryEntity.builder()
                .id(UUID.randomUUID())
                .status(BackupHistoryEntity.BackupStatus.SUCCESS)
                .filePath("/tmp/slib-backups/test.sql")
                .fileSizeBytes(1024L)
                .startedAt(LocalDateTime.now())
                .completedAt(LocalDateTime.now())
                .build();

        when(backupService.performBackup()).thenReturn(result);

        mockMvc.perform(post("/slib/system/backup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.message").value("Sao lưu thành công"));
    }

    @Test
    @DisplayName("triggerBackup_failure_returns500")
    void triggerBackup_failure_returns500() throws Exception {
        when(backupService.performBackup()).thenThrow(new RuntimeException("pg_dump not found"));

        mockMvc.perform(post("/slib/system/backup"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    @DisplayName("getHistory_returns200")
    void getHistory_returns200() throws Exception {
        when(backupService.getHistory()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/system/backup/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("getSchedule_noExisting_createsDefault")
    void getSchedule_noExisting_createsDefault() throws Exception {
        when(backupScheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(backupScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(get("/slib/system/backup/schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").value("03:00"))
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("updateSchedule_valid_returns200")
    void updateSchedule_valid_returns200() throws Exception {
        BackupScheduleEntity schedule = BackupScheduleEntity.builder()
                .id(1)
                .scheduleName("Daily Backup")
                .cronExpression("03:00")
                .backupType(BackupScheduleEntity.BackupType.FULL)
                .retainDays(30)
                .isActive(false)
                .build();

        when(backupScheduleRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(schedule));
        when(backupScheduleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        mockMvc.perform(put("/slib/system/backup/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"time\": \"04:00\", \"retainDays\": 14, \"isActive\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.time").value("04:00"))
                .andExpect(jsonPath("$.retainDays").value(14))
                .andExpect(jsonPath("$.isActive").value(true));
    }
}

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.system.SystemLogController;
import slib.com.example.entity.system.SystemLogEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.system.SystemLogService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-58: View system log
 * Test Report: doc/Report/UnitTestReport/FE57_TestReport.md
 */
@WebMvcTest(value = SystemLogController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-58: View system log - Unit Tests")
class FE58_ViewSystemLogTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private SystemLogService systemLogService;

        // =========================================
        // === UTCID01: Load first page of system logs ===
        // =========================================

        /**
         * UTCID01: Load first page of system logs
         * Precondition: Admin opens the system log tab
         * Expected: 200 OK with paginated content
         */
        @Test
        @DisplayName("UTCID01: Load first page of system logs returns 200 OK")
        void getLogs_firstPage_returns200OK() throws Exception {
                SystemLogEntity log = SystemLogEntity.builder()
                                .id(UUID.randomUUID())
                                .level(SystemLogEntity.LogLevel.INFO)
                                .category(SystemLogEntity.LogCategory.AUDIT)
                                .message("Test log message")
                                .createdAt(LocalDateTime.now())
                                .build();
                Page<SystemLogEntity> page = new PageImpl<>(List.of(log), PageRequest.of(0, 20), 1);

                when(systemLogService.getLogs(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                                .thenReturn(page);

                mockMvc.perform(get("/slib/system/logs"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray())
                                .andExpect(jsonPath("$.totalElements").value(1))
                                .andExpect(jsonPath("$.page").value(0));

                verify(systemLogService, times(1)).getLogs(isNull(), isNull(), isNull(), isNull(), isNull(), eq(0), eq(20));
        }

        // =========================================
        // === UTCID02: Filter logs by level/category ===
        // =========================================

        /**
         * UTCID02: Filter logs by level/category
         * Precondition: Admin opens the system log tab
         * Expected: 200 OK with filtered results
         */
        @Test
        @DisplayName("UTCID02: Filter logs by level/category returns 200 OK")
        void getLogs_filterByLevelAndCategory_returns200OK() throws Exception {
                Page<SystemLogEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

                when(systemLogService.getLogs(eq("ERROR"), eq("SYSTEM_ERROR"), isNull(), isNull(), isNull(), eq(0), eq(20)))
                                .thenReturn(page);

                mockMvc.perform(get("/slib/system/logs")
                                .param("level", "ERROR")
                                .param("category", "SYSTEM_ERROR"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());

                verify(systemLogService, times(1)).getLogs(eq("ERROR"), eq("SYSTEM_ERROR"), isNull(), isNull(), isNull(), eq(0), eq(20));
        }

        // =========================================
        // === UTCID03: Search logs by message keyword ===
        // =========================================

        /**
         * UTCID03: Search logs by message keyword
         * Precondition: Admin opens the system log tab
         * Expected: 200 OK with search results
         */
        @Test
        @DisplayName("UTCID03: Search logs by message keyword returns 200 OK")
        void getLogs_searchByKeyword_returns200OK() throws Exception {
                Page<SystemLogEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

                when(systemLogService.getLogs(isNull(), isNull(), eq("backup"), isNull(), isNull(), eq(0), eq(20)))
                                .thenReturn(page);

                mockMvc.perform(get("/slib/system/logs")
                                .param("search", "backup"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());

                verify(systemLogService, times(1)).getLogs(isNull(), isNull(), eq("backup"), isNull(), isNull(), eq(0), eq(20));
        }

        // =========================================
        // === UTCID04: Query logs by date range ===
        // =========================================

        /**
         * UTCID04: Query logs by date range
         * Precondition: Admin opens the system log tab
         * Expected: 200 OK with date-filtered results
         */
        @Test
        @DisplayName("UTCID04: Query logs by date range returns 200 OK")
        void getLogs_queryByDateRange_returns200OK() throws Exception {
                Page<SystemLogEntity> page = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);

                when(systemLogService.getLogs(isNull(), isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), eq(0), eq(20)))
                                .thenReturn(page);

                mockMvc.perform(get("/slib/system/logs")
                                .param("startDate", "2026-03-01T00:00:00")
                                .param("endDate", "2026-03-11T23:59:59"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isArray());

                verify(systemLogService, times(1)).getLogs(isNull(), isNull(), isNull(), any(LocalDateTime.class), any(LocalDateTime.class), eq(0), eq(20));
        }

        // =========================================
        // === UTCID05: Request a page with no results ===
        // =========================================

        /**
         * UTCID05: Request a page with no results
         * Precondition: Admin opens the system log tab
         * Expected: 200 OK with empty content
         */
        @Test
        @DisplayName("UTCID05: Request a page with no results returns 200 OK with empty content")
        void getLogs_emptyPage_returns200OK() throws Exception {
                Page<SystemLogEntity> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(5, 20), 0);

                when(systemLogService.getLogs(isNull(), isNull(), isNull(), isNull(), isNull(), eq(5), eq(20)))
                                .thenReturn(emptyPage);

                mockMvc.perform(get("/slib/system/logs")
                                .param("page", "5"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content").isEmpty())
                                .andExpect(jsonPath("$.totalElements").value(0));

                verify(systemLogService, times(1)).getLogs(isNull(), isNull(), isNull(), isNull(), isNull(), eq(5), eq(20));
        }

        // =========================================
        // === UTCID06: Load log-level statistics ===
        // =========================================

        /**
         * UTCID06: Load log-level statistics
         * Precondition: Admin opens the system log tab
         * Expected: 200 OK with stats map
         */
        @Test
        @DisplayName("UTCID06: Load log-level statistics returns 200 OK")
        void getStats_loadStatistics_returns200OK() throws Exception {
                Map<String, Object> stats = Map.of(
                                "ERROR", 5,
                                "WARN", 12,
                                "INFO", 100,
                                "DEBUG", 30);

                when(systemLogService.getStats(isNull(), isNull())).thenReturn(stats);

                mockMvc.perform(get("/slib/system/logs/stats"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ERROR").value(5))
                                .andExpect(jsonPath("$.WARN").value(12));

                verify(systemLogService, times(1)).getStats(isNull(), isNull());
        }
}

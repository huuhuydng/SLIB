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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.system.SystemLogController;
import slib.com.example.entity.system.SystemLogEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.SystemLogService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = SystemLogController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SystemLogController Unit Tests")
class SystemLogControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SystemLogService systemLogService;

    @Test
    @DisplayName("getLogs_noFilter_returns200")
    void getLogs_noFilter_returns200() throws Exception {
        Page<SystemLogEntity> emptyPage = new PageImpl<>(Collections.emptyList());
        when(systemLogService.getLogs(any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(emptyPage);

        mockMvc.perform(get("/slib/system/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @DisplayName("getLogs_withLevelFilter_returns200")
    void getLogs_withLevelFilter_returns200() throws Exception {
        SystemLogEntity logEntry = SystemLogEntity.builder()
                .id(UUID.randomUUID())
                .level(SystemLogEntity.LogLevel.ERROR)
                .category(SystemLogEntity.LogCategory.SYSTEM_ERROR)
                .service("TestService")
                .message("Test error message")
                .createdAt(LocalDateTime.now())
                .build();

        Page<SystemLogEntity> page = new PageImpl<>(Collections.singletonList(logEntry));
        when(systemLogService.getLogs(eq("ERROR"), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/slib/system/logs").param("level", "ERROR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("getStats_returns200WithCounts")
    void getStats_returns200WithCounts() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalErrors", 5L);
        stats.put("totalWarnings", 10L);
        stats.put("errorsLast24h", 2L);

        when(systemLogService.getStats(any(), any())).thenReturn(stats);

        mockMvc.perform(get("/slib/system/logs/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalErrors").value(5));
    }
}

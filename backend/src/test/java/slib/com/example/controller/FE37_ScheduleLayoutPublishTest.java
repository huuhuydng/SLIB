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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.zone_config.LayoutAdminController;
import slib.com.example.dto.zone_config.LayoutValidationResponse;
import slib.com.example.dto.zone_config.LayoutScheduleRequest;
import slib.com.example.dto.zone_config.LayoutScheduleResponse;
import slib.com.example.dto.zone_config.LayoutSnapshotRequest;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.zone_config.LayoutAdminService;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LayoutAdminController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-37: Schedule layout publish - Unit Tests")
class FE37_ScheduleLayoutPublishTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LayoutAdminService layoutAdminService;

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: Schedule layout publish with valid snapshot returns 200 OK")
    void schedulePublish_validRequest_returns200() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .snapshot(new LayoutSnapshotRequest())
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 22, 0))
                .build();

        LayoutScheduleResponse response = LayoutScheduleResponse.builder()
                .scheduleId(5L)
                .status("PENDING")
                .retryCount(0)
                .maxRetryCount(3)
                .build();

        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(5))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Schedule layout publish with version conflict returns 409 Conflict")
    void schedulePublish_versionConflict_returns409() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .snapshot(new LayoutSnapshotRequest())
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 22, 0))
                .build();

        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any()))
                .thenThrow(new IllegalStateException("Sơ đồ đã có phiên bản mới"));

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_VERSION_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Schedule layout publish with business conflict returns 409 Conflict")
    void schedulePublish_businessConflict_returns409() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .snapshot(new LayoutSnapshotRequest())
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 22, 0))
                .build();

        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(true)
                        .publishable(false)
                        .build());
        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Chưa thể lên lịch vì ghế A5 đang được sử dụng"));

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Chưa thể lên lịch vì ghế A5 đang được sử dụng"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: Schedule layout publish with invalid scheduled time returns 409 Conflict")
    void schedulePublish_invalidScheduledTime_returns409() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .snapshot(new LayoutSnapshotRequest())
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 8, 0))
                .build();

        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(true)
                        .publishable(true)
                        .build());
        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Thời điểm áp dụng phải lớn hơn thời gian hiện tại"));

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Thời điểm áp dụng phải lớn hơn thời gian hiện tại"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: Schedule layout publish without snapshot returns 409 Conflict")
    void schedulePublish_missingSnapshot_returns409() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 22, 0))
                .build();

        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Thiếu snapshot sơ đồ để lên lịch xuất bản"));

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Thiếu snapshot sơ đồ để lên lịch xuất bản"))
                .andExpect(jsonPath("$.validation").doesNotExist());
    }

    @Test
    @DisplayName("UTCID06: Schedule layout publish without permission returns 403 Forbidden")
    void schedulePublish_withoutPermission_returns403() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .snapshot(new LayoutSnapshotRequest())
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 22, 0))
                .build();

        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any()))
                .thenThrow(new AccessDeniedException("Bạn không có quyền lên lịch sơ đồ"));

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID07: Schedule layout publish when service fails unexpectedly returns 500 Internal Server Error")
    void schedulePublish_serviceFails_returns500() throws Exception {
        LayoutScheduleRequest request = LayoutScheduleRequest.builder()
                .snapshot(new LayoutSnapshotRequest())
                .scheduledFor(LocalDateTime.of(2026, 4, 20, 22, 0))
                .build();

        when(layoutAdminService.schedulePublish(any(LayoutScheduleRequest.class), any()))
                .thenThrow(new RuntimeException("Không thể lên lịch sơ đồ"));

        mockMvc.perform(post("/slib/layout-admin/schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}

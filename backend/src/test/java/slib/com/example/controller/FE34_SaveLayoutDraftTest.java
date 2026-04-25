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
import slib.com.example.dto.zone_config.LayoutDraftResponse;
import slib.com.example.dto.zone_config.LayoutSnapshotRequest;
import slib.com.example.dto.zone_config.LayoutValidationResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.zone_config.LayoutAdminService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = LayoutAdminController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-34: Save layout draft - Unit Tests")
class FE34_SaveLayoutDraftTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LayoutAdminService layoutAdminService;

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: Save layout draft with valid snapshot returns 200 OK")
    void saveDraft_validSnapshot_returns200() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();
        request.setBasedOnPublishedVersion(12L);

        LayoutDraftResponse response = LayoutDraftResponse.builder()
                .hasDraft(true)
                .basedOnPublishedVersion(12L)
                .updatedByName("Nguyen Huu Huy")
                .build();

        when(layoutAdminService.saveDraft(any(LayoutSnapshotRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/slib/layout-admin/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasDraft").value(true))
                .andExpect(jsonPath("$.basedOnPublishedVersion").value(12));

        verify(layoutAdminService, times(1)).saveDraft(any(LayoutSnapshotRequest.class), any());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Save layout draft with conflicted snapshot returns 409 Conflict")
    void saveDraft_conflictedSnapshot_returns409() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();
        request.setBasedOnPublishedVersion(12L);

        LayoutValidationResponse validation = LayoutValidationResponse.builder()
                .valid(true)
                .publishable(false)
                .build();

        when(layoutAdminService.saveDraft(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Sơ đồ đang có xung đột"));
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class))).thenReturn(validation);

        mockMvc.perform(post("/slib/layout-admin/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_CONFLICT"))
                .andExpect(jsonPath("$.validation.publishable").value(false));
    }

    @Test
    @DisplayName("UTCID03: Save layout draft with empty body returns 400 Bad Request")
    void saveDraft_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/slib/layout-admin/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UTCID04: Save layout draft without permission returns 403 Forbidden")
    void saveDraft_withoutPermission_returns403() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();
        request.setBasedOnPublishedVersion(12L);

        when(layoutAdminService.saveDraft(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new AccessDeniedException("Bạn không có quyền lưu sơ đồ nháp"));

        mockMvc.perform(post("/slib/layout-admin/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: Save layout draft when service fails unexpectedly returns 500 Internal Server Error")
    void saveDraft_serviceFails_returns500() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();
        request.setBasedOnPublishedVersion(12L);

        when(layoutAdminService.saveDraft(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new RuntimeException("Không thể lưu sơ đồ nháp"));

        mockMvc.perform(post("/slib/layout-admin/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}

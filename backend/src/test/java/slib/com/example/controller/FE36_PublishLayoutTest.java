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
import slib.com.example.dto.zone_config.LayoutPublishResponse;
import slib.com.example.dto.zone_config.LayoutSnapshotRequest;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.zone_config.LayoutAdminService;

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
@DisplayName("FE-36: Publish layout - Unit Tests")
class FE36_PublishLayoutTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LayoutAdminService layoutAdminService;

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: Publish layout with valid snapshot returns 200 OK")
    void publish_validSnapshot_returns200() throws Exception {
        LayoutPublishResponse response = LayoutPublishResponse.builder()
                .publishedVersion(32L)
                .publishedByName("Nguyen Huu Huy")
                .build();

        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publishedVersion").value(32))
                .andExpect(jsonPath("$.publishedByName").value("Nguyen Huu Huy"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Publish layout with version conflict returns 409 Conflict")
    void publish_versionConflict_returns409() throws Exception {
        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalStateException("Sơ đồ đã có phiên bản mới"));

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_VERSION_CONFLICT"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Publish layout with business conflict returns 409 Conflict")
    void publish_layoutConflict_returns409() throws Exception {
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(true)
                        .publishable(false)
                        .build());
        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Chưa thể xuất bản vì ghế A5 đang được sử dụng"));

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_CONFLICT"))
                .andExpect(jsonPath("$.message").value("Chưa thể xuất bản vì ghế A5 đang được sử dụng"));
    }

    @Test
    @DisplayName("UTCID04: Publish layout without permission returns 403 Forbidden")
    void publish_withoutPermission_returns403() throws Exception {
        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new AccessDeniedException("Bạn không có quyền xuất bản sơ đồ"));

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("UTCID05: Publish layout with empty body returns 400 Bad Request")
    void publish_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID06: Publish layout when service fails unexpectedly returns 500 Internal Server Error")
    void publish_serviceFails_returns500() throws Exception {
        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new RuntimeException("Không thể xuất bản sơ đồ"));

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}

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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.zone_config.LayoutAdminController;
import slib.com.example.dto.zone_config.LayoutConflictResponse;
import slib.com.example.dto.zone_config.LayoutSnapshotRequest;
import slib.com.example.dto.zone_config.LayoutValidationResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.zone_config.LayoutAdminService;

import java.util.List;

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
@DisplayName("FE-35: Validate layout before publish - Unit Tests")
class FE35_ValidateLayoutBeforePublishTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LayoutAdminService layoutAdminService;

    @Test
    @DisplayName("UTCID01: Validate layout with valid and publishable snapshot returns 200 OK")
    void validate_publishableSnapshot_returns200() throws Exception {
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(true)
                        .publishable(true)
                        .build());

        mockMvc.perform(post("/slib/layout-admin/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.publishable").value(true));
    }

    @Test
    @DisplayName("UTCID02: Validate layout with business conflict returns 200 and publishable false")
    void validate_occupiedSeatConflict_returns200WithPublishableFalse() throws Exception {
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(true)
                        .publishable(false)
                        .conflicts(List.of(LayoutConflictResponse.builder()
                                .code("PUBLISH_SEAT_IN_USE")
                                .message("Ghế A5 đang được sử dụng")
                                .build()))
                        .build());

        mockMvc.perform(post("/slib/layout-admin/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.publishable").value(false))
                .andExpect(jsonPath("$.conflicts[0].code").value("PUBLISH_SEAT_IN_USE"));
    }

    @Test
    @DisplayName("UTCID03: Validate layout with active booking conflict returns 200 and publishable false")
    void validate_activeBookingConflict_returns200WithPublishableFalse() throws Exception {
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(true)
                        .publishable(false)
                        .conflicts(List.of(LayoutConflictResponse.builder()
                                .code("PUBLISH_SEAT_IN_USE")
                                .message("Ghế A5 đang có lượt giữ chỗ đang hiệu lực")
                                .build()))
                        .build());

        mockMvc.perform(post("/slib/layout-admin/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.publishable").value(false))
                .andExpect(jsonPath("$.conflicts[0].message").value("Ghế A5 đang có lượt giữ chỗ đang hiệu lực"));
    }

    @Test
    @DisplayName("UTCID04: Validate layout with invalid geometric snapshot returns 409 Conflict")
    void validate_invalidGeometricSnapshot_returns409() throws Exception {
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenReturn(LayoutValidationResponse.builder()
                        .valid(false)
                        .publishable(false)
                        .conflicts(List.of(LayoutConflictResponse.builder()
                                .code("GEOMETRY_OVERLAP")
                                .message("Ghế bị chồng lên nhau")
                                .build()))
                        .build());

        mockMvc.perform(post("/slib/layout-admin/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.conflicts[0].code").value("GEOMETRY_OVERLAP"));
    }

    @Test
    @DisplayName("UTCID05: Validate layout with empty body returns 400 Bad Request")
    void validate_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/slib/layout-admin/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UTCID06: Validate layout when service fails unexpectedly returns 500 Internal Server Error")
    void validate_serviceFails_returns500() throws Exception {
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class)))
                .thenThrow(new RuntimeException("Không thể kiểm tra xung đột sơ đồ"));

        mockMvc.perform(post("/slib/layout-admin/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}

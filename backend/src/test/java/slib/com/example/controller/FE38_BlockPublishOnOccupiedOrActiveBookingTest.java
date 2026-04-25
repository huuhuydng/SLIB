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
@DisplayName("FE-38: Block publish when occupied seats or active booking are affected - Unit Tests")
class FE38_BlockPublishOnOccupiedOrActiveBookingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LayoutAdminService layoutAdminService;

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: Publish is blocked when occupied seat is affected")
    void publish_occupiedSeatConflict_returns409() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();

        LayoutValidationResponse validation = LayoutValidationResponse.builder()
                .valid(true)
                .publishable(false)
                .conflicts(List.of(LayoutConflictResponse.builder()
                        .code("PUBLISH_SEAT_IN_USE")
                        .title("Ghế đang được sử dụng hoặc giữ chỗ")
                        .message("A5 đang được sử dụng")
                        .build()))
                .build();

        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Chưa thể xuất bản vì A5 đang được sử dụng"));
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class))).thenReturn(validation);

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("LAYOUT_CONFLICT"))
                .andExpect(jsonPath("$.validation.publishable").value(false))
                .andExpect(jsonPath("$.validation.conflicts[0].code").value("PUBLISH_SEAT_IN_USE"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Publish is blocked when active booking is affected")
    void publish_activeBookingConflict_returns409() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();

        LayoutValidationResponse validation = LayoutValidationResponse.builder()
                .valid(true)
                .publishable(false)
                .conflicts(List.of(LayoutConflictResponse.builder()
                        .code("PUBLISH_SEAT_IN_USE")
                        .title("Ghế đang được sử dụng hoặc giữ chỗ")
                        .message("A5 đang có lượt giữ chỗ đang hiệu lực")
                        .build()))
                .build();

        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Chưa thể xuất bản vì A5 đang có lượt giữ chỗ đang hiệu lực"));
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class))).thenReturn(validation);

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.validation.conflicts[0].message").value("A5 đang có lượt giữ chỗ đang hiệu lực"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Publish is blocked when multiple seats conflict at the same time")
    void publish_multipleSeatConflicts_returns409() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();

        LayoutValidationResponse validation = LayoutValidationResponse.builder()
                .valid(true)
                .publishable(false)
                .conflicts(List.of(
                        LayoutConflictResponse.builder()
                                .code("PUBLISH_SEAT_IN_USE")
                                .title("Ghế đang được sử dụng hoặc giữ chỗ")
                                .message("A4 đang được sử dụng")
                                .build(),
                        LayoutConflictResponse.builder()
                                .code("PUBLISH_FUTURE_BOOKING_IMPACT")
                                .title("Ghế đang có lịch đặt sắp tới")
                                .message("A5 sẽ bị gỡ hoặc ngừng hoạt động")
                                .build()))
                .build();

        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Chưa thể xuất bản vì có nhiều ghế bị ảnh hưởng"));
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class))).thenReturn(validation);

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.validation.conflicts[0].code").value("PUBLISH_SEAT_IN_USE"))
                .andExpect(jsonPath("$.validation.conflicts[1].code").value("PUBLISH_FUTURE_BOOKING_IMPACT"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: Publish conflict response returns full validation payload")
    void publish_conflictResponseContainsValidationPayload() throws Exception {
        LayoutSnapshotRequest request = new LayoutSnapshotRequest();

        LayoutValidationResponse validation = LayoutValidationResponse.builder()
                .valid(true)
                .publishable(false)
                .conflicts(List.of(LayoutConflictResponse.builder()
                        .code("PUBLISH_SEAT_IN_USE")
                        .title("Ghế đang được sử dụng hoặc giữ chỗ")
                        .message("A5 đang được sử dụng")
                        .severity("ERROR")
                        .entityType("SEAT")
                        .entityKey("A5")
                        .build()))
                .build();

        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Chưa thể xuất bản vì A5 đang được sử dụng"));
        when(layoutAdminService.validate(any(LayoutSnapshotRequest.class))).thenReturn(validation);

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.validation.conflicts[0].severity").value("ERROR"))
                .andExpect(jsonPath("$.validation.conflicts[0].entityType").value("SEAT"))
                .andExpect(jsonPath("$.validation.conflicts[0].entityKey").value("A5"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: Publish conflict check when service fails unexpectedly returns 500 Internal Server Error")
    void publish_conflictCheckServiceFails_returns500() throws Exception {
        when(layoutAdminService.publish(any(LayoutSnapshotRequest.class), any()))
                .thenThrow(new RuntimeException("Không thể kiểm tra xung đột xuất bản"));

        mockMvc.perform(post("/slib/layout-admin/publish")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LayoutSnapshotRequest())))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}

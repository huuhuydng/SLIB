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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.entity.users.User;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-20: Change user status - Unit Tests")
class FE20_ChangeUserStatusTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private AuthService authService;

    @MockBean
    private CloudinaryService cloudinaryService;

    @MockBean
    private AsyncImportService asyncImportService;

    @MockBean
    private StagingImportService stagingImportService;

    @MockBean
    private SystemLogService systemLogService;

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: Lock user account with valid reason")
    void lockUserAccount_withValidReason() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        User mockUser = User.builder().isActive(false).lockReason("Vi phạm nội quy").build();
        when(userService.toggleUserActive(userId, false, "Vi phạm nội quy")).thenReturn(mockUser);

        mockMvc.perform(patch("/slib/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": false,
                                  "reason": "Vi phạm nội quy"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã khóa tài khoản"))
                .andExpect(jsonPath("$.isActive").value(false))
                .andExpect(jsonPath("$.lockReason").value("Vi phạm nội quy"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Unlock user account")
    void unlockUserAccount() throws Exception {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        User mockUser = User.builder().isActive(true).lockReason(null).build();
        when(userService.toggleUserActive(userId, true, null)).thenReturn(mockUser);

        mockMvc.perform(patch("/slib/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã mở khóa tài khoản"))
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Change user status with invalid UUID format")
    void changeUserStatus_withInvalidUuidFormat() throws Exception {
        mockMvc.perform(patch("/slib/users/invalid-uuid/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": false,
                                  "reason": "Vi phạm nội quy"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: Change user status without isActive field")
    void changeUserStatus_withoutIsActiveField() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(patch("/slib/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Thiếu trạng thái"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("isActive field is required"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: Lock user account without reason")
    void lockUserAccount_withoutReason() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(userService.toggleUserActive(userId, false, null))
                .thenThrow(new RuntimeException("Vui lòng nhập lý do khóa tài khoản"));

        mockMvc.perform(patch("/slib/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Vui lòng nhập lý do khóa tài khoản"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID06: Change status for non-existent userId")
    void changeStatus_forNonExistentUserId() throws Exception {
        UUID userId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        when(userService.toggleUserActive(any(UUID.class), anyBoolean(), any()))
                .thenThrow(new RuntimeException("User không tồn tại với ID: " + userId));

        mockMvc.perform(patch("/slib/users/{userId}/status", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "isActive": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User không tồn tại với ID: " + userId));
    }
}

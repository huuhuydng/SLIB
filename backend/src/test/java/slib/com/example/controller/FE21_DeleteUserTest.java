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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;

import java.util.UUID;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-21: Delete user account - Unit Tests")
class FE21_DeleteUserTest {

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
    @DisplayName("UTCID01: Delete user with valid admin token")
    void deleteUser_withValidAdminToken() throws Exception {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        doNothing().when(userService).deleteUserById(userId);

        mockMvc.perform(delete("/slib/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã xóa vĩnh viễn người dùng và dữ liệu liên quan"))
                .andExpect(jsonPath("$.userId").value(userId.toString()));
    }

    @Test
    @DisplayName("UTCID02: Delete user without authentication token")
    void deleteUser_withoutAuthenticationToken() throws Exception {
        UUID userId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(delete("/slib/users/{userId}", userId))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Delete user with invalid UUID format")
    void deleteUser_withInvalidUuidFormat() throws Exception {
        mockMvc.perform(delete("/slib/users/invalid-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: Delete non-existent userId")
    void deleteUser_withNonExistentUserId() throws Exception {
        UUID userId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        doThrow(new RuntimeException("User không tồn tại với ID: " + userId)).when(userService).deleteUserById(userId);

        mockMvc.perform(delete("/slib/users/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("User không tồn tại với ID: " + userId));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: Delete user with active bookings dependency")
    void deleteUser_withActiveBookingsDependency() throws Exception {
        UUID userId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        doThrow(new RuntimeException("Người dùng đang có dữ liệu liên quan không thể xóa")).when(userService).deleteUserById(userId);

        mockMvc.perform(delete("/slib/users/{userId}", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Người dùng đang có dữ liệu liên quan không thể xóa"));
    }
}

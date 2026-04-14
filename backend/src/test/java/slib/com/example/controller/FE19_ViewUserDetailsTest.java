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
import slib.com.example.dto.users.AdminUserListItemResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-19: View user details - Unit Tests")
class FE19_ViewUserDetailsTest {

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

    private AdminUserListItemResponse buildUser(UUID id, String email, String fullName, String role,
            Boolean active, String phone, String lockReason) {
        return AdminUserListItemResponse.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .userCode("SE" + id.toString().substring(0, 6))
                .role(role)
                .isActive(active)
                .lockReason(lockReason)
                .reputationScore(100)
                .avtUrl("https://example.com/avatar.png")
                .passwordChanged(true)
                .phone(phone)
                .dob(LocalDate.of(2003, 3, 15))
                .createdAt(LocalDateTime.of(2026, 4, 9, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 9, 9, 0))
                .build();
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: View admin user list with full detail fields")
    void viewAdminUserList_withFullDetailFields() throws Exception {
        when(userService.getAdminUsers(null, null, null)).thenReturn(List.of(
                buildUser(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "student1@fpt.edu.vn", "Nguyen Van A", "STUDENT", true, "0901234567", null)));

        mockMvc.perform(get("/slib/users/admin/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student1@fpt.edu.vn"))
                .andExpect(jsonPath("$[0].phone").value("0901234567"))
                .andExpect(jsonPath("$[0].passwordChanged").value(true))
                .andExpect(jsonPath("$[0].reputationScore").value(100));

        verify(userService, times(1)).getAdminUsers(null, null, null);
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: View admin user list with role status and search filters")
    void viewAdminUserList_withRoleStatusAndSearchFilters() throws Exception {
        when(userService.getAdminUsers(Role.LIBRARIAN, false, "thu")).thenReturn(List.of(
                buildUser(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "librarian@fpt.edu.vn", "Tran Thu Thu", "LIBRARIAN", false, null, "Vi phạm nội quy")));

        mockMvc.perform(get("/slib/users/admin/list")
                        .param("role", "LIBRARIAN")
                        .param("status", "locked")
                        .param("search", "thu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("LIBRARIAN"))
                .andExpect(jsonPath("$[0].isActive").value(false))
                .andExpect(jsonPath("$[0].lockReason").value("Vi phạm nội quy"));

        verify(userService, times(1)).getAdminUsers(Role.LIBRARIAN, false, "thu");
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: View admin user list returns empty list")
    void viewAdminUserList_returnsEmptyList() throws Exception {
        when(userService.getAdminUsers(null, null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/users/admin/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: View admin user list with invalid role filter")
    void viewAdminUserList_withInvalidRoleFilter() throws Exception {
        mockMvc.perform(get("/slib/users/admin/list")
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: View admin user list when service throws runtime exception")
    void viewAdminUserList_whenServiceThrowsRuntimeException() throws Exception {
        when(userService.getAdminUsers(null, null, null)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/users/admin/list"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID06: View admin user list with active status alias")
    void viewAdminUserList_withActiveStatusAlias() throws Exception {
        when(userService.getAdminUsers(Role.STUDENT, true, null)).thenReturn(List.of(
                buildUser(UUID.fromString("33333333-3333-3333-3333-333333333333"),
                        "student2@fpt.edu.vn", "Le Van B", "STUDENT", true, null, null)));

        mockMvc.perform(get("/slib/users/admin/list")
                        .param("role", "STUDENT")
                        .param("status", "hoạt động"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student2@fpt.edu.vn"))
                .andExpect(jsonPath("$[0].isActive").value(true));

        verify(userService, times(1)).getAdminUsers(Role.STUDENT, true, null);
    }
}

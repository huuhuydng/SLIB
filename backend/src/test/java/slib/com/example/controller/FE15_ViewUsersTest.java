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
import slib.com.example.dto.users.UserListItemResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;

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
@DisplayName("FE-15: View list of users in the system - Unit Tests")
class FE15_ViewUsersTest {

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

    private UserListItemResponse buildUser(UUID id, String email, String fullName, String role, boolean active) {
        return UserListItemResponse.builder()
                .id(id)
                .email(email)
                .fullName(fullName)
                .userCode("SE" + id.toString().substring(0, 6))
                .role(role)
                .isActive(active)
                .build();
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: View users with default filters")
    void viewUsers_withDefaultFilters() throws Exception {
        when(userService.getAllUsers(null, null, null)).thenReturn(List.of(
                buildUser(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "student1@fpt.edu.vn", "Nguyen Van A", "STUDENT", true)));

        mockMvc.perform(get("/slib/users/getall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("student1@fpt.edu.vn"))
                .andExpect(jsonPath("$[0].role").value("STUDENT"))
                .andExpect(jsonPath("$[0].isActive").value(true));

        verify(userService, times(1)).getAllUsers(null, null, null);
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: View users returns empty list")
    void viewUsers_returnsEmptyList() throws Exception {
        when(userService.getAllUsers(null, null, null)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/slib/users/getall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: View users with role status and search filters")
    void viewUsers_withRoleStatusAndSearchFilters() throws Exception {
        when(userService.getAllUsers(Role.STUDENT, true, "nguyen")).thenReturn(List.of(
                buildUser(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "student2@fpt.edu.vn", "Nguyen Van B", "STUDENT", true)));

        mockMvc.perform(get("/slib/users/getall")
                        .param("role", "STUDENT")
                        .param("status", "active")
                        .param("search", "nguyen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Nguyen Van B"))
                .andExpect(jsonPath("$[0].isActive").value(true));

        verify(userService, times(1)).getAllUsers(Role.STUDENT, true, "nguyen");
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: View users with invalid role filter")
    void viewUsers_withInvalidRoleFilter() throws Exception {
        mockMvc.perform(get("/slib/users/getall")
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: View users when service throws runtime exception")
    void viewUsers_whenServiceThrowsRuntimeException() throws Exception {
        when(userService.getAllUsers(null, null, null)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/slib/users/getall"))
                .andExpect(status().isInternalServerError());
    }
}

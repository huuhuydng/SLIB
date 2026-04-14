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
import slib.com.example.dto.users.AdminUserListItemResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.auth.AuthService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.system.SystemLogService;
import slib.com.example.service.users.AsyncImportService;
import slib.com.example.service.users.StagingImportService;
import slib.com.example.service.users.UserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-18: Add Librarian to the system - Unit Tests")
class FE18_AddLibrarianTest {

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

    private AdminUserListItemResponse buildLibrarian(UUID id, String email, String fullName, String phone) {
        return AdminUserListItemResponse.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .userCode("LIB001")
                .role("LIBRARIAN")
                .isActive(true)
                .lockReason(null)
                .reputationScore(100)
                .avtUrl(null)
                .passwordChanged(false)
                .phone(phone)
                .dob(LocalDate.of(1995, 5, 20))
                .createdAt(LocalDateTime.of(2026, 4, 9, 8, 0))
                .updatedAt(LocalDateTime.of(2026, 4, 9, 8, 0))
                .build();
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID01: Create librarian with valid data")
    void createLibrarian_withValidData() throws Exception {
        when(userService.createUser(any())).thenReturn(
                buildLibrarian(UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "librarian@fpt.edu.vn", "Nguyen Thu Thu", "0901234567"));

        mockMvc.perform(post("/slib/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Thu Thu",
                                  "email": "librarian@fpt.edu.vn",
                                  "userCode": "LIB001",
                                  "phone": "0901234567",
                                  "dob": "1995-05-20",
                                  "role": "LIBRARIAN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã tạo người dùng mới"))
                .andExpect(jsonPath("$.user.email").value("librarian@fpt.edu.vn"))
                .andExpect(jsonPath("$.user.role").value("LIBRARIAN"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID02: Create librarian without phone")
    void createLibrarian_withoutPhone() throws Exception {
        when(userService.createUser(any())).thenReturn(
                buildLibrarian(UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        "librarian2@fpt.edu.vn", "Tran Thu Thu", null));

        mockMvc.perform(post("/slib/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Tran Thu Thu",
                                  "email": "librarian2@fpt.edu.vn",
                                  "userCode": "LIB002",
                                  "role": "LIBRARIAN"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("librarian2@fpt.edu.vn"))
                .andExpect(jsonPath("$.user.phone").doesNotExist());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID03: Create librarian with invalid email format")
    void createLibrarian_withInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/slib/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Thu Thu",
                                  "email": "invalid-email",
                                  "userCode": "LIB003",
                                  "role": "LIBRARIAN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID04: Create librarian with blank fullName")
    void createLibrarian_withBlankFullName() throws Exception {
        mockMvc.perform(post("/slib/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "   ",
                                  "email": "librarian3@fpt.edu.vn",
                                  "userCode": "LIB004",
                                  "role": "LIBRARIAN"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID05: Create librarian with duplicate email")
    void createLibrarian_withDuplicateEmail() throws Exception {
        when(userService.createUser(any())).thenThrow(new RuntimeException("Email đã tồn tại"));

        mockMvc.perform(post("/slib/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Thu Thu",
                                  "email": "exists@fpt.edu.vn",
                                  "userCode": "LIB005",
                                  "role": "LIBRARIAN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Email đã tồn tại"));
    }

    @Test
    @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
    @DisplayName("UTCID06: Create librarian with duplicate userCode")
    void createLibrarian_withDuplicateUserCode() throws Exception {
        when(userService.createUser(any())).thenThrow(new RuntimeException("Mã người dùng đã tồn tại"));

        mockMvc.perform(post("/slib/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Nguyen Thu Thu",
                                  "email": "newlib@fpt.edu.vn",
                                  "userCode": "LIB001",
                                  "role": "LIBRARIAN"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Mã người dùng đã tồn tại"));
    }
}

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.controller.users.UserController;
import slib.com.example.dto.users.AuthResponse;
import slib.com.example.dto.users.ImportUserRequest;
import slib.com.example.dto.users.UserProfileResponse;
import slib.com.example.entity.users.ImportJob;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.entity.users.UserImportStaging;
import slib.com.example.service.AsyncImportService;
import slib.com.example.service.AuthService;
import slib.com.example.service.StagingImportService;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for UserController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Unit Tests")
class UserControllerUnitTest {

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

        @Autowired
        private ObjectMapper objectMapper;

        // =============================================
        // === LOGIN WITH GOOGLE ENDPOINT ===
        // =============================================

        @Test
        @DisplayName("loginWithGoogle_validToken_returns200WithAuthResponse")
        void loginWithGoogle_validToken_returns200WithAuthResponse() throws Exception {
                // Arrange
                String idToken = "valid.google.id.token";
                String fullName = "John Doe";
                String fcmToken = "fcm-device-token-123";

                Map<String, String> request = new HashMap<>();
                request.put("id_token", idToken);
                request.put("full_name", fullName);
                request.put("noti_device", fcmToken);

                AuthResponse response = AuthResponse.builder()
                                .accessToken("jwt-token-abc123")
                                .refreshToken("refresh-token-xyz")
                                .id(UUID.randomUUID().toString())
                                .email("john.doe@fpt.edu.vn")
                                .fullName("John Doe")
                                .userCode("DE123456")
                                .role("STUDENT")
                                .expiresIn(3600L)
                                .build();

                when(authService.loginWithGoogle(eq(idToken), eq(fullName), eq(fcmToken), any()))
                                .thenReturn(response);

                // Act & Assert
                mockMvc.perform(post("/slib/users/login-google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").exists())
                                .andExpect(jsonPath("$.refreshToken").exists())
                                .andExpect(jsonPath("$.email").value("john.doe@fpt.edu.vn"))
                                .andExpect(jsonPath("$.role").value("STUDENT"));

                verify(authService, times(1)).loginWithGoogle(eq(idToken), eq(fullName), eq(fcmToken), any());
        }

        @Test
        @DisplayName("loginWithGoogle_missingIdToken_returns400")
        void loginWithGoogle_missingIdToken_returns400() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("full_name", "Jane Doe");

                // Act & Assert
                mockMvc.perform(post("/slib/users/login-google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value("Thiếu Google ID Token"));

                verify(authService, never()).loginWithGoogle(any(), any(), any(), any());
        }

        @Test
        @DisplayName("loginWithGoogle_emptyIdToken_returns400")
        void loginWithGoogle_emptyIdToken_returns400() throws Exception {
                // Arrange
                Map<String, String> request = new HashMap<>();
                request.put("id_token", "");
                request.put("full_name", "Jane Doe");

                // Act & Assert
                mockMvc.perform(post("/slib/users/login-google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value("Thiếu Google ID Token"));

                verify(authService, never()).loginWithGoogle(any(), any(), any(), any());
        }

        @Test
        @DisplayName("loginWithGoogle_invalidToken_returns401")
        void loginWithGoogle_invalidToken_returns401() throws Exception {
                // Arrange
                String idToken = "invalid.token";
                String fullName = "John Doe";
                String fcmToken = "fcm-token";

                Map<String, String> request = new HashMap<>();
                request.put("id_token", idToken);
                request.put("full_name", fullName);
                request.put("noti_device", fcmToken);

                when(authService.loginWithGoogle(eq(idToken), eq(fullName), eq(fcmToken), any()))
                                .thenThrow(new RuntimeException("Invalid Google ID Token"));

                // Act & Assert
                mockMvc.perform(post("/slib/users/login-google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized())
                                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Lỗi:")));

                verify(authService, times(1)).loginWithGoogle(eq(idToken), eq(fullName), eq(fcmToken), any());
        }

        @Test
        @DisplayName("loginWithGoogle_emptyRequestBody_returns400")
        void loginWithGoogle_emptyRequestBody_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/users/login-google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest());

                verify(authService, never()).loginWithGoogle(any(), any(), any(), any());
        }

        // =============================================
        // === GET MY PROFILE ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("getMyProfile_authenticatedUser_returns200WithProfile")
        void getMyProfile_authenticatedUser_returns200WithProfile() throws Exception {
                // Arrange
                String email = "user@example.com";
                UserProfileResponse profile = UserProfileResponse.builder()
                                .id(UUID.randomUUID())
                                .email(email)
                                .fullName("Test User")
                                .userCode("SV001")
                                .role("STUDENT")
                                .isActive(true)
                                .build();

                when(userService.getMyProfile(email)).thenReturn(profile);

                // Act & Assert
                mockMvc.perform(get("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value(email))
                                .andExpect(jsonPath("$.fullName").value("Test User"))
                                .andExpect(jsonPath("$.userCode").value("SV001"))
                                .andExpect(jsonPath("$.role").value("STUDENT"))
                                .andExpect(jsonPath("$.active").value(true));

                verify(userService, times(1)).getMyProfile(email);
        }

        @Test
        @DisplayName("getMyProfile_noAuthentication_returns401")
        void getMyProfile_noAuthentication_returns401() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());

                verify(userService, never()).getMyProfile(any());
        }

        @Test
        @WithMockUser(username = "nonexistent@example.com")
        @DisplayName("getMyProfile_userNotFound_returns404")
        void getMyProfile_userNotFound_returns404() throws Exception {
                // Arrange
                String email = "nonexistent@example.com";
                when(userService.getMyProfile(email))
                                .thenThrow(new RuntimeException("User not found"));

                // Act & Assert
                mockMvc.perform(get("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$")
                                                .value(org.hamcrest.Matchers.containsString("Không tìm thấy user:")));

                verify(userService, times(1)).getMyProfile(email);
        }

        // =============================================
        // === UPDATE MY PROFILE ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("updateMyProfile_validData_returns200WithUpdatedUser")
        void updateMyProfile_validData_returns200WithUpdatedUser() throws Exception {
                // Arrange
                String email = "user@example.com";
                UUID userId = UUID.randomUUID();

                UserProfileResponse currentProfile = UserProfileResponse.builder()
                                .id(userId)
                                .email(email)
                                .fullName("Old Name")
                                .userCode("SV001")
                                .role("STUDENT")
                                .isActive(true)
                                .build();

                User updateRequest = new User();
                updateRequest.setFullName("Updated Name");
                updateRequest.setUserCode("SV002");

                User updatedUser = new User();
                updatedUser.setId(userId);
                updatedUser.setEmail(email);
                updatedUser.setFullName("Updated Name");
                updatedUser.setUserCode("SV002");

                when(userService.getMyProfile(email)).thenReturn(currentProfile);
                when(userService.updateUser(eq(userId), any(User.class))).thenReturn(updatedUser);

                // Act & Assert
                mockMvc.perform(patch("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(userId.toString()))
                                .andExpect(jsonPath("$.email").value(email))
                                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                                .andExpect(jsonPath("$.userCode").value("SV002"));

                verify(userService, times(1)).getMyProfile(email);
                verify(userService, times(1)).updateUser(eq(userId), any(User.class));
        }

        @Test
        @DisplayName("updateMyProfile_noAuthentication_returns401")
        void updateMyProfile_noAuthentication_returns401() throws Exception {
                // Arrange
                User updateRequest = new User();
                updateRequest.setFullName("New Name");

                // Act & Assert
                mockMvc.perform(patch("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isUnauthorized());

                verify(userService, never()).getMyProfile(any());
                verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("updateMyProfile_emptyRequestBody_returns400")
        void updateMyProfile_emptyRequestBody_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(patch("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(""))
                                .andExpect(status().isBadRequest());

                verify(userService, never()).getMyProfile(any());
                verify(userService, never()).updateUser(any(), any());
        }

        @Test
        @WithMockUser(username = "user@example.com")
        @DisplayName("updateMyProfile_serviceThrowsException_returns400")
        void updateMyProfile_serviceThrowsException_returns400() throws Exception {
                // Arrange
                String email = "user@example.com";
                UUID userId = UUID.randomUUID();

                UserProfileResponse currentProfile = UserProfileResponse.builder()
                                .id(userId)
                                .email(email)
                                .fullName("Current Name")
                                .userCode("SV001")
                                .role("STUDENT")
                                .isActive(true)
                                .build();

                User updateRequest = new User();
                updateRequest.setFullName("New Name");

                when(userService.getMyProfile(email)).thenReturn(currentProfile);
                when(userService.updateUser(eq(userId), any(User.class)))
                                .thenThrow(new RuntimeException("Update failed"));

                // Act & Assert
                mockMvc.perform(patch("/slib/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateRequest)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Lỗi update:")));

                verify(userService, times(1)).getMyProfile(email);
                verify(userService, times(1)).updateUser(eq(userId), any(User.class));
        }

        // =============================================
        // === GET ALL USERS ENDPOINT ===
        // =============================================

        @Test
        @DisplayName("getAllUsers_success_returns200WithUsersList")
        void getAllUsers_success_returns200WithUsersList() throws Exception {
                // Arrange
                User user1 = new User();
                user1.setId(UUID.randomUUID());
                user1.setEmail("user1@example.com");
                user1.setFullName("User One");

                User user2 = new User();
                user2.setId(UUID.randomUUID());
                user2.setEmail("user2@example.com");
                user2.setFullName("User Two");

                List<User> users = Arrays.asList(user1, user2);

                when(userService.getAllUsers()).thenReturn(users);

                // Act & Assert
                mockMvc.perform(get("/slib/users/getall")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].email").value("user1@example.com"))
                                .andExpect(jsonPath("$[1].email").value("user2@example.com"));

                verify(userService, times(1)).getAllUsers();
        }

        @Test
        @DisplayName("getAllUsers_emptyList_returns200WithEmptyArray")
        void getAllUsers_emptyList_returns200WithEmptyArray() throws Exception {
                // Arrange
                when(userService.getAllUsers()).thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/users/getall")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(userService, times(1)).getAllUsers();
        }

        @Test
        @DisplayName("getAllUsers_serviceThrowsException_returns500")
        void getAllUsers_serviceThrowsException_returns500() throws Exception {
                // Arrange
                when(userService.getAllUsers())
                                .thenThrow(new RuntimeException("Database error"));

                // Act & Assert
                mockMvc.perform(get("/slib/users/getall")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isInternalServerError());

                verify(userService, times(1)).getAllUsers();
        }

        // =============================================
        // === IMPORT USERS ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importUsers_validRequest_returns200WithSuccess")
        void importUsers_validRequest_returns200WithSuccess() throws Exception {
                // Arrange
                List<ImportUserRequest> requests = List.of(
                                ImportUserRequest.builder()
                                                .fullName("Nguyen Van A")
                                                .userCode("SE123456")
                                                .email("a.nguyenvan1@fpt.edu.vn")
                                                .role(Role.STUDENT)
                                                .build(),
                                ImportUserRequest.builder()
                                                .fullName("Tran Thi B")
                                                .userCode("SE123457")
                                                .email("b.tran thi2@fpt.edu.vn")
                                                .role(Role.STUDENT)
                                                .build());

                Map<String, Object> result = Map.of(
                                "success", true,
                                "imported", 2,
                                "failed", 0);

                when(userService.importUsers(any())).thenReturn(result);

                // Act & Assert
                mockMvc.perform(post("/slib/users/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.imported").value(2));

                verify(userService, times(1)).importUsers(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importUsers_emptyList_returns400")
        void importUsers_emptyList_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/users/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Danh sách user không được rỗng"));

                verify(userService, never()).importUsers(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importUsers_nullList_returns400")
        void importUsers_nullList_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/users/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("null"))
                                .andExpect(status().isBadRequest());

                verify(userService, never()).importUsers(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importUsers_serviceThrowsException_returns400")
        void importUsers_serviceThrowsException_returns400() throws Exception {
                // Arrange
                List<ImportUserRequest> requests = List.of(
                                ImportUserRequest.builder()
                                                .fullName("Nguyen Van A")
                                                .userCode("SE123456")
                                                .email("a.nguyenvan1@fpt.edu.vn")
                                                .role(Role.STUDENT)
                                                .build());

                when(userService.importUsers(any()))
                                .thenThrow(new RuntimeException("Duplicate email"));

                // Act & Assert
                mockMvc.perform(post("/slib/users/import")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Lỗi import: Duplicate email"));

                verify(userService, times(1)).importUsers(any());
        }

        // =============================================
        // === UPDATE USER STATUS (TOGGLE) ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("toggleUserStatus_activateUser_returns200WithSuccess")
        void toggleUserStatus_activateUser_returns200WithSuccess() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                Map<String, Boolean> request = Map.of("isActive", true);

                User updatedUser = new User();
                updatedUser.setId(userId);
                updatedUser.setIsActive(true);

                when(userService.toggleUserActive(eq(userId), eq(true))).thenReturn(updatedUser);

                // Act & Assert
                mockMvc.perform(patch("/slib/users/" + userId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đã mở khóa tài khoản"))
                                .andExpect(jsonPath("$.userId").value(userId.toString()))
                                .andExpect(jsonPath("$.isActive").value(true));

                verify(userService, times(1)).toggleUserActive(eq(userId), eq(true));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("toggleUserStatus_deactivateUser_returns200WithSuccess")
        void toggleUserStatus_deactivateUser_returns200WithSuccess() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                Map<String, Boolean> request = Map.of("isActive", false);

                User updatedUser = new User();
                updatedUser.setId(userId);
                updatedUser.setIsActive(false);

                when(userService.toggleUserActive(eq(userId), eq(false))).thenReturn(updatedUser);

                // Act & Assert
                mockMvc.perform(patch("/slib/users/" + userId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đã khóa tài khoản"))
                                .andExpect(jsonPath("$.isActive").value(false));

                verify(userService, times(1)).toggleUserActive(eq(userId), eq(false));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("toggleUserStatus_missingIsActive_returns400")
        void toggleUserStatus_missingIsActive_returns400() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                Map<String, String> request = Map.of("otherField", "value");

                // Act & Assert - Spring returns generic "Bad Request" for missing required field
                mockMvc.perform(patch("/slib/users/" + userId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("toggleUserStatus_userNotFound_returns400")
        void toggleUserStatus_userNotFound_returns400() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                Map<String, Boolean> request = Map.of("isActive", true);

                when(userService.toggleUserActive(eq(userId), eq(true)))
                                .thenThrow(new RuntimeException("User not found"));

                // Act & Assert
                mockMvc.perform(patch("/slib/users/" + userId + "/status")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("User not found"));

                verify(userService, times(1)).toggleUserActive(eq(userId), eq(true));
        }

        // =============================================
        // === DELETE USER ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("deleteUser_validUserId_returns200WithSuccess")
        void deleteUser_validUserId_returns200WithSuccess() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                doNothing().when(userService).deleteUserById(userId);

                // Act & Assert
                mockMvc.perform(delete("/slib/users/" + userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đã xoá user và tất cả dữ liệu liên quan thành công"))
                                .andExpect(jsonPath("$.userId").value(userId.toString()));

                verify(userService, times(1)).deleteUserById(userId);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("deleteUser_serviceThrowsException_returns400")
        void deleteUser_serviceThrowsException_returns400() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();
                doThrow(new RuntimeException("User has active reservations")).when(userService).deleteUserById(userId);

                // Act & Assert
                mockMvc.perform(delete("/slib/users/" + userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$").value("Lỗi xoá user: User has active reservations"));

                verify(userService, times(1)).deleteUserById(userId);
        }

        @Test
        @DisplayName("deleteUser_noAuthentication_returns401")
        void deleteUser_noAuthentication_returns401() throws Exception {
                // Arrange
                UUID userId = UUID.randomUUID();

                // Act & Assert
                mockMvc.perform(delete("/slib/users/" + userId)
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isUnauthorized());

                verify(userService, never()).deleteUserById(any());
        }

        // =============================================
        // === UPLOAD AVATAR ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("uploadAvatar_validFile_returns200WithUrl")
        void uploadAvatar_validFile_returns200WithUrl() throws Exception {
                // Arrange
                String userCode = "SE123456";
                String expectedUrl = "https://res.cloudinary.com/test/image/upload/v1/avatars/SE123456.jpg";

                when(cloudinaryService.uploadAvatar(any())).thenReturn(expectedUrl);

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/avatar")
                                .file("file", "avatar content".getBytes())
                                .param("userCode", userCode)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.url").value(expectedUrl))
                                .andExpect(jsonPath("$.userCode").value(userCode));

                verify(cloudinaryService, times(1)).uploadAvatar(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("uploadAvatar_emptyFile_returns400")
        void uploadAvatar_emptyFile_returns400() throws Exception {
                // Arrange
                String userCode = "SE123456";

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/avatar")
                                .file("file", new byte[0])
                                .param("userCode", userCode)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("File không được rỗng"));

                verify(cloudinaryService, never()).uploadAvatar(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("uploadAvatar_serviceThrowsException_returns400")
        void uploadAvatar_serviceThrowsException_returns400() throws Exception {
                // Arrange
                String userCode = "SE123456";
                when(cloudinaryService.uploadAvatar(any()))
                                .thenThrow(new RuntimeException("Cloudinary upload failed"));

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/avatar")
                                .file("file", "avatar content".getBytes())
                                .param("userCode", userCode)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Lỗi upload: Cloudinary upload failed"));

                verify(cloudinaryService, times(1)).uploadAvatar(any());
        }

        // =============================================
        // === BATCH UPLOAD AVATARS ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("uploadAvatarsBatch_validFiles_returns200WithResults")
        void uploadAvatarsBatch_validFiles_returns200WithResults() throws Exception {
                // Arrange
                MockMultipartFile file1 = new MockMultipartFile("files", "SE123456.jpg", "image/jpeg", "content1".getBytes());
                MockMultipartFile file2 = new MockMultipartFile("files", "SE123457.jpg", "image/jpeg", "content2".getBytes());

                when(cloudinaryService.uploadAvatar(any()))
                                .thenReturn("https://res.cloudinary.com/test/image/upload/v1/avatars/SE123456.jpg")
                                .thenReturn("https://res.cloudinary.com/test/image/upload/v1/avatars/SE123457.jpg");

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/avatars/batch")
                                .file(file1)
                                .file(file2)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(2))
                                .andExpect(jsonPath("$.success").value(2))
                                .andExpect(jsonPath("$.failed").value(0));

                verify(cloudinaryService, times(2)).uploadAvatar(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("uploadAvatarsBatch_emptyFilesList_returns400")
        void uploadAvatarsBatch_emptyFilesList_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(multipart("/slib/users/avatars/batch")
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest());

                verify(cloudinaryService, never()).uploadAvatar(any());
        }

        // =============================================
        // === BATCH DELETE AVATARS ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("deleteAvatarsBatch_validUrls_returns200WithResults")
        void deleteAvatarsBatch_validUrls_returns200WithResults() throws Exception {
                // Arrange
                List<String> urls = List.of(
                                "https://res.cloudinary.com/test/image/upload/v1/avatars/SE123456.jpg",
                                "https://res.cloudinary.com/test/image/upload/v1/avatars/SE123457.jpg");

                Map<String, Object> request = Map.of("urls", urls);
                when(cloudinaryService.deleteAvatars(urls)).thenReturn(2);

                // Act & Assert
                mockMvc.perform(delete("/slib/users/avatars/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.total").value(2))
                                .andExpect(jsonPath("$.deleted").value(2))
                                .andExpect(jsonPath("$.failed").value(0));

                verify(cloudinaryService, times(1)).deleteAvatars(urls);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("deleteAvatarsBatch_emptyUrlsList_returns400")
        void deleteAvatarsBatch_emptyUrlsList_returns400() throws Exception {
                // Arrange
                Map<String, Object> request = Map.of("urls", List.of());

                // Act & Assert
                mockMvc.perform(delete("/slib/users/avatars/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Khong co URL nao de xoa"));

                verify(cloudinaryService, never()).deleteAvatars(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("deleteAvatarsBatch_nullUrlsList_returns400")
        void deleteAvatarsBatch_nullUrlsList_returns400() throws Exception {
                // Arrange
                Map<String, Object> request = new HashMap<>();
                request.put("urls", null);

                // Act & Assert
                mockMvc.perform(delete("/slib/users/avatars/batch")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Khong co URL nao de xoa"));

                verify(cloudinaryService, never()).deleteAvatars(any());
        }

        // =============================================
        // === VALIDATE USERS ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("validateUsers_noDuplicates_returns200WithValidTrue")
        void validateUsers_noDuplicates_returns200WithValidTrue() throws Exception {
                // Arrange
                List<ImportUserRequest> requests = List.of(
                                ImportUserRequest.builder()
                                                .fullName("Nguyen Van A")
                                                .userCode("SE123456")
                                                .email("a.nguyenvan1@fpt.edu.vn")
                                                .role(Role.STUDENT)
                                                .build());

                when(userService.existsByEmail(any())).thenReturn(false);

                // Act & Assert
                mockMvc.perform(post("/slib/users/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(true))
                                .andExpect(jsonPath("$.duplicates").isArray())
                                .andExpect(jsonPath("$.duplicates").isEmpty());

                verify(userService, times(1)).existsByEmail(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("validateUsers_withDuplicates_returns200WithDuplicates")
        void validateUsers_withDuplicates_returns200WithDuplicates() throws Exception {
                // Arrange
                List<ImportUserRequest> requests = List.of(
                                ImportUserRequest.builder()
                                                .fullName("Nguyen Van A")
                                                .userCode("SE123456")
                                                .email("existing@fpt.edu.vn")
                                                .role(Role.STUDENT)
                                                .build());

                when(userService.existsByEmail("existing@fpt.edu.vn")).thenReturn(true);

                // Act & Assert
                mockMvc.perform(post("/slib/users/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.valid").value(false))
                                .andExpect(jsonPath("$.duplicates").isArray())
                                .andExpect(jsonPath("$.duplicates[0].field").value("email"))
                                .andExpect(jsonPath("$.duplicates[0].message").value("Email đã tồn tại: existing@fpt.edu.vn"));

                verify(userService, times(1)).existsByEmail("existing@fpt.edu.vn");
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("validateUsers_emptyList_returns400")
        void validateUsers_emptyList_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(post("/slib/users/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Danh sách user không được rỗng"));

                verify(userService, never()).existsByEmail(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("validateUsers_serviceThrowsException_returns400")
        void validateUsers_serviceThrowsException_returns400() throws Exception {
                // Arrange
                List<ImportUserRequest> requests = List.of(
                                ImportUserRequest.builder()
                                                .fullName("Nguyen Van A")
                                                .userCode("SE123456")
                                                .email("test@fpt.edu.vn")
                                                .role(Role.STUDENT)
                                                .build());

                when(userService.existsByEmail(any()))
                                .thenThrow(new RuntimeException("Database connection error"));

                // Act & Assert
                mockMvc.perform(post("/slib/users/validate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requests)))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Lỗi validate: Database connection error"));

                verify(userService, times(1)).existsByEmail(any());
        }

        // =============================================
        // === IMPORT FROM EXCEL ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importFromExcel_validExcelFile_returns200WithBatchId")
        void importFromExcel_validExcelFile_returns200WithBatchId() throws Exception {
                // Arrange
                UUID batchId = UUID.randomUUID();
                MockMultipartFile file = new MockMultipartFile("file", "users.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "excel content".getBytes());

                when(asyncImportService.startImport(any())).thenReturn(batchId);

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/import/excel")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                                .andExpect(jsonPath("$.status").value("PROCESSING"));

                verify(asyncImportService, times(1)).startImport(any());
                // processImportAsync is called internally by the controller - verify it was invoked
                verify(asyncImportService, atLeastOnce()).processImportAsync(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importFromExcel_emptyFile_returns400")
        void importFromExcel_emptyFile_returns400() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile("file", "users.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                new byte[0]);

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/import/excel")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("File không được rỗng"));

                verify(asyncImportService, never()).startImport(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importFromExcel_invalidFileExtension_returns400")
        void importFromExcel_invalidFileExtension_returns400() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile("file", "users.txt",
                                "text/plain", "not excel".getBytes());

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/import/excel")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Chỉ hỗ trợ file Excel (.xlsx, .xls)"));

                verify(asyncImportService, never()).startImport(any());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("importFromExcel_serviceThrowsException_returns400")
        void importFromExcel_serviceThrowsException_returns400() throws Exception {
                // Arrange
                MockMultipartFile file = new MockMultipartFile("file", "users.xlsx",
                                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                "excel content".getBytes());

                when(asyncImportService.startImport(any()))
                                .thenThrow(new RuntimeException("Invalid Excel format"));

                // Act & Assert
                mockMvc.perform(multipart("/slib/users/import/excel")
                                .file(file)
                                .contentType(MediaType.MULTIPART_FORM_DATA))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Lỗi import: Invalid Excel format"));

                verify(asyncImportService, times(1)).startImport(any());
        }

        // =============================================
        // === GET IMPORT STATUS ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("getImportStatus_validBatchId_returns200WithStatus")
        void getImportStatus_validBatchId_returns200WithStatus() throws Exception {
                // Arrange
                UUID batchId = UUID.randomUUID();
                ImportJob job = ImportJob.builder()
                                .batchId(batchId)
                                .fileName("users.xlsx")
                                .status(ImportJob.ImportJobStatus.COMPLETED)
                                .totalRows(100)
                                .validCount(95)
                                .invalidCount(5)
                                .importedCount(95)
                                .avatarCount(90)
                                .avatarUploaded(85)
                                .build();

                when(stagingImportService.getJobStatus(batchId)).thenReturn(job);

                // Act & Assert
                mockMvc.perform(get("/slib/users/import/" + batchId + "/status")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.batchId").value(batchId.toString()))
                                .andExpect(jsonPath("$.status").value("COMPLETED"))
                                .andExpect(jsonPath("$.totalRows").value(100))
                                .andExpect(jsonPath("$.validCount").value(95))
                                .andExpect(jsonPath("$.importedCount").value(95));

                verify(stagingImportService, times(1)).getJobStatus(batchId);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("getImportStatus_batchIdNotFound_returns404")
        void getImportStatus_batchIdNotFound_returns404() throws Exception {
                // Arrange
                UUID batchId = UUID.randomUUID();
                when(stagingImportService.getJobStatus(batchId)).thenReturn(null);

                // Act & Assert
                mockMvc.perform(get("/slib/users/import/" + batchId + "/status")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isNotFound());

                verify(stagingImportService, times(1)).getJobStatus(batchId);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("getImportStatus_invalidBatchIdFormat_returns400")
        void getImportStatus_invalidBatchIdFormat_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/slib/users/import/invalid-uuid/status")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Invalid batchId format"));

                verify(stagingImportService, never()).getJobStatus(any());
        }

        // =============================================
        // === GET IMPORT ERRORS ENDPOINT ===
        // =============================================

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("getImportErrors_validBatchId_returns200WithErrors")
        void getImportErrors_validBatchId_returns200WithErrors() throws Exception {
                // Arrange
                UUID batchId = UUID.randomUUID();
                List<UserImportStaging> failedRows = List.of(
                                UserImportStaging.builder()
                                                .rowNumber(1)
                                                .userCode("SE123456")
                                                .email("invalid@email")
                                                .fullName("Test User")
                                                .errorMessage("Invalid email format")
                                                .build(),
                                UserImportStaging.builder()
                                                .rowNumber(2)
                                                .userCode("SE123457")
                                                .email("missing@email")
                                                .fullName("Test User 2")
                                                .errorMessage("Missing required field")
                                                .build());

                when(stagingImportService.getFailedRows(batchId)).thenReturn(failedRows);

                // Act & Assert
                mockMvc.perform(get("/slib/users/import/" + batchId + "/errors")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.count").value(2))
                                .andExpect(jsonPath("$.errors").isArray())
                                .andExpect(jsonPath("$.errors[0].rowNumber").value(1))
                                .andExpect(jsonPath("$.errors[0].userCode").value("SE123456"))
                                .andExpect(jsonPath("$.errors[0].errorMessage").value("Invalid email format"));

                verify(stagingImportService, times(1)).getFailedRows(batchId);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("getImportErrors_emptyErrorsList_returns200WithEmptyArray")
        void getImportErrors_emptyErrorsList_returns200WithEmptyArray() throws Exception {
                // Arrange
                UUID batchId = UUID.randomUUID();
                when(stagingImportService.getFailedRows(batchId)).thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/users/import/" + batchId + "/errors")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.count").value(0))
                                .andExpect(jsonPath("$.errors").isArray())
                                .andExpect(jsonPath("$.errors").isEmpty());

                verify(stagingImportService, times(1)).getFailedRows(batchId);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("getImportErrors_invalidBatchIdFormat_returns400")
        void getImportErrors_invalidBatchIdFormat_returns400() throws Exception {
                // Act & Assert
                mockMvc.perform(get("/slib/users/import/invalid-uuid/errors")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.error").value("Invalid batchId format"));

                verify(stagingImportService, never()).getFailedRows(any());
        }
}

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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.dto.UserProfileResponse;
import slib.com.example.entity.users.User;
import slib.com.example.service.UserService;

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
@WebMvcTest(value = UserController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("UserController Unit Tests")
class UserControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

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

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", "jwt-token-abc123");
        response.put("user_id", UUID.randomUUID().toString());
        response.put("email", "john.doe@example.com");
        response.put("role", "STUDENT");

        when(userService.loginWithGoogle(idToken, fullName, fcmToken)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/slib/users/login-google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").exists())
                .andExpect(jsonPath("$.user_id").exists())
                .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(userService, times(1)).loginWithGoogle(idToken, fullName, fcmToken);
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

        verify(userService, never()).loginWithGoogle(any(), any(), any());
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

        verify(userService, never()).loginWithGoogle(any(), any(), any());
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

        when(userService.loginWithGoogle(idToken, fullName, fcmToken))
                .thenThrow(new RuntimeException("Invalid Google ID Token"));

        // Act & Assert
        mockMvc.perform(post("/slib/users/login-google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Lỗi:")));

        verify(userService, times(1)).loginWithGoogle(idToken, fullName, fcmToken);
    }

    @Test
    @DisplayName("loginWithGoogle_emptyRequestBody_returns400")
    void loginWithGoogle_emptyRequestBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/users/login-google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(userService, never()).loginWithGoogle(any(), any(), any());
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
                .studentCode("SV001")
                .role("STUDENT")
                .reputationScore(100)
                .isActive(true)
                .build();

        when(userService.getMyProfile(email)).thenReturn(profile);

        // Act & Assert
        mockMvc.perform(get("/slib/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.fullName").value("Test User"))
                .andExpect(jsonPath("$.studentCode").value("SV001"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.reputationScore").value(100))
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
                .andExpect(jsonPath("$").value(org.hamcrest.Matchers.containsString("Không tìm thấy user:")));

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
                .studentCode("SV001")
                .role("STUDENT")
                .reputationScore(100)
                .isActive(true)
                .build();

        User updateRequest = new User();
        updateRequest.setFullName("Updated Name");
        updateRequest.setStudentCode("SV002");

        User updatedUser = new User();
        updatedUser.setId(userId);
        updatedUser.setEmail(email);
        updatedUser.setFullName("Updated Name");
        updatedUser.setStudentCode("SV002");

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
                .andExpect(jsonPath("$.studentCode").value("SV002"));

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
                .studentCode("SV001")
                .role("STUDENT")
                .reputationScore(100)
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
}

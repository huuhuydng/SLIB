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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.users.UserController;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.AsyncImportService;
import slib.com.example.service.AuthService;
import slib.com.example.service.StagingImportService;
import slib.com.example.service.UserService;
import slib.com.example.service.chat.CloudinaryService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-18: View User Details
 * Test Report: doc/Report/FE18_TestReport.md
 *
 * Note: There is no dedicated GET /users/{userId} endpoint.
 * User details are retrieved through GET /users/getall.
 * Tests adapted to verify getall endpoint returns user list.
 */
@WebMvcTest(value = UserController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-18: View User Details - Unit Tests")
class FE18_ViewUserDetailsTest {

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

        @Test
        @DisplayName("UTCD01: View users list returns 200 OK")
        void viewUserDetails_validRequest_returns200OK() throws Exception {
                when(userService.getAllUsers()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isOk());

                verify(userService, times(1)).getAllUsers();
        }

        @Test
        @DisplayName("UTCD02: Service error returns 500")
        void viewUserDetails_serviceError_returns500() throws Exception {
                when(userService.getAllUsers()).thenThrow(new RuntimeException("Database error"));

                mockMvc.perform(get("/slib/users/getall"))
                        .andExpect(status().isInternalServerError());
        }
}

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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.dashboard.DashboardController;
import slib.com.example.dto.dashboard.DashboardStatsDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.dashboard.DashboardService;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-134: View AI prioritized students
 */
@WebMvcTest(value = DashboardController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-134: View AI prioritized students - Unit Tests")
class FE134_ViewAIPrioritizedStudentsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private DashboardService dashboardService;

        @MockBean
        private SimpMessagingTemplate messagingTemplate;

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD01: View top students - month range returns 200")
        void getTopStudents_monthRange_returns200() throws Exception {
                List<DashboardStatsDTO.TopStudentDTO> students = List.of(
                                DashboardStatsDTO.TopStudentDTO.builder()
                                                .userId(UUID.randomUUID()).fullName("Nguyen Van A")
                                                .userCode("SE001").totalVisits(10).totalMinutes(500).build());
                when(dashboardService.getTopStudents("month")).thenReturn(students);

                mockMvc.perform(get("/slib/dashboard/top-students?range=month"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].fullName").value("Nguyen Van A"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD02: View top students - week range returns 200 with empty list")
        void getTopStudents_weekRange_returns200() throws Exception {
                when(dashboardService.getTopStudents("week")).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/dashboard/top-students?range=week"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$").isEmpty());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD03: View top students - year range returns 200")
        void getTopStudents_yearRange_returns200() throws Exception {
                List<DashboardStatsDTO.TopStudentDTO> students = List.of(
                                DashboardStatsDTO.TopStudentDTO.builder()
                                                .userId(UUID.randomUUID()).fullName("Le Thi B")
                                                .userCode("SE002").totalVisits(50).totalMinutes(12000).build());
                when(dashboardService.getTopStudents("year")).thenReturn(students);

                mockMvc.perform(get("/slib/dashboard/top-students?range=year"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].totalMinutes").value(12000));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD04: View top students - default range (no param) returns 200")
        void getTopStudents_defaultRange_returns200() throws Exception {
                when(dashboardService.getTopStudents("month")).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/dashboard/top-students"))
                                .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD05: Service error - returns 500")
        void getTopStudents_serviceError_returns500() throws Exception {
                when(dashboardService.getTopStudents("month"))
                                .thenThrow(new RuntimeException("Analytics service unavailable"));

                mockMvc.perform(get("/slib/dashboard/top-students?range=month"))
                                .andExpect(status().isInternalServerError());
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCD06: View top students with multiple entries")
        void getTopStudents_multipleEntries_returns200() throws Exception {
                List<DashboardStatsDTO.TopStudentDTO> students = List.of(
                                DashboardStatsDTO.TopStudentDTO.builder()
                                                .userId(UUID.randomUUID()).fullName("Student A")
                                                .userCode("SE001").totalVisits(10).totalMinutes(500).build(),
                                DashboardStatsDTO.TopStudentDTO.builder()
                                                .userId(UUID.randomUUID()).fullName("Student B")
                                                .userCode("SE002").totalVisits(8).totalMinutes(400).build());
                when(dashboardService.getTopStudents("month")).thenReturn(students);

                mockMvc.perform(get("/slib/dashboard/top-students?range=month"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));
        }
}

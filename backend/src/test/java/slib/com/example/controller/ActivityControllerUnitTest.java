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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.entity.activity.ActivityLogEntity;
import slib.com.example.entity.activity.PointTransactionEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.ActivityService;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for ActivityController
 */
@WebMvcTest(value = ActivityController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ActivityController Unit Tests")
class ActivityControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ActivityService activityService;

    // =========================================
    // === GET ACTIVITY HISTORY ===
    // =========================================

    @Test
    @DisplayName("getActivityHistory_validUserId_returns200WithData")
    void getActivityHistory_validUserId_returns200WithData() throws Exception {
        UUID userId = UUID.randomUUID();
        List<ActivityLogEntity> activities = Arrays.asList(new ActivityLogEntity(), new ActivityLogEntity());

        when(activityService.getActivitiesByUser(userId)).thenReturn(activities);
        when(activityService.getTotalStudyHours(userId)).thenReturn(10.5);
        when(activityService.getTotalVisits(userId)).thenReturn(5L);

        mockMvc.perform(get("/slib/activities/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.activities.length()").value(2))
                .andExpect(jsonPath("$.totalStudyHours").value(10.5))
                .andExpect(jsonPath("$.totalVisits").value(5));

        verify(activityService).getActivitiesByUser(userId);
        verify(activityService).getTotalStudyHours(userId);
        verify(activityService).getTotalVisits(userId);
    }

    @Test
    @DisplayName("getActivityHistory_serviceThrowsException_returns400")
    void getActivityHistory_serviceThrowsException_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(activityService.getActivitiesByUser(userId)).thenThrow(new RuntimeException("User not found"));

        mockMvc.perform(get("/slib/activities/user/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === GET POINT TRANSACTIONS ===
    // =========================================

    @Test
    @DisplayName("getPointTransactions_validUserId_returns200WithData")
    void getPointTransactions_validUserId_returns200WithData() throws Exception {
        UUID userId = UUID.randomUUID();
        List<PointTransactionEntity> transactions = Arrays.asList(new PointTransactionEntity());

        when(activityService.getPointTransactionsByUser(userId)).thenReturn(transactions);
        when(activityService.getTotalEarnedPoints(userId)).thenReturn(100);
        when(activityService.getTotalLostPoints(userId)).thenReturn(20);

        mockMvc.perform(get("/slib/activities/points/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactions").isArray())
                .andExpect(jsonPath("$.transactions.length()").value(1))
                .andExpect(jsonPath("$.totalEarned").value(100))
                .andExpect(jsonPath("$.totalLost").value(20));

        verify(activityService).getPointTransactionsByUser(userId);
    }

    @Test
    @DisplayName("getPointTransactions_serviceThrowsException_returns400")
    void getPointTransactions_serviceThrowsException_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(activityService.getPointTransactionsByUser(userId)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/slib/activities/points/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === GET FULL ACTIVITY HISTORY ===
    // =========================================

    @Test
    @DisplayName("getFullActivityHistory_validUserId_returns200WithCombinedData")
    void getFullActivityHistory_validUserId_returns200WithCombinedData() throws Exception {
        UUID userId = UUID.randomUUID();
        List<ActivityLogEntity> activities = List.of(new ActivityLogEntity());
        List<PointTransactionEntity> transactions = List.of(new PointTransactionEntity());

        when(activityService.getActivitiesByUser(userId)).thenReturn(activities);
        when(activityService.getTotalStudyHours(userId)).thenReturn(5.0);
        when(activityService.getTotalVisits(userId)).thenReturn(3L);
        when(activityService.getPointTransactionsByUser(userId)).thenReturn(transactions);
        when(activityService.getTotalEarnedPoints(userId)).thenReturn(50);
        when(activityService.getTotalLostPoints(userId)).thenReturn(10);

        mockMvc.perform(get("/slib/activities/history/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activities").isArray())
                .andExpect(jsonPath("$.totalStudyHours").value(5.0))
                .andExpect(jsonPath("$.totalVisits").value(3))
                .andExpect(jsonPath("$.pointTransactions").isArray())
                .andExpect(jsonPath("$.totalPointsEarned").value(50))
                .andExpect(jsonPath("$.totalPointsLost").value(10));
    }

    @Test
    @DisplayName("getFullActivityHistory_serviceThrowsException_returns400")
    void getFullActivityHistory_serviceThrowsException_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        when(activityService.getActivitiesByUser(userId)).thenThrow(new RuntimeException("Error"));

        mockMvc.perform(get("/slib/activities/history/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === SEED SAMPLE DATA ===
    // =========================================

    @Test
    @DisplayName("seedSampleData_validUserId_returns200")
    void seedSampleData_validUserId_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(activityService).seedSampleData(userId);

        mockMvc.perform(post("/slib/activities/seed/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Sample data seeded successfully"));

        verify(activityService).seedSampleData(userId);
    }

    @Test
    @DisplayName("seedSampleData_serviceThrowsException_returns400")
    void seedSampleData_serviceThrowsException_returns400() throws Exception {
        UUID userId = UUID.randomUUID();
        doThrow(new RuntimeException("Seed error")).when(activityService).seedSampleData(userId);

        mockMvc.perform(post("/slib/activities/seed/{userId}", userId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}

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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.LibrarianNotificationService;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for LibrarianNotificationController
 */
@WebMvcTest(value = LibrarianNotificationController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LibrarianNotificationController Unit Tests")
class LibrarianNotificationControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibrarianNotificationService librarianNotificationService;

    @Test
    @DisplayName("getPendingCounts_returns200WithCounts")
    void getPendingCounts_returns200WithCounts() throws Exception {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("supportRequests", 3L);
        counts.put("complaints", 2L);
        counts.put("feedbacks", 5L);
        counts.put("chats", 1L);
        counts.put("violations", 4L);
        counts.put("total", 15L);

        when(librarianNotificationService.getPendingCounts()).thenReturn(counts);

        mockMvc.perform(get("/slib/librarian/pending-counts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.supportRequests").value(3))
                .andExpect(jsonPath("$.complaints").value(2))
                .andExpect(jsonPath("$.feedbacks").value(5))
                .andExpect(jsonPath("$.chats").value(1))
                .andExpect(jsonPath("$.violations").value(4))
                .andExpect(jsonPath("$.total").value(15));

        verify(librarianNotificationService).getPendingCounts();
    }

    @Test
    @DisplayName("getPendingCounts_allZero_returns200")
    void getPendingCounts_allZero_returns200() throws Exception {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("supportRequests", 0L);
        counts.put("complaints", 0L);
        counts.put("feedbacks", 0L);
        counts.put("chats", 0L);
        counts.put("violations", 0L);
        counts.put("total", 0L);

        when(librarianNotificationService.getPendingCounts()).thenReturn(counts);

        mockMvc.perform(get("/slib/librarian/pending-counts")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));

        verify(librarianNotificationService).getPendingCounts();
    }
}

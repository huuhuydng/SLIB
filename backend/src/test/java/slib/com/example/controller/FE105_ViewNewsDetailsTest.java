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
import slib.com.example.controller.news.NewsController;
import slib.com.example.entity.news.News;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.NewsService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-105: View News Details
 * Test Report: doc/Report/UnitTestReport/FE105_TestReport.md
 */
@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-105: View News Details - Unit Tests")
class FE105_ViewNewsDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        // =========================================
        // === UTCID01: Normal - news exists with details ===
        // =========================================
        @Test
        @DisplayName("UTCID01: View news details for existing news returns 200 OK")
        void viewNewsDetails_existingNews_returns200() throws Exception {
                News news = new News();
                news.setId(1L);
                news.setTitle("Thong bao mo cua thu vien");
                news.setViewCount(10);

                when(newsService.getNewsDetailAndIncrementView(eq(1L))).thenReturn(news);

                mockMvc.perform(get("/slib/news/public/detail/{id}", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Thong bao mo cua thu vien"));

                verify(newsService, times(1)).getNewsDetailAndIncrementView(eq(1L));
        }

        // =========================================
        // === UTCID02: View count incremented ===
        // =========================================
        @Test
        @DisplayName("UTCID02: View news details increments view count returns 200 OK")
        void viewNewsDetails_incrementsViewCount_returns200() throws Exception {
                News news = new News();
                news.setId(2L);
                news.setTitle("Tin tuc cap nhat");
                news.setViewCount(101);

                when(newsService.getNewsDetailAndIncrementView(eq(2L))).thenReturn(news);

                mockMvc.perform(get("/slib/news/public/detail/{id}", 2L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.viewCount").value(101));

                verify(newsService, times(1)).getNewsDetailAndIncrementView(eq(2L));
        }

        // =========================================
        // === UTCID03: News with zero views ===
        // =========================================
        @Test
        @DisplayName("UTCID03: View news details with zero views returns 200 OK")
        void viewNewsDetails_zeroViews_returns200() throws Exception {
                News news = new News();
                news.setId(3L);
                news.setTitle("Tin moi dang");
                news.setViewCount(1);

                when(newsService.getNewsDetailAndIncrementView(eq(3L))).thenReturn(news);

                mockMvc.perform(get("/slib/news/public/detail/{id}", 3L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.viewCount").value(1));

                verify(newsService, times(1)).getNewsDetailAndIncrementView(eq(3L));
        }

        // =========================================
        // === UTCID04: News not found ===
        // =========================================
        @Test
        @DisplayName("UTCID04: View news details for non-existent news returns error")
        void viewNewsDetails_notFound_returnsError() throws Exception {
                when(newsService.getNewsDetailAndIncrementView(eq(999L)))
                                .thenThrow(new RuntimeException("Khong tim thay tin tuc"));

                mockMvc.perform(get("/slib/news/public/detail/{id}", 999L))
                                .andExpect(status().isInternalServerError());

                verify(newsService, times(1)).getNewsDetailAndIncrementView(eq(999L));
        }

        // =========================================
        // === UTCID05: Service failure after view increment ===
        // =========================================
        @Test
        @DisplayName("UTCID05: View news details when save fails after view increment returns error")
        void viewNewsDetails_saveFails_returnsError() throws Exception {
                when(newsService.getNewsDetailAndIncrementView(eq(5L)))
                                .thenThrow(new RuntimeException("Loi luu du lieu"));

                mockMvc.perform(get("/slib/news/public/detail/{id}", 5L))
                                .andExpect(status().isInternalServerError());

                verify(newsService, times(1)).getNewsDetailAndIncrementView(eq(5L));
        }
}

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
import slib.com.example.dto.news.NewsListDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.news.NewsService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-108: View news & announcement details
 * Test Report: doc/Report/UnitTestReport/FE104_TestReport.md
 */
@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-108: View news & announcement details - Unit Tests")
class FE108_ViewNewsDetailsTest {

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
                NewsListDTO news = NewsListDTO.builder()
                        .id(1L)
                        .title("Thong bao mo cua thu vien")
                        .viewCount(10)
                        .build();

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
                NewsListDTO news = NewsListDTO.builder()
                        .id(2L)
                        .title("Tin tuc cap nhat")
                        .viewCount(101)
                        .build();

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
                NewsListDTO news = NewsListDTO.builder()
                        .id(3L)
                        .title("Tin moi dang")
                        .viewCount(1)
                        .build();

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

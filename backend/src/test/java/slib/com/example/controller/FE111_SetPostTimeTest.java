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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.news.NewsController;
import slib.com.example.dto.news.NewsListDTO;
import slib.com.example.dto.news.NewsUpsertRequest;
import slib.com.example.entity.news.News;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.news.NewsService;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-111: Set Post Time
 * Test Report: doc/Report/UnitTestReport/FE111_TestReport.md
 */
@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-111: Set Post Time - Unit Tests")
class FE111_SetPostTimeTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Normal - set post time via create ===
        // =========================================
        @Test
        @DisplayName("UTCID01: Create news with scheduled post time returns 200 OK with isPublished=false")
        void createNews_withPostTime_returns200() throws Exception {
                News news = new News();
                news.setTitle("Tin hen gio dang");
                news.setIsPublished(false);

                NewsListDTO dto = NewsListDTO.builder()
                        .id(1L).title("Tin hen gio dang").isPublished(false).build();

                when(newsService.createNews(any(NewsUpsertRequest.class))).thenReturn(dto);

                mockMvc.perform(post("/slib/news/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(news)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPublished").value(false));

                verify(newsService, times(1)).createNews(any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID02: Update post time on existing news ===
        // =========================================
        @Test
        @DisplayName("UTCID02: Update post time on existing news returns 200 OK with isPublished=false")
        void updateNews_withPostTime_returns200() throws Exception {
                NewsListDTO dto = NewsListDTO.builder()
                        .id(1L).title("Tin da cap nhat gio dang").isPublished(false).build();

                when(newsService.updateNews(eq(1L), any(NewsUpsertRequest.class))).thenReturn(dto);

                News newsDetails = new News();
                newsDetails.setTitle("Tin da cap nhat gio dang");
                newsDetails.setIsPublished(false);

                mockMvc.perform(put("/slib/news/admin/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newsDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPublished").value(false));

                verify(newsService, times(1)).updateNews(eq(1L), any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID03: Set post time to immediate ===
        // =========================================
        @Test
        @DisplayName("UTCID03: Create news with immediate publish returns 200 OK with isPublished=true")
        void createNews_immediatePublish_returns200() throws Exception {
                News news = new News();
                news.setTitle("Tin dang ngay");
                news.setIsPublished(true);

                NewsListDTO dto = NewsListDTO.builder()
                        .id(2L).title("Tin dang ngay").isPublished(true).build();

                when(newsService.createNews(any(NewsUpsertRequest.class))).thenReturn(dto);

                mockMvc.perform(post("/slib/news/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(news)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPublished").value(true));

                verify(newsService, times(1)).createNews(any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID04: Update post time with future date ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Update news with future post time returns 200 OK with isPublished=false")
        void updateNews_futurePostTime_returns200() throws Exception {
                NewsListDTO dto = NewsListDTO.builder()
                        .id(3L).title("Tin hen ngay tuong lai").isPublished(false).build();

                when(newsService.updateNews(eq(3L), any(NewsUpsertRequest.class))).thenReturn(dto);

                News newsDetails = new News();
                newsDetails.setTitle("Tin hen ngay tuong lai");
                newsDetails.setIsPublished(false);

                mockMvc.perform(put("/slib/news/admin/{id}", 3L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newsDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPublished").value(false));

                verify(newsService, times(1)).updateNews(eq(3L), any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID05: Update post time on non-existent news ===
        // =========================================
        @Test
        @DisplayName("UTCID05: Update post time on non-existent news returns error")
        void updateNews_notFound_returnsError() throws Exception {
                when(newsService.updateNews(eq(999L), any(NewsUpsertRequest.class)))
                                .thenThrow(new RuntimeException("Khong tim thay tin tuc"));

                News newsDetails = new News();
                newsDetails.setTitle("Tin khong ton tai");

                mockMvc.perform(put("/slib/news/admin/{id}", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newsDetails)))
                                .andExpect(status().isInternalServerError());

                verify(newsService, times(1)).updateNews(eq(999L), any(NewsUpsertRequest.class));
        }
}

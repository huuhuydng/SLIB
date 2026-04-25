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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-122: CRUD news & announcement
 * Test Report: doc/Report/UnitTestReport/FE109_TestReport.md
 */
@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-122: CRUD news & announcement - Unit Tests")
class FE122_CRUDNewsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Normal - create news ===
        // =========================================
        @Test
        @DisplayName("UTCID01: Create news with valid data returns 200 OK")
        void createNews_validData_returns200() throws Exception {
                News news = new News();
                news.setTitle("Tin tuc moi");
                news.setContent("Noi dung tin tuc moi");

                NewsListDTO dto = NewsListDTO.builder()
                        .id(1L).title("Tin tuc moi").build();

                when(newsService.createNews(any(NewsUpsertRequest.class))).thenReturn(dto);

                mockMvc.perform(post("/slib/news/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(news)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.title").value("Tin tuc moi"));

                verify(newsService, times(1)).createNews(any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID02: Get all news for admin ===
        // =========================================
        @Test
        @DisplayName("UTCID02: Get all news for admin returns 200 OK")
        void getAllNews_forAdmin_returns200() throws Exception {
                NewsListDTO dto = new NewsListDTO();
                dto.setId(1L);
                dto.setTitle("Tin tuc admin");

                when(newsService.getAllNewsForAdmin()).thenReturn(List.of(dto));

                mockMvc.perform(get("/slib/news/admin/all"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].title").value("Tin tuc admin"));

                verify(newsService, times(1)).getAllNewsForAdmin();
        }

        // =========================================
        // === UTCID03: Update existing news ===
        // =========================================
        @Test
        @DisplayName("UTCID03: Update existing news returns 200 OK")
        void updateNews_existingNews_returns200() throws Exception {
                NewsListDTO dto = NewsListDTO.builder()
                        .id(1L).title("Tin tuc da cap nhat").build();

                when(newsService.updateNews(eq(1L), any(NewsUpsertRequest.class))).thenReturn(dto);

                News newsDetails = new News();
                newsDetails.setTitle("Tin tuc da cap nhat");

                mockMvc.perform(put("/slib/news/admin/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(newsDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Tin tuc da cap nhat"));

                verify(newsService, times(1)).updateNews(eq(1L), any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID04: Delete news ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Delete existing news returns 200 OK")
        void deleteNews_existingNews_returns200() throws Exception {
                doNothing().when(newsService).deleteNews(eq(1L));

                mockMvc.perform(delete("/slib/news/admin/{id}", 1L))
                                .andExpect(status().isOk());

                verify(newsService, times(1)).deleteNews(eq(1L));
        }

        // =========================================
        // === UTCID05: Toggle pin ===
        // =========================================
        @Test
        @DisplayName("UTCID05: Toggle pin on existing news returns 200 OK")
        void togglePin_existingNews_returns200() throws Exception {
                News pinnedNews = new News();
                pinnedNews.setId(1L);
                pinnedNews.setIsPinned(true);

                when(newsService.togglePin(eq(1L))).thenReturn(pinnedNews);

                mockMvc.perform(patch("/slib/news/admin/{id}/pin", 1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPinned").value(true));

                verify(newsService, times(1)).togglePin(eq(1L));
        }

        // =========================================
        // === UTCID06: Toggle pin off ===
        // =========================================
        @Test
        @DisplayName("UTCID06: Toggle pin off on existing news returns 200 OK")
        void togglePinOff_existingNews_returns200() throws Exception {
                News unpinnedNews = new News();
                unpinnedNews.setId(2L);
                unpinnedNews.setIsPinned(false);

                when(newsService.togglePin(eq(2L))).thenReturn(unpinnedNews);

                mockMvc.perform(patch("/slib/news/admin/{id}/pin", 2L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPinned").value(false));

                verify(newsService, times(1)).togglePin(eq(2L));
        }

        // =========================================
        // === UTCID07: Delete non-existent news ===
        // =========================================
        @Test
        @DisplayName("UTCID07: Delete non-existent news returns error")
        void deleteNews_notFound_returnsError() throws Exception {
                doThrow(new RuntimeException("Khong tim thay tin tuc"))
                                .when(newsService).deleteNews(eq(999L));

                mockMvc.perform(delete("/slib/news/admin/{id}", 999L))
                                .andExpect(status().isInternalServerError());

                verify(newsService, times(1)).deleteNews(eq(999L));
        }
}

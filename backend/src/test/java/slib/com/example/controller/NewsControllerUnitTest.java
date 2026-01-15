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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.controller.news.NewsController;
import slib.com.example.entity.news.News;
import slib.com.example.service.NewsService;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for NewsController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 * Test Type: @WebMvcTest (Unit Tests only - no full context)
 */
@WebMvcTest(value = NewsController.class, 
    excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, 
    classes = {slib.com.example.security.JwtAuthenticationFilter.class}))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("NewsController Unit Tests")
class NewsControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    @Autowired
    private ObjectMapper objectMapper;

    // =====================================================
    // === GET PUBLIC NEWS (MOBILE APP) ENDPOINT ===
    // =====================================================

    @Test
    @DisplayName("getPublicNews_success_returns200WithNewsList")
    void getPublicNews_success_returns200WithNewsList() throws Exception {
        // Arrange
        News news1 = createNews(1L, "Library Opens Extended Hours", "content1", 1L, true, 100L);
        News news2 = createNews(2L, "New Book Arrivals", "content2", 2L, true, 50L);
        List<News> newsList = Arrays.asList(news1, news2);

        when(newsService.getPublicNews()).thenReturn(newsList);

        // Act & Assert
        mockMvc.perform(get("/slib/news/public")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Library Opens Extended Hours"))
                .andExpect(jsonPath("$[1].title").value("New Book Arrivals"));

        verify(newsService, times(1)).getPublicNews();
    }

    @Test
    @DisplayName("getPublicNews_emptyList_returns200WithEmptyArray")
    void getPublicNews_emptyList_returns200WithEmptyArray() throws Exception {
        // Arrange
        when(newsService.getPublicNews()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/news/public")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(newsService, times(1)).getPublicNews();
    }

    // ===========================================================
    // === GET PUBLIC NEWS BY CATEGORY ENDPOINT ===
    // ===========================================================

    @Test
    @DisplayName("getPublicNewsByCategory_validCategoryId_returns200WithFilteredNews")
    void getPublicNewsByCategory_validCategoryId_returns200WithFilteredNews() throws Exception {
        // Arrange
        Long categoryId = 5L;
        News news1 = createNews(3L, "Event Announcement", "content3", categoryId, true, 75L);
        List<News> newsList = List.of(news1);

        when(newsService.getPublicNewsByCategory(categoryId)).thenReturn(newsList);

        // Act & Assert
        mockMvc.perform(get("/slib/news/public/category/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Event Announcement"));

        verify(newsService, times(1)).getPublicNewsByCategory(categoryId);
    }

    @Test
    @DisplayName("getPublicNewsByCategory_noCategoryFound_returns200WithEmptyArray")
    void getPublicNewsByCategory_noCategoryFound_returns200WithEmptyArray() throws Exception {
        // Arrange
        Long categoryId = 999L;
        when(newsService.getPublicNewsByCategory(categoryId)).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/news/public/category/{categoryId}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(newsService, times(1)).getPublicNewsByCategory(categoryId);
    }

    // ==============================================
    // === GET NEWS DETAIL ENDPOINT ===
    // ==============================================

    @Test
    @DisplayName("getNewsDetail_validId_returns200AndIncrementsView")
    void getNewsDetail_validId_returns200AndIncrementsView() throws Exception {
        // Arrange
        Long newsId = 10L;
        News news = createNews(newsId, "Important Update", "Full content here", 3L, true, 200L);

        when(newsService.getNewsDetailAndIncrementView(newsId)).thenReturn(news);

        // Act & Assert
        mockMvc.perform(get("/slib/news/public/detail/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId))
                .andExpect(jsonPath("$.title").value("Important Update"))
                .andExpect(jsonPath("$.viewCount").value(200));

        verify(newsService, times(1)).getNewsDetailAndIncrementView(newsId);
    }

    @Test
    @DisplayName("getNewsDetail_notFound_throwsRuntimeException")
    void getNewsDetail_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long newsId = 999L;
        when(newsService.getNewsDetailAndIncrementView(newsId))
                .thenThrow(new RuntimeException("News not found"));

        // Act & Assert
        mockMvc.perform(get("/slib/news/public/detail/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(newsService, times(1)).getNewsDetailAndIncrementView(newsId);
    }

    // ========================================================
    // === GET ALL NEWS FOR ADMIN ENDPOINT ===
    // ========================================================

    @Test
    @DisplayName("getAllNewsForAdmin_success_returns200WithAllNews")
    void getAllNewsForAdmin_success_returns200WithAllNews() throws Exception {
        // Arrange
        News news1 = createNews(1L, "Published News", "content", 1L, true, 100L);
        News news2 = createNews(2L, "Draft News", "content", 2L, false, 0L);
        List<News> newsList = Arrays.asList(news1, news2);

        when(newsService.getAllNewsForAdmin()).thenReturn(newsList);

        // Act & Assert
        mockMvc.perform(get("/slib/news/admin/all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].isPublished").value(true))
                .andExpect(jsonPath("$[1].isPublished").value(false));

        verify(newsService, times(1)).getAllNewsForAdmin();
    }

    // ===========================================
    // === CREATE NEWS (ADMIN) ENDPOINT ===
    // ===========================================

    @Test
    @DisplayName("createNews_validData_returns200WithCreatedNews")
    void createNews_validData_returns200WithCreatedNews() throws Exception {
        // Arrange
        News request = createNews(null, "New Announcement", "Full content", 4L, false, 0L);
        News response = createNews(15L, "New Announcement", "Full content", 4L, false, 0L);

        when(newsService.createNews(any(News.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/slib/news/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(15))
                .andExpect(jsonPath("$.title").value("New Announcement"))
                .andExpect(jsonPath("$.isPublished").value(false));

        verify(newsService, times(1)).createNews(any(News.class));
    }

    @Test
    @DisplayName("createNews_emptyRequestBody_returns400")
    void createNews_emptyRequestBody_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/news/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(newsService, never()).createNews(any());
    }

    @Test
    @DisplayName("createNews_invalidJson_returns400")
    void createNews_invalidJson_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/slib/news/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest());

        verify(newsService, never()).createNews(any());
    }

    // ===========================================
    // === UPDATE NEWS (ADMIN) ENDPOINT ===
    // ===========================================

    @Test
    @DisplayName("updateNews_validData_returns200WithUpdatedNews")
    void updateNews_validData_returns200WithUpdatedNews() throws Exception {
        // Arrange
        Long newsId = 20L;
        News request = createNews(null, "Updated Title", "Updated content", 5L, true, 150L);
        News response = createNews(newsId, "Updated Title", "Updated content", 5L, true, 150L);

        when(newsService.updateNews(eq(newsId), any(News.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/slib/news/admin/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(newsId))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.isPublished").value(true));

        verify(newsService, times(1)).updateNews(eq(newsId), any(News.class));
    }

    @Test
    @DisplayName("updateNews_notFound_throwsRuntimeException")
    void updateNews_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long newsId = 999L;
        News request = createNews(null, "Non-existent", "content", 1L, true, 0L);

        when(newsService.updateNews(eq(newsId), any(News.class)))
                .thenThrow(new RuntimeException("News not found"));

        // Act & Assert
        mockMvc.perform(put("/slib/news/admin/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());

        verify(newsService, times(1)).updateNews(eq(newsId), any(News.class));
    }

    @Test
    @DisplayName("updateNews_emptyRequestBody_returns400")
    void updateNews_emptyRequestBody_returns400() throws Exception {
        // Arrange
        Long newsId = 20L;

        // Act & Assert
        mockMvc.perform(put("/slib/news/admin/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());

        verify(newsService, never()).updateNews(any(), any());
    }

    // ===========================================
    // === DELETE NEWS (ADMIN) ENDPOINT ===
    // ===========================================

    @Test
    @DisplayName("deleteNews_validId_returns200WithSuccessMessage")
    void deleteNews_validId_returns200WithSuccessMessage() throws Exception {
        // Arrange
        Long newsId = 25L;
        doNothing().when(newsService).deleteNews(newsId);

        // Act & Assert
        mockMvc.perform(delete("/slib/news/admin/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value("Đã xóa tin tức thành công!"));

        verify(newsService, times(1)).deleteNews(newsId);
    }

    @Test
    @DisplayName("deleteNews_notFound_throwsRuntimeException")
    void deleteNews_notFound_throwsRuntimeException() throws Exception {
        // Arrange
        Long newsId = 999L;
        doThrow(new RuntimeException("News not found")).when(newsService).deleteNews(newsId);

        // Act & Assert
        mockMvc.perform(delete("/slib/news/admin/{id}", newsId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(newsService, times(1)).deleteNews(newsId);
    }

    @Test
    @DisplayName("deleteNews_invalidIdType_returns400")
    void deleteNews_invalidIdType_returns400() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/slib/news/admin/{id}", "invalid")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        verify(newsService, never()).deleteNews(any());
    }

    // ==========================================
    // === HELPER METHOD TO CREATE TEST DATA ===
    // ==========================================

    /**
     * Helper method to create News objects for testing
     */
    private News createNews(Long id, String title, String content, Long categoryId, Boolean isPublished, Long viewCount) {
        News news = new News();
        news.setId(id);
        news.setTitle(title);
        news.setContent(content);
        // Note: categoryId parameter is not used as News entity uses Category object, not categoryId
        // If needed, create and set Category object instead
        news.setIsPublished(isPublished);
        news.setViewCount(viewCount.intValue()); // Convert Long to Integer
        news.setCreatedAt(LocalDateTime.now());
        news.setUpdatedAt(LocalDateTime.now());
        return news;
    }
}

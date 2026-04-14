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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = { slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-107: View list of news & announcements - Unit Tests")
class FE107_NewsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsService newsService;

    private NewsListDTO buildNews(Long id, String title, boolean published, boolean pinned) {
        return NewsListDTO.builder()
                .id(id)
                .title(title)
                .summary("Summary for " + title)
                .content("<p>Content</p>")
                .categoryId(1L)
                .categoryName("Library")
                .isPublished(published)
                .isPinned(pinned)
                .viewCount(12)
                .createdAt(LocalDateTime.of(2026, 4, 9, 8, 0))
                .publishedAt(LocalDateTime.of(2026, 4, 9, 9, 0))
                .imageUrl("https://example.com/news-" + id + ".png")
                .build();
    }

    @Test
    @DisplayName("UTCID01: View public news list with published items")
    void viewPublicNewsList_withPublishedItems() throws Exception {
        when(newsService.getPublicNews())
                .thenReturn(List.of(buildNews(1L, "Library schedule update", true, true)));

        mockMvc.perform(get("/slib/news/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Library schedule update"))
                .andExpect(jsonPath("$[0].isPublished").value(true))
                .andExpect(jsonPath("$[0].isPinned").value(true));
    }

    @Test
    @DisplayName("UTCID02: View admin news list with mixed publish states")
    void viewAdminNewsList_withMixedPublishStates() throws Exception {
        when(newsService.getAllNewsForAdmin())
                .thenReturn(List.of(
                        buildNews(2L, "Draft internal announcement", false, false),
                        buildNews(3L, "Published event notice", true, false)));

        mockMvc.perform(get("/slib/news/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Draft internal announcement"))
                .andExpect(jsonPath("$[0].isPublished").value(false))
                .andExpect(jsonPath("$[1].title").value("Published event notice"))
                .andExpect(jsonPath("$[1].isPublished").value(true));
    }

    @Test
    @DisplayName("UTCID03: View public news list by categoryId")
    void viewPublicNewsListByCategoryId() throws Exception {
        when(newsService.getPublicNewsByCategory(1L))
                .thenReturn(List.of(buildNews(4L, "Category filtered news", true, false)));

        mockMvc.perform(get("/slib/news/public/category/{categoryId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].title").value("Category filtered news"));
    }

    @Test
    @DisplayName("UTCID04: View public news list by invalid categoryId format")
    void viewPublicNewsListByInvalidCategoryIdFormat() throws Exception {
        mockMvc.perform(get("/slib/news/public/category/{categoryId}", "not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("UTCID05: View admin news list when service throws runtime exception")
    void viewAdminNewsList_whenServiceThrowsRuntimeException() throws Exception {
        when(newsService.getAllNewsForAdmin()).thenThrow(new RuntimeException("Database unavailable"));

        mockMvc.perform(get("/slib/news/admin/all"))
                .andExpect(status().isInternalServerError());
    }
}

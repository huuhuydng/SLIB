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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.news.NewBookController;
import slib.com.example.dto.news.NewBookResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.news.NewBookService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-110: View list of new books
 */
@WebMvcTest(value = NewBookController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-110: View list of new books - Unit Tests")
class FE110_ViewNewBooksTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewBookService newBookService;

        private NewBookResponse buildBook(Integer id, String title, boolean active, boolean pinned) {
                return NewBookResponse.builder()
                                .id(id).title(title).author("Tác giả " + id)
                                .isbn("978-0-" + id).coverUrl("https://img.example.com/" + id + ".jpg")
                                .description("Mô tả sách " + id).category("Công nghệ")
                                .publishYear(2025).arrivalDate(LocalDate.of(2025, 6, 1))
                                .isActive(active).isPinned(pinned)
                                .sourceUrl("https://opac.example.com/" + id)
                                .publisher("NXB Trẻ")
                                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                                .build();
        }

        // ===== Public endpoint: /slib/new-books/public =====

        @Test
        @DisplayName("UTCID01: View public new books - returns 200 with list")
        void getPublicBooks_success_returns200() throws Exception {
                List<NewBookResponse> books = List.of(
                                buildBook(1, "Lập trình Java", true, false),
                                buildBook(2, "AI căn bản", true, true));
                when(newBookService.getPublicBooks()).thenReturn(books);

                mockMvc.perform(get("/slib/new-books/public"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].title").value("Lập trình Java"))
                                .andExpect(jsonPath("$[1].title").value("AI căn bản"));

                verify(newBookService, times(1)).getPublicBooks();
        }

        @Test
        @DisplayName("UTCID02: View public new books - empty list returns 200")
        void getPublicBooks_empty_returns200() throws Exception {
                when(newBookService.getPublicBooks()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/new-books/public"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("UTCID03: View public books - verifies response fields")
        void getPublicBooks_verifyFields_returns200() throws Exception {
                List<NewBookResponse> books = List.of(buildBook(1, "Sách mới", true, true));
                when(newBookService.getPublicBooks()).thenReturn(books);

                mockMvc.perform(get("/slib/new-books/public"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].author").exists())
                                .andExpect(jsonPath("$[0].isbn").exists())
                                .andExpect(jsonPath("$[0].coverUrl").exists())
                                .andExpect(jsonPath("$[0].category").value("Công nghệ"))
                                .andExpect(jsonPath("$[0].isPinned").value(true));
        }

        // ===== Admin endpoint: /slib/new-books/admin =====

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID04: Admin views all new books - returns 200")
        void getAllForAdmin_success_returns200() throws Exception {
                List<NewBookResponse> books = List.of(
                                buildBook(1, "Sách active", true, false),
                                buildBook(2, "Sách inactive", false, false));
                when(newBookService.getAllForAdmin()).thenReturn(books);

                mockMvc.perform(get("/slib/new-books/admin"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[1].isActive").value(false));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID05: Admin views books - empty list returns 200")
        void getAllForAdmin_empty_returns200() throws Exception {
                when(newBookService.getAllForAdmin()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/new-books/admin"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("UTCID06: Service error on public books - returns 500")
        void getPublicBooks_serviceError_returns500() throws Exception {
                when(newBookService.getPublicBooks()).thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/new-books/public"))
                                .andExpect(status().isInternalServerError());
        }
}

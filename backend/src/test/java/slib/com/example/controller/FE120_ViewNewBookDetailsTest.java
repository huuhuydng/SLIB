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
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.news.NewBookService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-120: View basic information of new book
 */
@WebMvcTest(value = NewBookController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-120: View basic information of new book - Unit Tests")
class FE120_ViewNewBookDetailsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewBookService newBookService;

        private NewBookResponse buildBook(Integer id, String title) {
                return NewBookResponse.builder()
                                .id(id).title(title).author("Nguyễn Văn A")
                                .isbn("978-604-123").coverUrl("https://img.example.com/" + id + ".jpg")
                                .description("Mô tả chi tiết sách " + title).category("Khoa học")
                                .publishYear(2025).arrivalDate(LocalDate.of(2025, 5, 15))
                                .isActive(true).isPinned(false)
                                .sourceUrl("https://opac.fpt.edu.vn/" + id)
                                .publisher("NXB Giáo dục")
                                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                                .build();
        }

        // ===== Public detail: /slib/new-books/public/{id} =====

        @Test
        @DisplayName("UTCID01: View public book detail - valid ID returns 200")
        void getPublicBookDetail_validId_returns200() throws Exception {
                NewBookResponse book = buildBook(1, "Machine Learning cơ bản");
                when(newBookService.getPublicBookDetail(1)).thenReturn(book);

                mockMvc.perform(get("/slib/new-books/public/{id}", 1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.title").value("Machine Learning cơ bản"))
                                .andExpect(jsonPath("$.author").value("Nguyễn Văn A"))
                                .andExpect(jsonPath("$.category").value("Khoa học"));

                verify(newBookService, times(1)).getPublicBookDetail(1);
        }

        @Test
        @DisplayName("UTCID02: View public book detail - non-existent ID returns 404")
        void getPublicBookDetail_notFound_returns404() throws Exception {
                when(newBookService.getPublicBookDetail(999))
                                .thenThrow(new ResourceNotFoundException("Sách không tồn tại"));

                mockMvc.perform(get("/slib/new-books/public/{id}", 999))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("UTCID03: View public book detail - verifies all response fields")
        void getPublicBookDetail_allFields_returns200() throws Exception {
                NewBookResponse book = buildBook(5, "Spring Boot in Action");
                when(newBookService.getPublicBookDetail(5)).thenReturn(book);

                mockMvc.perform(get("/slib/new-books/public/{id}", 5))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isbn").value("978-604-123"))
                                .andExpect(jsonPath("$.coverUrl").exists())
                                .andExpect(jsonPath("$.description").exists())
                                .andExpect(jsonPath("$.publishYear").value(2025))
                                .andExpect(jsonPath("$.publisher").value("NXB Giáo dục"))
                                .andExpect(jsonPath("$.isActive").value(true))
                                .andExpect(jsonPath("$.isPinned").value(false));
        }

        // ===== Admin detail: /slib/new-books/admin/{id} =====

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID04: Admin views book detail - valid ID returns 200")
        void getAdminDetail_validId_returns200() throws Exception {
                NewBookResponse book = buildBook(2, "Kiến trúc phần mềm");
                when(newBookService.getAdminDetail(2)).thenReturn(book);

                mockMvc.perform(get("/slib/new-books/admin/{id}", 2))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(2))
                                .andExpect(jsonPath("$.title").value("Kiến trúc phần mềm"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID05: Admin views book detail - non-existent ID returns 404")
        void getAdminDetail_notFound_returns404() throws Exception {
                when(newBookService.getAdminDetail(999))
                                .thenThrow(new ResourceNotFoundException("Sách không tồn tại"));

                mockMvc.perform(get("/slib/new-books/admin/{id}", 999))
                                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("UTCID06: Service error when viewing book detail - returns 500")
        void getPublicBookDetail_serviceError_returns500() throws Exception {
                when(newBookService.getPublicBookDetail(1))
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/new-books/public/{id}", 1))
                                .andExpect(status().isInternalServerError());
        }
}

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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.news.NewBookController;
import slib.com.example.dto.news.NewBookRequest;
import slib.com.example.dto.news.NewBookResponse;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.service.news.NewBookService;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-121: CRUD new book
 */
@WebMvcTest(value = NewBookController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-121: CRUD new book - Unit Tests")
class FE121_CRUDNewBookTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockBean
        private NewBookService newBookService;

        private NewBookRequest validRequest() {
                return NewBookRequest.builder()
                                .title("Lập trình Python").author("Trần Văn B")
                                .isbn("978-604-999").coverUrl("https://img.example.com/python.jpg")
                                .description("Hướng dẫn lập trình Python từ cơ bản")
                                .category("Công nghệ").publishYear(2025)
                                .arrivalDate(LocalDate.of(2025, 6, 1))
                                .isActive(true).isPinned(false)
                                .sourceUrl("https://opac.fpt.edu.vn/123")
                                .publisher("NXB Trẻ")
                                .build();
        }

        private NewBookResponse buildResponse(Integer id, String title) {
                return NewBookResponse.builder()
                                .id(id).title(title).author("Trần Văn B")
                                .isbn("978-604-999").coverUrl("https://img.example.com/python.jpg")
                                .description("Hướng dẫn lập trình Python từ cơ bản")
                                .category("Công nghệ").publishYear(2025)
                                .arrivalDate(LocalDate.of(2025, 6, 1))
                                .isActive(true).isPinned(false)
                                .sourceUrl("https://opac.fpt.edu.vn/123")
                                .publisher("NXB Trẻ")
                                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                                .build();
        }

        // ===== CREATE =====

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID01: Create new book with valid data - returns 200")
        void create_validData_returns200() throws Exception {
                NewBookResponse resp = buildResponse(1, "Lập trình Python");
                when(newBookService.create(any(NewBookRequest.class), any())).thenReturn(resp);

                mockMvc.perform(post("/slib/new-books/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(1))
                                .andExpect(jsonPath("$.title").value("Lập trình Python"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID02: Create book with blank title - returns 400")
        void create_blankTitle_returns400() throws Exception {
                NewBookRequest req = validRequest();
                req.setTitle("");

                mockMvc.perform(post("/slib/new-books/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isBadRequest());
        }

        // ===== UPDATE =====

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID03: Update book with valid data - returns 200")
        void update_validData_returns200() throws Exception {
                NewBookRequest req = validRequest();
                req.setTitle("Python nâng cao");
                NewBookResponse resp = buildResponse(1, "Python nâng cao");
                when(newBookService.update(eq(1), any(NewBookRequest.class))).thenReturn(resp);

                mockMvc.perform(put("/slib/new-books/admin/{id}", 1)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Python nâng cao"));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID04: Update non-existent book - returns 404")
        void update_notFound_returns404() throws Exception {
                when(newBookService.update(eq(999), any(NewBookRequest.class)))
                                .thenThrow(new ResourceNotFoundException("Sách không tồn tại"));

                mockMvc.perform(put("/slib/new-books/admin/{id}", 999)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest())))
                                .andExpect(status().isNotFound());
        }

        // ===== DELETE =====

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID05: Delete book - valid ID returns 204")
        void delete_validId_returns204() throws Exception {
                doNothing().when(newBookService).delete(1);

                mockMvc.perform(delete("/slib/new-books/admin/{id}", 1))
                                .andExpect(status().isNoContent());

                verify(newBookService, times(1)).delete(1);
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID06: Delete non-existent book - returns 404")
        void delete_notFound_returns404() throws Exception {
                doThrow(new ResourceNotFoundException("Sách không tồn tại")).when(newBookService).delete(999);

                mockMvc.perform(delete("/slib/new-books/admin/{id}", 999))
                                .andExpect(status().isNotFound());
        }

        // ===== TOGGLE =====

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID07: Toggle active status - returns 200")
        void toggleActive_validId_returns200() throws Exception {
                NewBookResponse resp = buildResponse(1, "Lập trình Python");
                resp.setIsActive(false);
                when(newBookService.toggleActive(1)).thenReturn(resp);

                mockMvc.perform(patch("/slib/new-books/admin/{id}/toggle-active", 1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isActive").value(false));
        }

        @Test
        @WithMockUser(username = "admin@fpt.edu.vn", roles = "ADMIN")
        @DisplayName("UTCID08: Toggle pin status - returns 200")
        void togglePin_validId_returns200() throws Exception {
                NewBookResponse resp = buildResponse(1, "Lập trình Python");
                resp.setIsPinned(true);
                when(newBookService.togglePin(1)).thenReturn(resp);

                mockMvc.perform(patch("/slib/new-books/admin/{id}/pin", 1))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPinned").value(true));
        }
}

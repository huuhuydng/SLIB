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
 * Unit Tests for FE-116: Save news & announcement draft
 * Test Report: doc/Report/UnitTestReport/FE112_TestReport.md
 */
@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-116: Save news & announcement draft - Unit Tests")
class FE116_SaveDraftTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Normal - save new draft ===
        // =========================================
        @Test
        @DisplayName("UTCID01: Save new draft with title and content returns 200 OK")
        void saveDraft_newDraft_returns200() throws Exception {
                News draft = new News();
                draft.setTitle("Tin nhap");
                draft.setContent("Noi dung nhap");
                draft.setIsPublished(false);

                NewsListDTO dto = NewsListDTO.builder()
                        .id(1L).title("Tin nhap").isPublished(false).build();

                when(newsService.createNews(any(NewsUpsertRequest.class))).thenReturn(dto);

                mockMvc.perform(post("/slib/news/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(draft)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPublished").value(false))
                                .andExpect(jsonPath("$.title").value("Tin nhap"));

                verify(newsService, times(1)).createNews(any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID02: Update existing draft ===
        // =========================================
        @Test
        @DisplayName("UTCID02: Update existing draft returns 200 OK")
        void saveDraft_updateExisting_returns200() throws Exception {
                NewsListDTO dto = NewsListDTO.builder()
                        .id(1L).title("Tin nhap da cap nhat").isPublished(false).build();

                when(newsService.updateNews(eq(1L), any(NewsUpsertRequest.class))).thenReturn(dto);

                News draftDetails = new News();
                draftDetails.setTitle("Tin nhap da cap nhat");
                draftDetails.setIsPublished(false);

                mockMvc.perform(put("/slib/news/admin/{id}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(draftDetails)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.isPublished").value(false));

                verify(newsService, times(1)).updateNews(eq(1L), any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID03: Save draft with minimal content ===
        // =========================================
        @Test
        @DisplayName("UTCID03: Save draft with title only returns 200 OK")
        void saveDraft_titleOnly_returns200() throws Exception {
                News draft = new News();
                draft.setTitle("Chi co tieu de");
                draft.setIsPublished(false);

                NewsListDTO dto = NewsListDTO.builder()
                        .id(2L).title("Chi co tieu de").isPublished(false).build();

                when(newsService.createNews(any(NewsUpsertRequest.class))).thenReturn(dto);

                mockMvc.perform(post("/slib/news/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(draft)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.title").value("Chi co tieu de"));

                verify(newsService, times(1)).createNews(any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID04: Save draft when service fails ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Save draft when service fails returns error")
        void saveDraft_serviceFails_returnsError() throws Exception {
                when(newsService.createNews(any(NewsUpsertRequest.class)))
                                .thenThrow(new RuntimeException("Loi luu ban nhap"));

                News draft = new News();
                draft.setTitle("Tin loi");
                draft.setIsPublished(false);

                mockMvc.perform(post("/slib/news/admin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(draft)))
                                .andExpect(status().isInternalServerError());

                verify(newsService, times(1)).createNews(any(NewsUpsertRequest.class));
        }

        // =========================================
        // === UTCID05: Update draft that does not exist ===
        // =========================================
        @Test
        @DisplayName("UTCID05: Update non-existent draft returns error")
        void saveDraft_updateNotFound_returnsError() throws Exception {
                when(newsService.updateNews(eq(999L), any(NewsUpsertRequest.class)))
                                .thenThrow(new RuntimeException("Khong tim thay ban nhap"));

                News draftDetails = new News();
                draftDetails.setTitle("Tin khong ton tai");
                draftDetails.setIsPublished(false);

                mockMvc.perform(put("/slib/news/admin/{id}", 999L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(draftDetails)))
                                .andExpect(status().isInternalServerError());

                verify(newsService, times(1)).updateNews(eq(999L), any(NewsUpsertRequest.class));
        }
}

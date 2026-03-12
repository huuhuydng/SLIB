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
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.NewsService;

import java.util.Collections;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import slib.com.example.controller.news.NewsController;

/**
 * Unit Tests for FE-104: View News List
 * Test Report: doc/Report/FE104_TestReport.md
 */
@WebMvcTest(value = NewsController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-104: View News List - Unit Tests")
class FE104_NewsTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private NewsService newsService;

        @Test
        @DisplayName("UTCD01: View public news list returns 200 OK")
        void viewNewsList_validToken_returns200OK() throws Exception {
                when(newsService.getPublicNews()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/news/public"))
                        .andExpect(status().isOk());
        }

        @Test
        @DisplayName("UTCD02: View admin news list returns 200 OK")
        void viewAdminNewsList_returns200OK() throws Exception {
                when(newsService.getAllNewsForAdmin()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/news/admin/all"))
                        .andExpect(status().isOk());
        }
}

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
import slib.com.example.controller.news.CategoryController;
import slib.com.example.entity.news.Category;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.news.CategoryService;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-114: Create news & announcement category
 * Test Report: doc/Report/UnitTestReport/FE110_TestReport.md
 */
@WebMvcTest(value = CategoryController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-114: Create news & announcement category - Unit Tests")
class FE114_CRUDNewsCategoryTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CategoryService categoryService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === UTCID01: Normal - create category ===
        // =========================================
        @Test
        @DisplayName("UTCID01: Create category with valid name and colorCode")
        void createCategory_validData_returns200() throws Exception {
                Category category = new Category();
                category.setId(1L);
                category.setName("Su kien");
                category.setColorCode("#FF0000");

                when(categoryService.createCategory(eq("Su kien"), eq("#FF0000"))).thenReturn(category);

                mockMvc.perform(post("/slib/news-categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("name", "Su kien", "colorCode", "#FF0000"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Su kien"));

                verify(categoryService, times(1)).createCategory(eq("Su kien"), eq("#FF0000"));
        }

        // =========================================
        // === UTCID02: Create category without colorCode ===
        // =========================================
        @Test
        @DisplayName("UTCID02: Create category with name only")
        void createCategory_nameOnly_returns200() throws Exception {
                Category category = new Category();
                category.setId(2L);
                category.setName("Thong bao");

                when(categoryService.createCategory(eq("Thong bao"), isNull())).thenReturn(category);

                mockMvc.perform(post("/slib/news-categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("name", "Thong bao"))))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Thong bao"));

                verify(categoryService, times(1)).createCategory(eq("Thong bao"), isNull());
        }

        // =========================================
        // === UTCID03: List all categories ===
        // =========================================
        @Test
        @DisplayName("UTCID03: List all categories")
        void listCategories_returns200() throws Exception {
                Category cat1 = new Category();
                cat1.setName("Su kien");
                Category cat2 = new Category();
                cat2.setName("Thong bao");

                when(categoryService.getAllCategories()).thenReturn(List.of(cat1, cat2));

                mockMvc.perform(get("/slib/news-categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2));

                verify(categoryService, times(1)).getAllCategories();
        }

        // =========================================
        // === UTCID04: Create duplicate category ===
        // =========================================
        @Test
        @DisplayName("UTCID04: Create duplicate category")
        void createCategory_duplicate_returns400() throws Exception {
                when(categoryService.createCategory(eq("Su kien"), anyString()))
                                .thenThrow(new RuntimeException("Danh muc da ton tai"));

                mockMvc.perform(post("/slib/news-categories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(Map.of("name", "Su kien", "colorCode", "#FF0000"))))
                                .andExpect(status().isBadRequest());

                verify(categoryService, times(1)).createCategory(eq("Su kien"), anyString());
        }

        // =========================================
        // === UTCID05: Delete existing category ===
        // =========================================
        @Test
        @DisplayName("UTCID05: Delete existing category")
        void deleteCategory_existing_returns200() throws Exception {
                doNothing().when(categoryService).deleteCategory(eq(1L));

                mockMvc.perform(delete("/slib/news-categories/{id}", 1L))
                                .andExpect(status().isOk());

                verify(categoryService, times(1)).deleteCategory(eq(1L));
        }

        // =========================================
        // === UTCID06: Delete non-existent category ===
        // =========================================
        @Test
        @DisplayName("UTCID06: Delete non-existent category")
        void deleteCategory_notFound_returns400() throws Exception {
                doThrow(new RuntimeException("Khong tim thay danh muc"))
                                .when(categoryService).deleteCategory(eq(999L));

                mockMvc.perform(delete("/slib/news-categories/{id}", 999L))
                                .andExpect(status().isBadRequest());

                verify(categoryService, times(1)).deleteCategory(eq(999L));
        }
}

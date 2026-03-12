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
import slib.com.example.controller.news.CategoryController;
import slib.com.example.entity.news.Category;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.CategoryService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-106: View News Categories
 * Test Report: doc/Report/UnitTestReport/FE106_TestReport.md
 */
@WebMvcTest(value = CategoryController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-106: View News Categories - Unit Tests")
class FE106_ViewNewsCategoriesTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private CategoryService categoryService;

        // =========================================
        // === UTCID01: Normal - categories exist ===
        // =========================================
        @Test
        @DisplayName("UTCID01: View all categories with name and colorCode returns 200 OK")
        void viewCategories_categoriesExist_returns200() throws Exception {
                Category cat = new Category();
                cat.setId(1L);
                cat.setName("Su kien");
                cat.setColorCode("#FF0000");

                when(categoryService.getAllCategories()).thenReturn(List.of(cat));

                mockMvc.perform(get("/slib/news-categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].name").value("Su kien"))
                                .andExpect(jsonPath("$[0].colorCode").value("#FF0000"));

                verify(categoryService, times(1)).getAllCategories();
        }

        // =========================================
        // === UTCID02: Multiple categories ===
        // =========================================
        @Test
        @DisplayName("UTCID02: View multiple categories returns 200 OK")
        void viewCategories_multipleCategories_returns200() throws Exception {
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
        // === UTCID03: Categories with various colorCodes ===
        // =========================================
        @Test
        @DisplayName("UTCID03: View categories with different colorCodes returns 200 OK")
        void viewCategories_variousColorCodes_returns200() throws Exception {
                Category cat = new Category();
                cat.setName("Khuyen mai");
                cat.setColorCode("#00FF00");

                when(categoryService.getAllCategories()).thenReturn(List.of(cat));

                mockMvc.perform(get("/slib/news-categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].colorCode").value("#00FF00"));

                verify(categoryService, times(1)).getAllCategories();
        }

        // =========================================
        // === UTCID04: Empty category list ===
        // =========================================
        @Test
        @DisplayName("UTCID04: View categories when no categories exist returns empty list 200 OK")
        void viewCategories_emptyList_returns200() throws Exception {
                when(categoryService.getAllCategories()).thenReturn(Collections.emptyList());

                mockMvc.perform(get("/slib/news-categories"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(0));

                verify(categoryService, times(1)).getAllCategories();
        }

        // =========================================
        // === UTCID05: Repository failure ===
        // =========================================
        @Test
        @DisplayName("UTCID05: View categories when repository fails returns error")
        void viewCategories_repositoryFails_returnsError() throws Exception {
                when(categoryService.getAllCategories())
                                .thenThrow(new RuntimeException("Loi truy van co so du lieu"));

                mockMvc.perform(get("/slib/news-categories"))
                                .andExpect(status().isInternalServerError());

                verify(categoryService, times(1)).getAllCategories();
        }
}

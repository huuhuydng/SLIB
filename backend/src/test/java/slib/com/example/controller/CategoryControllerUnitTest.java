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
import slib.com.example.service.CategoryService;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for CategoryController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 */
@WebMvcTest(value = CategoryController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("CategoryController Unit Tests")
class CategoryControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Autowired
    private ObjectMapper objectMapper;

    // =========================================
    // === GET ALL CATEGORIES ===
    // =========================================

    @Test
    @DisplayName("getAllCategories_success_returns200WithList")
    void getAllCategories_success_returns200WithList() throws Exception {
        // Arrange
        Category cat1 = new Category();
        cat1.setId(1L);
        cat1.setName("Technology");
        cat1.setColorCode("#FF5733");

        Category cat2 = new Category();
        cat2.setId(2L);
        cat2.setName("Events");
        cat2.setColorCode("#33FF57");

        when(categoryService.getAllCategories()).thenReturn(Arrays.asList(cat1, cat2));

        // Act & Assert
        mockMvc.perform(get("/slib/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Technology"))
                .andExpect(jsonPath("$[1].name").value("Events"));

        verify(categoryService, times(1)).getAllCategories();
    }

    @Test
    @DisplayName("getAllCategories_empty_returns200WithEmptyList")
    void getAllCategories_empty_returns200WithEmptyList() throws Exception {
        // Arrange
        when(categoryService.getAllCategories()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/slib/categories")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================
    // === CREATE CATEGORY ===
    // =========================================

    @Test
    @DisplayName("createCategory_validRequest_returns200WithCategory")
    void createCategory_validRequest_returns200WithCategory() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("name", "New Category");
        request.put("colorCode", "#123456");

        Category createdCategory = new Category();
        createdCategory.setId(1L);
        createdCategory.setName("New Category");
        createdCategory.setColorCode("#123456");

        when(categoryService.createCategory(eq("New Category"), eq("#123456")))
                .thenReturn(createdCategory);

        // Act & Assert
        mockMvc.perform(post("/slib/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Category"))
                .andExpect(jsonPath("$.colorCode").value("#123456"));

        verify(categoryService, times(1)).createCategory("New Category", "#123456");
    }

    @Test
    @DisplayName("createCategory_emptyName_returns400")
    void createCategory_emptyName_returns400() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("name", "");
        request.put("colorCode", "#123456");

        // Act & Assert
        mockMvc.perform(post("/slib/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tên danh mục không được để trống"));

        verify(categoryService, never()).createCategory(anyString(), anyString());
    }

    @Test
    @DisplayName("createCategory_nullName_returns400")
    void createCategory_nullName_returns400() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("colorCode", "#123456");

        // Act & Assert
        mockMvc.perform(post("/slib/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Tên danh mục không được để trống"));

        verify(categoryService, never()).createCategory(anyString(), anyString());
    }

    @Test
    @DisplayName("createCategory_serviceThrowsException_returns400")
    void createCategory_serviceThrowsException_returns400() throws Exception {
        // Arrange
        Map<String, String> request = new HashMap<>();
        request.put("name", "Duplicate");
        request.put("colorCode", "#123456");

        when(categoryService.createCategory(eq("Duplicate"), any()))
                .thenThrow(new RuntimeException("Category already exists"));

        // Act & Assert
        mockMvc.perform(post("/slib/categories")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category already exists"));
    }

    // =========================================
    // === DELETE CATEGORY ===
    // =========================================

    @Test
    @DisplayName("deleteCategory_validId_returns200")
    void deleteCategory_validId_returns200() throws Exception {
        // Arrange
        Long categoryId = 1L;
        doNothing().when(categoryService).deleteCategory(categoryId);

        // Act & Assert
        mockMvc.perform(delete("/slib/categories/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đã xoá danh mục thành công"));

        verify(categoryService, times(1)).deleteCategory(categoryId);
    }

    @Test
    @DisplayName("deleteCategory_notFound_returns400")
    void deleteCategory_notFound_returns400() throws Exception {
        // Arrange
        Long categoryId = 999L;
        doThrow(new RuntimeException("Category not found"))
                .when(categoryService).deleteCategory(categoryId);

        // Act & Assert
        mockMvc.perform(delete("/slib/categories/{id}", categoryId)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Category not found"));
    }
}

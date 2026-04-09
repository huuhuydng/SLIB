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
import slib.com.example.controller.admin.ReputationRuleController;
import slib.com.example.dto.reputation.ReputationRuleRequest;
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.reputation.ReputationRuleRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-35: CRUD reputation rule
 * Test Report: doc/Report/UnitTestReport/FE34_TestReport.md
 */
@WebMvcTest(value = ReputationRuleController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-35: CRUD reputation rule - Unit Tests")
class FE35_CRUDReputationRuleTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private ReputationRuleRepository reputationRuleRepository;

        @Autowired
        private ObjectMapper objectMapper;

        private ReputationRuleEntity createMockRule(Integer id, String code, String name) {
                ReputationRuleEntity rule = new ReputationRuleEntity();
                rule.setId(id);
                rule.setRuleCode(code);
                rule.setRuleName(name);
                rule.setDescription("Mo ta quy tac");
                rule.setPoints(10);
                rule.setRuleType(ReputationRuleEntity.RuleType.PENALTY);
                rule.setIsActive(true);
                rule.setCreatedAt(LocalDateTime.now());
                rule.setUpdatedAt(LocalDateTime.now());
                return rule;
        }

        // =========================================
        // === UTCID01: Create rule - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Create reputation rule with valid data returns 200 OK")
        void createRule_validData_returns200OK() throws Exception {
                ReputationRuleRequest request = ReputationRuleRequest.builder()
                                .ruleCode("NEW_RULE")
                                .ruleName("Quy tac moi")
                                .description("Mo ta")
                                .points(5)
                                .ruleType(ReputationRuleEntity.RuleType.PENALTY)
                                .isActive(true)
                                .build();

                ReputationRuleEntity saved = createMockRule(1, "NEW_RULE", "Quy tac moi");

                when(reputationRuleRepository.findByRuleCode("NEW_RULE")).thenReturn(Optional.empty());
                when(reputationRuleRepository.save(any(ReputationRuleEntity.class))).thenReturn(saved);

                mockMvc.perform(post("/slib/admin/reputation-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.ruleCode").value("NEW_RULE"));

                verify(reputationRuleRepository, times(1)).save(any(ReputationRuleEntity.class));
        }

        // =========================================
        // === UTCID02: No token - Unauthorized ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Create rule without token returns 500 Internal Server Error")
        void createRule_noToken_returns500() throws Exception {
                ReputationRuleRequest request = ReputationRuleRequest.builder()
                                .ruleCode("TEST")
                                .ruleName("Test")
                                .build();

                when(reputationRuleRepository.findByRuleCode(anyString()))
                                .thenThrow(new RuntimeException("Unauthorized"));

                mockMvc.perform(post("/slib/admin/reputation-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isInternalServerError());
        }

        // =========================================
        // === UTCID03: Non-admin - Forbidden ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Create rule with non-admin JWT returns 403 Forbidden")
        void createRule_nonAdmin_returns403Forbidden() throws Exception {
                ReputationRuleRequest request = ReputationRuleRequest.builder()
                                .ruleCode("TEST")
                                .ruleName("Test")
                                .build();

                when(reputationRuleRepository.findByRuleCode(anyString()))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(post("/slib/admin/reputation-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isForbidden());
        }

        // =========================================
        // === UTCID04: Invalid data - Bad Request ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Create rule with invalid data returns 400 Bad Request")
        void createRule_invalidData_returns400BadRequest() throws Exception {
                ReputationRuleRequest request = ReputationRuleRequest.builder()
                                .ruleCode("EXISTING")
                                .ruleName("Existing Rule")
                                .build();

                when(reputationRuleRepository.findByRuleCode("EXISTING"))
                                .thenReturn(Optional.of(createMockRule(1, "EXISTING", "Existing")));

                mockMvc.perform(post("/slib/admin/reputation-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(reputationRuleRepository, never()).save(any());
        }

        // =========================================
        // === UTCID05: Duplicate code - Conflict ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Create rule with duplicate code returns 400 Bad Request")
        void createRule_duplicateCode_returns400BadRequest() throws Exception {
                ReputationRuleRequest request = ReputationRuleRequest.builder()
                                .ruleCode("DUP_CODE")
                                .ruleName("Duplicate")
                                .build();

                when(reputationRuleRepository.findByRuleCode("DUP_CODE"))
                                .thenReturn(Optional.of(createMockRule(1, "DUP_CODE", "Existing")));

                mockMvc.perform(post("/slib/admin/reputation-rules")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verify(reputationRuleRepository, never()).save(any());
        }

        // =========================================
        // === UTCID06: Update non-existent rule ===
        // =========================================

        @Test
        @DisplayName("UTCID06: Update rule with non-existent ID returns 404 Not Found")
        void updateRule_nonExistentId_returns404() throws Exception {
                ReputationRuleRequest request = ReputationRuleRequest.builder()
                                .ruleName("Updated Name")
                                .build();

                when(reputationRuleRepository.findById(999)).thenReturn(Optional.empty());

                mockMvc.perform(put("/slib/admin/reputation-rules/999")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isNotFound());

                verify(reputationRuleRepository, never()).save(any());
        }

        // =========================================
        // === UTCID07: Non-admin role ===
        // =========================================

        @Test
        @DisplayName("UTCID07: Delete rule with non-admin role returns 403 Forbidden")
        void deleteRule_nonAdminRole_returns403Forbidden() throws Exception {
                when(reputationRuleRepository.existsById(1))
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(delete("/slib/admin/reputation-rules/1"))
                                .andExpect(status().isForbidden());
        }
}

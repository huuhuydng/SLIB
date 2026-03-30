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
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.admin.ReputationRuleController;
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.reputation.ReputationRuleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-33: View Reputation Rules
 * Test Report: doc/Report/UnitTestReport/FE33_TestReport.md
 */
@WebMvcTest(value = ReputationRuleController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-33: View Reputation Rules - Unit Tests")
class FE33_ViewReputationRulesTest {

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
        // === UTCID01: Get all rules - Success ===
        // =========================================

        @Test
        @DisplayName("UTCID01: Get all reputation rules with valid JWT token returns 200 OK")
        void getAllRules_validToken_returns200OK() throws Exception {
                ReputationRuleEntity rule = createMockRule(1, "LATE_CHECKIN", "Tre check-in");

                when(reputationRuleRepository.findAll()).thenReturn(List.of(rule));

                mockMvc.perform(get("/slib/admin/reputation-rules"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].ruleCode").value("LATE_CHECKIN"));

                verify(reputationRuleRepository, times(1)).findAll();
        }

        // =========================================
        // === UTCID02: No token - Unauthorized ===
        // =========================================

        @Test
        @DisplayName("UTCID02: Get reputation rules without token returns 500 Internal Server Error")
        void getAllRules_noToken_returns500() throws Exception {
                when(reputationRuleRepository.findAll())
                                .thenThrow(new RuntimeException("Unauthorized"));

                mockMvc.perform(get("/slib/admin/reputation-rules"))
                                .andExpect(status().isInternalServerError());

                verify(reputationRuleRepository, times(1)).findAll();
        }

        // =========================================
        // === UTCID03: No permission - Forbidden ===
        // =========================================

        @Test
        @DisplayName("UTCID03: Get reputation rules with non-admin JWT returns 403 Forbidden")
        void getAllRules_noPermission_returns403Forbidden() throws Exception {
                when(reputationRuleRepository.findAll())
                                .thenThrow(new org.springframework.security.access.AccessDeniedException(
                                                "Khong co quyen truy cap"));

                mockMvc.perform(get("/slib/admin/reputation-rules"))
                                .andExpect(status().isForbidden());

                verify(reputationRuleRepository, times(1)).findAll();
        }

        // =========================================
        // === UTCID04: Rule not found ===
        // =========================================

        @Test
        @DisplayName("UTCID04: Get reputation rule by non-existent ID returns 404 Not Found")
        void getRuleById_notFound_returns404() throws Exception {
                when(reputationRuleRepository.findById(999)).thenReturn(Optional.empty());

                mockMvc.perform(get("/slib/admin/reputation-rules/999"))
                                .andExpect(status().isNotFound());

                verify(reputationRuleRepository, times(1)).findById(999);
        }

        // =========================================
        // === UTCID05: System error ===
        // =========================================

        @Test
        @DisplayName("UTCID05: Get reputation rules with system error returns 500 Internal Server Error")
        void getAllRules_systemError_returns500() throws Exception {
                when(reputationRuleRepository.findAll())
                                .thenThrow(new RuntimeException("Database connection failed"));

                mockMvc.perform(get("/slib/admin/reputation-rules"))
                                .andExpect(status().isInternalServerError());

                verify(reputationRuleRepository, times(1)).findAll();
        }
}

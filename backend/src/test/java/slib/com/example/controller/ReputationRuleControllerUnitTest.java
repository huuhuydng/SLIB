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
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.repository.reputation.ReputationRuleRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for ReputationRuleController
 */
@WebMvcTest(value = ReputationRuleController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
        slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ReputationRuleController Unit Tests")
class ReputationRuleControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReputationRuleRepository reputationRuleRepository;

    private ReputationRuleEntity createSampleRule(Integer id, String code, String name, Integer points,
            ReputationRuleEntity.RuleType type) {
        return ReputationRuleEntity.builder()
                .id(id)
                .ruleCode(code)
                .ruleName(name)
                .description("Test description")
                .points(points)
                .ruleType(type)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================
    // === GET ALL RULES ===
    // =========================================

    @Test
    @DisplayName("getAllRules_returns200WithData")
    void getAllRules_returns200WithData() throws Exception {
        ReputationRuleEntity rule1 = createSampleRule(1, "NO_SHOW", "Không check-in", -10,
                ReputationRuleEntity.RuleType.PENALTY);
        ReputationRuleEntity rule2 = createSampleRule(2, "CHECK_IN_BONUS", "Check-in đúng giờ", 5,
                ReputationRuleEntity.RuleType.REWARD);

        when(reputationRuleRepository.findAll()).thenReturn(Arrays.asList(rule1, rule2));

        mockMvc.perform(get("/slib/admin/reputation-rules")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ruleCode").value("NO_SHOW"))
                .andExpect(jsonPath("$[1].ruleCode").value("CHECK_IN_BONUS"));

        verify(reputationRuleRepository).findAll();
    }

    // =========================================
    // === GET RULE BY ID ===
    // =========================================

    @Test
    @DisplayName("getRuleById_existingId_returns200")
    void getRuleById_existingId_returns200() throws Exception {
        ReputationRuleEntity rule = createSampleRule(1, "NO_SHOW", "Không check-in", -10,
                ReputationRuleEntity.RuleType.PENALTY);

        when(reputationRuleRepository.findById(1)).thenReturn(Optional.of(rule));

        mockMvc.perform(get("/slib/admin/reputation-rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCode").value("NO_SHOW"))
                .andExpect(jsonPath("$.points").value(-10));
    }

    @Test
    @DisplayName("getRuleById_nonExistingId_returns404")
    void getRuleById_nonExistingId_returns404() throws Exception {
        when(reputationRuleRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(get("/slib/admin/reputation-rules/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // =========================================
    // === CREATE RULE ===
    // =========================================

    @Test
    @DisplayName("createRule_validRequest_returns200")
    void createRule_validRequest_returns200() throws Exception {
        ReputationRuleEntity saved = createSampleRule(1, "NEW_RULE", "New Rule", -5,
                ReputationRuleEntity.RuleType.PENALTY);

        when(reputationRuleRepository.findByRuleCode("NEW_RULE")).thenReturn(Optional.empty());
        when(reputationRuleRepository.save(any(ReputationRuleEntity.class))).thenReturn(saved);

        String requestJson = "{\"ruleCode\":\"NEW_RULE\",\"ruleName\":\"New Rule\",\"description\":\"Test\",\"points\":-5,\"ruleType\":\"PENALTY\",\"isActive\":true}";

        mockMvc.perform(post("/slib/admin/reputation-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleCode").value("NEW_RULE"));

        verify(reputationRuleRepository).save(any(ReputationRuleEntity.class));
    }

    @Test
    @DisplayName("createRule_duplicateCode_returns400")
    void createRule_duplicateCode_returns400() throws Exception {
        ReputationRuleEntity existing = createSampleRule(1, "NO_SHOW", "Existing", -10,
                ReputationRuleEntity.RuleType.PENALTY);

        when(reputationRuleRepository.findByRuleCode("NO_SHOW")).thenReturn(Optional.of(existing));

        String requestJson = "{\"ruleCode\":\"NO_SHOW\",\"ruleName\":\"Duplicate\",\"points\":-10,\"ruleType\":\"PENALTY\"}";

        mockMvc.perform(post("/slib/admin/reputation-rules")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isBadRequest());
    }

    // =========================================
    // === UPDATE RULE ===
    // =========================================

    @Test
    @DisplayName("updateRule_existingId_returns200")
    void updateRule_existingId_returns200() throws Exception {
        ReputationRuleEntity existing = createSampleRule(1, "NO_SHOW", "Old Name", -10,
                ReputationRuleEntity.RuleType.PENALTY);
        ReputationRuleEntity updated = createSampleRule(1, "NO_SHOW", "Updated Name", -15,
                ReputationRuleEntity.RuleType.PENALTY);

        when(reputationRuleRepository.findById(1)).thenReturn(Optional.of(existing));
        when(reputationRuleRepository.save(any(ReputationRuleEntity.class))).thenReturn(updated);

        String requestJson = "{\"ruleName\":\"Updated Name\",\"points\":-15}";

        mockMvc.perform(put("/slib/admin/reputation-rules/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName").value("Updated Name"));
    }

    @Test
    @DisplayName("updateRule_nonExistingId_returns404")
    void updateRule_nonExistingId_returns404() throws Exception {
        when(reputationRuleRepository.findById(999)).thenReturn(Optional.empty());

        String requestJson = "{\"ruleName\":\"Test\"}";

        mockMvc.perform(put("/slib/admin/reputation-rules/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestJson))
                .andExpect(status().isNotFound());
    }

    // =========================================
    // === TOGGLE RULE STATUS ===
    // =========================================

    @Test
    @DisplayName("toggleRuleStatus_existingId_returns200")
    void toggleRuleStatus_existingId_returns200() throws Exception {
        ReputationRuleEntity rule = createSampleRule(1, "NO_SHOW", "Test", -10,
                ReputationRuleEntity.RuleType.PENALTY);
        rule.setIsActive(true);

        ReputationRuleEntity toggled = createSampleRule(1, "NO_SHOW", "Test", -10,
                ReputationRuleEntity.RuleType.PENALTY);
        toggled.setIsActive(false);

        when(reputationRuleRepository.findById(1)).thenReturn(Optional.of(rule));
        when(reputationRuleRepository.save(any(ReputationRuleEntity.class))).thenReturn(toggled);

        mockMvc.perform(patch("/slib/admin/reputation-rules/1/toggle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    @DisplayName("toggleRuleStatus_nonExistingId_returns404")
    void toggleRuleStatus_nonExistingId_returns404() throws Exception {
        when(reputationRuleRepository.findById(999)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/slib/admin/reputation-rules/999/toggle")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    // =========================================
    // === DELETE RULE ===
    // =========================================

    @Test
    @DisplayName("deleteRule_existingId_returns204")
    void deleteRule_existingId_returns204() throws Exception {
        when(reputationRuleRepository.existsById(1)).thenReturn(true);
        doNothing().when(reputationRuleRepository).deleteById(1);

        mockMvc.perform(delete("/slib/admin/reputation-rules/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        verify(reputationRuleRepository).deleteById(1);
    }

    @Test
    @DisplayName("deleteRule_nonExistingId_returns404")
    void deleteRule_nonExistingId_returns404() throws Exception {
        when(reputationRuleRepository.existsById(999)).thenReturn(false);

        mockMvc.perform(delete("/slib/admin/reputation-rules/999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}

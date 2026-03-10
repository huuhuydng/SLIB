package slib.com.example.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import slib.com.example.dto.reputation.ReputationRuleRequest;
import slib.com.example.dto.reputation.ReputationRuleResponse;
import slib.com.example.entity.reputation.ReputationRuleEntity;
import slib.com.example.repository.reputation.ReputationRuleRepository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for managing reputation rules (Admin only)
 */
@RestController
@RequestMapping("/slib/admin/reputation-rules")
@PreAuthorize("hasRole('ADMIN')")
public class ReputationRuleController {

    private final ReputationRuleRepository reputationRuleRepository;

    public ReputationRuleController(ReputationRuleRepository reputationRuleRepository) {
        this.reputationRuleRepository = reputationRuleRepository;
    }

    /**
     * Get all reputation rules
     */
    @GetMapping
    public ResponseEntity<List<ReputationRuleResponse>> getAllRules() {
        List<ReputationRuleResponse> rules = reputationRuleRepository.findAll()
            .stream()
            .map(ReputationRuleResponse::fromEntity)
            .collect(Collectors.toList());
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific rule by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReputationRuleResponse> getRuleById(@PathVariable Integer id) {
        return reputationRuleRepository.findById(id)
            .map(rule -> ResponseEntity.ok(ReputationRuleResponse.fromEntity(rule)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create a new reputation rule
     */
    @PostMapping
    public ResponseEntity<ReputationRuleResponse> createRule(@RequestBody ReputationRuleRequest request) {
        // Check if rule code already exists
        if (reputationRuleRepository.findByRuleCode(request.getRuleCode()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        ReputationRuleEntity rule = ReputationRuleEntity.builder()
            .ruleCode(request.getRuleCode())
            .ruleName(request.getRuleName())
            .description(request.getDescription())
            .points(request.getPoints())
            .ruleType(request.getRuleType())
            .isActive(request.getIsActive() != null ? request.getIsActive() : true)
            .build();

        ReputationRuleEntity saved = reputationRuleRepository.save(rule);
        return ResponseEntity.ok(ReputationRuleResponse.fromEntity(saved));
    }

    /**
     * Update an existing reputation rule
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReputationRuleResponse> updateRule(
            @PathVariable Integer id,
            @RequestBody ReputationRuleRequest request) {
        
        return reputationRuleRepository.findById(id)
            .map(rule -> {
                if (request.getRuleName() != null) {
                    rule.setRuleName(request.getRuleName());
                }
                if (request.getDescription() != null) {
                    rule.setDescription(request.getDescription());
                }
                if (request.getPoints() != null) {
                    rule.setPoints(request.getPoints());
                }
                if (request.getRuleType() != null) {
                    rule.setRuleType(request.getRuleType());
                }
                if (request.getIsActive() != null) {
                    rule.setIsActive(request.getIsActive());
                }
                
                ReputationRuleEntity updated = reputationRuleRepository.save(rule);
                return ResponseEntity.ok(ReputationRuleResponse.fromEntity(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Toggle rule active status
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<ReputationRuleResponse> toggleRuleStatus(@PathVariable Integer id) {
        return reputationRuleRepository.findById(id)
            .map(rule -> {
                rule.setIsActive(!rule.getIsActive());
                ReputationRuleEntity updated = reputationRuleRepository.save(rule);
                return ResponseEntity.ok(ReputationRuleResponse.fromEntity(updated));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a reputation rule
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable Integer id) {
        if (!reputationRuleRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        reputationRuleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

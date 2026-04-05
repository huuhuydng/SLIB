package slib.com.example.controller.zone_config;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.zone_config.*;
import slib.com.example.service.zone_config.LayoutAdminService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/slib/layout-admin")
@RequiredArgsConstructor
public class LayoutAdminController {

    private final LayoutAdminService layoutAdminService;

    @GetMapping("/draft")
    public ResponseEntity<LayoutDraftResponse> getDraft() {
        return ResponseEntity.ok(layoutAdminService.getDraftOrPublishedSnapshot());
    }

    @GetMapping("/history")
    public ResponseEntity<List<LayoutHistoryResponse>> getHistory(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(layoutAdminService.getHistory(limit));
    }

    @PostMapping("/validate")
    public ResponseEntity<LayoutValidationResponse> validate(@RequestBody LayoutSnapshotRequest request) {
        LayoutValidationResponse response = layoutAdminService.validate(request);
        if (response.isValid()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @PostMapping("/draft")
    public ResponseEntity<?> saveDraft(@RequestBody LayoutSnapshotRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(layoutAdminService.saveDraft(request, authentication));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "LAYOUT_CONFLICT",
                    "message", ex.getMessage(),
                    "validation", layoutAdminService.validate(request)));
        }
    }

    @DeleteMapping("/draft")
    public ResponseEntity<LayoutDraftResponse> discardDraft(Authentication authentication) {
        return ResponseEntity.ok(layoutAdminService.discardDraft(authentication));
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody LayoutSnapshotRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(layoutAdminService.publish(request, authentication));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "LAYOUT_CONFLICT",
                    "message", ex.getMessage(),
                    "validation", layoutAdminService.validate(request)));
        }
    }
}

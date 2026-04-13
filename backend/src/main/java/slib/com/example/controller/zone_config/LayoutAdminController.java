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
    public ResponseEntity<LayoutDraftResponse> getDraft(Authentication authentication) {
        return ResponseEntity.ok(layoutAdminService.getDraftOrPublishedSnapshot(authentication));
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

    @GetMapping("/schedule")
    public ResponseEntity<LayoutScheduleResponse> getActiveSchedule() {
        return ResponseEntity.ok(layoutAdminService.getActiveSchedule());
    }

    @PostMapping("/schedule")
    public ResponseEntity<?> schedulePublish(@RequestBody LayoutScheduleRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(layoutAdminService.schedulePublish(request, authentication));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "LAYOUT_VERSION_CONFLICT",
                    "message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            LayoutSnapshotRequest snapshot = request != null ? request.getSnapshot() : null;
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "LAYOUT_CONFLICT",
                    "message", ex.getMessage(),
                    "validation", snapshot != null ? layoutAdminService.validate(snapshot) : null));
        }
    }

    @DeleteMapping("/schedule/{scheduleId}")
    public ResponseEntity<LayoutScheduleResponse> cancelSchedule(@PathVariable Long scheduleId,
                                                                Authentication authentication) {
        return ResponseEntity.ok(layoutAdminService.cancelScheduledPublish(scheduleId, authentication));
    }

    @PostMapping("/publish")
    public ResponseEntity<?> publish(@RequestBody LayoutSnapshotRequest request, Authentication authentication) {
        try {
            return ResponseEntity.ok(layoutAdminService.publish(request, authentication));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "LAYOUT_VERSION_CONFLICT",
                    "message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                    "error", "LAYOUT_CONFLICT",
                    "message", ex.getMessage(),
                    "validation", layoutAdminService.validate(request)));
        }
    }
}

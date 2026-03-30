package slib.com.example.controller.news;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import slib.com.example.dto.news.NewBookImportRequest;
import slib.com.example.dto.news.NewBookRequest;
import slib.com.example.dto.news.NewBookResponse;
import slib.com.example.service.news.NewBookService;

import java.util.List;

@RestController
@RequestMapping("/slib/new-books")
@RequiredArgsConstructor
public class NewBookController {

    private final NewBookService newBookService;

    @GetMapping("/public")
    public ResponseEntity<List<NewBookResponse>> getPublicBooks() {
        return ResponseEntity.ok(newBookService.getPublicBooks());
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<NewBookResponse> getPublicBookDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(newBookService.getPublicBookDetail(id));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<NewBookResponse>> getAllForAdmin() {
        return ResponseEntity.ok(newBookService.getAllForAdmin());
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<NewBookResponse> getAdminDetail(@PathVariable Integer id) {
        return ResponseEntity.ok(newBookService.getAdminDetail(id));
    }

    @PostMapping("/admin/preview")
    public ResponseEntity<NewBookRequest> previewFromUrl(@Valid @RequestBody NewBookImportRequest request) {
        return ResponseEntity.ok(newBookService.previewFromUrl(request.getUrl()));
    }

    @PostMapping("/admin")
    public ResponseEntity<NewBookResponse> create(
            @Valid @RequestBody NewBookRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(newBookService.create(request, userDetails));
    }

    @PutMapping("/admin/{id}")
    public ResponseEntity<NewBookResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody NewBookRequest request) {
        return ResponseEntity.ok(newBookService.update(id, request));
    }

    @PatchMapping("/admin/{id}/toggle-active")
    public ResponseEntity<NewBookResponse> toggleActive(@PathVariable Integer id) {
        return ResponseEntity.ok(newBookService.toggleActive(id));
    }

    @PatchMapping("/admin/{id}/pin")
    public ResponseEntity<NewBookResponse> togglePin(@PathVariable Integer id) {
        return ResponseEntity.ok(newBookService.togglePin(id));
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        newBookService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/batch")
    public ResponseEntity<?> deleteBatch(@RequestBody java.util.Map<String, java.util.List<Integer>> body) {
        try {
            java.util.List<Integer> ids = body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Danh sách ID không được trống"));
            }
            newBookService.deleteBatch(ids);
            return ResponseEntity.ok(java.util.Map.of("deleted", ids.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

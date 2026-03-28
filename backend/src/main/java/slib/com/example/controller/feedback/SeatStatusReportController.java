package slib.com.example.controller.feedback;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.feedback.CreateSeatStatusReportRequest;
import slib.com.example.dto.feedback.SeatStatusReportResponse;
import slib.com.example.entity.users.Role;
import slib.com.example.entity.users.User;
import slib.com.example.exception.ResourceNotFoundException;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.feedback.SeatStatusReportService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/slib/seat-status-reports")
@RequiredArgsConstructor
public class SeatStatusReportController {

    private final SeatStatusReportService seatStatusReportService;
    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<SeatStatusReportResponse> create(
            @RequestParam("seatId") Integer seatId,
            @RequestParam("issueType") String issueType,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID reporterId = getCurrentUser(userDetails).getId();
        CreateSeatStatusReportRequest request = new CreateSeatStatusReportRequest();
        request.setSeatId(seatId);
        request.setIssueType(issueType);
        request.setDescription(description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatStatusReportService.createReport(reporterId, request, image));
    }

    @GetMapping("/my")
    public ResponseEntity<List<SeatStatusReportResponse>> getMyReports(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID reporterId = getCurrentUser(userDetails).getId();
        return ResponseEntity.ok(seatStatusReportService.getMyReports(reporterId));
    }

    @GetMapping
    public ResponseEntity<List<SeatStatusReportResponse>> getAll(
            @RequestParam(value = "status", required = false) String status,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireStaff(userDetails);
        return ResponseEntity.ok(seatStatusReportService.getAll(status));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SeatStatusReportResponse> getById(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = getCurrentUser(userDetails);
        SeatStatusReportResponse response = seatStatusReportService.getById(id);
        if (currentUser.getRole() == Role.STUDENT && !currentUser.getId().equals(response.getReporterId())) {
            throw new AccessDeniedException("You do not have permission to access this seat status report");
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}/verify")
    public ResponseEntity<SeatStatusReportResponse> verify(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = requireStaff(userDetails).getId();
        return ResponseEntity.ok(seatStatusReportService.verifyReport(id, librarianId));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<SeatStatusReportResponse> reject(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = requireStaff(userDetails).getId();
        return ResponseEntity.ok(seatStatusReportService.rejectReport(id, librarianId));
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<SeatStatusReportResponse> resolve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID librarianId = requireStaff(userDetails).getId();
        return ResponseEntity.ok(seatStatusReportService.resolveReport(id, librarianId));
    }

    @DeleteMapping("/batch")
    public ResponseEntity<?> deleteBatch(
            @RequestBody java.util.Map<String, List<String>> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        requireStaff(userDetails);
        try {
            List<String> ids = body.get("ids");
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "Danh sách ID không được trống"));
            }
            List<UUID> uuids = ids.stream().map(UUID::fromString).collect(java.util.stream.Collectors.toList());
            seatStatusReportService.deleteBatch(uuids);
            return ResponseEntity.ok(java.util.Map.of("deleted", uuids.size()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(java.util.Map.of("error", e.getMessage()));
        }
    }

    private User getCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new AccessDeniedException("Session expired. Please log in again.");
        }
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()));
    }

    private User requireStaff(UserDetails userDetails) {
        User user = getCurrentUser(userDetails);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.LIBRARIAN) {
            throw new AccessDeniedException("Only librarian/admin can access this resource");
        }
        return user;
    }
}

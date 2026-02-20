package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.complaint.ComplaintDTO;
import slib.com.example.entity.complaint.ComplaintEntity;
import slib.com.example.entity.complaint.ComplaintEntity.ComplaintStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.ComplaintRepository;
import slib.com.example.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;

    /**
     * Lấy tất cả khiếu nại (cho thủ thư)
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getAll() {
        return complaintRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(ComplaintDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy khiếu nại theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getByStatus(ComplaintStatus status) {
        return complaintRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(ComplaintDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy khiếu nại của sinh viên
     */
    @Transactional(readOnly = true)
    public List<ComplaintDTO> getByStudent(UUID studentId) {
        return complaintRepository.findByUserIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(ComplaintDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Sinh viên tạo khiếu nại
     */
    @Transactional
    public ComplaintDTO create(UUID studentId, String subject, String content, String evidenceUrl,
            UUID pointTransactionId) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sinh viên"));

        ComplaintEntity complaint = ComplaintEntity.builder()
                .user(student)
                .subject(subject)
                .content(content)
                .evidenceUrl(evidenceUrl)
                .pointTransactionId(pointTransactionId)
                .status(ComplaintStatus.PENDING)
                .build();

        ComplaintEntity saved = complaintRepository.save(complaint);
        log.info("[Complaint] Sinh viên {} đã gửi khiếu nại: {}", student.getFullName(), subject);
        return ComplaintDTO.fromEntity(saved);
    }

    /**
     * Thủ thư chấp nhận khiếu nại (hoàn điểm)
     */
    @Transactional
    public ComplaintDTO accept(UUID complaintId, UUID librarianId, String note) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

        complaint.setStatus(ComplaintStatus.ACCEPTED);
        complaint.setResolvedBy(librarian);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolutionNote(note);

        ComplaintEntity saved = complaintRepository.save(complaint);
        log.info("[Complaint] Khiếu nại {} được CHẤP NHẬN bởi {}", complaintId, librarian.getFullName());

        // TODO: Tích hợp hoàn điểm uy tín thông qua PointTransactionService khi cần
        return ComplaintDTO.fromEntity(saved);
    }

    /**
     * Thủ thư từ chối khiếu nại
     */
    @Transactional
    public ComplaintDTO deny(UUID complaintId, UUID librarianId, String note) {
        ComplaintEntity complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khiếu nại"));

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thủ thư"));

        complaint.setStatus(ComplaintStatus.DENIED);
        complaint.setResolvedBy(librarian);
        complaint.setResolvedAt(LocalDateTime.now());
        complaint.setResolutionNote(note);

        ComplaintEntity saved = complaintRepository.save(complaint);
        log.info("[Complaint] Khiếu nại {} bị TỪ CHỐI bởi {}", complaintId, librarian.getFullName());
        return ComplaintDTO.fromEntity(saved);
    }

    /**
     * Đếm khiếu nại theo trạng thái
     */
    public long countByStatus(ComplaintStatus status) {
        return complaintRepository.countByStatus(status);
    }
}

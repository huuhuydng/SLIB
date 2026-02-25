package slib.com.example.service.support;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import slib.com.example.dto.support.SupportRequestDTO;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.ConversationStatus;
import slib.com.example.entity.notification.NotificationEntity.NotificationType;
import slib.com.example.entity.support.SupportRequest;
import slib.com.example.entity.support.SupportRequestStatus;
import slib.com.example.entity.users.User;
import slib.com.example.repository.UserRepository;
import slib.com.example.repository.support.SupportRequestRepository;
import slib.com.example.service.LibrarianNotificationService;
import slib.com.example.service.PushNotificationService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.chat.ConversationService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupportRequestService {

    private final SupportRequestRepository supportRequestRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final PushNotificationService pushNotificationService;
    private final ConversationService conversationService;
    private final LibrarianNotificationService librarianNotificationService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Tạo yêu cầu hỗ trợ mới
     */
    @Transactional
    public SupportRequestDTO create(UUID studentId, String description, List<MultipartFile> images) {
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));

        // Upload ảnh lên Cloudinary
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            for (MultipartFile image : images) {
                if (!image.isEmpty()) {
                    try {
                        String url = cloudinaryService.uploadImageChat(image);
                        imageUrls.add(url);
                        log.info("[SupportRequest] Uploaded image: {}", url);
                    } catch (Exception e) {
                        log.error("[SupportRequest] Failed to upload image: {}", e.getMessage());
                    }
                }
            }
        }

        SupportRequest request = SupportRequest.builder()
                .student(student)
                .description(description)
                .imageUrls(imageUrls.isEmpty() ? null : imageUrls.toArray(new String[0]))
                .status(SupportRequestStatus.PENDING)
                .build();

        SupportRequest saved = supportRequestRepository.save(request);
        log.info("[SupportRequest] Created support request {} by student {}", saved.getId(), studentId);

        broadcastDashboardUpdate("SUPPORT_UPDATE", "CREATED");
        librarianNotificationService.broadcastPendingCounts("SUPPORT_REQUEST", "CREATED");
        return SupportRequestDTO.fromEntity(saved);
    }

    /**
     * Lấy tất cả yêu cầu (cho thủ thư)
     */
    public List<SupportRequestDTO> getAll() {
        return supportRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(SupportRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy yêu cầu theo trạng thái
     */
    public List<SupportRequestDTO> getByStatus(SupportRequestStatus status) {
        return supportRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream()
                .map(SupportRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy yêu cầu của sinh viên
     */
    public List<SupportRequestDTO> getByStudent(UUID studentId) {
        return supportRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(SupportRequestDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật trạng thái yêu cầu
     */
    @Transactional
    public SupportRequestDTO updateStatus(UUID requestId, SupportRequestStatus status, UUID librarianId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Support request not found: " + requestId));

        request.setStatus(status);

        if (status == SupportRequestStatus.RESOLVED || status == SupportRequestStatus.REJECTED) {
            User librarian = userRepository.findById(librarianId)
                    .orElseThrow(() -> new RuntimeException("Librarian not found: " + librarianId));
            request.setResolvedBy(librarian);
            request.setResolvedAt(LocalDateTime.now());
        }

        SupportRequest saved = supportRequestRepository.save(request);
        log.info("[SupportRequest] Updated status of {} to {}", requestId, status);

        // Gửi thông báo cho sinh viên
        sendStatusNotification(saved, status);

        broadcastDashboardUpdate("SUPPORT_UPDATE", "STATUS_CHANGED");
        librarianNotificationService.broadcastPendingCounts("SUPPORT_REQUEST", "STATUS_CHANGED");
        return SupportRequestDTO.fromEntity(saved);
    }

    /**
     * Thủ thư phản hồi yêu cầu
     */
    @Transactional
    public SupportRequestDTO respond(UUID requestId, String response, UUID librarianId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Support request not found: " + requestId));

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Librarian not found: " + librarianId));

        request.setAdminResponse(response);
        request.setStatus(SupportRequestStatus.RESOLVED);
        request.setResolvedBy(librarian);
        request.setResolvedAt(LocalDateTime.now());

        SupportRequest saved = supportRequestRepository.save(request);
        log.info("[SupportRequest] Librarian {} responded to request {}", librarianId, requestId);

        // Gửi thông báo cho sinh viên khi thủ thư phản hồi
        sendStatusNotification(saved, SupportRequestStatus.RESOLVED);

        broadcastDashboardUpdate("SUPPORT_UPDATE", "RESPONDED");
        librarianNotificationService.broadcastPendingCounts("SUPPORT_REQUEST", "RESPONDED");
        return SupportRequestDTO.fromEntity(saved);
    }

    /**
     * Bắt đầu chat với sinh viên từ yêu cầu hỗ trợ
     * Tạo/reuse conversation, gán thủ thư, thêm SYSTEM message chứa nội dung yêu
     * cầu
     */
    @Transactional
    public UUID startChatForRequest(UUID requestId, UUID librarianId) {
        SupportRequest request = supportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Support request not found: " + requestId));

        UUID studentId = request.getStudent().getId();

        // Lấy hoặc tạo conversation cho sinh viên
        Conversation conversation = conversationService.getOrCreateConversation(studentId);

        User librarian = userRepository.findById(librarianId)
                .orElseThrow(() -> new RuntimeException("Librarian not found: " + librarianId));

        // Gán librarian và set HUMAN_CHATTING
        // LUÔN increment humanSession cho mỗi request mới
        // (để chỉ hiện SYSTEM message của request đang reply, không hiện cũ)
        conversation.setStatus(ConversationStatus.HUMAN_CHATTING);
        conversation.setLibrarian(librarian);
        conversation.setCurrentHumanSession(conversation.getCurrentHumanSession() + 1);
        conversation.setEscalatedAt(LocalDateTime.now());

        // Tạo nội dung SYSTEM message chứa thông tin yêu cầu hỗ trợ
        StringBuilder systemContent = new StringBuilder();
        systemContent.append("[YÊU CẦU HỖ TRỢ]\n");
        systemContent.append("Nội dung: ").append(request.getDescription());
        if (request.getImageUrls() != null && request.getImageUrls().length > 0) {
            systemContent.append("\n[IMAGES]");
            for (String url : request.getImageUrls()) {
                systemContent.append("\n").append(url);
            }
        }

        // Thêm SYSTEM message vào conversation
        conversationService.addMessageToConversation(
                conversation.getId(),
                librarianId,
                systemContent.toString(),
                "SYSTEM");

        // Gửi WebSocket notification cho sinh viên để mobile cập nhật real-time
        String librarianName = librarian.getFullName() != null ? librarian.getFullName() : "Thủ thư";
        conversationService.notifyStudentLibrarianJoined(conversation.getId(), studentId, librarianName);

        log.info("[SupportRequest] Started chat for request {} with student {}", requestId, studentId);

        return conversation.getId();
    }

    /**
     * Đếm số yêu cầu theo trạng thái
     */
    public long countByStatus(SupportRequestStatus status) {
        return supportRequestRepository.countByStatus(status);
    }

    /**
     * Gửi thông báo cho sinh viên khi trạng thái yêu cầu thay đổi
     * Sử dụng afterCommit để không ảnh hưởng transaction chính
     */
    private void sendStatusNotification(SupportRequest request, SupportRequestStatus status) {
        try {
            UUID studentId = request.getStudent().getId();
            UUID requestId = request.getId();
            String title;
            String body;

            switch (status) {
                case IN_PROGRESS:
                    title = "Yêu cầu đang được xử lý";
                    body = "Thủ thư đã tiếp nhận yêu cầu hỗ trợ của bạn";
                    break;
                case RESOLVED:
                    title = "Yêu cầu đã được giải quyết";
                    body = "Thủ thư đã phản hồi yêu cầu hỗ trợ của bạn. Mở ứng dụng để xem chi tiết.";
                    break;
                case REJECTED:
                    title = "Yêu cầu đã bị từ chối";
                    body = "Yêu cầu hỗ trợ của bạn đã bị từ chối";
                    break;
                default:
                    return;
            }

            // Capture các biến final để dùng trong callback
            final String notiTitle = title;
            final String notiBody = body;

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                pushNotificationService.sendToUser(
                                        studentId, notiTitle, notiBody,
                                        NotificationType.SUPPORT_REQUEST,
                                        requestId);
                                log.info("[SupportRequest] Sent notification to student {} for status {}", studentId,
                                        status);
                            } catch (Exception e) {
                                log.error("[SupportRequest] Failed to send notification: {}", e.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("[SupportRequest] Failed to prepare notification: {}", e.getMessage());
        }
    }

    private void broadcastDashboardUpdate(String type, String action) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard",
                    java.util.Map.of("type", type, "action", action, "timestamp", java.time.Instant.now().toString()));
        } catch (Exception e) {
            log.warn("Failed to broadcast dashboard update: {}", e.getMessage());
        }
    }
}

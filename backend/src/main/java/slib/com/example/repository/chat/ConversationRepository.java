package slib.com.example.repository.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.chat.Conversation;
import slib.com.example.entity.chat.ConversationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    /**
     * Tìm conversation đang active của một student (không phải RESOLVED)
     */
    Optional<Conversation> findByStudentIdAndStatusNot(UUID studentId, ConversationStatus status);

    /**
     * Tìm conversation đang active của student theo status cụ thể
     */
    Optional<Conversation> findByStudentIdAndStatus(UUID studentId, ConversationStatus status);

    /**
     * Lấy danh sách conversation theo status (cho Librarian xem danh sách chờ)
     */
    List<Conversation> findByStatusOrderByCreatedAtAsc(ConversationStatus status);

    /**
     * Lấy danh sách conversation theo status, sắp xếp theo thời gian escalate
     * Dùng cho queue waiting - đảm bảo ai ấn gặp thủ thư trước thì ở vị trí trước
     */
    List<Conversation> findByStatusOrderByEscalatedAtAsc(ConversationStatus status);

    /**
     * Lấy danh sách conversation đang được một Librarian xử lý
     */
    List<Conversation> findByLibrarianIdAndStatusOrderByUpdatedAtDesc(UUID librarianId, ConversationStatus status);

    /**
     * Lấy danh sách conversation đang chờ hoặc đang xử lý của Librarian
     */
    @Query("SELECT c FROM Conversation c WHERE c.status IN :statuses ORDER BY c.updatedAt DESC")
    List<Conversation> findByStatusIn(@Param("statuses") List<ConversationStatus> statuses);

    /**
     * Đếm số conversation đang chờ xử lý
     */
    long countByStatus(ConversationStatus status);

    /**
     * Tìm conversation gần nhất của student
     */
    Optional<Conversation> findTopByStudentIdOrderByCreatedAtDesc(UUID studentId);

    /**
     * Tìm tất cả conversations của student
     */
    List<Conversation> findByStudentId(UUID studentId);
}

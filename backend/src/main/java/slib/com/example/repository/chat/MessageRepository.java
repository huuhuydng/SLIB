package slib.com.example.repository.chat;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.stereotype.Repository;
import slib.com.example.entity.chat.Message;
import slib.com.example.entity.chat.MessageType;
// import slib.com.example.entity.users.User; // Không cần dùng nữa nếu chỉ lấy ID

import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // 1. Lấy lịch sử tin nhắn (Giữ nguyên của bạn - Rất chuẩn)
    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :user1Id AND m.receiver.id = :user2Id) OR " +
           "(m.sender.id = :user2Id AND m.receiver.id = :user1Id) " +
           "ORDER BY m.createdAt DESC") 
    Page<Message> findConversation(
            @Param("user1Id") UUID user1Id, 
            @Param("user2Id") UUID user2Id, 
            Pageable pageable
    );

    // 2. Lấy danh sách ID đối tác (SỬA LẠI THÀNH JPQL TRẢ VỀ UUID)
    @Query("SELECT DISTINCT CASE " +
           "  WHEN m.sender.id = :myId THEN m.receiver.id " +
           "  ELSE m.sender.id " +
           "END " +
           "FROM Message m " +
           "WHERE m.sender.id = :myId OR m.receiver.id = :myId")
    List<UUID> findConversationPartners(@Param("myId") UUID myId);


       // 3. Tìm kiếm tin nhắn theo từ khóa giữa hai người dùng
       @Query("SELECT m FROM Message m WHERE " +
              "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
              "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) " +
              "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
              "ORDER BY m.createdAt DESC")
       List<Message> searchMessages(@Param("userId1") UUID userId1, 
                                   @Param("userId2") UUID userId2, 
                                   @Param("keyword") String keyword);

        // 4. HÀM MỚI: Đếm số lượng tin nhắn mới hơn tin nhắn target (trong cuộc hội thoại cụ thể)
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
           "((m.sender.id = :userId1 AND m.receiver.id = :userId2) OR " +
           "(m.sender.id = :userId2 AND m.receiver.id = :userId1)) " +
           "AND m.createdAt > (SELECT target.createdAt FROM Message target WHERE target.id = :messageId)")
    long countMessagesNewerThan(@Param("userId1") UUID userId1, 
                                @Param("userId2") UUID userId2, 
                                @Param("messageId") UUID messageId);

     // 5. Đếm tổng số tin nhắn chưa đọc của user này (dùng cho Badge đỏ)
    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :myId AND m.isRead = false")
    long countUnreadMessages(@Param("myId") UUID myId);

    // 6. Đánh dấu tất cả tin nhắn từ partner gửi cho mình là ĐÃ ĐỌC (khi mở chat)
    @Modifying
    @Transactional
    @Query("UPDATE Message m SET m.isRead = true WHERE m.sender.id = :partnerId AND m.receiver.id = :myId AND m.isRead = false")
    void markAllAsRead(@Param("myId") UUID myId, @Param("partnerId") UUID partnerId);

    // 7. Đánh dấu tất cả tin nhắn từ partner gửi cho mình là ĐÃ ĐỌC (khi mở chat)
    @Query("SELECT m FROM Message m WHERE " +
       "((m.sender.id = :u1 AND m.receiver.id = :u2) OR (m.sender.id = :u2 AND m.receiver.id = :u1)) " +
       "AND m.attachmentUrl IS NOT NULL " +
       "ORDER BY m.createdAt DESC")
       List<Message> findAllMedia(@Param("u1") UUID u1, @Param("u2") UUID u2);

       // 8. HÀM MỚI: Lấy media theo phân loại (IMAGE hoặc FILE)
    // Giúp bạn hiển thị Grid ảnh riêng và danh sách Tài liệu riêng
    @Query("SELECT m FROM Message m WHERE " +
           "((m.sender.id = :u1 AND m.receiver.id = :u2) OR " +
           "(m.sender.id = :u2 AND m.receiver.id = :u1)) " +
           "AND m.attachmentUrl IS NOT NULL " +
           "AND m.type = :mType " + // Dựa trên cột message_type của bạn
           "ORDER BY m.createdAt DESC")
    List<Message> findAllMediaByType(
            @Param("u1") UUID u1, 
            @Param("u2") UUID u2, 
            @Param("mType") MessageType mType);

    // 9. HÀM MỚI: Đếm số tin chưa đọc từ một đối tác cụ thể
    // Dùng để hiển thị badge số thông báo riêng cho từng người ở Sidebar trái
    @Query("SELECT COUNT(m) FROM Message m WHERE " +
           "m.receiver.id = :myId AND m.sender.id = :partnerId AND m.isRead = false")
    long countUnreadFromPartner(@Param("myId") UUID myId, @Param("partnerId") UUID partnerId);
    
    // 10. (Tùy chọn) Tìm thời gian của tin nhắn cuối cùng giữa 2 người
    // Giúp bạn sắp xếp danh sách hội thoại theo thời gian mới nhất ở Backend
    @Query("SELECT MAX(m.createdAt) FROM Message m WHERE " +
           "(m.sender.id = :u1 AND m.receiver.id = :u2) OR " +
           "(m.sender.id = :u2 AND m.receiver.id = :u1)")
    java.time.LocalDateTime findLatestMessageTime(@Param("u1") UUID u1, @Param("u2") UUID u2);


    //11. Lấy tin nhắn cuối cùng giữa hai người
    // Dùng để hiển thị "nội dung xem trước" ở danh sách hội thoại bên trái
    @Query(value = "SELECT * FROM messages m WHERE " +
           "((m.sender_id = :u1 AND m.receiver_id = :u2) OR (m.sender_id = :u2 AND m.receiver_id = :u1)) " +
           "ORDER BY m.created_at DESC LIMIT 1", nativeQuery = true)
    Message findTopLatestMessage(@Param("u1") UUID u1, @Param("u2") UUID u2);
}


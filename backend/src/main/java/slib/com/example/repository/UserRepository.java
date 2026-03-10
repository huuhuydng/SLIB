package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.users.User;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    Optional<User> findByUserCode(String userCode);

    Optional<User> findByUsername(String username);

    Optional<User> findByNotiDevice(String notiDevice);

    Boolean existsByEmail(String email);

    Boolean existsByUserCode(String userCode);

    Boolean existsByUsername(String username);

    Boolean existsByPhone(String phone);

    /**
     * Tìm user bằng email hoặc username hoặc userCode (MSSV)
     */
    Optional<User> findByEmailOrUsernameOrUserCode(String email, String username, String userCode);

    /**
     * Tìm user bằng email hoặc username
     */
    Optional<User> findByEmailOrUsername(String email, String username);

    /**
     * Xóa notiDevice (FCM token) khỏi tất cả user khác khi user mới sync token
     * → đảm bảo 1 device chỉ nhận notification cho 1 user
     */
    @Modifying
    @Query("UPDATE User u SET u.notiDevice = null WHERE u.notiDevice = :token AND u.id != :userId")
    void clearNotiDeviceForOtherUsers(@Param("token") String token, @Param("userId") UUID userId);

    /**
     * Update avatar URL by userCode
     */
    @Modifying
    @Query("UPDATE User u SET u.avtUrl = :avatarUrl WHERE UPPER(u.userCode) = UPPER(:userCode)")
    void updateAvatarUrl(@Param("userCode") String userCode, @Param("avatarUrl") String avatarUrl);
}

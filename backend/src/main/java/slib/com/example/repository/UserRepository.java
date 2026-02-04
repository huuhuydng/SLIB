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

    /**
     * Tìm user bằng email hoặc username hoặc userCode (MSSV)
     */
    Optional<User> findByEmailOrUsernameOrUserCode(String email, String username, String userCode);

    /**
     * Update avatar URL by userCode
     */
    @Modifying
    @Query("UPDATE User u SET u.avtUrl = :avatarUrl WHERE UPPER(u.userCode) = UPPER(:userCode)")
    void updateAvatarUrl(@Param("userCode") String userCode, @Param("avatarUrl") String avatarUrl);
}

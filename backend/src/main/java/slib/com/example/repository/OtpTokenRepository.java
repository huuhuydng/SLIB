package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.users.OtpToken;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, UUID> {

    /**
     * Find valid (unused, not expired) OTP by email and token
     */
    @Query("SELECT o FROM OtpToken o WHERE o.email = :email AND o.token = :token " +
            "AND o.isUsed = false AND o.expiresAt > :now")
    Optional<OtpToken> findValidOtp(
            @Param("email") String email,
            @Param("token") String token,
            @Param("now") LocalDateTime now);

    /**
     * Find the latest valid OTP for an email
     */
    @Query("SELECT o FROM OtpToken o WHERE o.email = :email " +
            "AND o.isUsed = false AND o.expiresAt > :now " +
            "ORDER BY o.createdAt DESC")
    Optional<OtpToken> findLatestValidOtp(
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    /**
     * Invalidate all OTPs for an email (mark as used)
     */
    @Modifying
    @Query("UPDATE OtpToken o SET o.isUsed = true WHERE o.email = :email")
    void invalidateAllForEmail(@Param("email") String email);

    /**
     * Delete expired OTPs (cleanup job)
     */
    @Modifying
    @Query("DELETE FROM OtpToken o WHERE o.expiresAt < :now")
    void deleteExpiredTokens(@Param("now") LocalDateTime now);
}

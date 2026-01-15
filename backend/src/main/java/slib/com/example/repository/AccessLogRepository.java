package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.hce.AccessLog;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccessLogRepository extends JpaRepository<AccessLog, UUID> {
    @Query("SELECT a FROM AccessLog a WHERE a.user.id = :userId AND a.checkOutTime IS NULL ORDER BY a.checkInTime DESC")
    Optional<AccessLog> checkInUser(UUID userId);
}
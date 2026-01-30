package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import slib.com.example.entity.booking.ReservationEntity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReservationRepository extends JpaRepository<ReservationEntity, UUID> {
    List<ReservationEntity> findByUserId(UUID userId);

    List<ReservationEntity> findBySeat_SeatId(Integer seatId);

    List<ReservationEntity> findByEndTimeBeforeAndStatus(LocalDateTime time, String status);

    List<ReservationEntity> findByCreatedAtBeforeAndStatus(LocalDateTime time, String status);

    // Delete all reservations by seat ID (for cascade delete when seat is deleted)
    void deleteBySeat_SeatId(Integer seatId);

    // Delete all reservations by user ID (for cascade delete when user is deleted)
    void deleteByUser_Id(UUID userId);

    // Count total bookings by user ID
    long countByUserId(UUID userId);
}

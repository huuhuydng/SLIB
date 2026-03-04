package slib.com.example.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import slib.com.example.entity.users.StudentProfile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentProfileRepository extends JpaRepository<StudentProfile, UUID> {

    Optional<StudentProfile> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    // Delete student profile by user ID (for cascade delete when user is deleted)
    void deleteByUserId(UUID userId);

    // Find all profiles with eagerly loaded User entity
    @Query("SELECT sp FROM StudentProfile sp JOIN FETCH sp.user u WHERE u.role = 'STUDENT'")
    List<StudentProfile> findAllWithUser();
}

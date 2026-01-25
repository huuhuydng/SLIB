package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.repository.StudentProfileRepository;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    private final StudentProfileRepository studentProfileRepository;

    /**
     * Get student profile by user ID
     */
    public Optional<StudentProfileResponse> getProfileByUserId(UUID userId) {
        return studentProfileRepository.findByUserId(userId)
                .map(StudentProfileResponse::fromEntity);
    }

    /**
     * Get or create student profile for a user
     */
    @Transactional
    public StudentProfileResponse getOrCreateProfile(User user) {
        Optional<StudentProfile> existing = studentProfileRepository.findByUserId(user.getId());

        if (existing.isPresent()) {
            return StudentProfileResponse.fromEntity(existing.get());
        }

        // Create new profile with default values
        StudentProfile newProfile = StudentProfile.builder()
                .userId(user.getId())
                .user(user)
                .reputationScore(100)
                .totalStudyHours(0.0)
                .violationCount(0)
                .build();

        StudentProfile saved = studentProfileRepository.save(newProfile);
        return StudentProfileResponse.fromEntity(saved);
    }

    /**
     * Update study hours for a student
     */
    @Transactional
    public Optional<StudentProfileResponse> addStudyHours(UUID userId, double hours) {
        return studentProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setTotalStudyHours(profile.getTotalStudyHours() + hours);
                    return StudentProfileResponse.fromEntity(studentProfileRepository.save(profile));
                });
    }

    /**
     * Update reputation score
     */
    @Transactional
    public Optional<StudentProfileResponse> updateReputationScore(UUID userId, int newScore) {
        return studentProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setReputationScore(newScore);
                    return StudentProfileResponse.fromEntity(studentProfileRepository.save(profile));
                });
    }

    /**
     * Add violation and decrease reputation
     */
    @Transactional
    public Optional<StudentProfileResponse> addViolation(UUID userId, int penaltyPoints) {
        return studentProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setViolationCount(profile.getViolationCount() + 1);
                    profile.setReputationScore(Math.max(0, profile.getReputationScore() - penaltyPoints));
                    return StudentProfileResponse.fromEntity(studentProfileRepository.save(profile));
                });
    }
}

package slib.com.example.service.users;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import slib.com.example.dto.users.StudentProfileResponse;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.entity.users.StudentProfile;
import slib.com.example.entity.users.User;
import slib.com.example.exception.BadRequestException;
import slib.com.example.repository.booking.ReservationRepository;
import slib.com.example.repository.users.StudentProfileRepository;
import slib.com.example.repository.users.UserRepository;
import slib.com.example.service.booking.BookingPolicyService;
import slib.com.example.service.chat.CloudinaryService;
import slib.com.example.service.reputation.ReputationService;
import slib.com.example.service.system.LibrarySettingService;
import slib.com.example.util.UserValidationUtil;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentProfileService {

    @PersistenceContext
    private EntityManager entityManager;

    private final StudentProfileRepository studentProfileRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    private final ReservationRepository reservationRepository;
    private final LibrarySettingService librarySettingService;
    private final BookingPolicyService bookingPolicyService;
    private final ReputationService reputationService;

    /**
     * Get student profile by user ID
     */
    @Transactional(readOnly = true)
    public Optional<StudentProfileResponse> getProfileByUserId(UUID userId) {
        return studentProfileRepository.findByUserId(userId)
                .map(this::buildProfileResponse);
    }

    /**
     * Get or create student profile for a user
     */
    @Transactional
    public StudentProfileResponse getOrCreateProfile(User user) {
        Optional<StudentProfile> existing = studentProfileRepository.findByUserId(user.getId());

        if (existing.isPresent()) {
            return buildProfileResponse(existing.get());
        }

        // Create new profile with default values
        StudentProfile newProfile = StudentProfile.builder()
                .userId(user.getId())
                .user(user)
                .reputationScore(ReputationService.MAX_REPUTATION_SCORE)
                .totalStudyHours(0.0)
                .violationCount(0)
                .build();

        entityManager.persist(newProfile);
        entityManager.flush();
        return buildProfileResponse(newProfile);
    }

    /**
     * Update study hours for a student
     */
    @Transactional
    public Optional<StudentProfileResponse> addStudyHours(UUID userId, double hours) {
        return studentProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setTotalStudyHours(profile.getTotalStudyHours() + hours);
                    return buildProfileResponse(studentProfileRepository.save(profile));
                });
    }

    /**
     * Update reputation score
     */
    @Transactional
    public Optional<StudentProfileResponse> updateReputationScore(UUID userId, int newScore) {
        return studentProfileRepository.findByUserId(userId)
                .map(profile -> {
                    profile.setReputationScore(reputationService.clampReputationScore(newScore));
                    return buildProfileResponse(studentProfileRepository.save(profile));
                });
    }

    @Transactional
    public Optional<StudentProfileResponse> applyManualReputationAdjustment(
            UUID userId,
            int points,
            String reason,
            User performedBy) {
        if (!userRepository.existsById(userId)) {
            return Optional.empty();
        }

        StudentProfile profile = reputationService.applyManualAdjustment(userId, points, reason, performedBy);
        return Optional.of(buildProfileResponse(profile));
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
                    return buildProfileResponse(studentProfileRepository.save(profile));
                });
    }

    /**
     * Update basic user info (fullName, phone, dob)
     */
    @Transactional
    public Optional<StudentProfileResponse> updateUserInfo(UUID userId,
            slib.com.example.dto.users.UpdateProfileRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (request.getFullName() != null) {
                        user.setFullName(normalizeFullNameForProfileUpdate(request.getFullName()));
                    }
                    if (request.getPhone() != null) {
                        String normalizedPhone = normalizePhoneForProfileUpdate(request.getPhone());
                        if (normalizedPhone != null && !normalizedPhone.equals(user.getPhone())
                                && userRepository.existsByPhone(normalizedPhone)) {
                            throw new BadRequestException("Số điện thoại đã được sử dụng");
                        }
                        user.setPhone(normalizedPhone);
                    }
                    if (request.getDob() != null) {
                        user.setDob(parseDobForProfileUpdate(request.getDob()));
                    }
                    userRepository.save(user);
                    return getOrCreateProfile(user);
                });
    }

    /**
     * Update user avatar (xóa avatar cũ trước khi upload mới)
     */
    @Transactional
    public Optional<StudentProfileResponse> updateAvatar(UUID userId,
            org.springframework.web.multipart.MultipartFile file) throws Exception {
        return userRepository.findById(userId)
                .map(user -> {
                    try {
                        // Xóa avatar cũ nếu có
                        String oldAvatarUrl = user.getAvtUrl();
                        if (oldAvatarUrl != null && !oldAvatarUrl.isEmpty()) {
                            cloudinaryService.deleteAvatars(java.util.List.of(oldAvatarUrl));
                        }

                        // Upload avatar mới lên Cloudinary
                        String avatarUrl = cloudinaryService.uploadAvatar(file);
                        user.setAvtUrl(avatarUrl);
                        userRepository.save(user);
                        return getOrCreateProfile(user);
                    } catch (Exception e) {
                        throw new RuntimeException("Lỗi upload avatar: " + e.getMessage());
                    }
                });
    }

    private StudentProfileResponse buildProfileResponse(StudentProfile profile) {
        long bookingCount = reservationRepository.countByUserId(profile.getUserId());
        double studyHours = reservationRepository.getTotalStudyMinutesByUser(profile.getUserId()) / 60.0;
        LibrarySetting settings = librarySettingService.getSettings();
        StudentProfileResponse response = StudentProfileResponse.fromEntity(profile, bookingCount, studyHours);
        int currentReputation = profile.getReputationScore() != null ? profile.getReputationScore() : 100;
        response.setBookingRestriction(
                bookingPolicyService.getCurrentRestrictionStatus(profile.getUserId(), currentReputation, settings));
        return response;
    }

    private String normalizeFullNameForProfileUpdate(String value) {
        try {
            return UserValidationUtil.normalizeRequiredFullName(value);
        } catch (RuntimeException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private String normalizePhoneForProfileUpdate(String value) {
        try {
            return UserValidationUtil.normalizeOptionalPhone(value);
        } catch (RuntimeException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    private java.time.LocalDate parseDobForProfileUpdate(String value) {
        try {
            return UserValidationUtil.parseOptionalDob(value);
        } catch (RuntimeException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }
}

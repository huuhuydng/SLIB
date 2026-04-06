package slib.com.example.service.system;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import slib.com.example.dto.system.LibrarySettingDTO;
import slib.com.example.entity.library.LibrarySetting;
import slib.com.example.dto.booking.TimeSlotDTO;
import slib.com.example.repository.system.LibrarySettingRepository;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibrarySettingService {

    private final LibrarySettingRepository repository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Lấy cấu hình thư viện, tạo default nếu chưa có
     */
    public LibrarySetting getSettings() {
        return repository.findById(1).orElseGet(() -> repository.save(buildDefaultSettings()));
    }

    /**
     * Lấy cấu hình dạng DTO
     */
    public LibrarySettingDTO getSettingsDTO() {
        LibrarySetting settings = getSettings();
        return LibrarySettingDTO.builder()
                .openTime(settings.getOpenTime())
                .closeTime(settings.getCloseTime())
                .slotDuration(settings.getSlotDuration())
                .maxBookingDays(settings.getMaxBookingDays())
                .workingDays(settings.getWorkingDays())
                .maxBookingsPerDay(settings.getMaxBookingsPerDay())
                .maxHoursPerDay(settings.getMaxHoursPerDay())
                .autoCancelMinutes(settings.getAutoCancelMinutes())
                .autoCancelOnLeaveMinutes(settings.getAutoCancelOnLeaveMinutes())
                .minReputation(settings.getMinReputation())
                .libraryClosed(settings.getLibraryClosed())
                .closedReason(settings.getClosedReason())
                .notifyBookingSuccess(settings.getNotifyBookingSuccess())
                .notifyCheckinReminder(settings.getNotifyCheckinReminder())
                .notifyTimeExpiry(settings.getNotifyTimeExpiry())
                .notifyViolation(settings.getNotifyViolation())
                .notifyWeeklyReport(settings.getNotifyWeeklyReport())
                .notifyDeviceAlert(settings.getNotifyDeviceAlert())
                .build();
    }

    /**
     * Cập nhật cấu hình thư viện
     */
    @Transactional
    public LibrarySettingDTO updateSettings(LibrarySettingDTO dto) {
        LibrarySetting settings = getSettings();

        if (dto.getOpenTime() != null) {
            settings.setOpenTime(dto.getOpenTime());
        }
        if (dto.getCloseTime() != null) {
            settings.setCloseTime(dto.getCloseTime());
        }
        if (dto.getSlotDuration() != null) {
            settings.setSlotDuration(dto.getSlotDuration());
        }
        if (dto.getMaxBookingDays() != null) {
            settings.setMaxBookingDays(dto.getMaxBookingDays());
        }
        if (dto.getWorkingDays() != null) {
            settings.setWorkingDays(dto.getWorkingDays());
        }
        if (dto.getMaxBookingsPerDay() != null) {
            settings.setMaxBookingsPerDay(dto.getMaxBookingsPerDay());
        }
        if (dto.getMaxHoursPerDay() != null) {
            settings.setMaxHoursPerDay(dto.getMaxHoursPerDay());
        }
        if (dto.getAutoCancelMinutes() != null) {
            settings.setAutoCancelMinutes(dto.getAutoCancelMinutes());
        }
        if (dto.getAutoCancelOnLeaveMinutes() != null) {
            settings.setAutoCancelOnLeaveMinutes(dto.getAutoCancelOnLeaveMinutes());
        }
        if (dto.getMinReputation() != null) {
            settings.setMinReputation(dto.getMinReputation());
        }
        if (dto.getLibraryClosed() != null) {
            settings.setLibraryClosed(dto.getLibraryClosed());
        }
        if (dto.getClosedReason() != null) {
            settings.setClosedReason(dto.getClosedReason());
        }
        // Notification settings
        if (dto.getNotifyBookingSuccess() != null) {
            settings.setNotifyBookingSuccess(dto.getNotifyBookingSuccess());
        }
        if (dto.getNotifyCheckinReminder() != null) {
            settings.setNotifyCheckinReminder(dto.getNotifyCheckinReminder());
        }
        if (dto.getNotifyTimeExpiry() != null) {
            settings.setNotifyTimeExpiry(dto.getNotifyTimeExpiry());
        }
        if (dto.getNotifyViolation() != null) {
            settings.setNotifyViolation(dto.getNotifyViolation());
        }
        if (dto.getNotifyWeeklyReport() != null) {
            settings.setNotifyWeeklyReport(dto.getNotifyWeeklyReport());
        }
        if (dto.getNotifyDeviceAlert() != null) {
            settings.setNotifyDeviceAlert(dto.getNotifyDeviceAlert());
        }

        validateSettings(settings);
        repository.save(settings);
        return getSettingsDTO();
    }

    /**
     * Toggle trạng thái đóng/mở thư viện
     */
    @Transactional
    public LibrarySettingDTO toggleLibraryClosed(Boolean closed, String reason) {
        LibrarySetting settings = getSettings();
        boolean nextClosed = closed != null && closed;

        if (Boolean.TRUE.equals(settings.getLibraryClosed()) == nextClosed) {
            throw new IllegalArgumentException("Thư viện đã ở trạng thái này");
        }

        settings.setLibraryClosed(nextClosed);
        settings.setClosedReason(nextClosed ? normalizeClosedReason(reason) : null);

        validateSettings(settings);
        repository.save(settings);
        return getSettingsDTO();
    }

    /**
     * Khôi phục cấu hình mặc định
     */
    @Transactional
    public LibrarySettingDTO resetSettings() {
        LibrarySetting defaults = buildDefaultSettings();
        LibrarySetting settings = getSettings();

        settings.setOpenTime(defaults.getOpenTime());
        settings.setCloseTime(defaults.getCloseTime());
        settings.setSlotDuration(defaults.getSlotDuration());
        settings.setMaxBookingDays(defaults.getMaxBookingDays());
        settings.setWorkingDays(defaults.getWorkingDays());
        settings.setMaxBookingsPerDay(defaults.getMaxBookingsPerDay());
        settings.setMaxHoursPerDay(defaults.getMaxHoursPerDay());
        settings.setAutoCancelMinutes(defaults.getAutoCancelMinutes());
        settings.setAutoCancelOnLeaveMinutes(defaults.getAutoCancelOnLeaveMinutes());
        settings.setMinReputation(defaults.getMinReputation());
        settings.setLibraryClosed(defaults.getLibraryClosed());
        settings.setClosedReason(defaults.getClosedReason());
        settings.setNotifyBookingSuccess(defaults.getNotifyBookingSuccess());
        settings.setNotifyCheckinReminder(defaults.getNotifyCheckinReminder());
        settings.setNotifyTimeExpiry(defaults.getNotifyTimeExpiry());
        settings.setNotifyViolation(defaults.getNotifyViolation());
        settings.setNotifyWeeklyReport(defaults.getNotifyWeeklyReport());
        settings.setNotifyDeviceAlert(defaults.getNotifyDeviceAlert());

        repository.save(settings);
        return getSettingsDTO();
    }

    /**
     * Generate danh sách khung giờ từ cấu hình
     * Ví dụ: openTime=07:00, closeTime=21:00, slotDuration=60
     * -> [07:00-08:00, 08:00-09:00, ..., 20:00-21:00]
     */
    public List<TimeSlotDTO> generateTimeSlots() {
        LibrarySetting settings = getSettings();
        validateSettings(settings);
        List<TimeSlotDTO> slots = new ArrayList<>();

        LocalTime start = LocalTime.parse(settings.getOpenTime(), TIME_FORMATTER);
        LocalTime end = LocalTime.parse(settings.getCloseTime(), TIME_FORMATTER);
        int durationMinutes = settings.getSlotDuration();

        LocalTime current = start;
        while (current.plusMinutes(durationMinutes).compareTo(end) <= 0) {
            LocalTime slotEnd = current.plusMinutes(durationMinutes);

            String startStr = current.format(TIME_FORMATTER);
            String endStr = slotEnd.format(TIME_FORMATTER);

            slots.add(TimeSlotDTO.builder()
                    .startTime(startStr)
                    .endTime(endStr)
                    .label(startStr + " - " + endStr)
                    .build());

            current = slotEnd;
        }

        return slots;
    }

    private LibrarySetting buildDefaultSettings() {
        return LibrarySetting.builder()
                .id(1)
                .openTime("07:00")
                .closeTime("21:00")
                .slotDuration(60)
                .maxBookingDays(14)
                .workingDays("2,3,4,5,6")
                .maxBookingsPerDay(3)
                .maxHoursPerDay(4)
                .autoCancelMinutes(15)
                .autoCancelOnLeaveMinutes(30)
                .minReputation(0)
                .libraryClosed(false)
                .closedReason(null)
                .notifyBookingSuccess(true)
                .notifyCheckinReminder(true)
                .notifyTimeExpiry(true)
                .notifyViolation(true)
                .notifyWeeklyReport(false)
                .notifyDeviceAlert(true)
                .build();
    }

    private void validateSettings(LibrarySetting settings) {
        LocalTime openTime = parseTime(settings.getOpenTime(), "Giờ mở cửa");
        LocalTime closeTime = parseTime(settings.getCloseTime(), "Giờ đóng cửa");

        if (!openTime.isBefore(closeTime)) {
            throw new IllegalArgumentException("Giờ đóng cửa phải sau giờ mở cửa");
        }

        long totalMinutes = Duration.between(openTime, closeTime).toMinutes();
        Integer slotDuration = settings.getSlotDuration();
        if (slotDuration == null || slotDuration <= 0) {
            throw new IllegalArgumentException("Thời lượng mỗi slot phải lớn hơn 0");
        }
        if (slotDuration > totalMinutes) {
            throw new IllegalArgumentException("Thời lượng mỗi slot không được lớn hơn thời gian mở cửa");
        }

        if (settings.getMaxBookingDays() == null || settings.getMaxBookingDays() <= 0) {
            throw new IllegalArgumentException("Số ngày đặt trước tối đa phải lớn hơn 0");
        }
        if (settings.getMaxBookingsPerDay() == null || settings.getMaxBookingsPerDay() <= 0) {
            throw new IllegalArgumentException("Số lượt đặt tối đa mỗi ngày phải lớn hơn 0");
        }
        if (settings.getMaxHoursPerDay() == null || settings.getMaxHoursPerDay() <= 0) {
            throw new IllegalArgumentException("Số giờ đặt tối đa mỗi ngày phải lớn hơn 0");
        }
        if ((long) settings.getMaxHoursPerDay() * 60 > totalMinutes) {
            throw new IllegalArgumentException("Số giờ đặt tối đa mỗi ngày không được vượt quá tổng giờ hoạt động");
        }
        if (settings.getAutoCancelMinutes() == null || settings.getAutoCancelMinutes() <= 0) {
            throw new IllegalArgumentException("Thời gian tự hủy nếu không check-in phải lớn hơn 0");
        }
        if (settings.getAutoCancelOnLeaveMinutes() == null || settings.getAutoCancelOnLeaveMinutes() <= 0) {
            throw new IllegalArgumentException("Thời gian tự hủy khi rời chỗ phải lớn hơn 0");
        }
        if (settings.getMinReputation() == null || settings.getMinReputation() < 0) {
            throw new IllegalArgumentException("Điểm uy tín tối thiểu không được nhỏ hơn 0");
        }

        settings.setWorkingDays(normalizeWorkingDays(settings.getWorkingDays()));

        if (Boolean.TRUE.equals(settings.getLibraryClosed())) {
            settings.setClosedReason(normalizeClosedReason(settings.getClosedReason()));
        } else {
            settings.setClosedReason(null);
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        try {
            return LocalTime.parse(value, TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " phải đúng định dạng HH:mm");
        }
    }

    private String normalizeWorkingDays(String workingDays) {
        if (!StringUtils.hasText(workingDays)) {
            throw new IllegalArgumentException("Ngày làm việc không được để trống");
        }

        List<Integer> parsedDays = Arrays.stream(workingDays.split(","))
                .map(String::trim)
                .map(value -> {
                    try {
                        int day = Integer.parseInt(value);
                        if (day < 1 || day > 7) {
                            throw new IllegalArgumentException("Ngày làm việc chỉ được phép từ 1 đến 7");
                        }
                        return day;
                    } catch (NumberFormatException ex) {
                        throw new IllegalArgumentException("Ngày làm việc chỉ được phép từ 1 đến 7");
                    }
                })
                .toList();

        Set<Integer> uniqueDays = new LinkedHashSet<>(parsedDays);
        if (uniqueDays.size() != parsedDays.size()) {
            throw new IllegalArgumentException("Ngày làm việc không được trùng lặp");
        }

        return uniqueDays.stream()
                .sorted()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private String normalizeClosedReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Vui lòng nhập lý do khi khóa thư viện");
        }

        String normalized = reason.trim();
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("Lý do đóng thư viện không được vượt quá 500 ký tự");
        }
        return normalized;
    }
}

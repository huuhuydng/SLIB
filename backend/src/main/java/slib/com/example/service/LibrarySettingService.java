package slib.com.example.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import slib.com.example.dto.LibrarySettingDTO;
import slib.com.example.dto.TimeSlotDTO;
import slib.com.example.entity.LibrarySetting;
import slib.com.example.repository.LibrarySettingRepository;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LibrarySettingService {

    private final LibrarySettingRepository repository;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * Lấy cấu hình thư viện, tạo default nếu chưa có
     */
    public LibrarySetting getSettings() {
        return repository.findById(1).orElseGet(() -> {
            LibrarySetting defaultSettings = LibrarySetting.builder()
                    .id(1)
                    .openTime("07:00")
                    .closeTime("21:00")
                    .slotDuration(60)
                    .maxBookingDays(14)
                    .workingDays("2,3,4,5,6")
                    .maxBookingsPerDay(3)
                    .maxHoursPerDay(4)
                    .build();
            return repository.save(defaultSettings);
        });
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

        repository.save(settings);
        return getSettingsDTO();
    }

    /**
     * Toggle trạng thái đóng/mở thư viện
     */
    public LibrarySettingDTO toggleLibraryClosed(Boolean closed, String reason) {
        LibrarySetting settings = getSettings();
        settings.setLibraryClosed(closed != null ? closed : false);
        settings.setClosedReason(Boolean.TRUE.equals(closed) ? reason : null);
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
}

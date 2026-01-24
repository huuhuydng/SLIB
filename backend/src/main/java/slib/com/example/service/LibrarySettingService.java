package slib.com.example.service;

import org.springframework.stereotype.Service;
import slib.com.example.dto.LibrarySettingDTO;
import slib.com.example.dto.TimeSlotDTO;
import slib.com.example.entity.LibrarySetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class LibrarySettingService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final AtomicReference<LibrarySetting> settingsRef = new AtomicReference<>(createDefaultSettings());

    /**
     * Lấy cấu hình thư viện, tạo default nếu chưa có
     */
    public LibrarySetting getSettings() {
        return settingsRef.get();
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

        settingsRef.set(settings);
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

    private LibrarySetting createDefaultSettings() {
        return LibrarySetting.builder()
                .id(1)
                .openTime("07:00")
                .closeTime("21:00")
                .slotDuration(60)
                .maxBookingDays(14)
                .workingDays("2,3,4,5,6")
                .maxBookingsPerDay(3)
                .maxHoursPerDay(4)
                .build();
    }
}

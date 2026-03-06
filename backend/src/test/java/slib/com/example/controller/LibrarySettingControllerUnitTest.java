package slib.com.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.dto.LibrarySettingDTO;
import slib.com.example.dto.TimeSlotDTO;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.LibrarySettingService;

import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for LibrarySettingController
 * Testing Framework: JUnit 5, Mockito, MockMvc
 */
@WebMvcTest(value = LibrarySettingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("LibrarySettingController Unit Tests")
class LibrarySettingControllerUnitTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private LibrarySettingService librarySettingService;

        @Autowired
        private ObjectMapper objectMapper;

        // =========================================
        // === GET SETTINGS ===
        // =========================================

        @Test
        @DisplayName("getSettings_success_returns200WithSettings")
        void getSettings_success_returns200WithSettings() throws Exception {
                // Arrange
                LibrarySettingDTO settings = LibrarySettingDTO.builder()
                                .openTime("07:00")
                                .closeTime("21:00")
                                .slotDuration(60)
                                .maxBookingsPerDay(3)
                                .build();

                when(librarySettingService.getSettingsDTO()).thenReturn(settings);

                // Act & Assert
                mockMvc.perform(get("/slib/settings/library")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.openTime").value("07:00"))
                                .andExpect(jsonPath("$.closeTime").value("21:00"))
                                .andExpect(jsonPath("$.slotDuration").value(60))
                                .andExpect(jsonPath("$.maxBookingsPerDay").value(3));

                verify(librarySettingService, times(1)).getSettingsDTO();
        }

        // =========================================
        // === UPDATE SETTINGS ===
        // =========================================

        @Test
        @DisplayName("updateSettings_validRequest_returns200WithUpdatedSettings")
        void updateSettings_validRequest_returns200WithUpdatedSettings() throws Exception {
                // Arrange
                LibrarySettingDTO requestDto = LibrarySettingDTO.builder()
                                .openTime("08:00")
                                .closeTime("22:00")
                                .slotDuration(90)
                                .maxBookingsPerDay(5)
                                .build();

                LibrarySettingDTO responseDto = LibrarySettingDTO.builder()
                                .openTime("08:00")
                                .closeTime("22:00")
                                .slotDuration(90)
                                .maxBookingsPerDay(5)
                                .build();

                when(librarySettingService.updateSettings(any(LibrarySettingDTO.class)))
                                .thenReturn(responseDto);

                // Act & Assert
                mockMvc.perform(put("/slib/settings/library")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDto)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.openTime").value("08:00"))
                                .andExpect(jsonPath("$.closeTime").value("22:00"))
                                .andExpect(jsonPath("$.slotDuration").value(90))
                                .andExpect(jsonPath("$.maxBookingsPerDay").value(5));

                verify(librarySettingService, times(1)).updateSettings(any(LibrarySettingDTO.class));
        }

        // =========================================
        // === GET TIME SLOTS ===
        // =========================================

        @Test
        @DisplayName("getTimeSlots_success_returns200WithSlotsList")
        void getTimeSlots_success_returns200WithSlotsList() throws Exception {
                // Arrange
                TimeSlotDTO slot1 = new TimeSlotDTO();
                slot1.setStartTime("07:00");
                slot1.setEndTime("08:00");
                slot1.setLabel("07:00 - 08:00");

                TimeSlotDTO slot2 = new TimeSlotDTO();
                slot2.setStartTime("08:00");
                slot2.setEndTime("09:00");
                slot2.setLabel("08:00 - 09:00");

                TimeSlotDTO slot3 = new TimeSlotDTO();
                slot3.setStartTime("09:00");
                slot3.setEndTime("10:00");
                slot3.setLabel("09:00 - 10:00");

                when(librarySettingService.generateTimeSlots())
                                .thenReturn(Arrays.asList(slot1, slot2, slot3));

                // Act & Assert
                mockMvc.perform(get("/slib/settings/time-slots")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].startTime").value("07:00"))
                                .andExpect(jsonPath("$[0].endTime").value("08:00"))
                                .andExpect(jsonPath("$[1].startTime").value("08:00"))
                                .andExpect(jsonPath("$[2].startTime").value("09:00"));

                verify(librarySettingService, times(1)).generateTimeSlots();
        }

        @Test
        @DisplayName("getTimeSlots_empty_returns200WithEmptyList")
        void getTimeSlots_empty_returns200WithEmptyList() throws Exception {
                // Arrange
                when(librarySettingService.generateTimeSlots()).thenReturn(List.of());

                // Act & Assert
                mockMvc.perform(get("/slib/settings/time-slots")
                                .contentType(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$").isArray())
                                .andExpect(jsonPath("$.length()").value(0));
        }
}

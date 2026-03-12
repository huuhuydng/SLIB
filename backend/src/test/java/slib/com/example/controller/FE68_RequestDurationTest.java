package slib.com.example.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import slib.com.example.controller.LibrarySettingController;
import slib.com.example.dto.TimeSlotDTO;
import slib.com.example.exception.BadRequestException;
import slib.com.example.exception.GlobalExceptionHandler;
import slib.com.example.service.LibrarySettingService;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests for FE-68: Request Duration
 * Test Report: doc/Report/UnitTestReport/FE68_TestReport.md
 */
@WebMvcTest(value = LibrarySettingController.class, excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = {
                slib.com.example.security.JwtAuthenticationFilter.class }))
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("FE-68: Request Duration - Unit Tests")
class FE68_RequestDurationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private LibrarySettingService librarySettingService;

        // =========================================
        // === UTCID01: Settings record missing or invalid ===
        // =========================================

        /**
         * UTCID01: Settings record missing or invalid - duration validation fails
         * Precondition: Duration request or booking-slot request is made
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID01: Missing/invalid settings returns 400 Bad Request")
        void getTimeSlots_invalidSettings_returns400() throws Exception {
                when(librarySettingService.generateTimeSlots())
                                .thenThrow(new BadRequestException("Chua cau hinh thoi gian hoat dong cua thu vien"));

                mockMvc.perform(get("/slib/settings/time-slots"))
                                .andExpect(status().isBadRequest());

                verify(librarySettingService, times(1)).generateTimeSlots();
        }

        // =========================================
        // === UTCID02: Valid slot duration - normal slots ===
        // =========================================

        /**
         * UTCID02: Settings record exists with valid slot duration
         * Precondition: Duration depends on library open/close/slot settings
         * Expected: 200 OK with time slot list
         */
        @Test
        @DisplayName("UTCID02: Valid settings returns 200 OK with time slots")
        void getTimeSlots_validSettings_returns200OK() throws Exception {
                List<TimeSlotDTO> slots = List.of(
                                TimeSlotDTO.builder().startTime("07:00").endTime("08:00").label("07:00 - 08:00").build(),
                                TimeSlotDTO.builder().startTime("08:00").endTime("09:00").label("08:00 - 09:00").build(),
                                TimeSlotDTO.builder().startTime("09:00").endTime("10:00").label("09:00 - 10:00").build());

                when(librarySettingService.generateTimeSlots()).thenReturn(slots);

                mockMvc.perform(get("/slib/settings/time-slots"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(3))
                                .andExpect(jsonPath("$[0].startTime").value("07:00"))
                                .andExpect(jsonPath("$[0].endTime").value("08:00"))
                                .andExpect(jsonPath("$[0].label").value("07:00 - 08:00"));

                verify(librarySettingService, times(1)).generateTimeSlots();
        }

        // =========================================
        // === UTCID03: Valid settings - many slots ===
        // =========================================

        /**
         * UTCID03: Settings with longer operating hours generate many slots
         * Precondition: Duration depends on library open/close/slot settings
         * Expected: 200 OK with multiple time slots
         */
        @Test
        @DisplayName("UTCID03: Settings with many slots returns 200 OK")
        void getTimeSlots_manySlots_returns200OK() throws Exception {
                List<TimeSlotDTO> slots = List.of(
                                TimeSlotDTO.builder().startTime("06:00").endTime("07:00").label("06:00 - 07:00").build(),
                                TimeSlotDTO.builder().startTime("07:00").endTime("08:00").label("07:00 - 08:00").build(),
                                TimeSlotDTO.builder().startTime("08:00").endTime("09:00").label("08:00 - 09:00").build(),
                                TimeSlotDTO.builder().startTime("09:00").endTime("10:00").label("09:00 - 10:00").build(),
                                TimeSlotDTO.builder().startTime("10:00").endTime("11:00").label("10:00 - 11:00").build());

                when(librarySettingService.generateTimeSlots()).thenReturn(slots);

                mockMvc.perform(get("/slib/settings/time-slots"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(5));

                verify(librarySettingService, times(1)).generateTimeSlots();
        }

        // =========================================
        // === UTCID04: Invalid settings propagate error ===
        // =========================================

        /**
         * UTCID04: Invalid settings or booking-duration failure is propagated
         * Precondition: Settings or booking validation fails
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID04: Invalid settings propagate error returns 400 Bad Request")
        void getTimeSlots_settingsValidationFail_returns400() throws Exception {
                when(librarySettingService.generateTimeSlots())
                                .thenThrow(new BadRequestException("Thoi gian dong cua phai sau thoi gian mo cua"));

                mockMvc.perform(get("/slib/settings/time-slots"))
                                .andExpect(status().isBadRequest());

                verify(librarySettingService, times(1)).generateTimeSlots();
        }

        // =========================================
        // === UTCID05: Slot duration config error ===
        // =========================================

        /**
         * UTCID05: Slot duration configuration error
         * Precondition: Settings or booking validation fails
         * Expected: 400 Bad Request
         */
        @Test
        @DisplayName("UTCID05: Slot duration config error returns 400 Bad Request")
        void getTimeSlots_slotDurationError_returns400() throws Exception {
                when(librarySettingService.generateTimeSlots())
                                .thenThrow(new BadRequestException("Thoi luong slot khong hop le"));

                mockMvc.perform(get("/slib/settings/time-slots"))
                                .andExpect(status().isBadRequest());

                verify(librarySettingService, times(1)).generateTimeSlots();
        }
}

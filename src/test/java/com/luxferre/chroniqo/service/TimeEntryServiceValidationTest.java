package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static com.luxferre.chroniqo.service.TimeEntryValidationException.ValidationErrorType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TimeEntryService input validation with TimeEntryValidationException.
 * Tests the validation logic added in SEC-05 (Input Validation).
 *
 * @author LuxFerre86
 * @since 12.03.2026
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TimeEntryService Validation Tests (Custom Exception)")
class TimeEntryServiceValidationTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TimeEntryService timeEntryService;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");

        lenient().when(userService.getCurrentUser()).thenReturn(testUser);
    }

    @Nested
    @DisplayName("Date Validation")
    class DateValidation {

        @Test
        @DisplayName("Should reject null TimeEntryDTO")
        void shouldRejectNullTimeEntryDTO() {
            assertThatThrownBy(() -> timeEntryService.saveEntry(null))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("Time entry cannot be null")
                    .extracting(e -> ((TimeEntryValidationException) e).getErrorType())
                    .isEqualTo(ValidationErrorType.NULL_VALUE);

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject null date")
        void shouldRejectNullDate() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    null,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    30
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("Date cannot be null")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.NULL_VALUE);
                        assertThat(ex.getField()).isEqualTo("date");
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject future date")
        void shouldRejectFutureDate() {
            LocalDate futureDate = LocalDate.now().plusDays(1);
            TimeEntryDTO dto = new TimeEntryDTO(
                    futureDate,
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    30
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("Date cannot be in the future")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.FUTURE_DATE);
                        assertThat(ex.getField()).isEqualTo("date");
                        assertThat(ex.getRejectedValue()).isEqualTo(futureDate);
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should accept today's date")
        void shouldAcceptTodaysDate() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    30
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept past date")
        void shouldAcceptPastDate() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now().minusDays(5),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    30
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    @Nested
    @DisplayName("Break Minutes Validation")
    class BreakMinutesValidation {

        @Test
        @DisplayName("Should reject negative break minutes")
        void shouldRejectNegativeBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    -10
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("cannot be negative")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.NEGATIVE_VALUE);
                        assertThat(ex.getRejectedValue()).isEqualTo(-10);
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject break minutes exceeding maximum (480 minutes)")
        void shouldRejectExcessiveBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    500 // More than 8 hours
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("cannot exceed")
                    .hasMessageContaining("480")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.VALUE_TOO_LARGE);
                        assertThat(ex.getRejectedValue()).isEqualTo(500);
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should accept zero break minutes")
        void shouldAcceptZeroBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    0
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept maximum allowed break minutes (480)")
        void shouldAcceptMaximumBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(22, 0), // 13 hours to accommodate 8-hour break
                    480
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept null break minutes (defaults to 0)")
        void shouldAcceptNullBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    null
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    @Nested
    @DisplayName("Time Range Validation")
    class TimeRangeValidation {

        @Test
        @DisplayName("Should reject end time before start time")
        void shouldRejectEndTimeBeforeStartTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(17, 0), // Start
                    LocalTime.of(9, 0),  // End (before start!)
                    30
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("must be after")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INVALID_RANGE);
                        assertThat(ex.getField()).isEqualTo("timeRange");
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject end time equal to start time")
        void shouldRejectEndTimeEqualToStartTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 0), // Same as start
                    30
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("cannot be equal to start time")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INVALID_RANGE);
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should accept valid time range")
        void shouldAcceptValidTimeRange() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    30
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept one-minute time entry")
        void shouldAcceptOneMinuteEntry() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(9, 1),
                    0
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    @Nested
    @DisplayName("Break Duration vs Total Time Validation")
    class BreakDurationValidation {

        @Test
        @DisplayName("Should reject break longer than total time")
        void shouldRejectBreakLongerThanTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),   // Start
                    LocalTime.of(10, 0),  // End (60 minutes total)
                    120                    // 120 minutes break (exceeds total!)
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("Break duration")
                    .hasMessageContaining("exceeds total time")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INCONSISTENT_DATA);
                    });

            verify(timeEntryRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should accept break equal to total time (edge case)")
        void shouldAcceptBreakEqualToTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),   // Start
                    LocalTime.of(10, 0),  // End (60 minutes total)
                    60                     // 60 minutes break (equal to total)
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept break less than total time")
        void shouldAcceptBreakLessThanTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),   // Start
                    LocalTime.of(17, 0),  // End (8 hours = 480 minutes)
                    60                     // 1-hour break
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarios {

        @Test
        @DisplayName("Typical office day: 9-17 with 1 hour lunch")
        void typicalOfficeDay() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    60
            );

            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("User tries to log tomorrow's hours (should fail)")
        void cannotLogFutureHours() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now().plusDays(1), // Tomorrow
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0),
                    60
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.FUTURE_DATE);
                    });
        }

        @Test
        @DisplayName("User enters end time before start time (should fail)")
        void endTimeBeforeStartTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(17, 0), // Accidentally swapped
                    LocalTime.of(9, 0),
                    60
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INVALID_RANGE);
                    });
        }

        @Test
        @DisplayName("User enters 10 hour break for 8 hour shift (should fail)")
        void breakExceedsTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now(),
                    LocalTime.of(9, 0),
                    LocalTime.of(17, 0), // 8 hours
                    600                   // 10 hours break!
            );

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        // Could be either VALUE_TOO_LARGE (break > 480) or INCONSISTENT_DATA (break > total)
                        assertThat(ex.getErrorType()).isIn(
                                ValidationErrorType.VALUE_TOO_LARGE,
                                ValidationErrorType.INCONSISTENT_DATA
                        );
                    });
        }
    }
}
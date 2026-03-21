package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.event.TimeEntryChangedEvent;
import com.luxferre.chroniqo.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static com.luxferre.chroniqo.service.TimeEntryValidationException.ValidationErrorType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TimeEntryService Validation Tests")
class TimeEntryServiceValidationTest {

    @Mock
    private TimeEntryRepository timeEntryRepository;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private TimeEntryService timeEntryService;

    @BeforeEach
    void setUp() {
        User testUser = new User();
        testUser.setId("test-user-id");
        testUser.setEmail("test@example.com");
        lenient().when(userService.getCurrentUser()).thenReturn(testUser);
    }

    // =========================================================================
    // Date Validation
    // =========================================================================

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
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should reject null date")
        void shouldRejectNullDate() {
            TimeEntryDTO dto = new TimeEntryDTO(null, LocalTime.of(9, 0), LocalTime.of(17, 0), 30);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("Date cannot be null")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.NULL_VALUE);
                        assertThat(ex.getField()).isEqualTo("date");
                    });

            verify(timeEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should reject future date")
        void shouldRejectFutureDate() {
            LocalDate futureDate = LocalDate.now().plusDays(1);
            TimeEntryDTO dto = new TimeEntryDTO(futureDate, LocalTime.of(9, 0), LocalTime.of(17, 0), 30);

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
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should accept today's date and publish event")
        void shouldAcceptTodaysDate() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 30);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
            verify(eventPublisher).publishEvent(any(TimeEntryChangedEvent.class));
        }

        @Test
        @DisplayName("Should accept past date and publish event")
        void shouldAcceptPastDate() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now().minusDays(5), LocalTime.of(9, 0), LocalTime.of(17, 0), 30);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
            verify(eventPublisher).publishEvent(any(TimeEntryChangedEvent.class));
        }
    }

    // =========================================================================
    // Break Minutes Validation
    // =========================================================================

    @Nested
    @DisplayName("Break Minutes Validation")
    class BreakMinutesValidation {

        @Test
        @DisplayName("Should reject negative break minutes")
        void shouldRejectNegativeBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), -10);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("cannot be negative")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.NEGATIVE_VALUE);
                        assertThat(ex.getRejectedValue()).isEqualTo(-10);
                    });

            verify(timeEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should reject break minutes exceeding maximum (480)")
        void shouldRejectExcessiveBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 500);

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
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should accept zero break minutes")
        void shouldAcceptZeroBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 0);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept maximum allowed break minutes (480)")
        void shouldAcceptMaximumBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(22, 0), 480);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept null break minutes")
        void shouldAcceptNullBreakMinutes() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), null);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    // =========================================================================
    // Time Range Validation
    // =========================================================================

    @Nested
    @DisplayName("Time Range Validation")
    class TimeRangeValidation {

        @Test
        @DisplayName("Should reject end time before start time")
        void shouldRejectEndTimeBeforeStartTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(17, 0), LocalTime.of(9, 0), 30);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("must be after")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INVALID_RANGE);
                        assertThat(ex.getField()).isEqualTo("timeRange");
                    });

            verify(timeEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should reject end time equal to start time")
        void shouldRejectEndTimeEqualToStartTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 0), 30);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("cannot be equal to start time")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INVALID_RANGE);
                    });

            verify(timeEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should accept valid time range")
        void shouldAcceptValidTimeRange() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 30);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept one-minute time entry")
        void shouldAcceptOneMinuteEntry() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(9, 1), 0);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    // =========================================================================
    // Break vs Total Time
    // =========================================================================

    @Nested
    @DisplayName("Break Duration vs Total Time Validation")
    class BreakDurationValidation {

        @Test
        @DisplayName("Should reject break longer than total time")
        void shouldRejectBreakLongerThanTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 120);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .hasMessageContaining("Break duration")
                    .hasMessageContaining("exceeds total time")
                    .satisfies(e -> {
                        TimeEntryValidationException ex = (TimeEntryValidationException) e;
                        assertThat(ex.getErrorType()).isEqualTo(ValidationErrorType.INCONSISTENT_DATA);
                    });

            verify(timeEntryRepository, never()).save(any());
            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("Should accept break equal to total time (edge case)")
        void shouldAcceptBreakEqualToTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(10, 0), 60);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }

        @Test
        @DisplayName("Should accept break less than total time")
        void shouldAcceptBreakLessThanTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 60);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
        }
    }

    // =========================================================================
    // Event Publishing
    // =========================================================================

    @Nested
    @DisplayName("Event Publishing")
    class EventPublishing {

        @Test
        @DisplayName("saveEntry publishes event after successful save")
        void saveEntry_publishesEventAfterSave() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 30);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(eventPublisher).publishEvent(any(TimeEntryChangedEvent.class));
        }

        @Test
        @DisplayName("deleteEntries does not publish event when no entries exist")
        void deleteEntries_doesNotPublishWhenNoEntries() {
            when(timeEntryRepository.findByUserAndDateBetween(any(), any(), any())).thenReturn(List.of());

            timeEntryService.deleteEntries(LocalDate.now(), LocalDate.now());

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("deleteEntries publishes event when entries were deleted")
        void deleteEntries_publishesEventWhenEntriesDeleted() {
            when(timeEntryRepository.findByUserAndDateBetween(any(), any(), any()))
                    .thenReturn(List.of(new TimeEntry()));

            timeEntryService.deleteEntries(LocalDate.now(), LocalDate.now());

            verify(eventPublisher).publishEvent(any(TimeEntryChangedEvent.class));
        }

        @Test
        @DisplayName("saveEntry does not publish event on validation failure")
        void saveEntry_doesNotPublishOnValidationFailure() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), 30);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class);

            verify(eventPublisher, never()).publishEvent(any());
        }
    }

    // =========================================================================
    // Real-World Scenarios
    // =========================================================================

    @Nested
    @DisplayName("Real-World Scenarios")
    class RealWorldScenarios {

        @Test
        @DisplayName("Typical office day: 9-17 with 1 hour lunch")
        void typicalOfficeDay() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 60);
            when(timeEntryRepository.findByUserAndDate(any(), any())).thenReturn(null);

            timeEntryService.saveEntry(dto);

            verify(timeEntryRepository).save(any(TimeEntry.class));
            verify(eventPublisher).publishEvent(any(TimeEntryChangedEvent.class));
        }

        @Test
        @DisplayName("User tries to log tomorrow's hours — fails, no event")
        void cannotLogFutureHours() {
            TimeEntryDTO dto = new TimeEntryDTO(
                    LocalDate.now().plusDays(1), LocalTime.of(9, 0), LocalTime.of(17, 0), 60);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .satisfies(e -> assertThat(((TimeEntryValidationException) e).getErrorType())
                            .isEqualTo(ValidationErrorType.FUTURE_DATE));

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("User enters end time before start time — fails, no event")
        void endTimeBeforeStartTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(17, 0), LocalTime.of(9, 0), 60);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .satisfies(e -> assertThat(((TimeEntryValidationException) e).getErrorType())
                            .isEqualTo(ValidationErrorType.INVALID_RANGE));

            verify(eventPublisher, never()).publishEvent(any());
        }

        @Test
        @DisplayName("10-hour break for 8-hour shift — fails, no event")
        void breakExceedsTotalTime() {
            TimeEntryDTO dto = new TimeEntryDTO(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), 600);

            assertThatThrownBy(() -> timeEntryService.saveEntry(dto))
                    .isInstanceOf(TimeEntryValidationException.class)
                    .satisfies(e -> assertThat(((TimeEntryValidationException) e).getErrorType())
                            .isIn(ValidationErrorType.VALUE_TOO_LARGE, ValidationErrorType.INCONSISTENT_DATA));

            verify(eventPublisher, never()).publishEvent(any());
        }
    }
}
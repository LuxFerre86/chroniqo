package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.event.TimeEntryChangedEvent;
import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing time tracking entries with comprehensive validation.
 *
 * <p>Handles CRUD operations on {@link TimeEntry} entities and supports both
 * started (incomplete) and completed entries. All input data is validated before
 * persistence to ensure data integrity and prevent invalid states.
 *
 * <p>Validation rules enforced:
 * <ul>
 *   <li>Date must not be in the future</li>
 *   <li>Break duration must be between 0 and 480 minutes</li>
 *   <li>End time must be after start time</li>
 *   <li>Total work hours (including breaks) must not exceed 24 hours</li>
 *   <li>Break duration must not exceed total time span</li>
 * </ul>
 *
 * <p>Time entries are automatically transitioned between {@link TimeEntryStatus#STARTED}
 * and {@link TimeEntryStatus#COMPLETED} states based on whether both start and end
 * times are present.
 *
 * @author Luxferre86
 * @since 12.03.2026
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    // Validation constants
    private static final int MIN_BREAK_MINUTES = 0;
    private static final int MAX_BREAK_MINUTES = 480; // 8 hours
    private static final int MAX_WORK_HOURS = 24;
    static final int MAX_NOTES_LENGTH = 500;

    /**
     * Returns all time entries for the current user within the given date range.
     *
     * @param startDate first day of the range (inclusive)
     * @param endDate   last day of the range (inclusive)
     * @return list of {@link TimeEntryDTO} records
     */
    public List<TimeEntryDTO> getTimeEntries(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        log.info("Retrieving time entries between {} and {}", startDate, endDate);
        return timeEntryRepository
                .findByUserAndDateBetweenOrderByDateAscStartTimeAsc(user, startDate, endDate)
                .stream()
                .map(this::createTimeEntryDTO)
                .toList();
    }

    /**
     * Returns all time entries for the current user on a specific date.
     *
     * @param date the date to query
     * @return list of entries sorted by start time
     */
    public List<TimeEntryDTO> getTimeEntries(LocalDate date) {
        return getTimeEntries(date, date);
    }

    /**
     * Saves or updates a time entry after comprehensive input validation.
     * Creates a new entry if none exists for the date; updates the existing
     * one otherwise.
     *
     * @param timeEntryDTO the time entry data to persist
     * @throws TimeEntryValidationException if any validation rule is violated
     */
    @Transactional
    public void saveEntry(TimeEntryDTO timeEntryDTO) {
        // Validate input
        validateTimeEntry(timeEntryDTO);

        User user = userService.getCurrentUser();
        LocalDate date = timeEntryDTO.getDate();

        log.info("Saving time entry for date {}", date);

        TimeEntry entry = resolveEntryForSave(user, timeEntryDTO);
        validateNoOverlap(user, timeEntryDTO);

        entry.setDate(date);

        // Set start time
        entry.setStartTime(timeEntryDTO.getStartTime());

        // Set end time
        entry.setEndTime(timeEntryDTO.getEndTime());

        // Set break
        entry.setBreakMinutes(timeEntryDTO.getBreakMinutes());

        // Set notes
        entry.setNotes(timeEntryDTO.getNotes());

        // Determine status
        if (entry.getStartTime() != null && entry.getEndTime() != null) {
            entry.setStatus(TimeEntryStatus.COMPLETED);
            entry.setCompletedAt(LocalDateTime.now());
        } else if (entry.getStartTime() != null) {
            entry.setStatus(TimeEntryStatus.STARTED);
            entry.setCompletedAt(null);
        }

        timeEntryRepository.save(entry);

        log.info("Time entry saved for date {} with status {}", date, entry.getStatus());

        eventPublisher.publishEvent(new TimeEntryChangedEvent(user, getClass()));
    }

    /**
     * Deletes the time entry for the date carried in {@code timeEntryDTO}.
     *
     * @param timeEntryDTO identifies the entry to delete via its date
     */
    @Transactional
    public void deleteEntry(TimeEntryDTO timeEntryDTO) {
        User user = userService.getCurrentUser();
        if (timeEntryDTO.getId() != null) {
            timeEntryRepository.findByIdAndUser(timeEntryDTO.getId(), user).ifPresent(entry -> {
                log.info("Deleting time entry {} for date {}", entry.getId(), entry.getDate());
                timeEntryRepository.delete(entry);
                eventPublisher.publishEvent(new TimeEntryChangedEvent(user, getClass()));
            });
            return;
        }

        LocalDate date = timeEntryDTO.getDate();
        log.info("Deleting all time entries for date {} (legacy call path)", date);
        deleteEntries(date, date);
    }


    /**
     * Deletes all time entries for the current user within the given date range.
     *
     * @param startDate first day of the range (inclusive)
     * @param endDate   last day of the range (inclusive)
     */
    @Transactional
    public void deleteEntries(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        log.info("Deleting time entries between {} and {}", startDate, endDate);
        List<TimeEntry> timeEntryList = timeEntryRepository.findByUserAndDateBetween(user, startDate, endDate);
        if (!timeEntryList.isEmpty()) {
            timeEntryRepository.deleteAll(timeEntryList);
            log.info("Deleted {} time entries", timeEntryList.size());
            eventPublisher.publishEvent(new TimeEntryChangedEvent(user, getClass()));
        }
    }

    /**
     * Validates a time entry DTO according to the application's business rules.
     *
     * <p>Checks:
     * <ul>
     *   <li>DTO itself is not null</li>
     *   <li>Date is set and not in the future</li>
     *   <li>Break minutes (if set) are non-negative and do not exceed 8 hours</li>
     *   <li>End time is after start time (when both are set)</li>
     *   <li>End time is not equal to start time</li>
     *   <li>Break duration does not exceed total work span</li>
     *   <li>Total duration (with break) does not exceed 24 hours</li>
     *   <li>Notes (if set) must not exceed 500 characters</li>
     * </ul>
     *
     * @param dto the time entry to validate
     * @throws TimeEntryValidationException if any validation rule is violated
     */
    private void validateTimeEntry(TimeEntryDTO dto) {
        if (dto == null) {
            log.warn("Time entry validation failed: Time entry cannot be null");
            throw TimeEntryValidationException.nullValue("timeEntry",
                    "Time entry cannot be null");
        }

        if (dto.getDate() == null) {
            log.warn("Time entry validation failed: Date cannot be null");
            throw TimeEntryValidationException.nullValue("date",
                    "Date cannot be null");
        }

        if (dto.getStartTime() == null) {
            log.warn("Time entry validation failed: Start time cannot be null");
            throw TimeEntryValidationException.nullValue("startTime",
                    "Start time cannot be null");
        }

        // Validate date is not in the future
        if (dto.getDate().isAfter(LocalDate.now())) {
            log.warn("Time entry validation failed: Date {} is in the future", dto.getDate());
            throw TimeEntryValidationException.futureDate(dto.getDate());
        }

        // Validate break minutes
        Integer breakMinutes = dto.getBreakMinutes();
        if (breakMinutes != null) {
            if (breakMinutes < MIN_BREAK_MINUTES) {
                log.warn("Time entry validation failed: Break minutes {} is negative", breakMinutes);
                throw TimeEntryValidationException.negativeValue("Break minutes", breakMinutes);
            }
            if (breakMinutes > MAX_BREAK_MINUTES) {
                log.warn("Time entry validation failed: Break minutes {} exceeds maximum {} minutes", breakMinutes, MAX_BREAK_MINUTES);
                throw TimeEntryValidationException.valueTooLarge(
                        "Break minutes", breakMinutes, MAX_BREAK_MINUTES + " minutes");
            }
        }

        // Validate notes length
        String notes = dto.getNotes();
        if (notes != null && notes.length() > MAX_NOTES_LENGTH) {
            log.warn("Time entry validation failed: Notes length {} exceeds maximum {} characters", notes.length(), MAX_NOTES_LENGTH);
            throw TimeEntryValidationException.valueTooLarge(
                    "Notes", notes.length() + " characters", MAX_NOTES_LENGTH + " characters");
        }

        // Validate time range if both start and end times are present
        LocalTime startTime = dto.getStartTime();
        LocalTime endTime = dto.getEndTime();

        if (startTime != null && endTime != null) {
            // Check if end time is after start time
            if (endTime.isBefore(startTime)) {
                log.warn("Time entry validation failed: End time {} is before start time {}", endTime, startTime);
                throw TimeEntryValidationException.invalidRange(
                        String.format("End time (%s) must be after start time (%s)", endTime, startTime),
                        startTime, endTime
                );
            }

            // Check if end time equals start time
            if (endTime.equals(startTime)) {
                log.warn("Time entry validation failed: End time equals start time {}", startTime);
                throw TimeEntryValidationException.invalidRange(
                        "End time cannot be equal to start time",
                        startTime, endTime
                );
            }

            // Validate total work duration
            long totalMinutes = Duration.between(startTime, endTime).toMinutes();
            int effectiveBreakMinutes = breakMinutes != null ? breakMinutes : 0;
            long workMinutes = totalMinutes - effectiveBreakMinutes;

            if (workMinutes < 0) {
                log.warn("Time entry validation failed: Break duration {} exceeds total time {}", effectiveBreakMinutes, totalMinutes);
                throw TimeEntryValidationException.inconsistentData(
                        String.format("Break duration (%d minutes) exceeds total time (%d minutes)",
                                effectiveBreakMinutes, totalMinutes)
                );
            }

            // Check for unrealistic work hours (more than 24 hours)
            if (totalMinutes > MAX_WORK_HOURS * 60) {
                log.warn("Time entry validation failed: Total time {} minutes exceeds {} hours", totalMinutes, MAX_WORK_HOURS);
                throw TimeEntryValidationException.valueTooLarge(
                        "Total time", totalMinutes + " minutes", MAX_WORK_HOURS + " hours");
            }
        }
    }

    /**
     * Maps a {@link TimeEntry} JPA entity to a {@link TimeEntryDTO}.
     *
     * @param timeEntry the entity to convert
     * @return the corresponding DTO
     */
    TimeEntryDTO createTimeEntryDTO(TimeEntry timeEntry) {
        return new TimeEntryDTO(
                timeEntry.getId(),
                timeEntry.getDate(),
                timeEntry.getStartTime(),
                timeEntry.getEndTime(),
                timeEntry.getBreakMinutes(),
                timeEntry.getNotes());
    }

    private TimeEntry resolveEntryForSave(User user, TimeEntryDTO dto) {
        if (dto.getId() == null || dto.getId().isBlank()) {
            TimeEntry entry = new TimeEntry();
            entry.setUser(user);
            return entry;
        }

        return timeEntryRepository.findByIdAndUser(dto.getId(), user)
                .orElseThrow(() -> TimeEntryValidationException.inconsistentData(
                        "Time entry to update does not exist"));
    }

    private void validateNoOverlap(User user, TimeEntryDTO dto) {
        if (dto.getStartTime() == null) {
            return;
        }

        LocalTime candidateStart = dto.getStartTime();
        LocalTime candidateEnd = normalizeEnd(dto.getEndTime());

        List<TimeEntry> sameDayEntries = timeEntryRepository
                .findByUserAndDateOrderByStartTimeAsc(user, dto.getDate());

        for (TimeEntry existing : sameDayEntries) {
            if (Objects.equals(existing.getId(), dto.getId())) {
                continue;
            }

            if (existing.getStartTime() == null) {
                continue;
            }

            LocalTime existingStart = existing.getStartTime();
            LocalTime existingEnd = normalizeEnd(existing.getEndTime());
            if (overlaps(candidateStart, candidateEnd, existingStart, existingEnd)) {
                throw TimeEntryValidationException.invalidRange(
                        String.format("Time range overlaps with existing entry (%s - %s)",
                                existingStart,
                                existing.getEndTime() != null ? existing.getEndTime() : "OPEN"),
                        dto.getStartTime(),
                        dto.getEndTime() != null ? dto.getEndTime() : "OPEN");
            }
        }
    }

    private boolean overlaps(LocalTime candidateStart,
                             LocalTime candidateEnd,
                             LocalTime existingStart,
                             LocalTime existingEnd) {
        // Adjacency is allowed: [09:00-12:00] and [12:00-14:00] do not overlap.
        return candidateStart.isBefore(existingEnd) && candidateEnd.isAfter(existingStart);
    }

    private LocalTime normalizeEnd(LocalTime end) {
        return end != null ? end : LocalTime.MAX;
    }
}
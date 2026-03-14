package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service for managing time tracking entries.
 * Validates all input data before persistence to ensure data integrity.
 *
 * @author LuxFerre86
 * @since 12.03.2026
 */
@RequiredArgsConstructor
@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final UserService userService;

    // Validation constants
    private static final int MIN_BREAK_MINUTES = 0;
    private static final int MAX_BREAK_MINUTES = 480; // 8 hours
    private static final int MAX_WORK_HOURS = 24;

    public List<TimeEntryDTO> getTimeEntries(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        return timeEntryRepository.findByUserAndDateBetween(user, startDate, endDate)
                .stream()
                .map(this::createTimeEntryDTO)
                .toList();
    }

    public TimeEntryDTO getTimeEntry(LocalDate date) {
        return getTimeEntries(date, date).stream().findFirst().orElse(null);
    }

    /**
     * Saves or updates a time entry with comprehensive input validation.
     *
     * @param timeEntryDTO the time entry data to save
     * @throws TimeEntryValidationException if validation fails
     */
    @Transactional
    public void saveEntry(TimeEntryDTO timeEntryDTO) {
        // Validate input
        validateTimeEntry(timeEntryDTO);

        User user = userService.getCurrentUser();
        LocalDate date = timeEntryDTO.getDate();

        TimeEntry entry = timeEntryRepository.findByUserAndDate(user, date);
        if (entry == null) {
            entry = new TimeEntry();
            entry.setUser(user);
            entry.setDate(date);
        }

        // Set start time
        if (timeEntryDTO.getStartTime() != null) {
            entry.setStartTime(timeEntryDTO.getStartTime());
        }

        // Set end time
        if (timeEntryDTO.getEndTime() != null) {
            entry.setEndTime(timeEntryDTO.getEndTime());
        }

        // Set break
        entry.setBreakMinutes(timeEntryDTO.getBreakMinutes());

        // Determine status
        if (entry.getStartTime() != null && entry.getEndTime() != null) {
            entry.setStatus(TimeEntryStatus.COMPLETED);
            entry.setCompletedAt(LocalDateTime.now());
        } else if (entry.getStartTime() != null) {
            entry.setStatus(TimeEntryStatus.STARTED);
        }

        timeEntryRepository.save(entry);
    }

    @Transactional
    public void deleteEntry(TimeEntryDTO timeEntryDTO) {
        deleteEntries(timeEntryDTO.getDate(), timeEntryDTO.getDate());
    }

    @Transactional
    public void deleteEntries(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        timeEntryRepository.deleteAll(
                timeEntryRepository.findByUserAndDateBetween(user, startDate, endDate)
        );
    }

    /**
     * Validates a time entry DTO.
     *
     * @param dto the time entry to validate
     * @throws TimeEntryValidationException if validation fails
     */
    private void validateTimeEntry(TimeEntryDTO dto) {
        if (dto == null) {
            throw TimeEntryValidationException.nullValue("timeEntry",
                    "Time entry cannot be null");
        }

        if (dto.getDate() == null) {
            throw TimeEntryValidationException.nullValue("date",
                    "Date cannot be null");
        }

        // Validate date is not in the future
        if (dto.getDate().isAfter(LocalDate.now())) {
            throw TimeEntryValidationException.futureDate(dto.getDate());
        }

        // Validate break minutes
        Integer breakMinutes = dto.getBreakMinutes();
        if (breakMinutes != null) {
            if (breakMinutes < MIN_BREAK_MINUTES) {
                throw TimeEntryValidationException.negativeValue("Break minutes", breakMinutes);
            }
            if (breakMinutes > MAX_BREAK_MINUTES) {
                throw TimeEntryValidationException.valueTooLarge(
                        "Break minutes", breakMinutes, MAX_BREAK_MINUTES + " minutes");
            }
        }

        // Validate time range if both start and end times are present
        LocalTime startTime = dto.getStartTime();
        LocalTime endTime = dto.getEndTime();

        if (startTime != null && endTime != null) {
            // Check if end time is after start time
            if (endTime.isBefore(startTime)) {
                throw TimeEntryValidationException.invalidRange(
                        String.format("End time (%s) must be after start time (%s)", endTime, startTime),
                        startTime, endTime
                );
            }

            // Check if end time equals start time
            if (endTime.equals(startTime)) {
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
                throw TimeEntryValidationException.inconsistentData(
                        String.format("Break duration (%d minutes) exceeds total time (%d minutes)",
                                effectiveBreakMinutes, totalMinutes)
                );
            }

            // Check for unrealistic work hours (more than 24 hours)
            if (totalMinutes > MAX_WORK_HOURS * 60) {
                throw TimeEntryValidationException.valueTooLarge(
                        "Total time", totalMinutes + " minutes", MAX_WORK_HOURS + " hours");
            }
        }
    }

    TimeEntryDTO createTimeEntryDTO(TimeEntry timeEntry) {
        return new TimeEntryDTO(
                timeEntry.getDate(),
                timeEntry.getStartTime(),
                timeEntry.getEndTime(),
                timeEntry.getBreakMinutes());
    }
}
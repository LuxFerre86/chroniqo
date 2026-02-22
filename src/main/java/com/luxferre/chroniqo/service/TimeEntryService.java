package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@RequiredArgsConstructor
@Service
public class TimeEntryService {

    private final TimeEntryRepository repository;
    private final UserService userService;
    private final AbsenceRepository absenceRepository;

    public TimeEntryDTO getEntry(LocalDate date) {
        User user = userService.getCurrentUser();

        TimeEntry entry = repository.findByUserAndDate(user, date);
        if (entry == null) return null;

        return new TimeEntryDTO(
                entry.getDate().toString(),
                entry.getStartTime() != null ? entry.getStartTime().toString() : null,
                entry.getEndTime() != null ? entry.getEndTime().toString() : null,
                entry.getBreakMinutes()
        );
    }

    public TimeEntry getToday() {
        User user = userService.getCurrentUser();
        return repository.findByUserAndDate(user, LocalDate.now());
    }

    @Transactional
    public void saveEntry(TimeEntryDTO timeEntryDTO) {
        User user = userService.getCurrentUser();
        LocalDate date = LocalDate.parse(timeEntryDTO.getDate());

        TimeEntry entry = repository.findByUserAndDate(user, date);
        if (entry == null) {
            entry = new TimeEntry();
            entry.setUser(user);
            entry.setDate(date);
        }

        // Set start time
        if (timeEntryDTO.getStartTime() != null) {
            entry.setStartTime(LocalTime.parse(timeEntryDTO.getStartTime()));
        }

        // Set end time
        if (timeEntryDTO.getEndTime() != null) {
            entry.setEndTime(LocalTime.parse(timeEntryDTO.getEndTime()));
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

        repository.save(entry);

        deleteAbsence(user, timeEntryDTO);
    }

    @Transactional
    public void deleteEntry(TimeEntryDTO timeEntryDTO) {
        User user = userService.getCurrentUser();
        LocalDate date = LocalDate.parse(timeEntryDTO.getDate());

        TimeEntry entry = repository.findByUserAndDate(user, date);
        if (entry != null) {
            repository.delete(entry);
        }
    }

    public int calculateWorkedMinutes(TimeEntry entry) {
        // If entry not complete, calculate from start time to now
        if (entry.getEndTime() == null && entry.getStartTime() != null) {
            LocalTime now = LocalTime.now();
            Duration duration = Duration.between(entry.getStartTime(), now);
            int minutes = (int) duration.toMinutes();
            if (minutes < 0) minutes += 1440; // Handle day overflow
            if (entry.getBreakMinutes() != null) minutes -= entry.getBreakMinutes();
            return Math.max(0, minutes);
        }

        // Normal calculation for completed entries
        if (entry.getStartTime() == null) {
            return 0;
        }

        Duration duration = Duration.between(entry.getStartTime(), entry.getEndTime());
        int minutes = (int) duration.toMinutes();
        if (minutes < 0) minutes += 1440;
        if (entry.getBreakMinutes() != null) minutes -= entry.getBreakMinutes();
        return Math.max(0, minutes);
    }

    void deleteAbsence(User user, TimeEntryDTO timeEntryDTO) {
        Absence absence = absenceRepository.findByUserAndDate(user, LocalDate.parse(timeEntryDTO.getDate()));
        if (null != absence) {
            absenceRepository.delete(absence);
        }
    }
}

package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.TimeEntryStatus;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TimeEntryService {

    private final TimeEntryRepository timeEntryRepository;
    private final UserService userService;

    public List<TimeEntryDTO> getTimeEntries(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();
        return timeEntryRepository.findByUserAndDateBetween(user, startDate, endDate).stream().map(this::createTimeEntryDTO).toList();
    }

    public TimeEntryDTO getTimeEntry(LocalDate date) {
        return getTimeEntries(date, date).stream().findFirst().orElse(null);
    }

    @Transactional
    public void saveEntry(TimeEntryDTO timeEntryDTO) {
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
        timeEntryRepository.deleteAll(timeEntryRepository.findByUserAndDateBetween(user, startDate, endDate));
    }

    TimeEntryDTO createTimeEntryDTO(TimeEntry timeEntry) {
        return new TimeEntryDTO(
                timeEntry.getDate(),
                timeEntry.getStartTime(),
                timeEntry.getEndTime(),
                timeEntry.getBreakMinutes());
    }
}

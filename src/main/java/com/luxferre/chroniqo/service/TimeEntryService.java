package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
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
                entry.getStartTime().toString(),
                entry.getEndTime().toString(),
                entry.getBreakMinutes()
        );
    }

    public void saveEntry(TimeEntryDTO timeEntryDTO) {
        User user = userService.getCurrentUser();
        LocalDate date = LocalDate.parse(timeEntryDTO.getDate());

        TimeEntry entry = repository.findByUserAndDate(user, date);
        if (entry == null) {
            entry = new TimeEntry();
            entry.setUser(user);
            entry.setDate(date);
        }

        entry.setStartTime(LocalTime.parse(timeEntryDTO.getStartTime()));
        entry.setEndTime(LocalTime.parse(timeEntryDTO.getEndTime()));
        entry.setBreakMinutes(timeEntryDTO.getBreakMinutes());

        repository.save(entry);

        deleteAbsence(user, timeEntryDTO);
    }

    public void deleteEntry(TimeEntryDTO timeEntryDTO) {
        User user = userService.getCurrentUser();
        LocalDate date = LocalDate.parse(timeEntryDTO.getDate());

        TimeEntry entry = repository.findByUserAndDate(user, date);
        if (entry != null) {
            repository.delete(entry);
        }
    }

    public int calculateWorkedMinutes(TimeEntry entry) {
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

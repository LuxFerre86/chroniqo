package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.LongStream;

@RequiredArgsConstructor
@Service
public class AbsenceService {

    private final AbsenceRepository repository;
    private final UserRepository userRepository;
    private final TimeEntryRepository timeEntryRepository;

    public void saveAbsence(AbsenceRequest absenceRequest) {
        long days = absenceRequest.startDate().until(absenceRequest.endDate(), ChronoUnit.DAYS);

        User user = userRepository.findById("1").orElseThrow();

        if (absenceRequest.endDate().isBefore(absenceRequest.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }

        List<Absence> existingAbsence = repository.findByUserAndDateLessThanEqualAndDateGreaterThanEqual(user, absenceRequest.endDate(), absenceRequest.startDate());
        if (!existingAbsence.isEmpty()) {
            repository.deleteAll(existingAbsence);
        }

        LongStream.rangeClosed(0, days).forEach(dayIndex -> {
            LocalDate date = absenceRequest.startDate().plusDays(dayIndex);
            Absence absence = new Absence();
            absence.setUser(user);
            absence.setDate(date);
            absence.setType(absenceRequest.absenceType());

            repository.save(absence);
        });

        deleteTimeEntries(user, absenceRequest);
    }

    public void deleteAbsence(AbsenceRequest absenceRequest) {
        long days = absenceRequest.startDate().until(absenceRequest.endDate(), ChronoUnit.DAYS);

        User user = userRepository.findById("1").orElseThrow();

        if (absenceRequest.endDate().isBefore(absenceRequest.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date");
        }

        Absence existingAbsence = repository.findByUserAndDate(user, absenceRequest.startDate());
        if (null != existingAbsence) {
            repository.delete(existingAbsence);
        }
    }

    void deleteTimeEntries(User user, AbsenceRequest absenceRequest) {
        List<TimeEntry> timeEntries = timeEntryRepository.findByUserAndDateBetween(user, absenceRequest.startDate(), absenceRequest.endDate());
        if (!timeEntries.isEmpty()) {
            timeEntryRepository.deleteAll(timeEntries);
        }
    }

    public List<Absence> getAbsences(User user, LocalDate start, LocalDate end) {
        return repository
                .findByUserAndDateLessThanEqualAndDateGreaterThanEqual(
                        user, end, start
                );
    }

    public Absence getAbsence(LocalDate date) {
        User user = userRepository.findById("1").orElseThrow();
        return repository.findByUserAndDate(user, date);
    }
}

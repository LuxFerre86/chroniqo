package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.IsWeekendQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing user absences (vacation, sick leave, public holidays).
 *
 * <p>Absence ranges are stored one row per working day; weekend days within a
 * requested range are automatically skipped. Saving a new absence for a date
 * range that already contains absences replaces those existing records.
 *
 * @author Luxferre86
 * @since 14.02.2026
 */
@RequiredArgsConstructor
@Slf4j
@Service
public class AbsenceService {

    private final AbsenceRepository repository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserService userService;

    /**
     * Saves absences for every working day in the requested date range,
     * replacing any existing absences within that range.
     *
     * @param absenceRequest the range and type of absence to record
     */
    @Transactional
    public void saveAbsence(AbsenceRequest absenceRequest) {
        User user = userService.getCurrentUser();

        List<LocalDate> bookedDates = timeEntryRepository
                .findByUserAndDateBetween(user, absenceRequest.startDate(), absenceRequest.endDate()).stream()
                .map(TimeEntry::getDate)
                .filter(date -> !date.query(new IsWeekendQuery()))
                .distinct()
                .sorted()
                .toList();
        if (!bookedDates.isEmpty()) {
            throw TimeEntryValidationException.inconsistentData(String.format(
                    "Cannot record an absence: working time is already booked on %s. Remove the time entries first.",
                    bookedDates.stream().map(LocalDate::toString).collect(Collectors.joining(", "))));
        }

        List<Absence> existingAbsence = repository.findByUserAndDateBetween(user, absenceRequest.startDate(), absenceRequest.endDate());
        if (!existingAbsence.isEmpty()) {
            repository.deleteAll(existingAbsence);
            // Hibernate flushes inserts before deletes; without this the unique key (user_id, date) is violated.
            repository.flush();
        }

        repository.saveAll(absenceRequest.startDate().datesUntil(absenceRequest.endDate().plusDays(1)).filter(date -> !date.query(new IsWeekendQuery())).map(date -> {
            Absence absence = new Absence();
            absence.setUser(user);
            absence.setDate(date);
            absence.setType(absenceRequest.absenceType());
            return absence;
        }).toList());

        log.info("Absence saved between {} and {}", absenceRequest.startDate(), absenceRequest.endDate());
    }

    /**
     * Deletes the absence record for the given date, if one exists.
     *
     * @param date the date whose absence entry should be removed
     */
    @Transactional
    public void deleteAbsence(LocalDate date) {
        User user = userService.getCurrentUser();
        Absence absence = repository.findByUserAndDate(user, date);
        if (absence != null) {
            repository.delete(absence);
            log.info("Absence deleted for date: {}", date);
        }
    }

    /**
     * Deletes the absence records for the current user within the given date
     * range (inclusive), optionally restricted to a single absence type.
     *
     * @param startDate first day of the range (inclusive)
     * @param endDate   last day of the range (inclusive)
     * @param type      only absences of this type are removed; {@code null} removes all types
     */
    @Transactional
    public void deleteAbsences(LocalDate startDate, LocalDate endDate, AbsenceType type) {
        User user = userService.getCurrentUser();
        List<Absence> absenceList = repository.findByUserAndDateBetween(user, startDate, endDate).stream()
                .filter(absence -> type == null || absence.getType() == type)
                .toList();
        if (!absenceList.isEmpty()) {
            repository.deleteAll(absenceList);
            log.info("Absences deleted between {} and {}", startDate, endDate);
        }
    }

    /**
     * Returns all absences for the current user within the given date range.
     *
     * @param start first day of the range (inclusive)
     * @param end   last day of the range (inclusive)
     * @return list of {@link com.luxferre.chroniqo.model.Absence} records
     */
    public List<Absence> getAbsences(LocalDate start, LocalDate end) {
        User user = userService.getCurrentUser();
        return repository.findByUserAndDateBetween(user, start, end);
    }

    /**
     * Returns the absence record for the current user on the given date,
     * or {@code null} if no absence exists.
     *
     * @param date the date to query
     * @return the matching {@link com.luxferre.chroniqo.model.Absence}, or
     * {@code null}
     */
    public Absence getAbsence(LocalDate date) {
        User user = userService.getCurrentUser();
        return repository.findByUserAndDate(user, date);
    }
}
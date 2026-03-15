package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Facade service that coordinates time entries and absences.
 *
 * <p>Saving a time entry automatically removes any absence on the same date,
 * and saving an absence removes any time entries within the absence date range.
 * This mutual-exclusion invariant is enforced here rather than in the
 * individual services so that the UI only needs a single call per user action.
 *
 * @author Luxferre86
 * @since 01.03.2026
 */
@RequiredArgsConstructor
@Service
public class TimeTrackingService {

    private final TimeEntryService timeEntryService;
    private final AbsenceService absenceService;


    /**
     * Returns the time entry for the current user on the given date,
     * or {@code null} if none exists.
     *
     * @param date the date to query
     * @return the matching {@link TimeEntryDTO}, or
     * {@code null}
     */
    public TimeEntryDTO getTimeEntry(LocalDate date) {
        return timeEntryService.getTimeEntry(date);
    }

    /**
     * Returns all time entries for the current user within the given date range.
     *
     * @param startDate first day of the range (inclusive)
     * @param endDate   last day of the range (inclusive)
     * @return list of {@link TimeEntryDTO} records
     */
    public List<TimeEntryDTO> getTimeEntries(LocalDate startDate, LocalDate endDate) {
        return timeEntryService.getTimeEntries(startDate, endDate);
    }

    /**
     * Returns the absence record for the current user on the given date,
     * or {@code null} if none exists.
     *
     * @param date the date to query
     * @return the matching {@link com.luxferre.chroniqo.model.Absence}, or
     * {@code null}
     */
    public Absence getAbsence(LocalDate date) {
        return absenceService.getAbsence(date);
    }

    /**
     * Returns all absences for the current user within the given date range.
     *
     * @param start first day of the range (inclusive)
     * @param end   last day of the range (inclusive)
     * @return list of {@link com.luxferre.chroniqo.model.Absence} records
     */
    public List<Absence> getAbsences(LocalDate start, LocalDate end) {
        return absenceService.getAbsences(start, end);
    }

    /**
     * Saves or updates a time entry and removes any absence on the same date.
     *
     * @param timeEntryDTO the time entry data to persist
     * @throws TimeEntryValidationException if validation fails
     */
    @Transactional
    public void saveEntry(TimeEntryDTO timeEntryDTO) {
        timeEntryService.saveEntry(timeEntryDTO);
        absenceService.deleteAbsence(timeEntryDTO.getDate());
    }

    /**
     * Deletes the time entry for the date carried in {@code timeEntryDTO}.
     *
     * @param timeEntryDTO identifies the entry to delete via its date
     */
    @Transactional
    public void deleteEntry(TimeEntryDTO timeEntryDTO) {
        timeEntryService.deleteEntry(timeEntryDTO);
    }

    /**
     * Saves absence records for the requested date range and removes any time
     * entries that overlap with that range.
     *
     * @param absenceRequest the absence range and type to record
     */
    @Transactional
    public void saveAbsence(AbsenceRequest absenceRequest) {
        absenceService.saveAbsence(absenceRequest);
        timeEntryService.deleteEntries(absenceRequest.startDate(), absenceRequest.endDate());
    }

    /**
     * Deletes all absence records within the given date range (inclusive).
     *
     * @param startDate first day of the range (inclusive)
     * @param endDate   last day of the range (inclusive)
     */
    @Transactional
    public void deleteAbsences(LocalDate startDate, LocalDate endDate) {
        absenceService.deleteAbsences(startDate, endDate);
    }
}
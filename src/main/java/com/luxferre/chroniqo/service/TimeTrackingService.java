package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@Service
public class TimeTrackingService {

    private final TimeEntryService timeEntryService;
    private final AbsenceService absenceService;


    public TimeEntryDTO getTimeEntry(LocalDate date) {
        return timeEntryService.getTimeEntry(date);
    }

    public List<TimeEntryDTO> getTimeEntries(LocalDate startDate, LocalDate endDate) {
        return timeEntryService.getTimeEntries(startDate, endDate);
    }

    public Absence getAbsence(LocalDate date) {
        return absenceService.getAbsence(date);
    }

    public List<Absence> getAbsences(LocalDate start, LocalDate end) {
        return absenceService.getAbsences(start, end);
    }

    @Transactional
    public void saveEntry(TimeEntryDTO timeEntryDTO) {
        timeEntryService.saveEntry(timeEntryDTO);
        absenceService.deleteAbsence(timeEntryDTO.getDate());
    }

    @Transactional
    public void deleteEntry(TimeEntryDTO timeEntryDTO) {
        timeEntryService.deleteEntry(timeEntryDTO);
    }

    @Transactional
    public void saveAbsence(AbsenceRequest absenceRequest) {
        absenceService.saveAbsence(absenceRequest);
        timeEntryService.deleteEntries(absenceRequest.startDate(), absenceRequest.endDate());
    }

    @Transactional
    public void deleteAbsences(LocalDate startDate, LocalDate endDate) {
        absenceService.deleteAbsences(startDate, endDate);
    }
}

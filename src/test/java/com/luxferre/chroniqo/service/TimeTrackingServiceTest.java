package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TimeTrackingServiceTest {

    @InjectMocks
    private TimeTrackingService timeTrackingService;
    @Mock
    private AbsenceService absenceService;
    @Mock
    private TimeEntryService timeEntryService;

    @Test
    public void getTimeEntry_shouldForwardToTimeEntryService() {
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
        when(timeEntryService.getTimeEntry(any())).thenReturn(timeEntryDTO);

        TimeEntryDTO result = timeTrackingService.getTimeEntry(LocalDate.now());

        assertThat(result).isEqualTo(timeEntryDTO);
        verify(timeEntryService).getTimeEntry(LocalDate.now());
    }

    @Test
    public void getTimeEntries_shouldForwardToTimeEntryService() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1L);
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
        when(timeEntryService.getTimeEntries(any(), any())).thenReturn(Collections.singletonList(timeEntryDTO));

        List<TimeEntryDTO> result = timeTrackingService.getTimeEntries(today, tomorrow);

        assertThat(result).isNotNull().hasSize(1).containsExactly(timeEntryDTO);
        verify(timeEntryService).getTimeEntries(today, tomorrow);
    }

    @Test
    public void getAbsence_shouldForwardToAbsenceService() {
        Absence absence = new Absence();
        when(absenceService.getAbsence(any())).thenReturn(absence);

        Absence result = timeTrackingService.getAbsence(LocalDate.now());

        assertThat(result).isEqualTo(absence);
        verify(absenceService).getAbsence(LocalDate.now());
    }

    @Test
    public void getAbsences_shouldForwardToTimeEntryService() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1L);
        Absence absence = new Absence();
        when(absenceService.getAbsences(any(), any())).thenReturn(Collections.singletonList(absence));

        List<Absence> result = timeTrackingService.getAbsences(today, tomorrow);

        assertThat(result).isNotNull().hasSize(1).containsExactly(absence);
        verify(absenceService).getAbsences(today, tomorrow);
    }

    @Test
    public void saveEntry_shouldSaveEntryAndDeleteAbsence() {
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
        timeEntryDTO.setDate(LocalDate.now());

        timeTrackingService.saveEntry(timeEntryDTO);

        verify(timeEntryService).saveEntry(timeEntryDTO);
        verify(absenceService).deleteAbsence(LocalDate.now());
    }

    @Test
    public void saveAbsence_shouldSaveAbsenceAndDeleteTimeEntry() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1L);
        AbsenceRequest absenceRequest = new AbsenceRequest(today, tomorrow, AbsenceType.VACATION);

        timeTrackingService.saveAbsence(absenceRequest);

        verify(absenceService).saveAbsence(absenceRequest);
        verify(timeEntryService).deleteEntries(today, tomorrow);
    }

    @Test
    public void deleteEntry_shouldDeleteTimeEntry() {
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO();

        timeTrackingService.deleteEntry(timeEntryDTO);

        verify(timeEntryService).deleteEntry(timeEntryDTO);
    }

    @Test
    public void deleteAbsences_shouldDeleteAbsences() {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1L);

        timeTrackingService.deleteAbsences(today, tomorrow);

        verify(absenceService).deleteAbsences(today, tomorrow);
    }

    @Test
    public void getTimeEntries_emptyRange_returnsEmptyList() {
        LocalDate today = LocalDate.now();
        when(timeEntryService.getTimeEntries(today, today)).thenReturn(Collections.emptyList());

        List<TimeEntryDTO> result = timeTrackingService.getTimeEntries(today, today);

        assertThat(result).isEmpty();
    }

    @Test
    public void getAbsences_emptyRange_returnsEmptyList() {
        LocalDate today = LocalDate.now();
        when(absenceService.getAbsences(today, today)).thenReturn(Collections.emptyList());

        List<Absence> result = timeTrackingService.getAbsences(today, today);

        assertThat(result).isEmpty();
    }

    @Test
    public void saveEntry_deletesAbsenceForSameDateAsEntry() {
        LocalDate specificDate = LocalDate.of(2025, 6, 15);
        TimeEntryDTO timeEntryDTO = new TimeEntryDTO();
        timeEntryDTO.setDate(specificDate);

        timeTrackingService.saveEntry(timeEntryDTO);

        verify(absenceService).deleteAbsence(specificDate);
        verify(timeEntryService).saveEntry(timeEntryDTO);
    }

    @Test
    public void saveAbsence_deletesTimeEntriesForFullRange() {
        LocalDate start = LocalDate.of(2025, 6, 1);
        LocalDate end = LocalDate.of(2025, 6, 7);
        AbsenceRequest absenceRequest = new AbsenceRequest(start, end, AbsenceType.SICK);

        timeTrackingService.saveAbsence(absenceRequest);

        verify(timeEntryService).deleteEntries(start, end);
        verify(absenceService).saveAbsence(absenceRequest);
    }

}
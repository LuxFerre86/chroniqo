package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.service.TimeEntryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser("test@gmail.com")
@DisplayName("AbsenceController")
class AbsenceControllerTest extends AbstractApiControllerTest {

    private static final LocalDate START = LocalDate.of(2026, 7, 6);
    private static final LocalDate END = LocalDate.of(2026, 7, 10);

    @Test
    void list_returnsAbsencesOfRange() throws Exception {
        Absence vacation = new Absence();
        vacation.setDate(START);
        vacation.setType(AbsenceType.VACATION);
        Absence sick = new Absence();
        sick.setDate(END);
        sick.setType(AbsenceType.SICK);
        when(timeTrackingService.getAbsences(START, END)).thenReturn(List.of(vacation, sick));

        mockMvc.perform(get("/api/absences").param("from", "2026-07-06").param("to", "2026-07-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-07-06"))
                .andExpect(jsonPath("$[0].type").value("VACATION"))
                .andExpect(jsonPath("$[1].type").value("SICK"));
    }

    @Test
    void list_rangeTooLarge_returns422() throws Exception {
        mockMvc.perform(get("/api/absences").param("from", "2024-01-01").param("to", "2025-12-31"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALUE_TOO_LARGE"));
    }

    @Test
    void save_returns204AndDelegatesToService() throws Exception {
        mockMvc.perform(put("/api/absences").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-07-06\",\"endDate\":\"2026-07-10\",\"absenceType\":\"VACATION\"}"))
                .andExpect(status().isNoContent());

        verify(timeTrackingService).saveAbsence(new AbsenceRequest(START, END, AbsenceType.VACATION));
    }

    @Test
    void save_missingType_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(put("/api/absences").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-07-06\",\"endDate\":\"2026-07-10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.absenceType").exists());

        verify(timeTrackingService, never()).saveAbsence(any());
    }

    @Test
    void save_unknownType_returns400() throws Exception {
        mockMvc.perform(put("/api/absences").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-07-06\",\"endDate\":\"2026-07-10\",\"absenceType\":\"HOLIDAY\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void save_endBeforeStart_returns422() throws Exception {
        mockMvc.perform(put("/api/absences").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-07-10\",\"endDate\":\"2026-07-06\",\"absenceType\":\"SICK\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RANGE"));

        verify(timeTrackingService, never()).saveAbsence(any());
    }

    @Test
    void save_serviceRejects_returns422() throws Exception {
        doThrow(TimeEntryValidationException.inconsistentData("Absence overlaps"))
                .when(timeTrackingService).saveAbsence(any());

        mockMvc.perform(put("/api/absences").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDate\":\"2026-07-06\",\"endDate\":\"2026-07-10\",\"absenceType\":\"SICK\"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INCONSISTENT_DATA"));
    }

    @Test
    void delete_returns204AndDelegatesToService() throws Exception {
        mockMvc.perform(delete("/api/absences").with(csrf())
                        .param("from", "2026-07-06").param("to", "2026-07-10"))
                .andExpect(status().isNoContent());

        verify(timeTrackingService).deleteAbsences(START, END, null);
    }

    @Test
    void delete_withType_passesTypeToService() throws Exception {
        mockMvc.perform(delete("/api/absences").with(csrf())
                        .param("from", "2026-07-06").param("to", "2026-07-10").param("type", "VACATION"))
                .andExpect(status().isNoContent());

        verify(timeTrackingService).deleteAbsences(START, END, AbsenceType.VACATION);
    }

    @Test
    void delete_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(delete("/api/absences").param("from", "2026-07-06").param("to", "2026-07-10"))
                .andExpect(status().isForbidden());

        verify(timeTrackingService, never()).deleteAbsences(any(), any(), any());
    }
}

package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.dto.AbsenceTypeDTO;
import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser("test@gmail.com")
@DisplayName("SummaryController")
class SummaryControllerTest extends AbstractApiControllerTest {

    private static final LocalDate MONDAY = LocalDate.of(2026, 3, 2);
    private static final LocalDate TUESDAY = LocalDate.of(2026, 3, 3);

    @Test
    void summary_returnsDaySummariesWithFrontendFieldNames() throws Exception {
        when(summaryService.getSummary(MONDAY, TUESDAY)).thenReturn(List.of(
                new DaySummaryDTO(MONDAY, true, 450, 468, -18, null, null),
                new DaySummaryDTO(TUESDAY, false, 0, 0, 0, AbsenceTypeDTO.HOLIDAY, "Some Holiday")));

        mockMvc.perform(get("/api/summary").param("from", "2026-03-02").param("to", "2026-03-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].date").value("2026-03-02"))
                .andExpect(jsonPath("$[0].workday").value(true))
                .andExpect(jsonPath("$[0].workedMinutes").value(450))
                .andExpect(jsonPath("$[0].targetMinutes").value(468))
                .andExpect(jsonPath("$[0].balanceMinutes").value(-18))
                .andExpect(jsonPath("$[0].absenceType").doesNotExist())
                .andExpect(jsonPath("$[1].workday").value(false))
                .andExpect(jsonPath("$[1].absenceType").value("HOLIDAY"))
                .andExpect(jsonPath("$[1].absenceLabel").value("Some Holiday"));
    }

    @Test
    void summary_invalidRange_returns422() throws Exception {
        mockMvc.perform(get("/api/summary").param("from", "2026-03-03").param("to", "2026-03-02"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RANGE"));

        verify(summaryService, never()).getSummary(any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void dashboard_returnsTodayWeekBalanceAndProgress() throws Exception {
        when(summaryService.getToday()).thenReturn(new DaySummaryDTO(MONDAY, true, 100, 468, -368, null, null));
        List<DaySummaryDTO> week = List.of(
                new DaySummaryDTO(MONDAY, true, 100, 468, -368, null, null),
                new DaySummaryDTO(TUESDAY, true, 0, 468, -468, null, null));
        when(summaryService.getCurrentWeek()).thenReturn(week);
        when(summaryService.getCurrentBalance()).thenReturn(-120);
        when(summaryService.getWeeklyProgress(week)).thenReturn(new WeeklyProgressDTO(100, 2340, 4, true));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.today.workday").value(true))
                .andExpect(jsonPath("$.today.workedMinutes").value(100))
                .andExpect(jsonPath("$.week.length()").value(2))
                .andExpect(jsonPath("$.week[1].date").value("2026-03-03"))
                .andExpect(jsonPath("$.currentBalanceMinutes").value(-120))
                .andExpect(jsonPath("$.weeklyProgress.workedMinutes").value(100))
                .andExpect(jsonPath("$.weeklyProgress.targetMinutes").value(2340))
                .andExpect(jsonPath("$.weeklyProgress.percentage").value(4))
                .andExpect(jsonPath("$.weeklyProgress.hasTarget").value(true));
    }
}

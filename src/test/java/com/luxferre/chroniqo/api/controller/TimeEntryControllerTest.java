package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.service.TimeEntryValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser("test@gmail.com")
@DisplayName("TimeEntryController")
class TimeEntryControllerTest extends AbstractApiControllerTest {

    private static final String VALID_BODY = """
            {"date":"2026-03-02","startTime":"09:00","endTime":"17:00","breakMinutes":30,"notes":"  worked  "}
            """;

    @Test
    void list_returnsEntriesOfRange() throws Exception {
        TimeEntryDTO entry = new TimeEntryDTO("entry-1", LocalDate.of(2026, 3, 2),
                LocalTime.of(9, 0), LocalTime.of(17, 0), 30, "notes");
        when(timeTrackingService.getTimeEntries(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn(List.of(entry));

        mockMvc.perform(get("/api/time-entries").param("from", "2026-03-01").param("to", "2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("entry-1"))
                .andExpect(jsonPath("$[0].date").value("2026-03-02"))
                .andExpect(jsonPath("$[0].startTime").value("09:00:00"))
                .andExpect(jsonPath("$[0].endTime").value("17:00:00"))
                .andExpect(jsonPath("$[0].breakMinutes").value(30))
                .andExpect(jsonPath("$[0].notes").value("notes"));
    }

    @Test
    void list_missingParameter_returns400() throws Exception {
        mockMvc.perform(get("/api/time-entries").param("from", "2026-03-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_malformedDate_returns400() throws Exception {
        mockMvc.perform(get("/api/time-entries").param("from", "01.03.2026").param("to", "2026-03-31"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void list_fullLeapYear_isAllowed() throws Exception {
        mockMvc.perform(get("/api/time-entries").param("from", "2024-01-01").param("to", "2024-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void list_rangeLongerThan366Days_returns422() throws Exception {
        mockMvc.perform(get("/api/time-entries").param("from", "2024-01-01").param("to", "2025-01-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("VALUE_TOO_LARGE"));

        verify(timeTrackingService, never()).getTimeEntries(any(), any());
    }

    @Test
    void list_fromAfterTo_returns422() throws Exception {
        mockMvc.perform(get("/api/time-entries").param("from", "2026-03-10").param("to", "2026-03-01"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("INVALID_RANGE"));
    }

    @Test
    void create_returns201AndMapsRequestToDto() throws Exception {
        mockMvc.perform(post("/api/time-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        ArgumentCaptor<TimeEntryDTO> captor = ArgumentCaptor.forClass(TimeEntryDTO.class);
        verify(timeTrackingService).saveEntry(captor.capture());
        TimeEntryDTO dto = captor.getValue();
        assertThat(dto.getId()).isNull();
        assertThat(dto.getDate()).isEqualTo(LocalDate.of(2026, 3, 2));
        assertThat(dto.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(dto.getEndTime()).isEqualTo(LocalTime.of(17, 0));
        assertThat(dto.getBreakMinutes()).isEqualTo(30);
        assertThat(dto.getNotes()).isEqualTo("worked");
    }

    @Test
    void create_blankNotes_areStoredAsNull() throws Exception {
        String body = """
                {"date":"2026-03-02","startTime":"09:00","endTime":"17:00","breakMinutes":0,"notes":"   "}
                """;

        mockMvc.perform(post("/api/time-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        ArgumentCaptor<TimeEntryDTO> captor = ArgumentCaptor.forClass(TimeEntryDTO.class);
        verify(timeTrackingService).saveEntry(captor.capture());
        assertThat(captor.getValue().getNotes()).isNull();
    }

    @Test
    void create_notesTooLong_returns400WithFieldErrors() throws Exception {
        String body = "{\"date\":\"2026-03-02\",\"startTime\":\"09:00\",\"endTime\":\"17:00\",\"breakMinutes\":0,\"notes\":\""
                + "a".repeat(501) + "\"}";

        mockMvc.perform(post("/api/time-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.notes").exists());

        verify(timeTrackingService, never()).saveEntry(any());
    }

    @Test
    void create_businessRuleViolation_returns422WithCodeAndField() throws Exception {
        doThrow(TimeEntryValidationException.futureDate(LocalDate.of(2999, 1, 1)))
                .when(timeTrackingService).saveEntry(any());

        mockMvc.perform(post("/api/time-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("FUTURE_DATE"))
                .andExpect(jsonPath("$.field").value("date"))
                .andExpect(jsonPath("$.detail").value("Date cannot be in the future"));
    }

    @Test
    void create_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/time-entries").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withoutCsrfToken_returns403() throws Exception {
        mockMvc.perform(post("/api/time-entries")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isForbidden());

        verify(timeTrackingService, never()).saveEntry(any());
    }

    @Test
    void update_returns204AndKeepsIdFromPath() throws Exception {
        mockMvc.perform(put("/api/time-entries/entry-42").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNoContent());

        ArgumentCaptor<TimeEntryDTO> captor = ArgumentCaptor.forClass(TimeEntryDTO.class);
        verify(timeTrackingService).saveEntry(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("entry-42");
    }

    @Test
    void update_businessRuleViolation_returns422() throws Exception {
        doThrow(TimeEntryValidationException.negativeValue("breakMinutes", -1))
                .when(timeTrackingService).saveEntry(any());

        mockMvc.perform(put("/api/time-entries/entry-42").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("NEGATIVE_VALUE"))
                .andExpect(jsonPath("$.field").value("breakMinutes"));
    }

    @Test
    void delete_returns204AndPassesId() throws Exception {
        mockMvc.perform(delete("/api/time-entries/entry-42").with(csrf()))
                .andExpect(status().isNoContent());

        ArgumentCaptor<TimeEntryDTO> captor = ArgumentCaptor.forClass(TimeEntryDTO.class);
        verify(timeTrackingService).deleteEntry(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo("entry-42");
    }

    @Test
    void unexpectedFailure_returns500WithoutInternalDetails() throws Exception {
        doThrow(new IllegalStateException("secret internal detail"))
                .when(timeTrackingService).deleteEntry(any());

        mockMvc.perform(delete("/api/time-entries/entry-42").with(csrf()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("secret internal detail"))));
    }
}

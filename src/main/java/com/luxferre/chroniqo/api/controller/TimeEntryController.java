package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.TimeEntryRequest;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.service.TimeTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * CRUD endpoints for the authenticated user's time entries.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeTrackingService timeTrackingService;

    /**
     * Lists time entries within an inclusive date range, ordered by date and start time.
     *
     * @param from first day (inclusive)
     * @param to   last day (inclusive)
     * @return the matching entries
     */
    @GetMapping
    public List<TimeEntryDTO> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DateRanges.validate(from, to);
        return timeTrackingService.getTimeEntries(from, to);
    }

    /**
     * Creates a new time entry.
     *
     * @param request the entry data
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody TimeEntryRequest request) {
        timeTrackingService.saveEntry(toDto(null, request));
    }

    /**
     * Updates an existing time entry.
     *
     * @param id      the entry id
     * @param request the new entry data
     */
    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody TimeEntryRequest request) {
        timeTrackingService.saveEntry(toDto(id, request));
    }

    /**
     * Deletes a time entry. Deleting an unknown id is a no-op.
     *
     * @param id the entry id
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setId(id);
        timeTrackingService.deleteEntry(dto);
    }

    private TimeEntryDTO toDto(String id, TimeEntryRequest request) {
        String notes = request.notes() != null && !request.notes().isBlank() ? request.notes().trim() : null;
        return new TimeEntryDTO(id, request.date(), request.startTime(), request.endTime(), request.breakMinutes(), notes);
    }
}

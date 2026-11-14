package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.AbsenceRangeRequest;
import com.luxferre.chroniqo.api.dto.AbsenceResponse;
import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.service.TimeTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints for vacation and sick-leave absences of the authenticated user.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private final TimeTrackingService timeTrackingService;

    /**
     * Lists absences within an inclusive date range.
     *
     * @param from first day (inclusive)
     * @param to   last day (inclusive)
     * @return the recorded absences
     */
    @GetMapping
    public List<AbsenceResponse> list(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DateRanges.validate(from, to);
        return timeTrackingService.getAbsences(from, to).stream().map(AbsenceResponse::of).toList();
    }

    /**
     * Records an absence for every working day (Mon-Fri) in the range,
     * replacing any existing absences in that range.
     *
     * @param request the range and absence type
     */
    @PutMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void save(@Valid @RequestBody AbsenceRangeRequest request) {
        DateRanges.validate(request.startDate(), request.endDate());
        timeTrackingService.saveAbsence(new AbsenceRequest(request.startDate(), request.endDate(), request.absenceType()));
    }

    /**
     * Removes all absences within an inclusive date range.
     *
     * @param from first day (inclusive)
     * @param to   last day (inclusive)
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DateRanges.validate(from, to);
        timeTrackingService.deleteAbsences(from, to);
    }
}

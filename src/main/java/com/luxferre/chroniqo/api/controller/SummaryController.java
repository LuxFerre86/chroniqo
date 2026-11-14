package com.luxferre.chroniqo.api.controller;

import com.luxferre.chroniqo.api.dto.DashboardResponse;
import com.luxferre.chroniqo.api.dto.DaySummaryResponse;
import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-only endpoints delivering aggregated time-tracking figures.
 *
 * @author Luxferre86
 * @since 14.11.2026
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    /**
     * Returns one summary per day within an inclusive date range.
     *
     * @param from first day (inclusive)
     * @param to   last day (inclusive)
     * @return the day summaries ordered by date
     */
    @GetMapping("/summary")
    public List<DaySummaryResponse> summary(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        DateRanges.validate(from, to);
        return summaryService.getSummary(from, to).stream().map(DaySummaryResponse::of).toList();
    }

    /**
     * Returns everything the dashboard shows: today, the current week, the
     * running balance and the weekly progress.
     *
     * @return the dashboard data
     */
    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        List<DaySummaryDTO> week = summaryService.getCurrentWeek();
        return new DashboardResponse(
                DaySummaryResponse.of(summaryService.getToday()),
                week.stream().map(DaySummaryResponse::of).toList(),
                summaryService.getCurrentBalance(),
                summaryService.getWeeklyProgress(week));
    }
}

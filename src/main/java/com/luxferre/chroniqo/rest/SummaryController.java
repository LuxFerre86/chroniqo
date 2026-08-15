package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.service.SummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * REST controller for aggregated summary data.
 */
@RestController
@RequestMapping("/api/summary")
@RequiredArgsConstructor
public class SummaryController {

    private final SummaryService summaryService;

    @GetMapping("/month")
    public ResponseEntity<List<DaySummaryDTO>> getMonthSummary(@RequestParam int year, @RequestParam int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<DaySummaryDTO> summaries = summaryService.getSummary(ym.atDay(1), ym.atEndOfMonth());
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/weekly-progress")
    public ResponseEntity<WeeklyProgressDTO> getWeeklyProgress() {
        return ResponseEntity.ok(summaryService.getWeeklyProgress());
    }

    @GetMapping("/today")
    public ResponseEntity<DaySummaryDTO> getToday() {
        DaySummaryDTO today = summaryService.getToday();
        if (today == null) {
            today = new DaySummaryDTO(LocalDate.now(), true, 0, 0, 0, null, null);
        }
        return ResponseEntity.ok(today);
    }

    @GetMapping("/current-week")
    public ResponseEntity<List<DaySummaryDTO>> getCurrentWeek() {
        return ResponseEntity.ok(summaryService.getCurrentWeek());
    }
}

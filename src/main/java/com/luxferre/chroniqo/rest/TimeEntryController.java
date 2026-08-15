package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.service.TimeEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * REST controller for time entry CRUD operations.
 */
@RestController
@RequestMapping("/api/time-entries")
@RequiredArgsConstructor
public class TimeEntryController {

    private final TimeEntryService timeEntryService;

    @GetMapping
    public ResponseEntity<List<TimeEntryDTO>> getEntries(@RequestParam int year, @RequestParam int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<TimeEntryDTO> entries = timeEntryService.getTimeEntries(ym.atDay(1), ym.atEndOfMonth());
        return ResponseEntity.ok(entries);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createEntry(@RequestBody TimeEntryDTO dto) {
        timeEntryService.saveEntry(dto);
        return ResponseEntity.ok(Map.of("message", "Entry saved."));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, String>> updateEntry(@PathVariable String id, @RequestBody TimeEntryDTO dto) {
        dto.setId(id);
        timeEntryService.saveEntry(dto);
        return ResponseEntity.ok(Map.of("message", "Entry updated."));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteEntry(@PathVariable String id) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setId(id);
        timeEntryService.deleteEntry(dto);
        return ResponseEntity.ok(Map.of("message", "Entry deleted."));
    }

    @GetMapping("/day")
    public ResponseEntity<List<TimeEntryDTO>> getEntriesForDay(@RequestParam String date) {
        LocalDate localDate = LocalDate.parse(date);
        return ResponseEntity.ok(timeEntryService.getTimeEntries(localDate));
    }
}

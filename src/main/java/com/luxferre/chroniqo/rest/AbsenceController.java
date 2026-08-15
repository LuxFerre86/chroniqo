package com.luxferre.chroniqo.rest;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.service.AbsenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

/**
 * REST controller for absence management.
 */
@RestController
@RequestMapping("/api/absences")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService absenceService;

    @GetMapping
    public ResponseEntity<List<AbsenceResponse>> getAbsences(@RequestParam int year, @RequestParam int month) {
        YearMonth ym = YearMonth.of(year, month);
        List<AbsenceResponse> absences = absenceService.getAbsences(ym.atDay(1), ym.atEndOfMonth())
                .stream()
                .map(AbsenceResponse::of)
                .toList();
        return ResponseEntity.ok(absences);
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> saveAbsence(@RequestBody AbsenceRequest request) {
        absenceService.saveAbsence(request);
        return ResponseEntity.ok(Map.of("message", "Absence saved."));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, String>> deleteAbsence(@RequestParam String date) {
        absenceService.deleteAbsence(LocalDate.parse(date));
        return ResponseEntity.ok(Map.of("message", "Absence deleted."));
    }

    public record AbsenceResponse(String id, String date, String type) {
        static AbsenceResponse of(Absence a) {
            return new AbsenceResponse(a.getId(), a.getDate().toString(), a.getType().name());
        }
    }
}

package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.user.UserService;
import com.luxferre.chroniqo.util.IsWeekendQuery;
import com.vaadin.flow.component.UI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TimeTrackingService timeTrackingService;
    private final UserService userService;


    public DaySummaryDTO getToday() {
        LocalDate today = LocalDate.now();
        return getSummary(today, today).stream().findFirst().orElse(null);
    }

    public List<DaySummaryDTO> getCurrentWeek() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(UI.getCurrent().getLocale());
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);
        return getSummary(weekStart, weekEnd);
    }

    public List<DaySummaryDTO> getSummary(int year) {
        LocalDate yearStart = Year.of(year).atMonth(1).atDay(1);
        LocalDate yearEnd = yearStart.with(TemporalAdjusters.lastDayOfYear());
        return getSummary(yearStart, yearEnd);
    }

    public List<DaySummaryDTO> getSummary(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();

        List<TimeEntryDTO> entries =
                timeTrackingService.getTimeEntries(startDate, endDate);

        List<Absence> absences =
                timeTrackingService.getAbsences(startDate, endDate);

        int dailyTargetMinutes = (user.getWeeklyTargetHours() * 60) / 5;

        return startDate.datesUntil(endDate.plusDays(1L)).map(date -> createDaySummaryDTO(date, entries, absences, dailyTargetMinutes)).toList();
    }

    public WeeklyProgressDTO getWeeklyProgress() {
        List<DaySummaryDTO> currentWeek = getCurrentWeek();
        int workedMinutes = currentWeek.stream().mapToInt(DaySummaryDTO::workedMinutes).sum();
        int targetMinutes = currentWeek.stream().mapToInt(DaySummaryDTO::targetMinutes).sum();
        int percentage = targetMinutes > 0 ? (workedMinutes * 100) / targetMinutes : 0;
        boolean hasTarget = targetMinutes > 0;
        return new WeeklyProgressDTO(workedMinutes, targetMinutes, percentage, hasTarget);
    }

    DaySummaryDTO createDaySummaryDTO(LocalDate date, List<TimeEntryDTO> entries, List<Absence> absences, int dailyTargetMinutes) {
        TimeEntryDTO entry = entries.stream()
                .filter(e -> e.getDate().equals(date))
                .findFirst()
                .orElse(null);

        Absence absence = absences.stream()
                .filter(a -> !date.isBefore(a.getDate()) && !date.isAfter(a.getDate()))
                .findFirst()
                .orElse(null);

        boolean isWeekend = date.query(new IsWeekendQuery());
        boolean isWorkday = !isWeekend && absence == null;

        int workedMinutes = (entry != null && absence == null)
                ? calculateWorkedMinutes(entry)
                : 0;

        int targetMinutes = isWorkday ? dailyTargetMinutes : 0;
        int balance = isWeekend ? workedMinutes : workedMinutes - targetMinutes;

        return new DaySummaryDTO(
                date,
                isWorkday,
                workedMinutes,
                targetMinutes,
                balance,
                absence != null ? absence.getType() : null
        );
    }

    int calculateWorkedMinutes(TimeEntryDTO entry) {
        // If entry not complete, calculate from start time to now
        if (entry.getEndTime() == null && entry.getStartTime() != null) {
            LocalTime now = LocalTime.now();
            int minutes = Math.toIntExact(Duration.between(entry.getStartTime(), now).toMinutes());
            if (entry.getBreakMinutes() != null) {
                minutes -= entry.getBreakMinutes();
            }
            return Math.max(0, minutes);
        }

        // Normal calculation for completed entries
        if (entry.getStartTime() == null) {
            return 0;
        }

        Duration duration = Duration.between(entry.getStartTime(), entry.getEndTime());
        int minutes = (int) duration.toMinutes();
        if (minutes < 0) {
            minutes += 1440;
        }
        if (entry.getBreakMinutes() != null) {
            minutes -= entry.getBreakMinutes();
        }
        return Math.max(0, minutes);
    }


}
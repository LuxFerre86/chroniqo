package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.util.IsWeekendQuery;
import com.vaadin.flow.component.UI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SummaryService {

    private final AbsenceService absenceService;
    private final TimeEntryService timeEntryService;
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

        List<TimeEntry> entries =
                timeEntryService.findByUserAndDateBetween(startDate, endDate);

        List<Absence> absences =
                absenceService.getAbsences(user, startDate, endDate);

        int dailyTargetMinutes = (user.getWeeklyTargetHours() * 60) / 5;

        return startDate.datesUntil(endDate.plusDays(1L)).map(date -> createDaySummaryDTO(date, entries, absences, dailyTargetMinutes)).toList();
    }

    private DaySummaryDTO createDaySummaryDTO(LocalDate date, List<TimeEntry> entries, List<Absence> absences, int dailyTargetMinutes) {
        TimeEntry entry = entries.stream()
                .filter(e -> e.getDate().equals(date))
                .findFirst()
                .orElse(null);

        Absence absence = absences.stream()
                .filter(a -> !date.isBefore(a.getDate()) && !date.isAfter(a.getDate()))
                .findFirst()
                .orElse(null);

        int workedMinutes = entry != null
                ? timeEntryService.calculateWorkedMinutes(entry)
                : 0;

        int balance = 0;
        AbsenceType dayType = null;
        int targetMinutes = 0;

        if (absence != null) {
            dayType = absence.getType();
            workedMinutes = 0;
        } else if (date.query(new IsWeekendQuery())) {
            balance = workedMinutes;
        } else {
            balance = workedMinutes - dailyTargetMinutes;
            targetMinutes = dailyTargetMinutes;
        }

        return new DaySummaryDTO(
                date,
                workedMinutes > 0 ? workedMinutes : null,
                targetMinutes > 0 ? targetMinutes : null,
                balance,
                dayType
        );
    }

    public WeeklyProgressDTO getWeeklyProgress() {
        List<DaySummaryDTO> currentWeek = getCurrentWeek();
        int workedMinutes = currentWeek.stream().map(DaySummaryDTO::workedMinutes).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int targetMinutes = currentWeek.stream().map(DaySummaryDTO::targetMinutes).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
        int percentage = targetMinutes > 0 ? (workedMinutes * 100) / targetMinutes : 0;
        return new WeeklyProgressDTO(workedMinutes, targetMinutes, percentage);
    }
}
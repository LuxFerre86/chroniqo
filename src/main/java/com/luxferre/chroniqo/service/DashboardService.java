package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.model.TimeEntry;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.TimeEntryRepository;
import com.vaadin.flow.component.UI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TimeEntryRepository timeEntryRepository;
    private final TimeEntryService timeEntryService;
    private final UserService userService;
    private final MonthService monthService;
    private final YearService yearService;

    /**
     * Get today's summary
     */
    public DaySummaryDTO getTodaySummary() {
        User user = userService.getCurrentUser();
        TimeEntry timeEntry = timeEntryService.getToday();
        if (timeEntry != null) {
            int dailyTargetMinutes = (user.getWeeklyTargetHours() * 60) / 5;
            int workedMinutes = timeEntryService.calculateWorkedMinutes(timeEntry);
            int balance = workedMinutes - dailyTargetMinutes;
            if (isWeekend(timeEntry.getDate())) {
                balance = workedMinutes;
            }
            return new DaySummaryDTO(timeEntry.getDate(), workedMinutes, dailyTargetMinutes, balance, null);
        }
        return null;
    }

    /**
     * Get last 7 days summary for chart
     */
    public List<DaySummaryDTO> getWeekSummary() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(UI.getCurrent().getLocale());
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);

        List<DaySummaryDTO> weekData = new ArrayList<>();

        for (LocalDate date = weekStart; !date.isAfter(weekEnd); date = date.plusDays(1)) {
            LocalDate finalDate = date;
            DaySummaryDTO summary = monthService.getMonth(date.getYear(), date.getMonthValue())
                    .stream()
                    .filter(s -> s.date().equals(finalDate))
                    .findFirst()
                    .orElse(new DaySummaryDTO(date, null, null, null, null));

            weekData.add(summary);
        }

        return weekData;
    }

    /**
     * Get weekly target progress
     */
    public WeeklyProgress getWeeklyProgress() {
        User user = userService.getCurrentUser();
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(UI.getCurrent().getLocale());
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);

        int targetMinutes = user.getWeeklyTargetHours() * 60;
        int workedMinutes = 0;

        for (LocalDate date = weekStart; !date.isAfter(today) && !date.isAfter(weekEnd); date = date.plusDays(1)) {
            TimeEntry entry = timeEntryRepository.findByUserAndDate(user, date);
            if (entry != null) {
                workedMinutes += timeEntryService.calculateWorkedMinutes(entry);
            }
        }

        int percentage = targetMinutes > 0 ? (workedMinutes * 100) / targetMinutes : 0;

        return new WeeklyProgress(workedMinutes, targetMinutes, percentage);
    }

    /**
     * Get current balance
     */
    public int getCurrentBalance() {
        LocalDate today = LocalDate.now();
        return yearService.getYear(today.getYear())
                .stream()
                .filter(s -> s.date().isBefore(today) || s.date().equals(today))
                .filter(s -> s.balanceMinutes() != null)
                .mapToInt(DaySummaryDTO::balanceMinutes)
                .sum();
    }

    private boolean isWeekend(LocalDate date) {
        DayOfWeek dow = date.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    // Helper class for weekly progress
    public record WeeklyProgress(int workedMinutes, int targetMinutes, int percentage) {
    }
}
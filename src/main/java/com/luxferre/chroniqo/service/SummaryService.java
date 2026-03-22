package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.event.AbsenceBroadcaster;
import com.luxferre.chroniqo.service.event.BroadcastListener;
import com.luxferre.chroniqo.service.event.TimeEntryBroadcaster;
import com.luxferre.chroniqo.service.event.UserBroadcaster;
import com.luxferre.chroniqo.service.user.UserService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Set;

/**
 * Computes aggregated time-tracking summaries for the dashboard and monthly
 * calendar view.
 *
 * <p>The central method is {@link #getSummary(LocalDate, LocalDate)},
 * which joins time entries and absences for a date range and produces one
 * {@link DaySummaryDTO} per calendar day, including worked minutes, daily
 * target, running balance, and absence type. Daily targets and workday
 * classification are based on the user's configured working days.
 *
 * @author Luxferre86
 * @since 28.02.2026
 */
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TimeTrackingService timeTrackingService;
    private final UserService userService;
    private final TimeEntryBroadcaster timeEntryBroadcaster;
    private final AbsenceBroadcaster absenceBroadcaster;
    private final UserBroadcaster userBroadcaster;

    /**
     * Returns the summary for today.
     *
     * @return today's {@link DaySummaryDTO}, or {@code null} if no data is available
     */
    public DaySummaryDTO getToday() {
        LocalDate today = LocalDate.now();
        return getSummary(today, today).stream().findFirst().orElse(null);
    }

    /**
     * Returns summaries for every day of the current ISO week
     * (Monday through Sunday) relative to the current UI locale.
     *
     * @return list of {@link DaySummaryDTO}, one per day
     */
    public List<DaySummaryDTO> getCurrentWeek() {
        LocalDate today = LocalDate.now();
        WeekFields weekFields = WeekFields.of(UI.getCurrent().getLocale());
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);
        return getSummary(weekStart, weekEnd);
    }

    /**
     * Returns summaries for every calendar day in the given year.
     *
     * @param year the calendar year to summarize
     * @return list of {@link DaySummaryDTO}, one per day
     */
    public List<DaySummaryDTO> getSummary(int year) {
        LocalDate yearStart = Year.of(year).atMonth(1).atDay(1);
        LocalDate yearEnd = yearStart.with(TemporalAdjusters.lastDayOfYear());
        return getSummary(yearStart, yearEnd);
    }

    /**
     * Returns summaries for every calendar day between {@code startDate} and
     * {@code endDate} (inclusive).
     *
     * @param startDate first day of the range (inclusive)
     * @param endDate   last day of the range (inclusive)
     * @return list of {@link DaySummaryDTO}, one per day
     */
    public List<DaySummaryDTO> getSummary(LocalDate startDate, LocalDate endDate) {
        User user = userService.getCurrentUser();

        List<TimeEntryDTO> entries = timeTrackingService.getTimeEntries(startDate, endDate);
        List<Absence> absences = timeTrackingService.getAbsences(startDate, endDate);

        Set<DayOfWeek> workingDays = user.getWorkingDaysOrDefault();
        int dailyTargetMinutes = calculateDailyTargetMinutes(
                user.getWeeklyTargetHours(), workingDays.size());

        return startDate.datesUntil(endDate.plusDays(1L))
                .map(date -> createDaySummaryDTO(date, entries, absences,
                        dailyTargetMinutes, workingDays))
                .toList();
    }

    /**
     * Returns the aggregated progress towards the user's weekly hour target
     * for the current ISO week.
     *
     * @return a {@link WeeklyProgressDTO} with worked minutes, target minutes,
     *         percentage, and a flag indicating whether a target is configured
     */
    public WeeklyProgressDTO getWeeklyProgress() {
        List<DaySummaryDTO> currentWeek = getCurrentWeek();
        int workedMinutes = currentWeek.stream().mapToInt(DaySummaryDTO::workedMinutes).sum();
        int targetMinutes = currentWeek.stream().mapToInt(DaySummaryDTO::targetMinutes).sum();
        int percentage = targetMinutes > 0 ? (workedMinutes * 100) / targetMinutes : 0;
        boolean hasTarget = targetMinutes > 0;
        return new WeeklyProgressDTO(workedMinutes, targetMinutes, percentage, hasTarget);
    }

    /**
     * Registers a broadcast listener for time entry, absence, and user events
     * for the current user.
     *
     * @param listener the listener to register
     * @return a set of registrations for the listener
     */
    public Set<Registration> register(BroadcastListener listener) {
        User user = userService.getCurrentUser();
        return Set.of(
                timeEntryBroadcaster.register(user, listener),
                absenceBroadcaster.register(user, listener),
                userBroadcaster.register(user, listener));
    }

    /**
     * Builds a {@link DaySummaryDTO} for a single date by correlating time
     * entries, absences, the user's daily target, and their configured working
     * days.
     *
     * @param date               the calendar date to summarise
     * @param entries            all time entries for the enclosing date range
     * @param absences           all absences for the enclosing date range
     * @param dailyTargetMinutes the user's daily working-time target in minutes
     * @param workingDays        the user's configured set of working days
     * @return the computed day summary
     */
    DaySummaryDTO createDaySummaryDTO(LocalDate date, List<TimeEntryDTO> entries,
                                      List<Absence> absences, int dailyTargetMinutes,
                                      Set<DayOfWeek> workingDays) {
        TimeEntryDTO entry = entries.stream()
                .filter(e -> e.getDate().equals(date))
                .findFirst()
                .orElse(null);

        Absence absence = absences.stream()
                .filter(a -> !date.isBefore(a.getDate()) && !date.isAfter(a.getDate()))
                .findFirst()
                .orElse(null);

        boolean isConfiguredWorkday = workingDays.contains(date.getDayOfWeek());
        boolean isWorkday = isConfiguredWorkday && absence == null;

        int workedMinutes = (entry != null && absence == null)
                ? calculateWorkedMinutes(entry)
                : 0;

        int targetMinutes = isWorkday ? dailyTargetMinutes : 0;
        // On non-working days any time worked counts as pure positive balance
        int balance = isConfiguredWorkday ? workedMinutes - targetMinutes : workedMinutes;

        return new DaySummaryDTO(
                date,
                isWorkday,
                workedMinutes,
                targetMinutes,
                balance,
                absence != null ? absence.getType() : null
        );
    }

    /**
     * Computes net worked minutes for a time entry.
     *
     * <p>For a completed entry the result is
     * {@code (endTime - startTime) - breakMinutes}, clamped to zero.
     * For an in-progress entry (no end time) the calculation uses
     * the current wall-clock time as a provisional end.
     *
     * @param entry the time entry to evaluate
     * @return net worked minutes, never negative
     */
    int calculateWorkedMinutes(TimeEntryDTO entry) {
        if (entry.getEndTime() == null && entry.getStartTime() != null) {
            LocalTime now = LocalTime.now();
            int minutes = Math.toIntExact(
                    Duration.between(entry.getStartTime(), now).toMinutes());
            if (entry.getBreakMinutes() != null) {
                minutes -= entry.getBreakMinutes();
            }
            return Math.max(0, minutes);
        }

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

    /**
     * Converts a weekly target in hours to a daily target in minutes based on
     * the number of configured working days per week.
     *
     * @param weeklyTargetHours the weekly target in whole hours (0–80)
     * @param workingDaysCount  the number of working days per week (1–7)
     * @return daily target in minutes; zero when either parameter is zero
     */
    int calculateDailyTargetMinutes(int weeklyTargetHours, int workingDaysCount) {
        if (workingDaysCount <= 0) return 0;
        int weeklyHours = Math.clamp(weeklyTargetHours, 0, 80);
        return (weeklyHours * 60) / workingDaysCount;
    }
}
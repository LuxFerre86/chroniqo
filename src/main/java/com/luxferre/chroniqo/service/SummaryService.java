package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
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
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;

/**
 * Computes aggregated time-tracking summaries for the dashboard and monthly
 * calendar view.
 *
 * <p>The central method is {@link #getSummary(LocalDate, LocalDate)}, which
 * joins time entries, absences, and public holidays for a date range and
 * produces one {@link DaySummaryDTO} per calendar day. Daily targets and
 * workday classification respect the user's configured working days and country
 * / subdivision for automatic public holiday detection.
 *
 * <p>Public holidays are resolved on the fly via {@link PublicHolidayService}
 * and never stored in the database. A manually recorded absence always takes
 * precedence over an automatically detected public holiday.
 *
 * @author Luxferre86
 * @since 28.02.2026
 */
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TimeTrackingService timeTrackingService;
    private final UserService userService;
    private final PublicHolidayService publicHolidayService;
    private final TimeEntryBroadcaster timeEntryBroadcaster;
    private final AbsenceBroadcaster absenceBroadcaster;
    private final UserBroadcaster userBroadcaster;

    /**
     * Returns the summary for today.
     *
     * @return today's {@link DaySummaryDTO}, or {@code null} if no data is
     * available
     */
    public DaySummaryDTO getToday() {
        LocalDate today = LocalDate.now();
        return getSummary(today, today).stream().findFirst().orElse(null);
    }

    /**
     * Returns summaries for every day of the current ISO week (Monday through
     * Sunday) relative to the current UI locale.
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

        Set<LocalDate> holidays = user.getCountryCode() != null
                ? loadHolidaysForRange(user.getCountryCode(),
                user.getSubdivisionCode(), startDate, endDate)
                : Set.of();

        return startDate.datesUntil(endDate.plusDays(1L))
                .map(date -> createDaySummaryDTO(
                        date, entries, absences,
                        dailyTargetMinutes, workingDays, holidays))
                .toList();
    }

    /**
     * Returns the aggregated progress towards the user's weekly hour target for
     * the current ISO week.
     *
     * @return a {@link WeeklyProgressDTO} with worked minutes, target minutes,
     * percentage, and a flag indicating whether a target is configured
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
     * entries, absences, public holidays, the user's daily target, and their
     * configured working days.
     *
     * <p>Priority order for a given date:
     * <ol>
     *   <li>Manually recorded absence (vacation, sick, or manually entered
     *       holiday) — always wins</li>
     *   <li>Automatically detected public holiday (from country/subdivision)</li>
     *   <li>Non-working day per the user's working-days configuration</li>
     *   <li>Normal working day with target and balance calculation</li>
     * </ol>
     *
     * @param date               the calendar date to summarize
     * @param entries            all time entries for the enclosing date range
     * @param absences           all manually recorded absences for the range
     * @param dailyTargetMinutes the user's daily working-time target in minutes
     * @param workingDays        the user's configured set of working days
     * @param holidays           public holiday dates for the relevant year(s)
     * @return the computed day summary
     */
    DaySummaryDTO createDaySummaryDTO(LocalDate date, List<TimeEntryDTO> entries,
                                      List<Absence> absences, int dailyTargetMinutes,
                                      Set<DayOfWeek> workingDays,
                                      Set<LocalDate> holidays) {
        TimeEntryDTO entry = entries.stream()
                .filter(e -> e.getDate().equals(date))
                .findFirst()
                .orElse(null);

        Absence absence = absences.stream()
                .filter(a -> a.getDate().equals(date))
                .findFirst()
                .orElse(null);

        // Public holiday only applies when no manual absence is recorded
        boolean isPublicHoliday = absence == null && holidays.contains(date);

        boolean isConfiguredWorkday = workingDays.contains(date.getDayOfWeek());
        boolean isWorkday = isConfiguredWorkday && absence == null && !isPublicHoliday;

        int workedMinutes = (entry != null && absence == null && !isPublicHoliday)
                ? calculateWorkedMinutes(entry)
                : 0;

        int targetMinutes = isWorkday ? dailyTargetMinutes : 0;
        // On non-configured workdays or public holidays, any time worked is surplus
        int balance = (isConfiguredWorkday && !isPublicHoliday)
                ? workedMinutes - targetMinutes
                : workedMinutes;

        AbsenceType effectiveAbsenceType = absence != null
                ? absence.getType()
                : (isPublicHoliday ? AbsenceType.HOLIDAY : null);


        String absenceName = getAbsenceName(date, isPublicHoliday, absence);

        return new DaySummaryDTO(
                date,
                isWorkday,
                workedMinutes,
                targetMinutes,
                balance,
                effectiveAbsenceType, absenceName);
    }

    /**
     * Computes the net working time in minutes for a time entry by subtracting
     * the break duration from the gross duration.
     *
     * <p>Handles three cases:
     * <ul>
     *   <li>Entry with start and end time: computes duration and subtracts break</li>
     *   <li>Entry with only start time (incomplete): computes duration from start
     *       to now and subtracts break</li>
     *   <li>Entry without start time: returns zero (not started)</li>
     * </ul>
     *
     * <p>Negative results (e.g. when break exceeds working time) are clamped to zero.
     * Midnight-crossing durations are handled automatically.
     *
     * @param entry the time entry DTO to compute
     * @return net working time in minutes, never negative
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
            minutes += 1440; // midnight crossing
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

    /**
     * Resolves the display name for an absence or public holiday on the given date.
     *
     * <p>Priority order:
     * <ol>
     *   <li>If a manual absence exists, returns its type name (capitalized)</li>
     *   <li>If the date is a public holiday, attempts to load the holiday name
     *       from the jollyday library</li>
     *   <li>Otherwise, returns {@code null}</li>
     * </ol>
     *
     * @param date            the date to resolve
     * @param isPublicHoliday whether the date matches a public holiday
     * @param absence         a manually recorded absence, or {@code null}
     * @return the absence/holiday name, or {@code null}
     */
    String getAbsenceName(LocalDate date, boolean isPublicHoliday, Absence absence) {
        if (absence != null) {
            return StringUtils.capitalize(absence.getType().name().toLowerCase());
        } else if (isPublicHoliday) {
            // Attempt to find a matching holiday name for the date
            User currentUser = userService.getCurrentUser();
            return publicHolidayService.getHoliday(date, currentUser.getCountryCode(), currentUser.getSubdivisionCode()).map(holiday -> holiday.getDescription(Locale.ROOT)).orElse("Holiday");
        }
        return null;
    }

    /**
     * Loads and merges public holidays from jollyday for all calendar years
     * covered by the given date range.
     *
     * <p>For single-year ranges, delegates directly to
     * {@link PublicHolidayService#getHolidays(String, String, Year)}. For
     * multi-year ranges, loads holidays for each year separately and merges
     * them into a single immutable set.
     *
     * @param countryCode     ISO country code (case-insensitive)
     * @param subdivisionCode full ISO 3166-2 code, or {@code null}
     * @param start           first day of the range (inclusive)
     * @param end             last day of the range (inclusive)
     * @return immutable set of all public holidays in the range
     */
    private Set<LocalDate> loadHolidaysForRange(String countryCode,
                                                String subdivisionCode,
                                                LocalDate start, LocalDate end) {
        if (start.getYear() == end.getYear()) {
            return publicHolidayService.getHolidays(
                    countryCode, subdivisionCode, Year.from(start));
        }
        Set<LocalDate> merged = new HashSet<>();
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            merged.addAll(publicHolidayService.getHolidays(
                    countryCode, subdivisionCode, Year.of(year)));
        }
        return Collections.unmodifiableSet(merged);
    }
}
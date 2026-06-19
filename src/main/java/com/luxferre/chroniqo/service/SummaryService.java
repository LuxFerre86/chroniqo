package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceTypeDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.*;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class SummaryService {

    private final TimeTrackingService timeTrackingService;
    private final UserService userService;
    private final PublicHolidayService publicHolidayService;
    private final TimeEntryBroadcaster timeEntryBroadcaster;
    private final AbsenceBroadcaster absenceBroadcaster;
    private final UserBroadcaster userBroadcaster;
    private final Clock clock;

    /**
     * Returns the summary for today.
     *
     * @return today's {@link DaySummaryDTO}, or {@code null} if no data is
     * available
     */
    public DaySummaryDTO getToday() {
        LocalDate today = LocalDate.now(clock);
        return getSummary(today, today).stream().findFirst().orElse(null);
    }

    /**
     * Returns summaries for every day of the current ISO week (Monday through
     * Sunday) relative to the current UI locale.
     *
     * @return list of {@link DaySummaryDTO}, one per day
     */
    public List<DaySummaryDTO> getCurrentWeek() {
        LocalDate today = LocalDate.now(clock);
        WeekFields weekFields = WeekFields.of(UI.getCurrent().getLocale());
        LocalDate weekStart = today.with(weekFields.dayOfWeek(), 1);
        LocalDate weekEnd = today.with(weekFields.dayOfWeek(), 7);
        return getSummary(weekStart, weekEnd);
    }

    /**
     * Returns the running time balance from the first day of the current year
     * up to and including today.
     *
     * @return summed balance minutes for the current year-to-date period
     */
    public int getCurrentBalance() {
        LocalDate today = LocalDate.now(clock);
        LocalDate yearStart = today.with(TemporalAdjusters.firstDayOfYear());
        return getSummary(yearStart, today).stream()
                .mapToInt(DaySummaryDTO::balanceMinutes)
                .sum();
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
        Map<LocalDate, List<TimeEntryDTO>> entriesByDate = entries.stream()
                .collect(Collectors.groupingBy(TimeEntryDTO::getDate));
        Map<LocalDate, Absence> absencesByDate = new HashMap<>();
        for (Absence absence : absences) {
            absencesByDate.put(absence.getDate(), absence);
        }

        Set<DayOfWeek> workingDays = user.getWorkingDaysOrDefault();
        int dailyTargetMinutes = calculateDailyTargetMinutes(
                user.getWeeklyTargetHours(), workingDays.size());

        Set<LocalDate> holidays = user.getCountryCode() != null
                ? loadHolidaysForRange(user.getCountryCode(),
                user.getSubdivisionCode(), startDate, endDate)
                : Set.of();

        return startDate.datesUntil(endDate.plusDays(1L))
                .map(date -> createDaySummaryDTO(
                        date,
                        entriesByDate.getOrDefault(date, List.of()),
                        absencesByDate.get(date),
                        dailyTargetMinutes,
                        workingDays,
                        holidays,
                        user.getCountryCode(),
                        user.getSubdivisionCode()))
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
        log.info("Retrieving weekly progress");
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
     * Compatibility overload for tests and existing call sites that still pass
     * a single entry instead of a list.
     */
    DaySummaryDTO createDaySummaryDTO(LocalDate date,
                                      TimeEntryDTO entry,
                                      Absence absence,
                                      int dailyTargetMinutes,
                                      Set<DayOfWeek> workingDays,
                                      Set<LocalDate> holidays,
                                      String countryCode,
                                      String subdivisionCode) {
        List<TimeEntryDTO> entries = entry != null ? List.of(entry) : List.of();
        return createDaySummaryDTO(
                date,
                entries,
                absence,
                dailyTargetMinutes,
                workingDays,
                holidays,
                countryCode,
                subdivisionCode);
    }

    private DaySummaryDTO createDaySummaryDTO(LocalDate date,
                                              List<TimeEntryDTO> entries,
                                              Absence absence,
                                              int dailyTargetMinutes,
                                              Set<DayOfWeek> workingDays,
                                              Set<LocalDate> holidays,
                                              String countryCode,
                                              String subdivisionCode) {

        // Public holiday only applies when no manual absence is recorded
        boolean isPublicHoliday = absence == null && holidays.contains(date);

        boolean isConfiguredWorkday = workingDays.contains(date.getDayOfWeek());
        boolean isWorkday = isConfiguredWorkday && absence == null && !isPublicHoliday;

        int workedMinutes = entries.stream()
                .mapToInt(this::calculateWorkedMinutes)
                .sum();

        int targetMinutes = isWorkday ? dailyTargetMinutes : 0;
        // On non-configured workdays or public holidays, any time worked is surplus
        int balance = (isConfiguredWorkday && !isPublicHoliday)
                ? workedMinutes - targetMinutes
                : workedMinutes;

        AbsenceTypeDTO effectiveAbsenceType = absence != null
                ? AbsenceTypeDTO.of(absence.getType())
                : (isPublicHoliday ? AbsenceTypeDTO.HOLIDAY : null);

        String absenceName = getAbsenceName(
                date, isPublicHoliday, absence, countryCode, subdivisionCode);

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
            LocalTime now = LocalTime.now(clock);
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
     */

    private String getAbsenceName(LocalDate date,
                                  boolean isPublicHoliday,
                                  Absence absence,
                                  String countryCode,
                                  String subdivisionCode) {
        if (absence != null) {
            return StringUtils.capitalize(absence.getType().name().toLowerCase());
        } else if (isPublicHoliday) {
            return publicHolidayService
                    .getHoliday(date, countryCode, subdivisionCode)
                    .map(holiday -> holiday.getDescription(Locale.ROOT))
                    .orElse("Holiday");
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
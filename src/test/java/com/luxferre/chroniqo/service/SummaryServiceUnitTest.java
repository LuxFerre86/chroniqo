package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.dto.WeeklyProgressDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.service.event.AbsenceBroadcaster;
import com.luxferre.chroniqo.service.event.TimeEntryBroadcaster;
import com.luxferre.chroniqo.service.event.UserBroadcaster;
import com.luxferre.chroniqo.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Year;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SummaryServiceUnitTest {

    private SummaryService summaryService;

    // Monday 2026-03-02 — not a public holiday anywhere in Germany
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 3, 2);
    // Saturday 2026-03-07
    private static final LocalDate WEEKEND = LocalDate.of(2026, 3, 7);
    // New Year's Day — nationwide holiday in all German states
    private static final LocalDate NEW_YEARS_DAY = LocalDate.of(2026, 1, 1);

    private static final int DAILY_TARGET_MINUTES = 468; // 39h / 5 days * 60
    private static final Set<LocalDate> NO_HOLIDAYS = Set.of();

    @BeforeEach
    void setUp() {
        summaryService = new SummaryService(
                mock(TimeTrackingService.class),
                mock(UserService.class),
                mock(PublicHolidayService.class),
                mock(TimeEntryBroadcaster.class),
                mock(AbsenceBroadcaster.class),
                mock(UserBroadcaster.class));
    }

    // =========================================================================
    // calculateWorkedMinutes
    // =========================================================================

    @Nested
    @DisplayName("calculateWorkedMinutes")
    class CalculateWorkedMinutes {

        @Test
        void completedEntry_standardDay_returnsCorrectMinutes() {
            assertThat(summaryService.calculateWorkedMinutes(
                    entry(LocalTime.of(8, 0), LocalTime.of(17, 0), 30)))
                    .isEqualTo(510);
        }

        @Test
        void completedEntry_withBreak_subtractsBreak() {
            assertThat(summaryService.calculateWorkedMinutes(
                    entry(LocalTime.of(7, 15), LocalTime.of(17, 45), 70)))
                    .isEqualTo(560);
        }

        @Test
        void completedEntry_noBreak_returnsRawDuration() {
            assertThat(summaryService.calculateWorkedMinutes(
                    entry(LocalTime.of(9, 0), LocalTime.of(17, 0), null)))
                    .isEqualTo(480);
        }

        @Test
        void completedEntry_midnightCrossing_handledCorrectly() {
            assertThat(summaryService.calculateWorkedMinutes(
                    entry(LocalTime.of(22, 0), LocalTime.of(2, 0), 0)))
                    .isEqualTo(240);
        }

        @Test
        void startedEntry_noStartTime_returnsZero() {
            assertThat(summaryService.calculateWorkedMinutes(
                    entry(null, null, null)))
                    .isEqualTo(0);
        }

        @Test
        void startedEntry_breakLargerThanWorkedTime_returnsZeroNotNegative() {
            assertThat(summaryService.calculateWorkedMinutes(
                    entry(LocalTime.of(9, 0), LocalTime.of(9, 30), 60)))
                    .isEqualTo(0);
        }

        @Test
        void startedEntry_futureStartTime_returnsZeroNotNegative() {
            TimeEntryDTO e = new TimeEntryDTO();
            e.setDate(LocalDate.now());
            e.setStartTime(LocalTime.now().plusMinutes(30));
            assertThat(summaryService.calculateWorkedMinutes(e))
                    .isGreaterThanOrEqualTo(0);
        }
    }

    // =========================================================================
    // calculateDailyTargetMinutes
    // =========================================================================

    @Nested
    @DisplayName("calculateDailyTargetMinutes")
    class CalculateDailyTargetMinutes {

        @Test
        void fiveDayWeek_40h_returns480() {
            assertThat(summaryService.calculateDailyTargetMinutes(40, 5)).isEqualTo(480);
        }

        @Test
        void fourDayWeek_40h_returns600() {
            assertThat(summaryService.calculateDailyTargetMinutes(40, 4)).isEqualTo(600);
        }

        @Test
        void threeDayWeek_39h_returns780() {
            assertThat(summaryService.calculateDailyTargetMinutes(39, 3)).isEqualTo(780);
        }

        @Test
        void zeroHours_returnsZero() {
            assertThat(summaryService.calculateDailyTargetMinutes(0, 5)).isZero();
        }

        @Test
        void zeroDays_returnsZero() {
            assertThat(summaryService.calculateDailyTargetMinutes(40, 0)).isZero();
        }

        @Test
        void negativeHours_clampedToZero() {
            assertThat(summaryService.calculateDailyTargetMinutes(-10, 5)).isZero();
        }

        @Test
        void hoursAboveMax_clampedToEighty() {
            // 80h / 5 days = 960 min/day
            assertThat(summaryService.calculateDailyTargetMinutes(999, 5)).isEqualTo(960);
        }
    }

    // =========================================================================
    // createDaySummaryDTO — standard workday / weekend logic
    // =========================================================================

    @Nested
    @DisplayName("createDaySummaryDTO — standard workday/weekend")
    class CreateDaySummaryDTO {

        @Test
        void weekday_withCompletedEntry_correctBalanceAndTarget() {
            TimeEntryDTO e = entry(WEEKDAY, LocalTime.of(7, 15), LocalTime.of(17, 45), 70);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKDAY, List.of(e), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS, NO_HOLIDAYS);

            assertThat(result.date()).isEqualTo(WEEKDAY);
            assertThat(result.isWorkday()).isTrue();
            assertThat(result.workedMinutes()).isEqualTo(560);
            assertThat(result.targetMinutes()).isEqualTo(DAILY_TARGET_MINUTES);
            assertThat(result.balanceMinutes()).isEqualTo(560 - DAILY_TARGET_MINUTES);
            assertThat(result.absenceType()).isNull();
        }

        @Test
        void weekday_withNoEntry_balanceIsNegativeTarget() {
            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKDAY, List.of(), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS, NO_HOLIDAYS);

            assertThat(result.isWorkday()).isTrue();
            assertThat(result.workedMinutes()).isZero();
            assertThat(result.targetMinutes()).isEqualTo(DAILY_TARGET_MINUTES);
            assertThat(result.balanceMinutes()).isEqualTo(-DAILY_TARGET_MINUTES);
        }

        @Test
        void weekend_withEntry_balanceEqualsWorkedMinutes_noTarget() {
            TimeEntryDTO e = entry(WEEKEND, LocalTime.of(10, 0), LocalTime.of(14, 0), 0);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKEND, List.of(e), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS, NO_HOLIDAYS);

            assertThat(result.isWorkday()).isFalse();
            assertThat(result.workedMinutes()).isEqualTo(240);
            assertThat(result.targetMinutes()).isZero();
            assertThat(result.balanceMinutes()).isEqualTo(240);
        }

        @Test
        void weekend_withNoEntry_zeroEverything() {
            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKEND, List.of(), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS, NO_HOLIDAYS);

            assertThat(result.workedMinutes()).isZero();
            assertThat(result.targetMinutes()).isZero();
            assertThat(result.balanceMinutes()).isZero();
        }

        @ParameterizedTest
        @EnumSource(AbsenceType.class)
        void weekday_withAbsence_workedMinutesIsZero_absenceTypeSet(AbsenceType type) {
            TimeEntryDTO e = entry(WEEKDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);
            Absence a = absence(WEEKDAY, type);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKDAY, List.of(e), List.of(a),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS, NO_HOLIDAYS);

            assertThat(result.workedMinutes()).isZero();
            assertThat(result.targetMinutes()).isZero();
            assertThat(result.balanceMinutes()).isZero();
            assertThat(result.absenceType()).isEqualTo(type);
        }

        @Test
        void entry_forDifferentDate_isIgnored() {
            TimeEntryDTO e = entry(WEEKDAY.plusDays(1),
                    LocalTime.of(9, 0), LocalTime.of(17, 0), 30);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKDAY, List.of(e), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS, NO_HOLIDAYS);

            assertThat(result.workedMinutes()).isZero();
        }
    }

    // =========================================================================
    // createDaySummaryDTO — configurable working days
    // =========================================================================

    @Nested
    @DisplayName("createDaySummaryDTO — configurable working days")
    class ConfigurableWorkingDays {

        @Test
        void saturdayIsWorkday_inSixDayWeek_hasTarget() {
            LocalDate saturday = LocalDate.now()
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));
            Set<DayOfWeek> sixDayWeek = EnumSet.of(
                    DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    saturday, List.of(), List.of(), 480, sixDayWeek, NO_HOLIDAYS);

            assertThat(result.isWorkday()).isTrue();
            assertThat(result.targetMinutes()).isEqualTo(480);
            assertThat(result.balanceMinutes()).isEqualTo(-480);
        }

        @Test
        void mondayIsNotWorkday_inFourDayWeek_noTarget() {
            LocalDate monday = LocalDate.now()
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            Set<DayOfWeek> tueThuFri = EnumSet.of(
                    DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    monday, List.of(), List.of(), 480, tueThuFri, NO_HOLIDAYS);

            assertThat(result.isWorkday()).isFalse();
            assertThat(result.targetMinutes()).isZero();
        }

        @Test
        void mondayIsNotWorkday_workedAnywayOnMonday_countsAsSurplus() {
            LocalDate monday = LocalDate.now()
                    .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
            Set<DayOfWeek> tueThuFri = EnumSet.of(
                    DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                    DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
            TimeEntryDTO e = entry(monday, LocalTime.of(9, 0), LocalTime.of(13, 0), 0);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    monday, List.of(e), List.of(), 480, tueThuFri, NO_HOLIDAYS);

            assertThat(result.isWorkday()).isFalse();
            assertThat(result.targetMinutes()).isZero();
            assertThat(result.workedMinutes()).isEqualTo(240);
            assertThat(result.balanceMinutes()).isEqualTo(240); // surplus
        }
    }

    // =========================================================================
    // createDaySummaryDTO — public holidays
    // =========================================================================

    @Nested
    @DisplayName("createDaySummaryDTO — public holidays")
    class PublicHolidays {

        @Test
        void publicHoliday_noEntry_isNotWorkday_absenceTypeHoliday() {
            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    NEW_YEARS_DAY, List.of(), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS,
                    Set.of(NEW_YEARS_DAY));

            assertThat(result.isWorkday()).isFalse();
            assertThat(result.targetMinutes()).isZero();
            assertThat(result.workedMinutes()).isZero();
            assertThat(result.balanceMinutes()).isZero();
            assertThat(result.absenceType()).isEqualTo(AbsenceType.HOLIDAY);
        }

        @Test
        void publicHoliday_workedOnThatDay_zeroWorkedMinutes_holidayBadge() {
            // Entry present but public holiday suppresses it (no target, no worked time)
            TimeEntryDTO e = entry(NEW_YEARS_DAY,
                    LocalTime.of(9, 0), LocalTime.of(13, 0), 0);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    NEW_YEARS_DAY, List.of(e), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS,
                    Set.of(NEW_YEARS_DAY));

            assertThat(result.isWorkday()).isFalse();
            assertThat(result.targetMinutes()).isZero();
            assertThat(result.workedMinutes()).isZero();
            assertThat(result.absenceType()).isEqualTo(AbsenceType.HOLIDAY);
        }

        @Test
        void manualAbsence_overridesPublicHoliday() {
            Absence vacation = absence(NEW_YEARS_DAY, AbsenceType.VACATION);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    NEW_YEARS_DAY, List.of(), List.of(vacation),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS,
                    Set.of(NEW_YEARS_DAY));

            assertThat(result.absenceType()).isEqualTo(AbsenceType.VACATION);
        }

        @Test
        void regularWorkday_notInHolidaySet_notAffected() {
            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    WEEKDAY, List.of(), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS,
                    Set.of(NEW_YEARS_DAY)); // holiday is a different date

            assertThat(result.isWorkday()).isTrue();
            assertThat(result.absenceType()).isNull();
        }

        @Test
        void emptyHolidaySet_noEffect() {
            // When no country is configured the holiday set is empty;
            // New Year's Day is then treated as a normal working day
            DaySummaryDTO result = summaryService.createDaySummaryDTO(
                    NEW_YEARS_DAY, List.of(), List.of(),
                    DAILY_TARGET_MINUTES, User.DEFAULT_WORKING_DAYS,
                    NO_HOLIDAYS);

            assertThat(result.isWorkday()).isTrue();
            assertThat(result.absenceType()).isNull();
        }
    }

    // =========================================================================
    // PublicHolidayService — live jollyday (no mocking)
    // =========================================================================

    @Nested
    @DisplayName("PublicHolidayService — live jollyday integration")
    class PublicHolidayServiceIntegration {

        private PublicHolidayService holidayService;

        @BeforeEach
        void setUp() {
            CountrySubdivisionRegistry registry = new CountrySubdivisionRegistry();
            registry.init();
            holidayService = new PublicHolidayService(registry);
        }

        @Test
        void newYearsDay2026_isHolidayInGermany() {
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 1, 1), "DE", null)).isTrue();
        }

        @Test
        void christmasDay2026_isHolidayInGermany() {
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 12, 25), "DE", null)).isTrue();
        }

        @Test
        void epiphany2026_isHolidayInBavaria_notInBerlin() {
            // 6 Jan — only BY, BW, ST in Germany
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 1, 6), "DE", "DE-BY")).isTrue();
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 1, 6), "DE", "DE-BE")).isFalse();
        }

        @Test
        void fronleichnam2026_isHolidayInNRW_notInHamburg() {
            // Corpus Christi 2026 = 4 June
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 6, 4), "DE", "DE-NW")).isTrue();
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 6, 4), "DE", "DE-HH")).isFalse();
        }

        @Test
        void regularWorkday_isNotHoliday() {
            // 2026-03-02 (Monday) — no holiday anywhere in Germany
            assertThat(holidayService.isHoliday(WEEKDAY, "DE", "DE-BY")).isFalse();
        }

        @Test
        void unsupportedCountry_returnsEmptySet() {
            // A country code that jollyday has no calendar for
            Set<LocalDate> result = holidayService.getHolidays("XX", null, Year.of(2026));
            assertThat(result).isEmpty();
        }

        @Test
        void nullCountry_returnsEmptySet() {
            assertThat(holidayService.getHolidays(null, null, Year.of(2026))).isEmpty();
        }

        @Test
        void results_areCached_sameInstanceReturned() {
            Set<LocalDate> first = holidayService.getHolidays("DE", null, Year.of(2026));
            Set<LocalDate> second = holidayService.getHolidays("DE", null, Year.of(2026));
            assertThat(first).isSameAs(second);
        }

        @Test
        void newYearsDay_isHolidayInAustria() {
            assertThat(holidayService.isHoliday(
                    LocalDate.of(2026, 1, 1), "AT", null)).isTrue();
        }
    }

    // =========================================================================
    // CountrySubdivisionRegistry
    // =========================================================================

    @Nested
    @DisplayName("CountrySubdivisionRegistry")
    class CountrySubdivisionRegistryTests {

        private CountrySubdivisionRegistry registry;

        @BeforeEach
        void setUp() {
            registry = new CountrySubdivisionRegistry();
            registry.init();
        }

        @Test
        void allCountries_containsGermany() {
            assertThat(registry.getAllCountries()).containsKey("DE");
            assertThat(registry.getAllCountries().get("DE")).isEqualTo("Germany");
        }

        @Test
        void allCountries_isSortedAlphabetically() {
            List<String> names = new java.util.ArrayList<>(
                    registry.getAllCountries().values());
            List<String> sorted = names.stream().sorted().toList();
            assertThat(names).isEqualTo(sorted);
        }

        @Test
        void germanySubdivisions_containsBavaria() {
            Map<String, String> subs = registry.getSubdivisions("DE");
            assertThat(subs).containsKey("DE-BY");
            assertThat(subs.get("DE-BY")).isEqualTo("Bayern");
        }

        @Test
        void subdivisions_caseInsensitive() {
            assertThat(registry.getSubdivisions("de"))
                    .isEqualTo(registry.getSubdivisions("DE"));
        }

        @Test
        void hasHolidaySupport_germany_true() {
            assertThat(registry.hasHolidaySupport("DE")).isTrue();
        }

        @Test
        void hasHolidaySupport_unknownCode_false() {
            assertThat(registry.hasHolidaySupport("XX")).isFalse();
        }

        @Test
        void toJollyDaySubdivision_extractsSuffix() {
            assertThat(registry.toJollyDaySubdivision("DE-BY")).isEqualTo("BY");
            assertThat(registry.toJollyDaySubdivision("GB-ENG")).isEqualTo("ENG");
            assertThat(registry.toJollyDaySubdivision("BY")).isEqualTo("BY");
        }
    }

    // =========================================================================
    // getWeeklyProgress
    // =========================================================================

    @Nested
    @DisplayName("getWeeklyProgress")
    class GetWeeklyProgress {

        private SummaryService spySummaryService;

        @BeforeEach
        void setUp() {
            TimeTrackingService mockTts = mock(TimeTrackingService.class);
            UserService mockUs = mock(UserService.class);
            PublicHolidayService mockPhs = mock(PublicHolidayService.class);

            spySummaryService = new SummaryService(
                    mockTts, mockUs, mockPhs,
                    mock(TimeEntryBroadcaster.class),
                    mock(AbsenceBroadcaster.class),
                    mock(UserBroadcaster.class));

            User user = new User();
            user.setWeeklyTargetHours(39);
            when(mockUs.getCurrentUser()).thenReturn(user);
            when(mockTts.getTimeEntries(any(), any())).thenReturn(Collections.emptyList());
            when(mockTts.getAbsences(any(), any())).thenReturn(Collections.emptyList());
            when(mockPhs.getHolidays(any(), any(), any(Year.class)))
                    .thenReturn(Set.of());

            com.vaadin.flow.component.UI ui = new com.vaadin.flow.component.UI();
            ui.setLocale(java.util.Locale.GERMANY);
            com.vaadin.flow.component.UI.setCurrent(ui);
        }

        @Test
        void normalWeek_hasTargetTrue_percentageZeroWithNoEntries() {
            WeeklyProgressDTO result = spySummaryService.getWeeklyProgress();
            assertThat(result.hasTarget()).isTrue();
            assertThat(result.targetMinutes()).isGreaterThan(0);
            assertThat(result.percentage()).isEqualTo(0);
        }

        @Test
        void weeklyProgressDTO_noTarget_hasTargetFalse() {
            WeeklyProgressDTO noTarget = new WeeklyProgressDTO(0, 0, 0, false);
            assertThat(noTarget.hasTarget()).isFalse();
        }

        @Test
        void weeklyProgressDTO_over100Percent_notCappedInDTO() {
            WeeklyProgressDTO over100 = new WeeklyProgressDTO(600, 468, 128, true);
            assertThat(over100.percentage()).isEqualTo(128);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TimeEntryDTO entry(LocalTime start, LocalTime end, Integer breakMin) {
        return entry(WEEKDAY, start, end, breakMin);
    }

    private TimeEntryDTO entry(LocalDate date, LocalTime start, LocalTime end,
                               Integer breakMin) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setDate(date);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setBreakMinutes(breakMin);
        return dto;
    }

    private Absence absence(LocalDate date, AbsenceType type) {
        Absence a = new Absence();
        a.setDate(date);
        a.setType(type);
        return a;
    }
}
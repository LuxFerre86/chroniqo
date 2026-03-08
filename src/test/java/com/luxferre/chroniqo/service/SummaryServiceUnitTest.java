package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.TimeEntryDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SummaryServiceUnitTest {

    // Package-private methods are tested directly without Spring context
    private SummaryService summaryService;

    // Monday 2026-03-02 (a weekday, not a public holiday in most states)
    private static final LocalDate WEEKDAY = LocalDate.of(2026, 3, 2);
    // Saturday 2026-03-07
    private static final LocalDate WEEKEND = LocalDate.of(2026, 3, 7);
    private static final int DAILY_TARGET_MINUTES = 468; // 39h / 5 days * 60

    @BeforeEach
    void setUp() {
        summaryService = new SummaryService(
                mock(TimeTrackingService.class),
                mock(UserService.class)
        );
    }

    // =========================================================================
    // calculateWorkedMinutes
    // =========================================================================

    @Nested
    class CalculateWorkedMinutes {

        @Test
        void completedEntry_standardDay_returnsCorrectMinutes() {
            TimeEntryDTO entry = entry(LocalTime.of(8, 0), LocalTime.of(17, 0), 30);

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isEqualTo(510); // 9h - 30min = 510min
        }

        @Test
        void completedEntry_withBreak_subtractsBreak() {
            TimeEntryDTO entry = entry(LocalTime.of(7, 15), LocalTime.of(17, 45), 70);

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isEqualTo(560); // 10h30 - 70min = 560min
        }

        @Test
        void completedEntry_noBreak_returnsRawDuration() {
            TimeEntryDTO entry = entry(LocalTime.of(9, 0), LocalTime.of(17, 0), null);

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isEqualTo(480); // exactly 8h
        }

        @Test
        void completedEntry_midnightCrossing_handledCorrectly() {
            // Start 22:00, end 02:00 – crosses midnight
            TimeEntryDTO entry = entry(LocalTime.of(22, 0), LocalTime.of(2, 0), 0);

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isEqualTo(240); // 4 hours
        }

        @Test
        void startedEntry_noStartTime_returnsZero() {
            TimeEntryDTO entry = entry(null, null, null);

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isEqualTo(0);
        }

        @Test
        void startedEntry_breakLargerThanWorkedTime_returnsZeroNotNegative() {
            // This is the bug-fix scenario: break > actual duration must not go negative
            TimeEntryDTO entry = entry(LocalTime.of(9, 0), LocalTime.of(9, 30), 60);

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isEqualTo(0);
            assertThat(result).isGreaterThanOrEqualTo(0);
        }

        @Test
        void startedEntry_futurStartTime_returnsZeroNotNegative() {
            // Start time in the future means no time has passed yet
            TimeEntryDTO entry = new TimeEntryDTO();
            entry.setDate(LocalDate.now());
            entry.setStartTime(LocalTime.now().plusMinutes(30));
            // no endTime means "running"

            int result = summaryService.calculateWorkedMinutes(entry);

            assertThat(result).isGreaterThanOrEqualTo(0);
        }
    }

    // =========================================================================
    // createDaySummaryDTO
    // =========================================================================

    @Nested
    class CreateDaySummaryDTO {

        @Test
        void weekday_withCompletedEntry_correctBalanceAndTarget() {
            TimeEntryDTO entry = entry(WEEKDAY, LocalTime.of(7, 15), LocalTime.of(17, 45), 70);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKDAY, List.of(entry), Collections.emptyList(), DAILY_TARGET_MINUTES);

            assertThat(result.date()).isEqualTo(WEEKDAY);
            assertThat(result.workedMinutes()).isEqualTo(560);
            assertThat(result.targetMinutes()).isEqualTo(DAILY_TARGET_MINUTES);
            assertThat(result.balanceMinutes()).isEqualTo(560 - DAILY_TARGET_MINUTES); // +92
            assertThat(result.absenceType()).isNull();
        }

        @Test
        void weekday_withNoEntry_balanceIsNegativeTarget() {
            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKDAY, Collections.emptyList(), Collections.emptyList(), DAILY_TARGET_MINUTES);

            assertThat(result.date()).isEqualTo(WEEKDAY);
            assertThat(result.workedMinutes()).isEqualTo(0);
            assertThat(result.targetMinutes()).isEqualTo(DAILY_TARGET_MINUTES);
            assertThat(result.balanceMinutes()).isEqualTo(-DAILY_TARGET_MINUTES);
            assertThat(result.absenceType()).isNull();
        }

        @Test
        void weekend_withEntry_balanceEqualsWorkedMinutes_noTarget() {
            TimeEntryDTO entry = entry(WEEKEND, LocalTime.of(10, 0), LocalTime.of(14, 0), 0);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKEND, List.of(entry), Collections.emptyList(), DAILY_TARGET_MINUTES);

            assertThat(result.date()).isEqualTo(WEEKEND);
            assertThat(result.workedMinutes()).isEqualTo(240);
            assertThat(result.targetMinutes()).isEqualTo(0); // no target on weekends
            assertThat(result.balanceMinutes()).isEqualTo(240); // all weekend work is surplus
            assertThat(result.absenceType()).isNull();
        }

        @Test
        void weekend_withNoEntry_zeroBalance_noTarget() {
            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKEND, Collections.emptyList(), Collections.emptyList(), DAILY_TARGET_MINUTES);

            assertThat(result.workedMinutes()).isEqualTo(0);
            assertThat(result.targetMinutes()).isEqualTo(0);
            assertThat(result.balanceMinutes()).isEqualTo(0);
        }

        @ParameterizedTest
        @EnumSource(AbsenceType.class)
        void weekday_withAbsence_workedMinutesIsZero_absenceTypeSet(AbsenceType absenceType) {
            // Even if there is a time entry, absence takes priority
            TimeEntryDTO entry = entry(WEEKDAY, LocalTime.of(9, 0), LocalTime.of(17, 0), 0);
            Absence absence = absence(WEEKDAY, absenceType);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKDAY, List.of(entry), List.of(absence), DAILY_TARGET_MINUTES);

            assertThat(result.workedMinutes()).isEqualTo(0);
            assertThat(result.targetMinutes()).isEqualTo(0);
            assertThat(result.balanceMinutes()).isEqualTo(0);
            assertThat(result.absenceType()).isEqualTo(absenceType);
        }

        @Test
        void weekday_balanceAndWorkedMinutesNeverNegativeForWorked() {
            // Break larger than work duration → workedMinutes must be 0, not negative
            TimeEntryDTO entry = entry(WEEKDAY, LocalTime.of(9, 0), LocalTime.of(9, 20), 60);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKDAY, List.of(entry), Collections.emptyList(), DAILY_TARGET_MINUTES);

            assertThat(result.workedMinutes()).isGreaterThanOrEqualTo(0);
            // balance may be negative (not enough hours worked), but workedMinutes must not be
        }

        @Test
        void entry_forDifferentDate_isIgnored() {
            // Entry exists but for a different date — should not affect the day being evaluated
            LocalDate otherDay = WEEKDAY.plusDays(1);
            TimeEntryDTO entry = entry(otherDay, LocalTime.of(9, 0), LocalTime.of(17, 0), 30);

            DaySummaryDTO result = summaryService.createDaySummaryDTO(WEEKDAY, List.of(entry), Collections.emptyList(), DAILY_TARGET_MINUTES);

            assertThat(result.workedMinutes()).isEqualTo(0);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private TimeEntryDTO entry(LocalTime start, LocalTime end, Integer breakMinutes) {
        return entry(WEEKDAY, start, end, breakMinutes);
    }

    private TimeEntryDTO entry(LocalDate date, LocalTime start, LocalTime end, Integer breakMinutes) {
        TimeEntryDTO dto = new TimeEntryDTO();
        dto.setDate(date);
        dto.setStartTime(start);
        dto.setEndTime(end);
        dto.setBreakMinutes(breakMinutes);
        return dto;
    }

    private Absence absence(LocalDate date, AbsenceType type) {
        Absence absence = new Absence();
        absence.setDate(date);
        absence.setType(type);
        return absence;
    }
}
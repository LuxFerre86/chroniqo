package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.CustomHolidayDTO;
import com.luxferre.chroniqo.dto.DaySummaryDTO;
import com.luxferre.chroniqo.dto.AbsenceTypeDTO;
import com.luxferre.chroniqo.model.AbsenceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test user: Mon-Fri, Hamburg. 2031-03-03 is a Monday.
 */
@SpringBootTest
@Transactional
@WithMockUser("test@gmail.com")
class CustomHolidayServiceIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);
    private static final LocalDate FRIDAY = LocalDate.of(2031, 3, 7);

    @Autowired
    private CustomHolidayService customHolidayService;
    @Autowired
    private SummaryService summaryService;
    @Autowired
    private VacationService vacationService;
    @Autowired
    private AbsenceService absenceService;

    private DaySummaryDTO day(LocalDate date) {
        return summaryService.getSummary(date, date).getFirst();
    }

    @Test
    void createAndList_returnsEntryAndDeleteRemovesIt() {
        CustomHolidayDTO created = customHolidayService.create("  Bridge day ", MONDAY, MONDAY, false);

        assertThat(created.name()).isEqualTo("Bridge day");
        assertThat(customHolidayService.list()).extracting(CustomHolidayDTO::id).contains(created.id());

        customHolidayService.delete(created.id());

        assertThat(customHolidayService.list()).extracting(CustomHolidayDTO::id).doesNotContain(created.id());
    }

    @Test
    void delete_unknownId_isNotFound() {
        assertThatThrownBy(() -> customHolidayService.delete("does-not-exist")).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void create_invalidData_isRejected() {
        assertThatThrownBy(() -> customHolidayService.create(" ", MONDAY, MONDAY, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> customHolidayService.create("x", FRIDAY, MONDAY, false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> customHolidayService.create("x", LocalDate.of(2031, 12, 30), LocalDate.of(2032, 1, 2), true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> customHolidayService.create("x", MONDAY, MONDAY.plusDays(400), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void summary_customHoliday_hasNoTargetAndIsMarkedAsHoliday() {
        customHolidayService.create("Bridge day", MONDAY, MONDAY, false);

        DaySummaryDTO summary = day(MONDAY);

        assertThat(summary.targetMinutes()).isZero();
        assertThat(summary.isWorkday()).isFalse();
        assertThat(summary.absenceType()).isEqualTo(AbsenceTypeDTO.HOLIDAY);
        assertThat(summary.absenceLabel()).isEqualTo("Bridge day");
        assertThat(day(MONDAY.plusDays(1)).targetMinutes()).isPositive();
    }

    @Test
    void summary_range_coversEveryDayOfTheRange() {
        customHolidayService.create("Company holiday", MONDAY, FRIDAY, false);

        assertThat(summaryService.getSummary(MONDAY, FRIDAY)).allSatisfy(d -> assertThat(d.targetMinutes()).isZero());
    }

    @Test
    void summary_recurring_appliesInEveryYear() {
        customHolidayService.create("New Year's Eve", LocalDate.of(2030, 12, 31), LocalDate.of(2030, 12, 31), true);

        assertThat(day(LocalDate.of(2031, 12, 31)).absenceLabel()).isEqualTo("New Year's Eve");
        assertThat(day(LocalDate.of(2032, 12, 31)).absenceLabel()).isEqualTo("New Year's Eve");
        assertThat(day(LocalDate.of(2031, 12, 30)).absenceLabel()).isNotEqualTo("New Year's Eve");
    }

    @Test
    void summary_workedTimeOnCustomHoliday_countsAsOvertime() {
        customHolidayService.create("Bridge day", MONDAY, MONDAY, false);
        List<DaySummaryDTO> before = summaryService.getSummary(MONDAY, MONDAY);

        assertThat(before.getFirst().balanceMinutes()).isEqualTo(before.getFirst().workedMinutes());
    }

    @Test
    void vacation_customHolidayDoesNotCountAsVacationDay() {
        vacationService.setEntitlement(2031, 10);
        customHolidayService.create("Bridge day", MONDAY, MONDAY, false);

        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.VACATION));

        assertThat(vacationService.getAccount(2031).planned()).isEqualTo(4);
    }
}

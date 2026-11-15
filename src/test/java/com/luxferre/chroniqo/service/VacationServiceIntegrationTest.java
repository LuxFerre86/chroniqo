package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.dto.AbsenceRequest;
import com.luxferre.chroniqo.dto.VacationAccountDTO;
import com.luxferre.chroniqo.model.AbsenceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test user: Mon-Fri, Hamburg. 2031-03-03 is a Monday; 2031-05-01 is a public holiday.
 */
@SpringBootTest
@Transactional
@WithMockUser("test@gmail.com")
class VacationServiceIntegrationTest {

    private static final LocalDate MONDAY = LocalDate.of(2031, 3, 3);
    private static final LocalDate FRIDAY = LocalDate.of(2031, 3, 7);

    @Autowired
    private VacationService vacationService;
    @Autowired
    private AbsenceService absenceService;

    @Test
    void entitlement_withoutAnyValue_defaultsTo30() {
        assertThat(vacationService.getAccount(2030).entitlement()).isEqualTo(30);
    }

    @Test
    void entitlement_isInheritedByLaterYears() {
        vacationService.setEntitlement(2031, 27);

        assertThat(vacationService.getAccount(2031).entitlement()).isEqualTo(27);
        assertThat(vacationService.getAccount(2033).entitlement()).isEqualTo(27);
        assertThat(vacationService.getAccount(2030).entitlement()).isEqualTo(30);
    }

    @Test
    void setEntitlement_outOfRange_isRejected() {
        assertThatThrownBy(() -> vacationService.setEntitlement(2031, -1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> vacationService.setEntitlement(2031, 367)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void account_countsFutureVacationAsPlanned() {
        vacationService.setEntitlement(2031, 10);
        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.VACATION));

        VacationAccountDTO account = vacationService.getAccount(2031);

        assertThat(account).isEqualTo(new VacationAccountDTO(2031, 10, 0, 5, 5));
    }

    @Test
    void account_countsPastVacationAsTaken() {
        vacationService.setEntitlement(2025, 10);
        absenceService.saveAbsence(new AbsenceRequest(LocalDate.of(2025, 3, 3), LocalDate.of(2025, 3, 7), AbsenceType.VACATION));

        assertThat(vacationService.getAccount(2025)).isEqualTo(new VacationAccountDTO(2025, 10, 5, 0, 5));
    }

    @Test
    void account_ignoresWeekendsHolidaysAndOtherTypes() {
        vacationService.setEntitlement(2031, 10);
        // Wed 30.04. - Fri 02.05.: 01.05. is a public holiday
        absenceService.saveAbsence(new AbsenceRequest(LocalDate.of(2031, 4, 30), LocalDate.of(2031, 5, 2), AbsenceType.VACATION));
        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.SPECIAL_LEAVE));

        assertThat(vacationService.getAccount(2031).planned()).isEqualTo(2);
    }

    @Test
    void saveAbsence_vacationBeyondEntitlement_isBlocked() {
        vacationService.setEntitlement(2031, 4);

        assertThatThrownBy(() -> absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.VACATION)))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining("Not enough vacation days for 2031")
                .hasMessageContaining("5 requested, 4 remaining of 4");
    }

    @Test
    void saveAbsence_entitlementZero_isBlocked() {
        vacationService.setEntitlement(2031, 0);

        assertThatThrownBy(() -> absenceService.saveAbsence(new AbsenceRequest(MONDAY, MONDAY, AbsenceType.VACATION)))
                .isInstanceOf(TimeEntryValidationException.class);
    }

    @Test
    void saveAbsence_rebookingSameRange_doesNotCountTwice() {
        vacationService.setEntitlement(2031, 5);
        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.VACATION));

        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.VACATION));

        assertThat(vacationService.getAccount(2031).remaining()).isZero();
    }

    @Test
    void saveAbsence_otherTypes_ignoreEntitlement() {
        vacationService.setEntitlement(2031, 0);

        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.SPECIAL_LEAVE));
        absenceService.saveAbsence(new AbsenceRequest(MONDAY, FRIDAY, AbsenceType.SICK));

        assertThat(vacationService.getAccount(2031).planned()).isZero();
    }

    @Test
    void saveAbsence_acrossNewYear_checksEachYearSeparately() {
        vacationService.setEntitlement(2030, 1);
        vacationService.setEntitlement(2031, 10);
        // Mon 30.12.2030 - Fri 03.01.2031: 2 days in 2030, 3 days in 2031 (1.1. is a holiday -> 2 counted)
        assertThatThrownBy(() -> absenceService.saveAbsence(
                new AbsenceRequest(LocalDate.of(2030, 12, 30), LocalDate.of(2031, 1, 3), AbsenceType.VACATION)))
                .isInstanceOf(TimeEntryValidationException.class)
                .hasMessageContaining("2030");
    }
}

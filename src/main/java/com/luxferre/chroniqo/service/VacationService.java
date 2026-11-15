package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.i18n.Messages;
import com.luxferre.chroniqo.dto.VacationAccountDTO;
import com.luxferre.chroniqo.model.Absence;
import com.luxferre.chroniqo.model.AbsenceType;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.model.VacationEntitlement;
import com.luxferre.chroniqo.repository.AbsenceRepository;
import com.luxferre.chroniqo.repository.VacationEntitlementRepository;
import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages the annual vacation entitlement and computes the vacation account.
 *
 * <p>Only {@link AbsenceType#VACATION} days on configured working days that
 * are not public holidays count against the entitlement. A year without its
 * own entitlement inherits the value of the latest earlier year; when none
 * exists {@value #DEFAULT_ENTITLEMENT_DAYS} days apply.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VacationService {

    static final int DEFAULT_ENTITLEMENT_DAYS = 30;
    public static final int MAX_ENTITLEMENT_DAYS = 366;

    private final VacationEntitlementRepository entitlementRepository;
    private final AbsenceRepository absenceRepository;
    private final UserService userService;
    private final PublicHolidayService publicHolidayService;
    private final CustomHolidayService customHolidayService;
    private final Clock clock;

    /**
     * Returns the effective entitlement of the user for a year.
     */
    public int getEntitlement(User user, int year) {
        return entitlementRepository.findByUserAndYear(user, year)
                .or(() -> entitlementRepository.findFirstByUserAndYearLessThanOrderByYearDesc(user, year))
                .map(VacationEntitlement::getDays)
                .orElse(DEFAULT_ENTITLEMENT_DAYS);
    }

    /**
     * Sets the entitlement of the current user for a year.
     *
     * @throws IllegalArgumentException if {@code days} is outside 0..366
     */
    @Transactional
    public void setEntitlement(int year, int days) {
        if (days < 0 || days > MAX_ENTITLEMENT_DAYS) {
            throw new IllegalArgumentException(Messages.get("error.vacation.entitlement", String.valueOf(MAX_ENTITLEMENT_DAYS)));
        }
        User user = userService.getCurrentUser();
        VacationEntitlement entitlement = entitlementRepository.findByUserAndYear(user, year)
                .orElseGet(() -> {
                    VacationEntitlement created = new VacationEntitlement();
                    created.setUser(user);
                    created.setYear(year);
                    return created;
                });
        entitlement.setDays(days);
        entitlementRepository.save(entitlement);
        log.info("Vacation entitlement for {} set to {} days", year, days);
    }

    /**
     * Returns the vacation account of the current user for a year.
     */
    @Transactional(readOnly = true)
    public VacationAccountDTO getAccount(int year) {
        User user = userService.getCurrentUser();
        LocalDate today = LocalDate.now(clock);
        LocalDate start = Year.of(year).atDay(1);
        LocalDate end = start.with(java.time.temporal.TemporalAdjusters.lastDayOfYear());

        int taken = 0;
        int planned = 0;
        for (LocalDate date : countedVacationDates(user, start, end, null)) {
            if (date.isAfter(today)) planned++;
            else taken++;
        }
        int entitlement = getEntitlement(user, year);
        return new VacationAccountDTO(year, entitlement, taken, planned, entitlement - taken - planned);
    }

    /**
     * Verifies that booking vacation for the given range stays within the
     * entitlement of every year the range touches. Existing absences in the
     * range are replaced by the booking and therefore not counted.
     *
     * @throws TimeEntryValidationException if the entitlement would be exceeded
     */
    public void validateBooking(User user, LocalDate start, LocalDate end) {
        Map<Integer, Integer> requested = new HashMap<>();
        for (LocalDate date : countableDates(user, start, end)) {
            requested.merge(date.getYear(), 1, Integer::sum);
        }
        for (Map.Entry<Integer, Integer> entry : requested.entrySet()) {
            int year = entry.getKey();
            LocalDate yearStart = Year.of(year).atDay(1);
            LocalDate yearEnd = Year.of(year).atMonth(12).atEndOfMonth();
            int alreadyUsed = countedVacationDates(user, yearStart, yearEnd, new LocalDate[]{start, end}).size();
            int entitlement = getEntitlement(user, year);
            int remaining = entitlement - alreadyUsed;
            if (entry.getValue() > remaining) {
                throw TimeEntryValidationException.inconsistentData(String.format(
                        "Not enough vacation days for %d: %d requested, %d remaining of %d.",
                        year, entry.getValue(), Math.max(0, remaining), entitlement));
            }
        }
    }

    private List<LocalDate> countedVacationDates(User user, LocalDate start, LocalDate end, LocalDate[] excludedRange) {
        Set<LocalDate> holidays = holidaysFor(user, start, end);
        Set<DayOfWeek> workingDays = user.getWorkingDaysOrDefault();
        return absenceRepository.findByUserAndDateBetween(user, start, end).stream()
                .filter(a -> a.getType() == AbsenceType.VACATION)
                .map(Absence::getDate)
                .filter(d -> excludedRange == null || d.isBefore(excludedRange[0]) || d.isAfter(excludedRange[1]))
                .filter(d -> workingDays.contains(d.getDayOfWeek()) && !holidays.contains(d))
                .toList();
    }

    private List<LocalDate> countableDates(User user, LocalDate start, LocalDate end) {
        Set<LocalDate> holidays = holidaysFor(user, start, end);
        Set<DayOfWeek> workingDays = user.getWorkingDaysOrDefault();
        return start.datesUntil(end.plusDays(1))
                .filter(d -> workingDays.contains(d.getDayOfWeek()) && !holidays.contains(d))
                .toList();
    }

    private Set<LocalDate> holidaysFor(User user, LocalDate start, LocalDate end) {
        Set<LocalDate> holidays = new java.util.HashSet<>(customHolidayService.getDays(user, start, end).keySet());
        if (user.getCountryCode() == null) return holidays;
        for (int year = start.getYear(); year <= end.getYear(); year++) {
            holidays.addAll(publicHolidayService.getHolidays(
                    user.getCountryCode(), user.getSubdivisionCode(), Year.of(year)));
        }
        return holidays;
    }
}

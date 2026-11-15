package com.luxferre.chroniqo.service;

import com.luxferre.chroniqo.i18n.Messages;
import com.luxferre.chroniqo.dto.CustomHolidayDTO;
import com.luxferre.chroniqo.model.CustomHoliday;
import com.luxferre.chroniqo.model.User;
import com.luxferre.chroniqo.repository.CustomHolidayRepository;
import com.luxferre.chroniqo.service.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Manages user-defined days off that behave like public holidays: the daily
 * target is zero, they do not count as vacation days and time booked on them
 * counts as overtime.
 *
 * @author Luxferre86
 * @since 15.11.2026
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomHolidayService {

    static final int MAX_ENTRIES_PER_USER = 200;
    static final int MAX_SPAN_DAYS = 366;
    private static final int MIN_YEAR = 2000;
    private static final int MAX_YEAR = 2100;

    private final CustomHolidayRepository repository;
    private final UserService userService;

    /**
     * Lists the custom days off of the current user, earliest first.
     */
    public List<CustomHolidayDTO> list() {
        return repository.findByUserOrderByStartDateAsc(userService.getCurrentUser()).stream()
                .map(CustomHolidayService::toDto)
                .toList();
    }

    /**
     * Creates a custom day off for the current user.
     *
     * @throws IllegalArgumentException if the data is invalid or the limit is reached
     */
    @Transactional
    public CustomHolidayDTO create(String name, LocalDate startDate, LocalDate endDate, boolean recurring) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > 100) {
            throw new IllegalArgumentException(Messages.get("error.holiday.name"));
        }
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(Messages.get("error.holiday.datesRequired"));
        }
        if (startDate.getYear() < MIN_YEAR || endDate.getYear() > MAX_YEAR) {
            throw new IllegalArgumentException(Messages.get("error.holiday.yearRange", String.valueOf(MIN_YEAR), String.valueOf(MAX_YEAR)));
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(Messages.get("error.holiday.endBeforeStart"));
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) >= MAX_SPAN_DAYS) {
            throw new IllegalArgumentException(Messages.get("error.holiday.maxSpan", String.valueOf(MAX_SPAN_DAYS)));
        }
        if (recurring && startDate.getYear() != endDate.getYear()) {
            throw new IllegalArgumentException(Messages.get("error.holiday.recurringYear"));
        }

        User user = userService.getCurrentUser();
        if (repository.findByUserOrderByStartDateAsc(user).size() >= MAX_ENTRIES_PER_USER) {
            throw new IllegalArgumentException(Messages.get("error.holiday.maxEntries", String.valueOf(MAX_ENTRIES_PER_USER)));
        }

        CustomHoliday holiday = new CustomHoliday();
        holiday.setUser(user);
        holiday.setName(trimmed);
        holiday.setStartDate(startDate);
        holiday.setEndDate(endDate);
        holiday.setRecurring(recurring);
        return toDto(repository.save(holiday));
    }

    /**
     * Deletes a custom day off of the current user.
     *
     * @throws NoSuchElementException if it does not exist or belongs to another user
     */
    @Transactional
    public void delete(String id) {
        User user = userService.getCurrentUser();
        CustomHoliday holiday = repository.findByIdAndUser(id, user)
                .orElseThrow(() -> new NoSuchElementException(Messages.get("error.holiday.notFound")));
        repository.delete(holiday);
    }

    /**
     * Expands the user's custom days off to concrete dates within a range.
     *
     * @param user  the owner
     * @param start first day of the range (inclusive)
     * @param end   last day of the range (inclusive)
     * @return date to name; the earliest entry wins when entries overlap
     */
    public Map<LocalDate, String> getDays(User user, LocalDate start, LocalDate end) {
        Map<LocalDate, String> days = new HashMap<>();
        for (CustomHoliday holiday : repository.findByUserOrderByStartDateAsc(user)) {
            if (holiday.isRecurring()) {
                for (int year = start.getYear(); year <= end.getYear(); year++) {
                    addRange(days, holiday.getName(),
                            inYear(holiday.getStartDate(), year), inYear(holiday.getEndDate(), year), start, end);
                }
            } else {
                addRange(days, holiday.getName(), holiday.getStartDate(), holiday.getEndDate(), start, end);
            }
        }
        return days;
    }

    private static void addRange(Map<LocalDate, String> days, String name,
                                 LocalDate from, LocalDate to, LocalDate start, LocalDate end) {
        LocalDate first = from.isBefore(start) ? start : from;
        LocalDate last = to.isAfter(end) ? end : to;
        for (LocalDate d = first; !d.isAfter(last); d = d.plusDays(1)) {
            days.putIfAbsent(d, name);
        }
    }

    // 29 February falls back to 28 February in non-leap years
    private static LocalDate inYear(LocalDate date, int year) {
        int day = Math.min(date.getDayOfMonth(), YearMonth.of(year, date.getMonth()).lengthOfMonth());
        return LocalDate.of(year, date.getMonth(), day);
    }

    private static CustomHolidayDTO toDto(CustomHoliday h) {
        return new CustomHolidayDTO(h.getId(), h.getName(), h.getStartDate(), h.getEndDate(), h.isRecurring());
    }
}
